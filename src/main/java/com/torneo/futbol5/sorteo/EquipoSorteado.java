package com.torneo.futbol5.sorteo;

import com.torneo.futbol5.model.Jugador;

import java.util.List;

/**
 * Resultado del sorteo para UN equipo, todavía sin guardar en la base de datos.
 *
 * @param numero     número de equipo (1, 2, 3, ...)
 * @param titulares  siempre 5 jugadores; si hay arquero, va primero
 * @param suplentes  0 o más jugadores que esperan en el banco
 */
public record EquipoSorteado(int numero, List<Jugador> titulares, List<Jugador> suplentes) {

    public EquipoSorteado {
        titulares = List.copyOf(titulares);
        suplentes = List.copyOf(suplentes);
    }

    public int totalJugadores() {
        return titulares.size() + suplentes.size();
    }
}
