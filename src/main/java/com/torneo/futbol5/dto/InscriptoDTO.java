package com.torneo.futbol5.dto;

import com.torneo.futbol5.model.Inscripcion;
import com.torneo.futbol5.model.Jugador;
import com.torneo.futbol5.model.Posicion;

import java.time.LocalDateTime;

/** Un renglón del listado de inscriptos. */
public record InscriptoDTO(
        Long inscripcionId,
        Long jugadorId,
        String nombre,
        String apellido,
        String email,
        Posicion posicion,
        LocalDateTime fechaInscripcion
) {
    public static InscriptoDTO desde(Inscripcion inscripcion) {
        Jugador j = inscripcion.getJugador();
        return new InscriptoDTO(inscripcion.getId(), j.getId(), j.getNombre(), j.getApellido(),
                j.getEmail(), j.getPosicion(), inscripcion.getFechaInscripcion());
    }
}
