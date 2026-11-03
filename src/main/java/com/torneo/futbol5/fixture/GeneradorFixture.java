package com.torneo.futbol5.fixture;

import com.torneo.futbol5.model.DiaJuego;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <h2>Generador del fixture "todos contra todos" (método del círculo)</h2>
 *
 * <p>Arma el calendario de un torneo de liga a una sola rueda: cada equipo juega
 * <b>exactamente una vez</b> contra cada uno de los demás, y en cada fecha cada equipo
 * juega como mucho un partido. Gana el torneo el que suma más puntos.</p>
 *
 * <h3>Cantidades</h3>
 * <pre>
 *  Equipos | Fechas | Partidos por fecha | Partidos totales | Sábado / Domingo
 *     8    |   7    |        4           |        28        |  fechas 1-4 / 5-7
 *     6    |   5    |        3           |        15        |  fechas 1-3 / 4-5
 *     5    |   5    |  2 (1 equipo libre)|        10        |  fechas 1-3 / 4-5
 *     4    |   3    |        2           |         6        |  fechas 1-2 / 3
 * </pre>
 * Fórmulas: partidos = n·(n−1)/2 ; fechas = n−1 si n es par, n si es impar.
 *
 * <h3>Cómo funciona el método del círculo</h3>
 * <pre>
 *  Si la cantidad es impar se agrega un equipo ficticio "LIBRE": el que le toca
 *  contra LIBRE descansa esa fecha.
 *
 *  Se ubican los equipos en dos filas y se enfrentan los que quedan uno arriba del otro.
 *  El primero queda FIJO y los demás GIRAN una posición en cada fecha:
 *
 *    Fecha 1:  E1 E2 E3 E4        Fecha 2:  E1 E8 E2 E3        Fecha 3:  E1 E7 E8 E2
 *              E8 E7 E6 E5                  E7 E6 E5 E4                  E6 E5 E4 E3
 *              (E1-E8, E2-E7,...)
 *
 *  Después de n−1 giros todos los pares se cruzaron una sola vez.
 * </pre>
 *
 * <p>Las fechas se reparten entre los dos días del fin de semana: la primera mitad
 * (redondeando para arriba) el sábado y el resto el domingo.</p>
 *
 * <p>Es una clase genérica y sin dependencias de Spring ni de la base de datos:
 * se usa con entidades {@code Equipo} en la aplicación y con números en los tests.</p>
 */
public class GeneradorFixture {

    /**
     * @param equipos equipos del torneo (al menos 2, sin repetidos), en el orden del sorteo
     * @return las fechas del torneo, numeradas desde 1
     */
    public <T> List<Jornada<T>> generar(List<T> equipos) {
        Objects.requireNonNull(equipos, "equipos");
        if (equipos.size() < 2) {
            throw new IllegalArgumentException("Se necesitan al menos 2 equipos para armar el fixture.");
        }
        if (equipos.stream().distinct().count() != equipos.size()) {
            throw new IllegalArgumentException("Hay equipos repetidos.");
        }

        // Con cantidad impar se suma un lugar vacío (null = "LIBRE")
        List<T> ronda = new ArrayList<>(equipos);
        if (ronda.size() % 2 != 0) {
            ronda.add(null);
        }
        int n = ronda.size();               // siempre par
        int cantidadFechas = n - 1;
        int fechasDelSabado = (cantidadFechas + 1) / 2;   // mitad redondeada hacia arriba

        T fijo = ronda.get(0);
        List<T> giratorios = new ArrayList<>(ronda.subList(1, n));

        List<Jornada<T>> fixture = new ArrayList<>(cantidadFechas);
        for (int f = 0; f < cantidadFechas; f++) {
            // Disposición de esta fecha: el fijo + los que giran
            List<T> posiciones = new ArrayList<>(n);
            posiciones.add(fijo);
            posiciones.addAll(giratorios);

            List<Cruce<T>> partidos = new ArrayList<>();
            T libre = null;
            for (int i = 0; i < n / 2; i++) {
                T a = posiciones.get(i);          // fila de arriba
                T b = posiciones.get(n - 1 - i);  // fila de abajo
                if (a == null || b == null) {
                    libre = (a == null) ? b : a;  // el rival del "LIBRE" descansa
                    continue;
                }
                // Alternar quién figura como local para que no sea siempre el mismo
                boolean invertir = (f + i) % 2 == 1;
                partidos.add(invertir ? new Cruce<>(b, a) : new Cruce<>(a, b));
            }

            DiaJuego dia = (f < fechasDelSabado) ? DiaJuego.SABADO : DiaJuego.DOMINGO;
            fixture.add(new Jornada<>(f + 1, dia, partidos, libre));

            // Girar: el último de los giratorios pasa al principio
            giratorios.add(0, giratorios.remove(giratorios.size() - 1));
        }
        return fixture;
    }

    /** Partidos totales de una liga a una rueda: n·(n−1)/2. */
    public static int partidosTotales(int equipos) {
        return equipos * (equipos - 1) / 2;
    }
}
