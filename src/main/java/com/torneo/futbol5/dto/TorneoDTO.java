package com.torneo.futbol5.dto;

import com.torneo.futbol5.model.EstadoTorneo;
import com.torneo.futbol5.model.Torneo;
import com.torneo.futbol5.sorteo.SorteadorEquipos;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Resumen del torneo de la semana (lo usa el encabezado del frontend). */
public record TorneoDTO(
        Long id,
        String nombre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        LocalDate fechaSabado,
        LocalDate fechaDomingo,
        EstadoTorneo estado,
        LocalDateTime fechaSorteo,
        long cantidadInscriptos,
        int equiposPosibles,
        int suplentesPosibles,
        int minimoJugadores,
        int maximoEquipos,
        int cupoMaximo
) {
    public static TorneoDTO desde(Torneo t, long inscriptos) {
        int n = (int) inscriptos;
        return new TorneoDTO(t.getId(), t.getNombre(), t.getFechaInicio(), t.getFechaFin(),
                t.getFechaSabado(), t.getFechaDomingo(),
                t.getEstado(), t.getFechaSorteo(), inscriptos,
                SorteadorEquipos.cantidadDeEquipos(n),
                SorteadorEquipos.cantidadDeSuplentes(n),
                SorteadorEquipos.MINIMO_JUGADORES,
                SorteadorEquipos.MAXIMO_EQUIPOS,
                SorteadorEquipos.CUPO_MAXIMO);
    }
}
