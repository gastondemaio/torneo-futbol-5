package com.torneo.futbol5.exception;

import org.springframework.http.HttpStatus;

/**
 * Error esperable que se le muestra al usuario tal cual
 * (ej.: "Ya estás inscripto", "Faltan jugadores para sortear").
 */
public class ReglaNegocioException extends RuntimeException {

    private final HttpStatus status;

    public ReglaNegocioException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
