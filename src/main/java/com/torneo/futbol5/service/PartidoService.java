package com.torneo.futbol5.service;

import com.torneo.futbol5.dto.EquipoRef;
import com.torneo.futbol5.dto.FixtureDTO;
import com.torneo.futbol5.dto.JornadaDTO;
import com.torneo.futbol5.dto.PartidoDTO;
import com.torneo.futbol5.dto.ResultadoRequest;
import com.torneo.futbol5.dto.TablaDTO;
import com.torneo.futbol5.exception.ReglaNegocioException;
import com.torneo.futbol5.fixture.Cruce;
import com.torneo.futbol5.fixture.GeneradorFixture;
import com.torneo.futbol5.fixture.Jornada;
import com.torneo.futbol5.model.DiaJuego;
import com.torneo.futbol5.model.Equipo;
import com.torneo.futbol5.model.Partido;
import com.torneo.futbol5.model.Torneo;
import com.torneo.futbol5.repository.EquipoRepository;
import com.torneo.futbol5.repository.PartidoRepository;
import com.torneo.futbol5.tabla.CalculadoraTabla;
import com.torneo.futbol5.tabla.FilaTabla;
import com.torneo.futbol5.tabla.ResultadoPartido;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fixture, resultados y tabla de posiciones del torneo de la semana.
 *
 * Reglas:
 *  - Al sortear se genera el fixture todos contra todos (una rueda), repartido
 *    entre el sábado y el domingo de la semana.
 *  - El organizador carga los goles de cada partido; puede corregirlos o borrarlos.
 *  - La tabla se calcula siempre a partir de los resultados: 3 puntos por victoria,
 *    1 por empate, 0 por derrota. Desempate: diferencia de gol, goles a favor, sorteo.
 *  - Gana el torneo el que suma más puntos cuando se jugaron todos los partidos.
 */
@Service
public class PartidoService {

    private final TorneoService torneoService;
    private final PartidoRepository partidoRepository;
    private final EquipoRepository equipoRepository;
    private final GeneradorFixture generadorFixture = new GeneradorFixture();
    private final CalculadoraTabla calculadoraTabla = new CalculadoraTabla();

    public PartidoService(TorneoService torneoService,
                          PartidoRepository partidoRepository,
                          EquipoRepository equipoRepository) {
        this.torneoService = torneoService;
        this.partidoRepository = partidoRepository;
        this.equipoRepository = equipoRepository;
    }

    // ------------------------------------------------------------------
    //  Fixture (lo usa SorteoService dentro de su transacción)
    // ------------------------------------------------------------------

    /** Crea y guarda los partidos para los equipos recién sorteados. */
    @Transactional
    public List<Partido> generarFixture(Torneo torneo, List<Equipo> equipos) {
        List<Equipo> porNumero = ordenarPorNumero(equipos);
        List<Jornada<Equipo>> jornadas = generadorFixture.generar(porNumero);

        List<Partido> partidos = new ArrayList<>();
        for (Jornada<Equipo> jornada : jornadas) {
            LocalDate dia = jornada.dia() == DiaJuego.SABADO ? torneo.getFechaSabado() : torneo.getFechaDomingo();
            int orden = 1;
            for (Cruce<Equipo> cruce : jornada.partidos()) {
                partidos.add(new Partido(torneo, jornada.numero(), jornada.dia(), dia, orden++,
                        cruce.local(), cruce.visitante()));
            }
        }
        return partidoRepository.saveAll(partidos);
    }

    /** Borra el fixture (y sus resultados) del torneo. */
    @Transactional
    public void borrarFixture(Torneo torneo) {
        partidoRepository.borrarPorTorneo(torneo);
    }

    // ------------------------------------------------------------------
    //  Consultas
    // ------------------------------------------------------------------

    /** Fixture del torneo de esta semana agrupado por fecha (vacío si no se sorteó). */
    @Transactional
    public FixtureDTO fixture() {
        Torneo torneo = torneoService.obtenerOCrearTorneoActual();
        List<Partido> partidos = partidoRepository.buscarFixture(torneo);
        List<Equipo> equipos = ordenarPorNumero(equipoRepository.findByTorneo(torneo));

        // Agrupar por número de fecha (TreeMap: queda ordenado 1, 2, 3, ...)
        Map<Integer, List<Partido>> porJornada = new TreeMap<>();
        for (Partido p : partidos) {
            porJornada.computeIfAbsent(p.getJornada(), k -> new ArrayList<>()).add(p);
        }

        List<JornadaDTO> jornadas = new ArrayList<>();
        for (Map.Entry<Integer, List<Partido>> e : porJornada.entrySet()) {
            List<Partido> dePartidos = e.getValue();
            Partido primero = dePartidos.get(0);
            jornadas.add(new JornadaDTO(e.getKey(), primero.getDia(), primero.getFecha(),
                    dePartidos.stream().map(PartidoDTO::desde).toList(),
                    EquipoRef.desde(equipoLibre(equipos, dePartidos))));
        }

        int jugados = (int) partidos.stream().filter(Partido::estaJugado).count();
        return new FixtureDTO(torneo.getFechaSabado(), torneo.getFechaDomingo(),
                jornadas, jugados, partidos.size());
    }

