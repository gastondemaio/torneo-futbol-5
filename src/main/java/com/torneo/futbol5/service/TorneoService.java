package com.torneo.futbol5.service;

import com.torneo.futbol5.dto.TorneoDTO;
import com.torneo.futbol5.model.Torneo;
import com.torneo.futbol5.repository.InscripcionRepository;
import com.torneo.futbol5.repository.TorneoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

/**
 * Maneja el "torneo de la semana": existe uno por semana (lunes a domingo)
 * y se crea solo la primera vez que alguien lo consulta.
 */
@Service
public class TorneoService {

    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DIA_MES_ANIO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TorneoRepository torneoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final Clock reloj;

    public TorneoService(TorneoRepository torneoRepository,
                         InscripcionRepository inscripcionRepository,
                         Clock reloj) {
        this.torneoRepository = torneoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.reloj = reloj;
    }

    /** Devuelve el torneo de la semana actual; si no existe todavía, lo crea. */
    @Transactional
    public Torneo obtenerOCrearTorneoActual() {
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate domingo = lunes.plusDays(6);

        return torneoRepository.findByFechaInicio(lunes)
                .orElseGet(() -> torneoRepository.save(new Torneo(
                        "Torneo semanal del " + lunes.format(DIA_MES) + " al " + domingo.format(DIA_MES_ANIO),
                        lunes, domingo)));
    }

    @Transactional
    public TorneoDTO resumenTorneoActual() {
        Torneo torneo = obtenerOCrearTorneoActual();
        return TorneoDTO.desde(torneo, inscripcionRepository.countByTorneo(torneo));
    }
}
