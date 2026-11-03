package com.torneo.futbol5.service;

import com.torneo.futbol5.dto.InscripcionRequest;
import com.torneo.futbol5.dto.InscriptoDTO;
import com.torneo.futbol5.exception.ReglaNegocioException;
import com.torneo.futbol5.model.Inscripcion;
import com.torneo.futbol5.model.Jugador;
import com.torneo.futbol5.model.Torneo;
import com.torneo.futbol5.repository.InscripcionRepository;
import com.torneo.futbol5.repository.JugadorRepository;
import com.torneo.futbol5.sorteo.SorteadorEquipos;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Alta y baja de inscripciones al torneo de la semana.
 *
 * Reglas:
 *  - El jugador se identifica por email: si ya existe, se reutiliza y se actualizan sus datos.
 *  - No se puede inscribir dos veces en el mismo torneo.
 *  - Cupo máximo de 48 jugadores por semana (8 equipos con un suplente cada uno).
 *  - Una vez hecho el sorteo, las inscripciones quedan cerradas.
 */
@Service
public class InscripcionService {

    private final TorneoService torneoService;
    private final JugadorRepository jugadorRepository;
    private final InscripcionRepository inscripcionRepository;

    public InscripcionService(TorneoService torneoService,
                              JugadorRepository jugadorRepository,
                              InscripcionRepository inscripcionRepository) {
        this.torneoService = torneoService;
        this.jugadorRepository = jugadorRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    @Transactional
    public InscriptoDTO inscribir(InscripcionRequest datos) {
        Torneo torneo = torneoService.obtenerOCrearTorneoActual();
        if (!torneo.estaAbierto()) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT,
                    "Los equipos de esta semana ya se sortearon: las inscripciones están cerradas.");
        }

        String email = datos.email().trim().toLowerCase(Locale.ROOT);
        String nombre = capitalizar(datos.nombre());
        String apellido = capitalizar(datos.apellido());

        // Buscar jugador existente por email o crear uno nuevo
        Jugador jugador = jugadorRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> new Jugador(nombre, apellido, email, datos.posicion()));
        jugador.setNombre(nombre);
        jugador.setApellido(apellido);
        jugador.setPosicion(datos.posicion());
        jugador = jugadorRepository.save(jugador);

        if (inscripcionRepository.existsByTorneoAndJugador(torneo, jugador)) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT,
                    "El email " + email + " ya está inscripto en el torneo de esta semana.");
        }

        // Cupo: 8 equipos x (5 titulares + 1 suplente) = 48 jugadores
        if (inscripcionRepository.countByTorneo(torneo) >= SorteadorEquipos.CUPO_MAXIMO) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT,
                    "El cupo de esta semana está completo: " + SorteadorEquipos.CUPO_MAXIMO
                    + " jugadores (" + SorteadorEquipos.MAXIMO_EQUIPOS + " equipos con suplente).");
        }

        Inscripcion inscripcion = inscripcionRepository.save(new Inscripcion(torneo, jugador));
        return InscriptoDTO.desde(inscripcion);
    }

    @Transactional
    public List<InscriptoDTO> listarInscriptos() {
        Torneo torneo = torneoService.obtenerOCrearTorneoActual();
        return inscripcionRepository.findByTorneoOrderByFechaInscripcionAscIdAsc(torneo).stream()
                .map(InscriptoDTO::desde)
                .toList();
    }

    /** Jugadores inscriptos al torneo indicado (los usa el sorteo, dentro de su transacción). */
    @Transactional
    public List<Jugador> jugadoresInscriptos(Torneo torneo) {
        return inscripcionRepository.findByTorneoOrderByFechaInscripcionAscIdAsc(torneo).stream()
                .map(Inscripcion::getJugador)
                .toList();
    }

    @Transactional
    public void darDeBaja(Long inscripcionId) {
        Inscripcion inscripcion = inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> new ReglaNegocioException(HttpStatus.NOT_FOUND,
                        "No existe la inscripción " + inscripcionId + "."));

        Torneo actual = torneoService.obtenerOCrearTorneoActual();
        if (!inscripcion.getTorneo().getId().equals(actual.getId())) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT,
                    "Solo se pueden dar de baja inscripciones del torneo de esta semana.");
        }
        if (!actual.estaAbierto()) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT,
                    "Los equipos ya se sortearon. Anulá el sorteo antes de dar de baja a alguien.");
        }
        inscripcionRepository.delete(inscripcion);
    }

    /**
     * Borra TODAS las inscripciones del torneo de esta semana de una sola vez
     * (botón "Borrar anotados" del organizador). También elimina los jugadores
     * que quedan sin ninguna inscripción, como los de prueba.
     *
     * @return cantidad de inscripciones borradas
     */
    @Transactional
    public int borrarTodas() {
        Torneo actual = torneoService.obtenerOCrearTorneoActual();
        if (!actual.estaAbierto()) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT,
                    "Los equipos ya se sortearon. Anulá el sorteo antes de borrar a los anotados.");
        }
        int borradas = inscripcionRepository.borrarPorTorneo(actual);
        jugadorRepository.borrarSinInscripciones();
        return borradas;
    }

    /** "  juan  pablo " -> "Juan Pablo" ; "DI MARÍA" -> "Di María". */
    private static String capitalizar(String texto) {
        String[] palabras = texto.trim().toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : palabras) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}
