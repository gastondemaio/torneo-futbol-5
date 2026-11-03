package com.torneo.futbol5.dto;

import java.util.List;

/** Formato único de error que recibe el frontend: { "mensaje": "...", "detalles": [...] } */
public record ErrorDTO(String mensaje, List<String> detalles) {

    public ErrorDTO(String mensaje) {
        this(mensaje, List.of());
    }
}
