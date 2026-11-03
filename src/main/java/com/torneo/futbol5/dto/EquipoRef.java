package com.torneo.futbol5.dto;

import com.torneo.futbol5.model.Equipo;

/** Referencia corta a un equipo (para fixture y tabla). */
public record EquipoRef(Long id, int numero, String nombre) {

    public static EquipoRef desde(Equipo e) {
        return e == null ? null : new EquipoRef(e.getId(), e.getNumero(), e.getNombre());
    }
}
