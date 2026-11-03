package com.torneo.futbol5.dto;

import com.torneo.futbol5.model.Posicion;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** JSON que envía el formulario de inscripción. */
public record InscripcionRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre admite hasta 60 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 60, message = "El apellido admite hasta 60 caracteres")
        String apellido,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 120, message = "El email admite hasta 120 caracteres")
        String email,

        @NotNull(message = "Elegí una posición: ARQUERO, DEFENSOR o DELANTERO")
        Posicion posicion
) {
}
