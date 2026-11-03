package com.torneo.futbol5.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Tabla JUGADOR: una persona, identificada por su email. */
@Entity
@Table(name = "jugador")
public class Jugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 60)
    private String apellido;

    @Column(nullable = false, length = 120, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Posicion posicion;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDateTime fechaAlta;

    protected Jugador() {
        // requerido por JPA
    }

    public Jugador(String nombre, String apellido, String email, Posicion posicion) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.posicion = posicion;
    }

    @PrePersist
    void alCrear() {
        if (fechaAlta == null) {
            fechaAlta = LocalDateTime.now();
        }
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Posicion getPosicion() { return posicion; }
    public void setPosicion(Posicion posicion) { this.posicion = posicion; }
    public LocalDateTime getFechaAlta() { return fechaAlta; }

    @Override
    public String toString() {
        return getNombreCompleto() + " (" + posicion + ")";
    }
}
