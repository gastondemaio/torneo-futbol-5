package com.torneo.futbol5.repository;

import com.torneo.futbol5.model.Equipo;
import com.torneo.futbol5.model.Torneo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    /** Equipos del torneo con sus integrantes y jugadores (una sola consulta, sin N+1). */
    @Query("""
           select distinct e from Equipo e
             left join fetch e.integrantes i
             left join fetch i.jugador
            where e.torneo = :torneo
            order by e.numero
           """)
    List<Equipo> buscarConIntegrantes(@Param("torneo") Torneo torneo);

    List<Equipo> findByTorneo(Torneo torneo);
}
