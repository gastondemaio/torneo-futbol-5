package com.torneo.futbol5.controller;

import com.torneo.futbol5.dto.FixtureDTO;
import com.torneo.futbol5.dto.ResultadoSorteoDTO;
import com.torneo.futbol5.dto.TablaDTO;
import com.torneo.futbol5.dto.TorneoDTO;
import com.torneo.futbol5.service.PartidoService;
import com.torneo.futbol5.service.SorteoService;
import com.torneo.futbol5.service.TorneoService;
import org.springframework.web.bind.annotation.*;

/**
 * GET    /api/torneos/actual          -> datos del torneo de la semana
 * GET    /api/torneos/actual/equipos  -> equipos sorteados
 * GET    /api/torneos/actual/fixture  -> partidos del sábado y domingo, agrupados por fecha
 * GET    /api/torneos/actual/tabla    -> tabla de posiciones (3 / 1 / 0 puntos)
 * POST   /api/torneos/actual/sorteo   -> (admin) sortear / re-sortear equipos
 * DELETE /api/torneos/actual/sorteo   -> (admin) anular sorteo y reabrir inscripciones
 */
@RestController
@RequestMapping("/api/torneos/actual")
public class TorneoController {

    private final TorneoService torneoService;
    private final SorteoService sorteoService;
    private final PartidoService partidoService;

    public TorneoController(TorneoService torneoService, SorteoService sorteoService,
                            PartidoService partidoService) {
        this.torneoService = torneoService;
        this.sorteoService = sorteoService;
        this.partidoService = partidoService;
    }

    @GetMapping
    public TorneoDTO torneoActual() {
        return torneoService.resumenTorneoActual();
    }

    @GetMapping("/equipos")
    public ResultadoSorteoDTO equipos() {
        return sorteoService.verEquipos();
    }

    @GetMapping("/fixture")
    public FixtureDTO fixture() {
        return partidoService.fixture();
    }

    @GetMapping("/tabla")
    public TablaDTO tabla() {
        return partidoService.tabla();
    }

    @PostMapping("/sorteo")
    public ResultadoSorteoDTO sortear() {
        return sorteoService.sortear();
    }

    @DeleteMapping("/sorteo")
    public TorneoDTO anularSorteo() {
        return sorteoService.anularSorteo();
    }
}
