package com.torneo.futbol5;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicación.
 * Levanta el servidor en http://localhost:8080 (API REST + frontend).
 */
@SpringBootApplication
public class Futbol5Application {

    public static void main(String[] args) {
        SpringApplication.run(Futbol5Application.class, args);
    }
}
