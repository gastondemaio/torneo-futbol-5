package com.torneo.futbol5.dto;

import java.util.List;

/** Respuesta de "sortear" y de "ver equipos". */
public record ResultadoSorteoDTO(
        TorneoDTO torneo,
        List<EquipoDTO> equipos,
        List<String> advertencias
) {
}
