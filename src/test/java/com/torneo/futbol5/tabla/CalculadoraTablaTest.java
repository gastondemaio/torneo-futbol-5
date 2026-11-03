package com.torneo.futbol5.tabla;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tests de la tabla de posiciones. Ejecutar con: mvn test */
class CalculadoraTablaTest {

    private static final List<Long> EQUIPOS = List.of(1L, 2L, 3L, 4L);

    private static ResultadoPartido r(long local, long visitante, int gl, int gv) {
        return new ResultadoPartido(local, visitante, gl, gv);
    }

    private static FilaTabla fila(List<FilaTabla> tabla, long equipoId) {
        return tabla.stream().filter(f -> f.equipoId() == equipoId).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Victoria 3, empate 1, derrota 0 y estadísticas PJ PG PE PP GF GC DG")
    void puntajeYEstadisticas() {
        List<FilaTabla> tabla = new CalculadoraTabla().calcular(EQUIPOS, List.of(
                r(1, 2, 3, 1),   // gana 1
                r(3, 4, 2, 2),   // empate
                r(2, 3, 0, 1))); // gana 3

        FilaTabla e1 = fila(tabla, 1);
        assertEquals(3, e1.pts());
        assertEquals(1, e1.pj());
        assertEquals(2, e1.dg());

        FilaTabla e3 = fila(tabla, 3);
        assertEquals(4, e3.pts());   // empate (1) + victoria (3)
        assertEquals(2, e3.pj());
        assertEquals(1, e3.pg());
        assertEquals(1, e3.pe());
        assertEquals(3, e3.gf());
        assertEquals(2, e3.gc());

        FilaTabla e2 = fila(tabla, 2);
        assertEquals(0, e2.pts());
        assertEquals(2, e2.pp());

        assertEquals(3L, tabla.get(0).equipoId(), "el de más puntos va primero");
        assertEquals(1, tabla.get(0).posicion());
    }

    @Test
    @DisplayName("Igualdad en puntos: desempata la diferencia de gol")
    void desempatePorDiferenciaDeGol() {
        List<FilaTabla> tabla = new CalculadoraTabla().calcular(EQUIPOS, List.of(
                r(1, 3, 1, 0),   // 1: 3 pts, DG +1
                r(2, 4, 5, 0))); // 2: 3 pts, DG +5
        assertEquals(2L, tabla.get(0).equipoId());
        assertEquals(1L, tabla.get(1).equipoId());
        assertFalse(tabla.get(1).empatadoConAnterior());
    }

    @Test
    @DisplayName("Igualdad en puntos y diferencia: desempatan los goles a favor")
    void desempatePorGolesAFavor() {
        List<FilaTabla> tabla = new CalculadoraTabla().calcular(EQUIPOS, List.of(
                r(1, 3, 1, 0),   // 1: 3 pts, DG +1, GF 1
                r(2, 4, 4, 3))); // 2: 3 pts, DG +1, GF 4
        assertEquals(2L, tabla.get(0).equipoId());
        assertEquals(1L, tabla.get(1).equipoId());
    }

    @Test
    @DisplayName("Igualdad total: se marca para sorteo")
    void igualdadTotal_marcaSorteo() {
        List<FilaTabla> tabla = new CalculadoraTabla().calcular(EQUIPOS, List.of(
                r(1, 3, 2, 0),
                r(2, 4, 2, 0)));
        assertEquals(1L, tabla.get(0).equipoId(), "mientras no se sortea, orden por número");
        assertTrue(tabla.get(1).empatadoConAnterior());
        assertFalse(tabla.get(0).empatadoConAnterior());
    }

    @Test
    @DisplayName("Sin partidos jugados: todos en cero, en orden de número")
    void sinPartidos() {
        List<FilaTabla> tabla = new CalculadoraTabla().calcular(EQUIPOS, List.of());
        assertEquals(4, tabla.size());
        assertEquals(1L, tabla.get(0).equipoId());
        tabla.forEach(f -> assertEquals(0, f.pts()));
    }
}
