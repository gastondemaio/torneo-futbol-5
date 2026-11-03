package com.torneo.futbol5.controller;

import com.torneo.futbol5.service.DemoService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * POST /api/admin/jugadores-demo?cantidad=23  -> inscribe jugadores ficticios (para pruebas)
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final DemoService demoService;

    public AdminController(DemoService demoService) {
        this.demoService = demoService;
    }

    @PostMapping("/jugadores-demo")
    public Map<String, Object> cargarDemo(@RequestParam(defaultValue = "23") int cantidad) {
        int cargados = demoService.cargarJugadores(cantidad);
        return Map.of("cargados", cargados);
    }
}
