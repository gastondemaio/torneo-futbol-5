package com.torneo.futbol5.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * GET /api/version -> { "version": "1.3" }
 *
 * El frontend lo consulta al cargar: si el servidor que está corriendo es de otra
 * versión (por ejemplo, quedó levantada una copia vieja del proyecto), la página
 * muestra un aviso en lugar de fallar en silencio.
 * Cambiar este número junto con VERSION_ESPERADA en frontend/js/app.js.
 */
@RestController
public class VersionController {

    public static final String VERSION = "1.3";

    @GetMapping("/api/version")
    public Map<String, String> version() {
        return Map.of("version", VERSION);
    }
}
