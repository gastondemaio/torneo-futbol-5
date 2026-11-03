package com.torneo.futbol5.config;

import com.torneo.futbol5.sorteo.SorteadorEquipos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    /** Reloj con la zona horaria del torneo: define qué "semana" es hoy. */
    @Bean
    public Clock reloj(@Value("${torneo.zona-horaria}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }

    /** El algoritmo de sorteo es una clase Java pura; acá la registramos como bean. */
    @Bean
    public SorteadorEquipos sorteadorEquipos() {
        return new SorteadorEquipos();
    }

    /**
     * CORS: permite llamar a la API desde otro origen, por ejemplo abriendo
     * frontend/index.html con doble clic (origen "file://") o desde Live Server
     * de VS Code (http://127.0.0.1:5500). Si se usa http://localhost:8080 no hace falta.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