    /** Tabla de posiciones calculada con los resultados cargados hasta ahora. */
    @Transactional
    public TablaDTO tabla() {
        Torneo torneo = torneoService.obtenerOCrearTorneoActual();
        List<Equipo> equipos = ordenarPorNumero(equipoRepository.findByTorneo(torneo));
        List<Partido> partidos = partidoRepository.buscarFixture(torneo);

        List<ResultadoPartido> resultados = partidos.stream()
                .filter(Partido::estaJugado)
                .map(p -> new ResultadoPartido(p.getEquipoLocal().getId(), p.getEquipoVisitante().getId(),
                        p.getGolesLocal(), p.getGolesVisitante()))
                .toList();

        List<FilaTabla> filas = calculadoraTabla.calcular(
                equipos.stream().map(Equipo::getId).toList(), resultados);

        Map<Long, Equipo> porId = equipos.stream()
                .collect(Collectors.toMap(Equipo::getId, Function.identity()));
        List<TablaDTO.Fila> filasDto = filas.stream()
                .map(f -> new TablaDTO.Fila(f.posicion(), EquipoRef.desde(porId.get(f.equipoId())),
                        f.pj(), f.pg(), f.pe(), f.pp(), f.gf(), f.gc(), f.dg(), f.pts(),
                        f.empatadoConAnterior()))
                .toList();

        int jugados = resultados.size();
        int totales = partidos.size();
        boolean finalizado = totales > 0 && jugados == totales;
        boolean empateArriba = finalizado && filasDto.size() > 1 && filasDto.get(1).empatadoConAnterior();
        EquipoRef campeon = (finalizado && !empateArriba) ? filasDto.get(0).equipo() : null;

        return new TablaDTO(filasDto, jugados, totales, finalizado, campeon, empateArriba);
    }

    // ------------------------------------------------------------------
    //  Resultados
    // ------------------------------------------------------------------

    @Transactional
    public PartidoDTO cargarResultado(Long partidoId, ResultadoRequest datos) {
        Partido partido = buscarDelTorneoActual(partidoId);
        partido.registrarResultado(datos.golesLocal(), datos.golesVisitante());
        return PartidoDTO.desde(partido);
    }

    @Transactional
    public PartidoDTO borrarResultado(Long partidoId) {
        Partido partido = buscarDelTorneoActual(partidoId);
        partido.borrarResultado();
        return PartidoDTO.desde(partido);
    }

    // ------------------------------------------------------------------
    //  Auxiliares
    // ------------------------------------------------------------------

    private Partido buscarDelTorneoActual(Long partidoId) {
        Partido partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new ReglaNegocioException(HttpStatus.NOT_FOUND,
                        "No existe el partido " + partidoId + "."));
        Torneo actual = torneoService.obtenerOCrearTorneoActual();
        if (!partido.getTorneo().getId().equals(actual.getId())) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT,
                    "Solo se pueden cargar resultados del torneo de esta semana.");
        }
        return partido;
    }

    /** Con cantidad impar de equipos, el que no aparece en ningún partido de la fecha descansa. */
    private static Equipo equipoLibre(List<Equipo> equipos, List<Partido> partidosDeLaFecha) {
        Set<Long> juegan = new HashSet<>();
        for (Partido p : partidosDeLaFecha) {
            juegan.add(p.getEquipoLocal().getId());
            juegan.add(p.getEquipoVisitante().getId());
        }
        List<Equipo> libres = equipos.stream().filter(e -> !juegan.contains(e.getId())).toList();
        return libres.size() == 1 ? libres.get(0) : null;
    }

    private static List<Equipo> ordenarPorNumero(List<Equipo> equipos) {
        return equipos.stream().sorted(Comparator.comparingInt(Equipo::getNumero)).toList();
    }
}
