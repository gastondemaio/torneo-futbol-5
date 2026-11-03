package com.torneo.futbol5.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** JSON para cargar el resultado de un partido: { "golesLocal": 3, "golesVisitante": 1 } */
public record ResultadoRequest(
        @NotNull(message = "Faltan los goles del primer equipo")
        @Min(value = 0, message = "Los goles no pueden ser negativos")
        @Max(value = 99, message = "Revisá los goles: el máximo es 99")
        Integer golesLocal,

        @NotNull(message = "Faltan los goles del segundo equipo")
        @Min(value = 0, message = "Los goles no pueden ser negativos")
        @Max(value = 99, message = "Revisá los goles: el máximo es 99")
        Integer golesVisitante
) {
}
