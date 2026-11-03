package com.torneo.futbol5.dto;

import com.torneo.futbol5.model.DiaJuego;
import com.torneo.futbol5.model.Partido;

import java.time.LocalDate;

/** Un partido del fixture, con su resultado si ya se jugó. */
public record PartidoDTO(
        Long id,
        int jornada,
        DiaJuego dia,
        LocalDate fecha,
        int orden,
        EquipoRef local,
        EquipoRef visitante,
        Integer golesLocal,
        Integer golesVisitante,
        boolean jugado
) {
    public static PartidoDTO desde(Partido p) {
        return new PartidoDTO(p.getId(), p.getJornada(), p.getDia(), p.getFecha(), p.getOrden(),
                EquipoRef.desde(p.getEquipoLocal()), EquipoRef.desde(p.getEquipoVisitante()),
                p.getGolesLocal(), p.getGolesVisitante(), p.estaJugado());
    }
}
