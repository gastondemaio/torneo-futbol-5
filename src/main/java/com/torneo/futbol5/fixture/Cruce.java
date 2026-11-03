package com.torneo.futbol5.fixture;

/** Un partido del fixture: quién juega contra quién (todavía sin resultado). */
public record Cruce<T>(T local, T visitante) {
}
