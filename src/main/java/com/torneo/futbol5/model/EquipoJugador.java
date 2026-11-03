package com.torneo.futbol5.model;

import jakarta.persistence.*;

/** Tabla EQUIPO_JUGADOR: qué jugador integra qué equipo y con qué rol. */
@Entity
@Table(name = "equipo_jugador",
       uniqueConstraints = @UniqueConstraint(columnNames = {"equipo_id", "jugador_id"}))
public class EquipoJugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jugador_id", nullable = false)
    private Jugador jugador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RolEquipo rol;

    /** Orden dentro del equipo (el arquero titular, si hay, es el 1). */
    @Column(nullable = false)
    private int orden;

    protected EquipoJugador() {
        // requerido por JPA
    }

    public EquipoJugador(Equipo equipo, Jugador jugador, RolEquipo rol, int orden) {
        this.equipo = equipo;
        this.jugador = jugador;
        this.rol = rol;
        this.orden = orden;
    }

    public Long getId() { return id; }
    public Equipo getEquipo() { return equipo; }
    public Jugador getJugador() { return jugador; }
    public RolEquipo getRol() { return rol; }
    public int getOrden() { return orden; }
}
