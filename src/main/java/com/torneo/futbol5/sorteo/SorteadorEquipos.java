package com.torneo.futbol5.sorteo;

import com.torneo.futbol5.model.Jugador;
import com.torneo.futbol5.model.Posicion;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * <h2>Algoritmo de sorteo de equipos de fútbol 5</h2>
 *
 * <p>Recibe la lista de jugadores inscriptos y los reparte <b>al azar</b> en equipos
 * de 5 titulares, tratando de que los equipos queden <b>equilibrados por posición</b>
 * (un arquero por equipo cuando se puede, y una mezcla pareja de defensores y delanteros).</p>
 *
 * <h3>Reglas de negocio que implementa</h3>
 * <ol>
 *   <li>Cada equipo tiene exactamente {@value #JUGADORES_POR_EQUIPO} titulares.</li>
 *   <li>Se necesitan al menos {@value #MINIMO_EQUIPOS} equipos para jugar un torneo,
 *       o sea un mínimo de {@value #MINIMO_JUGADORES} inscriptos.</li>
 *   <li>Cantidad de equipos = inscriptos / 5 (división entera), con un
 *       <b>máximo de {@value #MAXIMO_EQUIPOS} equipos</b> por sorteo.</li>
 *   <li><b>Suplentes:</b> los inscriptos que no completan un equipo quedan como
 *       suplentes, repartidos parejo entre los equipos (ningún equipo tiene más
 *       de un suplente de diferencia con otro). Nadie queda afuera.
 *       <br>Ejemplo: 23 inscriptos → 4 equipos de 5 titulares + 3 suplentes
 *       (tres equipos tienen 1 suplente y uno no tiene).</li>
 *   <li><b>Cupo:</b> {@value #CUPO_MAXIMO} inscriptos, es decir 8 equipos con
 *       {@value #MAXIMO_SUPLENTES_POR_EQUIPO} suplente cada uno (8 × 6). Con el cupo
 *       lleno ningún equipo tiene más de un suplente.
 *       Ejemplo: 46 inscriptos → 8 equipos + 6 suplentes.</li>
 *   <li>Si hay menos arqueros que equipos, algún equipo queda sin arquero de oficio
 *       (lo informa el servicio como advertencia). Si hay más arqueros que equipos,
 *       los arqueros que sobran se reparten como un jugador más.</li>
 * </ol>
 *
 * <h3>Cómo funciona (4 pasos)</h3>
 * <pre>
 *  PASO 1  Separar por posición       ARQ: [a1 a2 a3]  DEF: [d1..d8]  DEL: [x1..x12]
 *          y mezclar cada grupo        (Collections.shuffle con un Random)
 *
 *  PASO 2  Repartir "en ronda"         cursor →  E1  E2  E3  E4  E1  E2 ...
 *          primero arqueros, después   a1→E3, a2→E4, a3→E1, d1→E2, d2→E3 ...
 *          defensores y delanteros.    El cursor NO vuelve a cero entre grupos,
 *          El cursor arranca en un     así cada equipo recibe casi la misma
 *          equipo al azar.             cantidad total (diferencia máxima: 1).
 *
 *  PASO 3  En cada plantel elegir      1 arquero (si tiene) + alternar DEF/DEL
 *          los 5 titulares             hasta completar 5. El resto → SUPLENTES.
 *
 *  PASO 4  Devolver la lista de EquipoSorteado (todavía no se guarda nada).
 * </pre>
 *
 * <p><b>Por qué funciona el equilibrio:</b> repartir una lista "en ronda" garantiza que
 * cada equipo reciba la misma cantidad de jugadores de esa lista, con diferencia máxima
 * de 1. Como los arqueros se reparten primero, cada equipo recibe como mucho un arquero
 * antes de que alguno reciba el segundo. Y como cada grupo se mezcló antes, <i>quién</i>
 * va a cada equipo es aleatorio.</p>
 *
 * <p>La clase no depende de Spring ni de la base de datos: recibe objetos y devuelve
 * objetos. Por eso se puede probar con tests unitarios simples usando un {@link Random}
 * con semilla fija (resultado reproducible).</p>
 */
public class SorteadorEquipos {

    /** Titulares por equipo (fútbol 5). */
    public static final int JUGADORES_POR_EQUIPO = 5;

    /** Mínimo de equipos para que tenga sentido un torneo. */
    public static final int MINIMO_EQUIPOS = 2;

    /** Mínimo de inscriptos para poder sortear: 2 equipos x 5 = 10. */
    public static final int MINIMO_JUGADORES = JUGADORES_POR_EQUIPO * MINIMO_EQUIPOS;

    /** Máximo de equipos que se arman en un sorteo (regla del torneo). */
    public static final int MAXIMO_EQUIPOS = 8;

    /** Suplentes que puede tener como máximo cada equipo. */
    public static final int MAXIMO_SUPLENTES_POR_EQUIPO = 1;

    /** Cupo de inscripción: 8 equipos x (5 titulares + 1 suplente) = 48 jugadores. */
    public static final int CUPO_MAXIMO =
            MAXIMO_EQUIPOS * (JUGADORES_POR_EQUIPO + MAXIMO_SUPLENTES_POR_EQUIPO);

    /** Fuente de azar. En producción es SecureRandom; en los tests, un Random con semilla. */
    private final Random random;

    /** Constructor usado por la aplicación: azar "real", no predecible. */
    public SorteadorEquipos() {
        this(new SecureRandom());
    }

    /** Constructor para tests: con {@code new Random(42)} el sorteo siempre da lo mismo. */
    public SorteadorEquipos(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    // ------------------------------------------------------------------
    //  Cálculos auxiliares (también los usa el frontend vía la API)
    // ------------------------------------------------------------------

    /** Cuántos equipos completos se forman con esa cantidad de inscriptos (tope: 8). */
    public static int cantidadDeEquipos(int inscriptos) {
        return Math.min(inscriptos / JUGADORES_POR_EQUIPO, MAXIMO_EQUIPOS);
    }

    /** Cuántos jugadores quedan como suplentes: todos los que no son titulares. */
    public static int cantidadDeSuplentes(int inscriptos) {
        return inscriptos - cantidadDeEquipos(inscriptos) * JUGADORES_POR_EQUIPO;
    }

    // ------------------------------------------------------------------
    //  Sorteo
    // ------------------------------------------------------------------

    /**
     * Realiza el sorteo.
     *
     * @param inscriptos jugadores anotados (sin repetidos)
     * @return equipos numerados desde 1, cada uno con 5 titulares y sus suplentes
     * @throws IllegalArgumentException si hay menos de {@value #MINIMO_JUGADORES} o más de
     *                                  {@value #CUPO_MAXIMO} inscriptos, o jugadores repetidos
     */
    public List<EquipoSorteado> sortear(List<Jugador> inscriptos) {
        validar(inscriptos);

        int cantidadEquipos = cantidadDeEquipos(inscriptos.size());

        // ---------------- PASO 1: separar por posición y mezclar ----------------
        // Mezclar cada grupo por separado es lo que hace aleatorio el resultado:
        // el orden de cada lista decide a qué equipo va cada jugador.
        List<Jugador> arqueros   = mezclar(filtrarPorPosicion(inscriptos, Posicion.ARQUERO));
        List<Jugador> defensores = mezclar(filtrarPorPosicion(inscriptos, Posicion.DEFENSOR));
        List<Jugador> delanteros = mezclar(filtrarPorPosicion(inscriptos, Posicion.DELANTERO));

        // ---------------- PASO 2: repartir en ronda (round-robin) ----------------
        // "planteles" = todos los jugadores de cada equipo, sin distinguir aún
        // titulares de suplentes.
        List<List<Jugador>> planteles = new ArrayList<>();
        for (int i = 0; i < cantidadEquipos; i++) {
            planteles.add(new ArrayList<>());
        }

        // Orden de reparto: arqueros primero (para que haya uno por equipo),
        // luego defensores y por último delanteros.
        List<Jugador> ordenDeReparto = new ArrayList<>(inscriptos.size());
        ordenDeReparto.addAll(arqueros);
        ordenDeReparto.addAll(defensores);
        ordenDeReparto.addAll(delanteros);

        // El cursor arranca en un equipo al azar para que el "Equipo 1" no sea
        // siempre el que recibe al primer arquero o al jugador extra.
        int cursor = random.nextInt(cantidadEquipos);
        for (Jugador jugador : ordenDeReparto) {
            planteles.get(cursor).add(jugador);
            // Avanza al siguiente equipo; al llegar al último vuelve al primero.
            // IMPORTANTE: no se reinicia al cambiar de posición, así los totales
            // por equipo quedan parejos (diferencia máxima de 1 jugador).
            cursor = (cursor + 1) % cantidadEquipos;
        }

        // ---------------- PASO 3: titulares y suplentes ----------------
        List<EquipoSorteado> equipos = new ArrayList<>(cantidadEquipos);
        for (int i = 0; i < cantidadEquipos; i++) {
            equipos.add(armarEquipo(i + 1, planteles.get(i)));
        }

        // ---------------- PASO 4: devolver ----------------
        return equipos;
    }

    /**
     * Dentro del plantel de un equipo (5 o más jugadores) elige los 5 titulares:
     * <ol>
     *   <li>Un arquero, si el equipo tiene alguno.</li>
     *   <li>Alterna defensor / delantero, empezando por la posición de la que
     *       el equipo tiene más jugadores (si empatan, se decide al azar).
     *       Si una posición se agota, sigue con la otra.</li>
     *   <li>Si aún faltan titulares (equipo con varios arqueros), completa con
     *       los arqueros que sobran (uno de ellos jugará de campo).</li>
     * </ol>
     * Los que no entran entre los 5 titulares quedan como suplentes.
     */
    private EquipoSorteado armarEquipo(int numero, List<Jugador> plantel) {
        // Copias modificables: vamos "sacando" jugadores de estas listas.
        // Ya vienen mezcladas del PASO 1, así que sacar el primero es sacar uno al azar.
        List<Jugador> arq = new ArrayList<>(filtrarPorPosicion(plantel, Posicion.ARQUERO));
        List<Jugador> def = new ArrayList<>(filtrarPorPosicion(plantel, Posicion.DEFENSOR));
        List<Jugador> del = new ArrayList<>(filtrarPorPosicion(plantel, Posicion.DELANTERO));

        List<Jugador> titulares = new ArrayList<>(JUGADORES_POR_EQUIPO);

        // 1) Arquero titular
        if (!arq.isEmpty()) {
            titulares.add(arq.remove(0));
        }

        // 2) Jugadores de campo alternando posiciones
        boolean tocaDefensor = def.size() > del.size()
                || (def.size() == del.size() && random.nextBoolean());

        while (titulares.size() < JUGADORES_POR_EQUIPO && (!def.isEmpty() || !del.isEmpty())) {
            List<Jugador> origen = tocaDefensor ? def : del;
            if (origen.isEmpty()) {            // se agotó esa posición:
                origen = tocaDefensor ? del : def; // usamos la otra
            }
            titulares.add(origen.remove(0));
            tocaDefensor = !tocaDefensor;
        }

        // 3) Caso raro: faltan titulares y solo quedan arqueros
        while (titulares.size() < JUGADORES_POR_EQUIPO && !arq.isEmpty()) {
            titulares.add(arq.remove(0));
        }

        // Todo lo que quedó en las listas va al banco de suplentes
        List<Jugador> suplentes = new ArrayList<>();
        suplentes.addAll(def);
        suplentes.addAll(del);
        suplentes.addAll(arq);

        return new EquipoSorteado(numero, titulares, suplentes);
    }

    // ------------------------------------------------------------------
    //  Utilidades privadas
    // ------------------------------------------------------------------

    private void validar(List<Jugador> inscriptos) {
        if (inscriptos == null || inscriptos.size() < MINIMO_JUGADORES) {
            int cantidad = inscriptos == null ? 0 : inscriptos.size();
            throw new IllegalArgumentException(
                    "Se necesitan al menos " + MINIMO_JUGADORES + " inscriptos para sortear "
                    + MINIMO_EQUIPOS + " equipos (hay " + cantidad + ").");
        }
        // Tope del torneo: 8 equipos con 1 suplente cada uno como máximo.
        // Con 48 o menos, el reparto en ronda deja a lo sumo 6 jugadores por equipo.
        if (inscriptos.size() > CUPO_MAXIMO) {
            throw new IllegalArgumentException(
                    "El cupo es de " + CUPO_MAXIMO + " jugadores (" + MAXIMO_EQUIPOS
                    + " equipos con " + MAXIMO_SUPLENTES_POR_EQUIPO + " suplente cada uno); hay "
                    + inscriptos.size() + ".");
        }
        // Un mismo jugador no puede aparecer dos veces (comparamos por referencia/equals).
        if (new HashSet<>(inscriptos).size() != inscriptos.size()) {
            throw new IllegalArgumentException("La lista de inscriptos tiene jugadores repetidos.");
        }
    }

    private static List<Jugador> filtrarPorPosicion(List<Jugador> jugadores, Posicion posicion) {
        List<Jugador> resultado = new ArrayList<>();
        for (Jugador j : jugadores) {
            if (j.getPosicion() == posicion) {
                resultado.add(j);
            }
        }
        return resultado;
    }

    /** Devuelve una copia de la lista en orden aleatorio (algoritmo Fisher-Yates de Java). */
    private List<Jugador> mezclar(List<Jugador> jugadores) {
        List<Jugador> copia = new ArrayList<>(jugadores);
        Collections.shuffle(copia, random);
        return copia;
    }
}
