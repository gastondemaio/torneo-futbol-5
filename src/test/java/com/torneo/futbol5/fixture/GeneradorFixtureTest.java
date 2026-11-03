package com.torneo.futbol5.fixture;

import com.torneo.futbol5.model.DiaJuego;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/** Tests del fixture todos contra todos. Ejecutar con: mvn test */
class GeneradorFixtureTest {

    private static List<Integer> equipos(int n) {
        return IntStream.rangeClosed(1, n).boxed().toList();
    }

    /** Verifica las reglas de una liga a una rueda para n equipos. */
    private static void verificarLiga(int n) {
        List<Jornada<Integer>> fixture = new GeneradorFixture().generar(equipos(n));

        int fechasEsperadas = (n % 2 == 0) ? n - 1 : n;
        assertEquals(fechasEsperadas, fixture.size(), "fechas con " + n + " equipos");

        Set<String> cruces = new HashSet<>();
        int partidos = 0;
        for (Jornada<Integer> j : fixture) {
            Set<Integer> juegan = new HashSet<>();
            for (Cruce<Integer> c : j.partidos()) {
                assertNotEquals(c.local(), c.visitante());
                assertTrue(juegan.add(c.local()), "un equipo juega 2 veces en la fecha " + j.numero());
                assertTrue(juegan.add(c.visitante()), "un equipo juega 2 veces en la fecha " + j.numero());
                String par = Math.min(c.local(), c.visitante()) + "-" + Math.max(c.local(), c.visitante());
                assertTrue(cruces.add(par), "cruce repetido: " + par);
                partidos++;
            }
            if (n % 2 == 1) {
                assertNotNull(j.libre(), "con cantidad impar alguien descansa");
                assertFalse(juegan.contains(j.libre()));
                assertEquals(n - 1, juegan.size());
            } else {
                assertNull(j.libre());
                assertEquals(n, juegan.size(), "todos juegan en cada fecha");
            }
        }
        assertEquals(n * (n - 1) / 2, partidos, "todos contra todos una vez");
        assertEquals(GeneradorFixture.partidosTotales(n), partidos);
    }

    @Test
    @DisplayName("2 a 8 equipos: todos contra todos, una vez, y nadie juega dos veces por fecha")
    void todosContraTodos() {
        for (int n = 2; n <= 8; n++) {
            verificarLiga(n);
        }
    }

    @Test
    @DisplayName("8 equipos -> 28 partidos: fechas 1 a 4 el sábado y 5 a 7 el domingo")
    void ochoEquipos_repartoSabadoDomingo() {
        List<Jornada<Integer>> fixture = new GeneradorFixture().generar(equipos(8));
        assertEquals(7, fixture.size());
        for (Jornada<Integer> j : fixture) {
            assertEquals(4, j.partidos().size());
            assertEquals(j.numero() <= 4 ? DiaJuego.SABADO : DiaJuego.DOMINGO, j.dia());
        }
        long sabado = fixture.stream().filter(j -> j.dia() == DiaJuego.SABADO)
                .mapToInt(j -> j.partidos().size()).sum();
        assertEquals(16, sabado);
        assertEquals(12, 28 - sabado);
    }

    @Test
    @DisplayName("5 equipos: cada equipo descansa exactamente una fecha")
    void impar_cadaUnoDescansaUnaVez() {
        List<Jornada<Integer>> fixture = new GeneradorFixture().generar(equipos(5));
        Set<Integer> libres = new HashSet<>();
        fixture.forEach(j -> assertTrue(libres.add(j.libre())));
        assertEquals(Set.of(1, 2, 3, 4, 5), libres);
    }

    @Test
    @DisplayName("Menos de 2 equipos -> error")
    void pocosEquipos() {
        assertThrows(IllegalArgumentException.class, () -> new GeneradorFixture().generar(List.of(1)));
    }
}
