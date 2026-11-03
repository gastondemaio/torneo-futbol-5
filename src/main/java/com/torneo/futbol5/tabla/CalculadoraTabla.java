package com.torneo.futbol5.tabla;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>Tabla de posiciones del torneo</h2>
 *
 * <p>Gana el torneo el equipo que suma más puntos al terminar todos los partidos.</p>
 *
 * <h3>Puntaje</h3>
 * <ul>
 *   <li>Partido ganado: {@value #PUNTOS_VICTORIA} puntos</li>
 *   <li>Partido empatado: {@value #PUNTOS_EMPATE} punto</li>
 *   <li>Partido perdido: {@value #PUNTOS_DERROTA} puntos</li>
 * </ul>
 *
 * <h3>Orden y desempate</h3>
 * Mismo criterio que la tabla anual de la Liga Profesional (AFA), sin la tabla de
 * fair play porque el torneo no registra tarjetas:
 * <ol>
 *   <li>Más puntos.</li>
 *   <li>Mejor diferencia de gol (goles a favor − goles en contra).</li>
 *   <li>Más goles a favor.</li>
 *   <li>Si sigue la igualdad: sorteo. La tabla marca esas filas con
 *       {@code empatadoConAnterior = true} y, mientras tanto, las ordena por número de equipo.</li>
 * </ol>
 *
 * <p>La tabla NO se guarda en la base de datos: se calcula cada vez a partir de los
 * resultados de los partidos. Así nunca puede quedar desactualizada respecto de ellos
 * (es un dato derivado).</p>
 */
public class CalculadoraTabla {

    public static final int PUNTOS_VICTORIA = 3;
    public static final int PUNTOS_EMPATE = 1;
    public static final int PUNTOS_DERROTA = 0;

    /** Orden de la tabla: puntos, diferencia de gol y goles a favor, de mayor a menor. */
    private static final Comparator<Acumulado> CRITERIO = Comparator
            .comparingInt(Acumulado::pts).reversed()
            .thenComparing(Comparator.comparingInt(Acumulado::dg).reversed())
            .thenComparing(Comparator.comparingInt((Acumulado a) -> a.gf).reversed());

    /**
     * @param equipoIds  todos los equipos del torneo, en orden de número (los que todavía
     *                   no jugaron también aparecen, con 0 puntos)
     * @param resultados partidos ya jugados
     * @return la tabla ordenada, posición 1 primero
     */
    public List<FilaTabla> calcular(List<Long> equipoIds, List<ResultadoPartido> resultados) {
        // 1) Una fila en cero por equipo (LinkedHashMap conserva el orden por número)
        Map<Long, Acumulado> filas = new LinkedHashMap<>();
        for (Long id : equipoIds) {
            filas.put(id, new Acumulado(id));
        }

        // 2) Sumar cada resultado a los dos equipos que jugaron
        for (ResultadoPartido r : resultados) {
            Acumulado local = filas.get(r.localId());
            Acumulado visitante = filas.get(r.visitanteId());
            if (local == null || visitante == null) {
                throw new IllegalArgumentException("El resultado incluye un equipo que no es del torneo.");
            }
            local.sumar(r.golesLocal(), r.golesVisitante());
            visitante.sumar(r.golesVisitante(), r.golesLocal());
        }

        // 3) Ordenar. sort() es estable: los empatados en todo quedan por número de equipo
        List<Acumulado> orden = new ArrayList<>(filas.values());
        orden.sort(CRITERIO);

        // 4) Numerar y marcar los empates que se definirían por sorteo
        List<FilaTabla> tabla = new ArrayList<>(orden.size());
        for (int i = 0; i < orden.size(); i++) {
            Acumulado a = orden.get(i);
            boolean empatado = i > 0 && CRITERIO.compare(orden.get(i - 1), a) == 0;
            tabla.add(new FilaTabla(i + 1, a.equipoId, a.pj(), a.pg, a.pe, a.pp,
                    a.gf, a.gc, a.dg(), a.pts(), empatado));
        }
        return tabla;
    }

    /** Contadores de un equipo mientras se recorren los resultados. */
    private static final class Acumulado {
        final Long equipoId;
        int pg, pe, pp, gf, gc;

        Acumulado(Long equipoId) {
            this.equipoId = equipoId;
        }

        void sumar(int golesPropios, int golesRival) {
            gf += golesPropios;
            gc += golesRival;
            if (golesPropios > golesRival) pg++;
            else if (golesPropios == golesRival) pe++;
            else pp++;
        }

        int pj()  { return pg + pe + pp; }
        int dg()  { return gf - gc; }
        int pts() { return pg * PUNTOS_VICTORIA + pe * PUNTOS_EMPATE + pp * PUNTOS_DERROTA; }
    }
}
