package com.torneo.futbol5.sorteo;

import com.torneo.futbol5.model.Jugador;
import com.torneo.futbol5.model.Posicion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios del algoritmo de sorteo. No levantan Spring ni base de datos.
 * Ejecutar con:  mvn test
 */
class SorteadorEquiposTest {

    /** Crea n jugadores: 'arq' arqueros, 'def' defensores y el resto delanteros. */
    private static List<Jugador> jugadores(int n, int arq, int def) {
        List<Jugador> lista = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Posicion p = i < arq ? Posicion.ARQUERO : (i < arq + def ? Posicion.DEFENSOR : Posicion.DELANTERO);
            lista.add(new Jugador("J" + i, "Apellido" + i, "j" + i + "@test.com", p));
        }
        return lista;
    }

    @Test
    @DisplayName("23 inscriptos -> 4 equipos de 5 titulares y 3 suplentes en total")
    void inscriptosNoMultiploDeCinco_generaSuplentes() {
        List<EquipoSorteado> equipos = new SorteadorEquipos(new Random(1)).sortear(jugadores(23, 4, 9));

        assertEquals(4, equipos.size());
        equipos.forEach(e -> assertEquals(5, e.titulares().size(), "cada equipo tiene 5 titulares"));
        assertEquals(3, equipos.stream().mapToInt(e -> e.suplentes().size()).sum());
        // los suplentes quedan repartidos: ningún equipo tiene más de 1
        equipos.forEach(e -> assertTrue(e.suplentes().size() <= 1));
    }

    @Test
    @DisplayName("Múltiplo de 5 -> sin suplentes")
    void multiploDeCinco_sinSuplentes() {
        List<EquipoSorteado> equipos = new SorteadorEquipos(new Random(2)).sortear(jugadores(20, 4, 8));
        assertEquals(4, equipos.size());
        equipos.forEach(e -> assertTrue(e.suplentes().isEmpty()));
    }

    @Test
    @DisplayName("Nadie queda afuera ni aparece dos veces")
    void todosLosJugadoresAparecenUnaSolaVez() {
        List<Jugador> inscriptos = jugadores(37, 5, 15);
        List<EquipoSorteado> equipos = new SorteadorEquipos(new Random(3)).sortear(inscriptos);

        Set<Jugador> vistos = new HashSet<>();
        for (EquipoSorteado e : equipos) {
            e.titulares().forEach(j -> assertTrue(vistos.add(j), "repetido: " + j));
            e.suplentes().forEach(j -> assertTrue(vistos.add(j), "repetido: " + j));
        }
        assertEquals(new HashSet<>(inscriptos), vistos);
    }

    @Test
    @DisplayName("Con un arquero por equipo, cada equipo tiene su arquero titular")
    void unArqueroTitularPorEquipo() {
        List<EquipoSorteado> equipos = new SorteadorEquipos(new Random(4)).sortear(jugadores(30, 6, 12));
        for (EquipoSorteado e : equipos) {
            long arqTitulares = e.titulares().stream().filter(j -> j.getPosicion() == Posicion.ARQUERO).count();
            assertEquals(1, arqTitulares);
            assertEquals(Posicion.ARQUERO, e.titulares().get(0).getPosicion(), "el arquero va primero");
        }
    }

    @Test
    @DisplayName("Defensores y delanteros quedan parejos entre equipos (diferencia <= 1)")
    void equilibrioPorPosicion() {
        List<EquipoSorteado> equipos = new SorteadorEquipos(new Random(5)).sortear(jugadores(25, 5, 10));
        int min = Integer.MAX_VALUE, max = 0;
        for (EquipoSorteado e : equipos) {
            int def = (int) e.titulares().stream().filter(j -> j.getPosicion() == Posicion.DEFENSOR).count()
                    + (int) e.suplentes().stream().filter(j -> j.getPosicion() == Posicion.DEFENSOR).count();
            min = Math.min(min, def);
            max = Math.max(max, def);
        }
        assertTrue(max - min <= 1, "diferencia de defensores entre equipos: " + (max - min));
    }

    @Test
    @DisplayName("Misma semilla -> mismo resultado; semillas distintas -> resultados distintos")
    void esAleatorioPeroReproducibleConSemilla() {
        List<Jugador> inscriptos = jugadores(20, 4, 8);
        List<EquipoSorteado> a = new SorteadorEquipos(new Random(99)).sortear(inscriptos);
        List<EquipoSorteado> b = new SorteadorEquipos(new Random(99)).sortear(inscriptos);
        assertEquals(a, b);

        boolean huboDiferencia = false;
        for (int semilla = 0; semilla < 10 && !huboDiferencia; semilla++) {
            huboDiferencia = !a.equals(new SorteadorEquipos(new Random(semilla)).sortear(inscriptos));
        }
        assertTrue(huboDiferencia);
    }

    @Test
    @DisplayName("Menos de 10 inscriptos -> error")
    void pocosJugadores_lanzaExcepcion() {
        SorteadorEquipos sorteador = new SorteadorEquipos(new Random(6));
        assertThrows(IllegalArgumentException.class, () -> sorteador.sortear(jugadores(9, 1, 4)));
    }

    @Test
    @DisplayName("Cupo lleno (48) -> 8 equipos, cada uno con exactamente 1 suplente")
    void cupoLleno_ochoEquiposConUnSuplente() {
        List<EquipoSorteado> equipos = new SorteadorEquipos(new Random(8)).sortear(jugadores(48, 8, 18));
        assertEquals(SorteadorEquipos.MAXIMO_EQUIPOS, equipos.size());
        equipos.forEach(e -> {
            assertEquals(5, e.titulares().size());
            assertEquals(1, e.suplentes().size());
        });
    }

    @Test
    @DisplayName("Más de 40 inscriptos -> nunca más de 8 equipos (46 -> 8 equipos + 6 suplentes)")
    void nuncaMasDeOchoEquipos() {
        List<EquipoSorteado> equipos = new SorteadorEquipos(new Random(9)).sortear(jugadores(46, 6, 18));
        assertEquals(8, equipos.size());
        assertEquals(6, equipos.stream().mapToInt(e -> e.suplentes().size()).sum());
        assertEquals(8, SorteadorEquipos.cantidadDeEquipos(46));
        assertEquals(6, SorteadorEquipos.cantidadDeSuplentes(46));
    }

    @Test
    @DisplayName("Más de 48 inscriptos -> error de cupo")
    void excedeCupo_lanzaExcepcion() {
        SorteadorEquipos sorteador = new SorteadorEquipos(new Random(10));
        assertThrows(IllegalArgumentException.class, () -> sorteador.sortear(jugadores(49, 8, 20)));
    }

    @Test
    @DisplayName("Sin arqueros también funciona (todos de campo)")
    void sinArqueros() {
        List<EquipoSorteado> equipos = new SorteadorEquipos(new Random(7)).sortear(jugadores(14, 0, 7));
        assertEquals(2, equipos.size());
        assertEquals(4, equipos.stream().mapToInt(e -> e.suplentes().size()).sum());
        equipos.forEach(e -> assertEquals(5, e.titulares().size()));
    }
}
