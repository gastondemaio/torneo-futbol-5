package com.torneo.futbol5.repository;

import com.torneo.futbol5.model.Inscripcion;
import com.torneo.futbol5.model.Jugador;
import com.torneo.futbol5.model.Torneo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    /** Inscriptos de un torneo, en orden de llegada, trayendo los datos del jugador en la misma consulta. */
    @EntityGraph(attributePaths = "jugador")
    List<Inscripcion> findByTorneoOrderByFechaInscripcionAscIdAsc(Torneo torneo);

    boolean existsByTorneoAndJugador(Torneo torneo, Jugador jugador);

    long countByTorneo(Torneo torneo);

    /** Borra de una sola vez (un único DELETE) todas las inscripciones del torneo. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Inscripcion i where i.torneo = :torneo")
    int borrarPorTorneo(@Param("torneo") Torneo torneo);
}
