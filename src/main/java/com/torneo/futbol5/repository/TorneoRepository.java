package com.torneo.futbol5.repository;

import com.torneo.futbol5.model.Torneo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TorneoRepository extends JpaRepository<Torneo, Long> {

    Optional<Torneo> findByFechaInicio(LocalDate fechaInicio);
}
