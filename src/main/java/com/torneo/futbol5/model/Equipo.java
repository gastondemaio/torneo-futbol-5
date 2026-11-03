package com.torneo.futbol5.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/** Tabla EQUIPO: un equipo resultante del sorteo de un torneo. */
@Entity
@Table(name = "equipo",
       uniqueConstraints = @UniqueConstraint(columnNames = {"torneo_id", "numero"}))
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "torneo_id", nullable = false)
    private Torneo torneo;

    @Column(nullable = false)
    private int numero;

    @Column(nullable = false, length = 60)
    private String nombre;

    /** Integrantes (tabla EQUIPO_JUGADOR). Se guardan y borran junto con el equipo. */
    @OneToMany(mappedBy = "equipo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<EquipoJugador> integrantes = new ArrayList<>();

    protected Equipo() {
        // requerido por JPA
    }

    public Equipo(Torneo torneo, int numero, String nombre) {
        this.torneo = torneo;
        this.numero = numero;
        this.nombre = nombre;
    }

    /** Agrega un jugador al equipo con su rol (titular/suplente) y orden de aparición. */
    public void agregarIntegrante(Jugador jugador, RolEquipo rol) {
        integrantes.add(new EquipoJugador(this, jugador, rol, integrantes.size() + 1));
    }

    public List<EquipoJugador> getIntegrantes(RolEquipo rol) {
        return integrantes.stream()
                .filter(i -> i.getRol() == rol)
                .sorted(java.util.Comparator.comparingInt(EquipoJugador::getOrden))
                .toList();
    }

    public Long getId() { return id; }
    public Torneo getTorneo() { return torneo; }
    public int getNumero() { return numero; }
    public String getNombre() { return nombre; }
    public List<EquipoJugador> getIntegrantes() { return integrantes; }
}
