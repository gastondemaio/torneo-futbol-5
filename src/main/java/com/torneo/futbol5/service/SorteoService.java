package com.torneo.futbol5.service;

import com.torneo.futbol5.dto.EquipoDTO;
import com.torneo.futbol5.dto.ResultadoSorteoDTO;
import com.torneo.futbol5.dto.TorneoDTO;
import com.torneo.futbol5.exception.ReglaNegocioException;
import com.torneo.futbol5.model.Equipo;
import com.torneo.futbol5.model.Jugador;
import com.torneo.futbol5.model.RolEquipo;
import com.torneo.futbol5.model.Torneo;
import com.torneo.futbol5.repository.EquipoRepository;
import com.torneo.futbol5.sorteo.EquipoSorteado;
import com.torneo.futbol5.sorteo.SorteadorEquipos;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquesta el sorteo: lee inscriptos de la BD, llama al {@link SorteadorEquipos}
 * (que hace la lógica pura), guarda los equipos resultantes y genera el fixture
 * de partidos todos contra todos para el sábado y el domingo.
 */
@Service
public class SorteoService {

    /** Nombres de los equipos según el color de la pechera. */
    private static final String[] COLORES = {
            "Rojo", "Azul", "Verde", "Amarillo", "Negro", "Blanco",
            "Naranja", "Violeta", "Celeste", "Gris", "Bordó", "Rosa"
    };

    private final TorneoService torneoService;
    private final InscripcionService inscripcionService;
    private final EquipoRepository equipoRepository;
    private final PartidoService partidoService;
    private final SorteadorEquipos sorteador;
    private final Clock reloj;

    public SorteoService(TorneoService torneoService,
                         InscripcionService inscripcionService,
                         EquipoRepository equipoRepository,
                         PartidoService partidoService,
                         SorteadorEquipos sorteador,
                         Clock reloj) {
        this.torneoService = torneoService;
        this.inscripcionService = inscripcionService;
        this.equipoRepository = equipoRepository;
        this.partidoService = partidoService;
        this.sorteador = sorteador;
        this.reloj = reloj;
    }

    /**
     * Sortea (o vuelve a sortear) los equipos del torneo de la semana y arma el fixture.
     * Todo ocurre en una transacción: si algo falla, no queda un sorteo a medias.
     * Volver a sortear borra los partidos y los resultados que se hubieran cargado.
     */
    @Transactional
    public ResultadoSorteoDTO sortear() {
        Torneo torneo = torneoService.obtenerOCrearTorneoActual();
        List<Jugador> inscriptos = inscripcionService.jugadoresInscriptos(torneo);

        if (inscriptos.size() < SorteadorEquipos.MINIMO_JUGADORES) {
            int faltan = SorteadorEquipos.MINIMO_JUGADORES - inscriptos.size();
            throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Hay " + inscriptos.size() + " inscriptos. Faltan " + faltan
                    + " para poder armar al menos " + SorteadorEquipos.MINIMO_EQUIPOS + " equipos.");
        }

        // 1) Si ya había un sorteo, se borran sus partidos y equipos (re-sorteo)
        borrarSorteoAnterior(torneo);

        // 2) Lógica pura del sorteo
        List<EquipoSorteado> sorteados = sorteador.sortear(inscriptos);

        // 3) Persistir equipos e integrantes (EQUIPO + EQUIPO_JUGADOR)
        List<Equipo> equipos = new ArrayList<>();
        for (EquipoSorteado s : sorteados) {
            Equipo equipo = new Equipo(torneo, s.numero(), nombreDeEquipo(s.numero()));
            s.titulares().forEach(j -> equipo.agregarIntegrante(j, RolEquipo.TITULAR));
            s.suplentes().forEach(j -> equipo.agregarIntegrante(j, RolEquipo.SUPLENTE));
            equipos.add(equipo);
        }
        equipoRepository.saveAll(equipos);

        // 4) Fixture todos contra todos: sábado y domingo (tabla PARTIDO)
        partidoService.generarFixture(torneo, equipos);

        // 5) Cerrar inscripciones
        torneo.marcarSorteado(LocalDateTime.now(reloj));

        List<EquipoDTO> dtos = equipos.stream().map(EquipoDTO::desde).toList();
        return new ResultadoSorteoDTO(TorneoDTO.desde(torneo, inscriptos.size()), dtos, advertencias(dtos));
    }

    /** Equipos del último sorteo (lista vacía si todavía no se sorteó). */
    @Transactional
    public ResultadoSorteoDTO verEquipos() {
        Torneo torneo = torneoService.obtenerOCrearTorneoActual();
        List<EquipoDTO> dtos = equipoRepository.buscarConIntegrantes(torneo).stream()
                .map(EquipoDTO::desde).toList();
        return new ResultadoSorteoDTO(torneoService.resumenTorneoActual(), dtos, advertencias(dtos));
    }

    /** Borra partidos y equipos, y vuelve a abrir las inscripciones. */
    @Transactional
    public TorneoDTO anularSorteo() {
        Torneo torneo = torneoService.obtenerOCrearTorneoActual();
        borrarSorteoAnterior(torneo);
        torneo.reabrir();
        return torneoService.resumenTorneoActual();
    }

    private void borrarSorteoAnterior(Torneo torneo) {
        // Primero los partidos (referencian a los equipos), después los equipos
        partidoService.borrarFixture(torneo);
        equipoRepository.deleteAll(equipoRepository.findByTorneo(torneo));
        // flush: ejecuta los DELETE ya, antes de insertar equipos con los mismos números
        equipoRepository.flush();
    }

    private static String nombreDeEquipo(int numero) {
        return numero <= COLORES.length ? "Equipo " + COLORES[numero - 1] : "Equipo " + numero;
    }

    /** Avisos para el organizador (no impiden el sorteo). */
    private static List<String> advertencias(List<EquipoDTO> equipos) {
        List<String> avisos = new ArrayList<>();
        for (EquipoDTO e : equipos) {
            if (!e.tieneArqueroTitular()) {
                avisos.add(e.nombre() + " no tiene arquero inscripto: un jugador de campo tendrá que atajar.");
            }
        }
        return avisos;
    }
}
