package com.torneo.futbol5.dto;

import com.torneo.futbol5.model.Equipo;
import com.torneo.futbol5.model.EquipoJugador;
import com.torneo.futbol5.model.Jugador;
import com.torneo.futbol5.model.Posicion;
import com.torneo.futbol5.model.RolEquipo;

import java.util.List;

/** Un equipo ya sorteado, listo para mostrar. */
public record EquipoDTO(
        Long id,
        int numero,
        String nombre,
        List<Integrante> titulares,
        List<Integrante> suplentes
) {
    /** Jugador dentro de un equipo. */
    public record Integrante(Long jugadorId, String nombre, String apellido, Posicion posicion) {
        static Integrante desde(EquipoJugador ej) {
            Jugador j = ej.getJugador();
            return new Integrante(j.getId(), j.getNombre(), j.getApellido(), j.getPosicion());
        }
    }

    public static EquipoDTO desde(Equipo e) {
        return new EquipoDTO(e.getId(), e.getNumero(), e.getNombre(),
                e.getIntegrantes(RolEquipo.TITULAR).stream().map(Integrante::desde).toList(),
                e.getIntegrantes(RolEquipo.SUPLENTE).stream().map(Integrante::desde).toList());
    }

    public boolean tieneArqueroTitular() {
        return titulares.stream().anyMatch(i -> i.posicion() == Posicion.ARQUERO);
    }
}
