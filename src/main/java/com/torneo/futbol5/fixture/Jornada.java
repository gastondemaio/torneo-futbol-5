package com.torneo.futbol5.fixture;

import com.torneo.futbol5.model.DiaJuego;

import java.util.List;

/**
 * Una "fecha" del torneo: una ronda en la que cada equipo juega como mucho un partido.
 *
 * @param numero   1, 2, 3, ...
 * @param dia      SABADO o DOMINGO
 * @param partidos los cruces de esa fecha
 * @param libre    el equipo que descansa (solo si hay cantidad impar de equipos), o null
 */
public record Jornada<T>(int numero, DiaJuego dia, List<Cruce<T>> partidos, T libre) {

    public Jornada {
        partidos = List.copyOf(partidos);
    }
}
