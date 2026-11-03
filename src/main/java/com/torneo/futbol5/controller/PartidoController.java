package com.torneo.futbol5.controller;

import com.torneo.futbol5.dto.PartidoDTO;
import com.torneo.futbol5.dto.ResultadoRequest;
import com.torneo.futbol5.service.PartidoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * PUT    /api/partidos/{id}/resultado  -> cargar o corregir el resultado
 *                                         JSON: { "golesLocal": 3, "golesVisitante": 1 }
 * DELETE /api/partidos/{id}/resultado  -> borrar el resultado (el partido vuelve a "sin jugar")
 */
@RestController
@RequestMapping("/api/partidos")
public class PartidoController {

    private final PartidoService partidoService;

    public PartidoController(PartidoService partidoService) {
        this.partidoService = partidoService;
    }

    @PutMapping("/{id}/resultado")
    public PartidoDTO cargarResultado(@PathVariable Long id, @Valid @RequestBody ResultadoRequest datos) {
        return partidoService.cargarResultado(id, datos);
    }

    @DeleteMapping("/{id}/resultado")
    public PartidoDTO borrarResultado(@PathVariable Long id) {
        return partidoService.borrarResultado(id);
    }
}
