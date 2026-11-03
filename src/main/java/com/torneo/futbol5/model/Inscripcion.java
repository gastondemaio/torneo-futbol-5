package com.torneo.futbol5.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Tabla INSCRIPCION: un jugador anotado en un torneo (relación N a N). */
@Entity
@Table(name = "inscripcion",
       uniqueConstraints = @UniqueConstraint(columnNames = {"torneo_id", "jugador_id"}))
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "torneo_id", nullable = false)
    private Torneo torneo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jugador_id", nullable = false)
    private Jugador jugador;

    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDateTime fechaInscripcion;

    protected Inscripcion() {
        // requerido por JPA
    }

    public Inscripcion(Torneo torneo, Jugador jugador) {
        this.torneo = torneo;
        this.jugador = jugador;
    }

    @PrePersist
    void alCrear() {
        if (fechaInscripcion == null) {
            fechaInscripcion = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Torneo getTorneo() { return torneo; }
    public Jugador getJugador() { return jugador; }
    public LocalDateTime getFechaInscripcion() { return fechaInscripcion; }
}
