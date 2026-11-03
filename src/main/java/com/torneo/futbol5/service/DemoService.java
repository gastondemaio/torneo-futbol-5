package com.torneo.futbol5.service;

import com.torneo.futbol5.dto.InscripcionRequest;
import com.torneo.futbol5.exception.ReglaNegocioException;
import com.torneo.futbol5.model.Posicion;
import com.torneo.futbol5.sorteo.SorteadorEquipos;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

/**
 * Carga jugadores ficticios para probar o presentar la app sin tipear 20 inscripciones.
 * Reparto aproximado: 15% arqueros, 40% defensores, 45% delanteros.
 */
@Service
public class DemoService {

    private static final String[] NOMBRES = {
            "Lucas", "Martín", "Diego", "Facundo", "Nicolás", "Matías", "Agustín", "Santiago",
            "Joaquín", "Tomás", "Franco", "Gonzalo", "Emiliano", "Lautaro", "Julián", "Valentín",
            "Camila", "Sofía", "Florencia", "Lucía", "Micaela", "Rocío", "Julieta", "Ramiro"
    };
    private static final String[] APELLIDOS = {
            "González", "Rodríguez", "Gómez", "Fernández", "López", "Díaz", "Martínez", "Pérez",
            "Romero", "Sosa", "Benítez", "Acosta", "Medina", "Herrera", "Ruiz", "Ojeda",
            "Ramírez", "Torres", "Vera", "Ledesma", "Cabrera", "Aguirre", "Molina", "Ríos"
    };

    private final InscripcionService inscripcionService;
    private final TorneoService torneoService;
    private final Random random = new Random();

    public DemoService(InscripcionService inscripcionService, TorneoService torneoService) {
        this.inscripcionService = inscripcionService;
        this.torneoService = torneoService;
    }

    /**
     * Inscribe jugadores ficticios sin pasarse del cupo (48).
     * Si se piden más de los lugares libres, carga solo los que entran.
     *
     * @return cuántos jugadores se inscribieron efectivamente
     */
    @Transactional
    public int cargarJugadores(int cantidad) {
        int cupo = SorteadorEquipos.CUPO_MAXIMO;
        if (cantidad < 1 || cantidad > cupo) {
            throw new ReglaNegocioException(HttpStatus.BAD_REQUEST,
                    "La cantidad debe estar entre 1 y " + cupo + ".");
        }
        long anotados = torneoService.resumenTorneoActual().cantidadInscriptos();
        int lugaresLibres = (int) Math.max(0, cupo - anotados);
        if (lugaresLibres == 0) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT,
                    "El cupo de " + cupo + " jugadores ya está completo.");
        }
        int aCargar = Math.min(cantidad, lugaresLibres);

        int sello = random.nextInt(100_000);
        for (int i = 0; i < aCargar; i++) {
            String nombre = NOMBRES[random.nextInt(NOMBRES.length)];
            String apellido = APELLIDOS[random.nextInt(APELLIDOS.length)];
            String email = sinTildes(nombre + "." + apellido).toLowerCase() + "." + sello + "." + i + "@demo.com";
            inscripcionService.inscribir(new InscripcionRequest(nombre, apellido, email, posicionAlAzar()));
        }
        return aCargar;
    }

    private Posicion posicionAlAzar() {
        int r = random.nextInt(100);
        if (r < 15) return Posicion.ARQUERO;
        if (r < 55) return Posicion.DEFENSOR;
        return Posicion.DELANTERO;
    }

    private static String sinTildes(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
