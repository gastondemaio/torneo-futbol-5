package com.torneo.futbol5.dto;

import java.util.List;

/**
 * Tabla de posiciones del torneo de la semana.
 *
 * @param filas                posiciones ordenadas
 * @param partidosJugados      cuántos partidos tienen resultado
 * @param partidosTotales      cuántos partidos tiene el fixture
 * @param finalizado           true cuando se jugaron todos los partidos
 * @param campeon              el primero de la tabla si el torneo terminó y no hay igualdad total arriba
 * @param empateEnPrimerPuesto true si terminó y el primer puesto se define por sorteo
 */
public record TablaDTO(
        List<Fila> filas,
        int partidosJugados,
        int partidosTotales,
        boolean finalizado,
        EquipoRef campeon,
        boolean empateEnPrimerPuesto
) {
    /** Un renglón de la tabla. */
    public record Fila(int posicion, EquipoRef equipo,
                       int pj, int pg, int pe, int pp,
                       int gf, int gc, int dg, int pts,
                       boolean empatadoConAnterior) {
    }
}
