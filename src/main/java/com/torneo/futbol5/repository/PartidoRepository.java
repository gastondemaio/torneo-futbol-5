package com.torneo.futbol5.repository;

import com.torneo.futbol5.model.Partido;
import com.torneo.futbol5.model.Torneo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PartidoRepository extends JpaRepository<Partido, Long> {

    /** Fixture completo del torneo, con los dos equipos de cada partido (una sola consulta). */
    @Query("""
           select p from Partido p
             join fetch p.equipoLocal
             join fetch p.equipoVisitante
            where p.torneo = :torneo
            order by p.jornada, p.orden
           """)
    List<Partido> buscarFixture(@Param("torneo") Torneo torneo);

    /**
     * Borra todos los partidos del torneo (al re-sortear o anular el sorteo).
     * No limpia el contexto de persistencia: el torneo y los jugadores ya cargados
     * en la misma transacción siguen administrados por JPA.
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from Partido p where p.torneo = :torneo")
    int borrarPorTorneo(@Param("torneo") Torneo torneo);
}
