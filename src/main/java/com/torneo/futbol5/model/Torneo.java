package com.torneo.futbol5.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Tabla TORNEO: un torneo por semana (lunes a domingo). */
@Entity
@Table(name = "torneo")
public class Torneo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "fecha_inicio", nullable = false, unique = true)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTorneo estado = EstadoTorneo.ABIERTO;

    @Column(name = "fecha_sorteo")
    private LocalDateTime fechaSorteo;

    protected Torneo() {
        // requerido por JPA
    }

    public Torneo(String nombre, LocalDate fechaInicio, LocalDate fechaFin) {
        this.nombre = nombre;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    /** Los partidos se juegan el fin de semana: sábado (lunes + 5 días). */
    public LocalDate getFechaSabado() {
        return fechaInicio.plusDays(5);
    }

    /** ... y domingo (lunes + 6 días). */
    public LocalDate getFechaDomingo() {
        return fechaInicio.plusDays(6);
    }

    public boolean estaAbierto() {
        return estado == EstadoTorneo.ABIERTO;
    }

    public void marcarSorteado(LocalDateTime momento) {
        this.estado = EstadoTorneo.SORTEADO;
        this.fechaSorteo = momento;
    }

    public void reabrir() {
        this.estado = EstadoTorneo.ABIERTO;
        this.fechaSorteo = null;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public EstadoTorneo getEstado() { return estado; }
    public LocalDateTime getFechaSorteo() { return fechaSorteo; }
}
