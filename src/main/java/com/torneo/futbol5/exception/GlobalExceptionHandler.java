package com.torneo.futbol5.exception;

import com.torneo.futbol5.dto.ErrorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Convierte cualquier excepción en una respuesta JSON {@link ErrorDTO}
 * con el código HTTP adecuado, para que el frontend siempre pueda mostrar "mensaje".
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorDTO> reglaDeNegocio(ReglaNegocioException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ErrorDTO(ex.getMessage()));
    }

    /** Falló alguna validación del formulario (@NotBlank, @Email, ...). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> validacion(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorDTO("Revisá los datos del formulario.", detalles));
    }

    /** JSON mal formado o posición inexistente (ej.: "MEDIOCAMPISTA"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDTO> jsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErrorDTO(
                "Datos inválidos. La posición debe ser ARQUERO, DEFENSOR o DELANTERO."));
    }

    /** El sorteador rechazó la lista (pocos jugadores, repetidos). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDTO> argumentoInvalido(IllegalArgumentException ex) {
        return ResponseEntity.unprocessableEntity().body(new ErrorDTO(ex.getMessage()));
    }

    /** Violación de UNIQUE en la base (ej.: dos inscripciones simultáneas del mismo email). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDTO> integridad(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorDTO("El dato ya existe o entra en conflicto con otro registro."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> inesperado(Exception ex) {
        // Errores HTTP "normales" de Spring (404 de un archivo, 405 método no permitido, ...)
        if (ex instanceof ErrorResponse errorHttp) {
            return ResponseEntity.status(errorHttp.getStatusCode())
                    .body(new ErrorDTO("No se pudo procesar la solicitud (" + errorHttp.getStatusCode() + ")."));
        }
        log.error("Error inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorDTO("Ocurrió un error inesperado en el servidor."));
    }
}
