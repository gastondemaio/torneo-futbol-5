package com.torneo.futbol5.dto;

import java.time.LocalDate;
import java.util.List;

/** Fixture completo del torneo de la semana (vacío si todavía no se sorteó). */
public record FixtureDTO(
        LocalDate fechaSabado,
        LocalDate fechaDomingo,
        List<JornadaDTO> jornadas,
        int partidosJugados,
        int partidosTotales
) {
}
