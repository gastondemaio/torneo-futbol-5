package com.torneo.futbol5.dto;

import com.torneo.futbol5.model.DiaJuego;

import java.time.LocalDate;
import java.util.List;

/** Una fecha del fixture: sus partidos y, si la cantidad de equipos es impar, quién descansa. */
public record JornadaDTO(int numero, DiaJuego dia, LocalDate fecha, List<PartidoDTO> partidos, EquipoRef libre) {
}
