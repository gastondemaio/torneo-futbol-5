package com.torneo.futbol5.model;

/**
 * ABIERTO: se aceptan inscripciones.
 * SORTEADO: los equipos ya están armados; las inscripciones quedan cerradas
 *           hasta que el administrador anule el sorteo.
 */
public enum EstadoTorneo {
    ABIERTO,
    SORTEADO
}
