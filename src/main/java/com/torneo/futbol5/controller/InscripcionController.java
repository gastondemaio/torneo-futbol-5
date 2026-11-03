package com.torneo.futbol5.controller;

import com.torneo.futbol5.dto.InscripcionRequest;
import com.torneo.futbol5.dto.InscriptoDTO;
import com.torneo.futbol5.service.InscripcionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * GET    /api/inscripciones        -> listado de inscriptos de la semana
 * POST   /api/inscripciones        -> inscribir jugador (JSON InscripcionRequest)
 * DELETE /api/inscripciones/{id}   -> dar de baja una inscripción
 * DELETE /api/inscripciones        -> borrar TODOS los anotados de la semana
 */
@RestController
@RequestMapping("/api/inscripciones")
public class InscripcionController {

    private final InscripcionService inscripcionService;

    public InscripcionController(InscripcionService inscripcionService) {
        this.inscripcionService = inscripcionService;
    }

    @GetMapping
    public List<InscriptoDTO> listar() {
        return inscripcionService.listarInscriptos();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InscriptoDTO inscribir(@Valid @RequestBody InscripcionRequest datos) {
        return inscripcionService.inscribir(datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void darDeBaja(@PathVariable Long id) {
        inscripcionService.darDeBaja(id);
    }

    /** Borra todos los anotados de la semana. Responde { "borrados": n }. */
    @DeleteMapping
    public Map<String, Integer> borrarTodas() {
        return Map.of("borrados", inscripcionService.borrarTodas());
    }
}
