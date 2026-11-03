package com.torneo.futbol5.repository;

import com.torneo.futbol5.model.Jugador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface JugadorRepository extends JpaRepository<Jugador, Long> {

    Optional<Jugador> findByEmailIgnoreCase(String email);

    /**
     * Borra los jugadores que ya no tienen ninguna inscripción (en ninguna semana)
     * ni forman parte de ningún equipo. Así no quedan "huérfanos", por ejemplo
     * los jugadores de prueba. Los que jugaron semanas anteriores se conservan.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           delete from Jugador j
            where not exists (select 1 from Inscripcion i where i.jugador = j)
              and not exists (select 1 from EquipoJugador ej where ej.jugador = j)
           """)
    int borrarSinInscripciones();
}
