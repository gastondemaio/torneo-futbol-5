package com.torneo.futbol5.model;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * Tabla PARTIDO: un cruce del fixture entre dos equipos del mismo torneo.
 * Mientras no se carga el resultado, los goles son null.
 */
@Entity
@Table(name = "partido",
       uniqueConstraints = @UniqueConstraint(columnNames = {"torneo_id", "equipo_local_id", "equipo_visitante_id"}))
public class Partido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "torneo_id", nullable = false)
    private Torneo torneo;

    /** Número de fecha (ronda): 1, 2, 3, ... */
    @Column(nullable = false)
    private int jornada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DiaJuego dia;

    /** Día calendario en que se juega (el sábado o el domingo de la semana). */
    @Column(nullable = false)
    private LocalDate fecha;

    /** Orden del partido dentro de su fecha. */
    @Column(nullable = false)
    private int orden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipo_local_id", nullable = false)
    private Equipo equipoLocal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipo_visitante_id", nullable = false)
    private Equipo equipoVisitante;

    @Column(name = "goles_local")
    private Integer golesLocal;

    @Column(name = "goles_visitante")
    private Integer golesVisitante;

    protected Partido() {
        // requerido por JPA
    }

    public Partido(Torneo torneo, int jornada, DiaJuego dia, LocalDate fecha, int orden,
                   Equipo equipoLocal, Equipo equipoVisitante) {
        this.torneo = torneo;
        this.jornada = jornada;
        this.dia = dia;
        this.fecha = fecha;
        this.orden = orden;
        this.equipoLocal = equipoLocal;
        this.equipoVisitante = equipoVisitante;
    }

    public boolean estaJugado() {
        return golesLocal != null && golesVisitante != null;
    }

    public void registrarResultado(int golesLocal, int golesVisitante) {
        if (golesLocal < 0 || golesVisitante < 0) {
            throw new IllegalArgumentException("Los goles no pueden ser negativos.");
        }
        this.golesLocal = golesLocal;
        this.golesVisitante = golesVisitante;
    }

    public void borrarResultado() {
        this.golesLocal = null;
        this.golesVisitante = null;
    }

    public Long getId() { return id; }
    public Torneo getTorneo() { return torneo; }
    public int getJornada() { return jornada; }
    public DiaJuego getDia() { return dia; }
    public LocalDate getFecha() { return fecha; }
    public int getOrden() { return orden; }
    public Equipo getEquipoLocal() { return equipoLocal; }
    public Equipo getEquipoVisitante() { return equipoVisitante; }
    public Integer getGolesLocal() { return golesLocal; }
    public Integer getGolesVisitante() { return golesVisitante; }
}
