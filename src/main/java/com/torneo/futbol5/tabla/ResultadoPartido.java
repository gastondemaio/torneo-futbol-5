package com.torneo.futbol5.tabla;

/** Resultado final de un partido ya jugado (entrada de la calculadora de la tabla). */
public record ResultadoPartido(Long localId, Long visitanteId, int golesLocal, int golesVisitante) {

    public ResultadoPartido {
        if (golesLocal < 0 || golesVisitante < 0) {
            throw new IllegalArgumentException("Los goles no pueden ser negativos.");
        }
        if (localId == null || localId.equals(visitanteId)) {
            throw new IllegalArgumentException("Un partido tiene que ser entre dos equipos distintos.");
        }
    }
}
