package com.torneo.futbol5.tabla;

/**
 * Un renglón de la tabla de posiciones.
 *
 * @param posicion            1, 2, 3, ...
 * @param equipoId            id del equipo
 * @param pj pg pe pp         partidos jugados, ganados, empatados y perdidos
 * @param gf gc               goles a favor y en contra
 * @param dg                  diferencia de gol (gf − gc)
 * @param pts                 puntos (3 por victoria, 1 por empate, 0 por derrota)
 * @param empatadoConAnterior true si iguala al de arriba en puntos, diferencia de gol y
 *                            goles a favor: ese desempate se resuelve por sorteo
 */
public record FilaTabla(int posicion, Long equipoId,
                        int pj, int pg, int pe, int pp,
                        int gf, int gc, int dg, int pts,
                        boolean empatadoConAnterior) {
}
