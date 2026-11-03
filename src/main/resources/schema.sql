-- =====================================================================
--  Torneo semanal de Fútbol 5 - Script de creación de tablas
--  Compatible con MySQL 8 (>= 8.0.16) y con H2 en modo MySQL.
--  Spring Boot lo ejecuta automáticamente al iniciar (IF NOT EXISTS
--  evita errores si las tablas ya existen).
-- =====================================================================

-- Personas que alguna vez se inscribieron. El email las identifica.
CREATE TABLE IF NOT EXISTS jugador (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(60)  NOT NULL,
    apellido    VARCHAR(60)  NOT NULL,
    email       VARCHAR(120) NOT NULL,
    posicion    VARCHAR(20)  NOT NULL,
    fecha_alta  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_jugador_email    UNIQUE (email),
    CONSTRAINT ck_jugador_posicion CHECK (posicion IN ('ARQUERO', 'DEFENSOR', 'DELANTERO'))
);

-- Un torneo por semana (de lunes a domingo).
CREATE TABLE IF NOT EXISTS torneo (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(100) NOT NULL,
    fecha_inicio  DATE         NOT NULL,
    fecha_fin     DATE         NOT NULL,
    estado        VARCHAR(20)  NOT NULL DEFAULT 'ABIERTO',
    fecha_sorteo  TIMESTAMP    NULL,
    CONSTRAINT ck_torneo_fechas CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT uk_torneo_inicio UNIQUE (fecha_inicio),
    CONSTRAINT ck_torneo_estado CHECK (estado IN ('ABIERTO', 'SORTEADO'))
);

-- Relación N a N entre Jugador y Torneo: quién se anotó a qué torneo.
CREATE TABLE IF NOT EXISTS inscripcion (
    id                 BIGINT    AUTO_INCREMENT PRIMARY KEY,
    torneo_id          BIGINT    NOT NULL,
    jugador_id         BIGINT    NOT NULL,
    fecha_inscripcion  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_inscripcion_torneo_jugador UNIQUE (torneo_id, jugador_id),
    CONSTRAINT fk_inscripcion_torneo  FOREIGN KEY (torneo_id)  REFERENCES torneo (id)  ON DELETE CASCADE,
    CONSTRAINT fk_inscripcion_jugador FOREIGN KEY (jugador_id) REFERENCES jugador (id) ON DELETE CASCADE
);

-- Equipos generados por el sorteo de un torneo.
CREATE TABLE IF NOT EXISTS equipo (
    id         BIGINT      AUTO_INCREMENT PRIMARY KEY,
    torneo_id  BIGINT      NOT NULL,
    numero     INT         NOT NULL,
    nombre     VARCHAR(60) NOT NULL,
    CONSTRAINT uk_equipo_torneo_numero UNIQUE (torneo_id, numero),
    CONSTRAINT fk_equipo_torneo FOREIGN KEY (torneo_id) REFERENCES torneo (id) ON DELETE CASCADE
);

-- Integrantes de cada equipo: titulares (5) y suplentes (0 o más).
CREATE TABLE IF NOT EXISTS equipo_jugador (
    id          BIGINT      AUTO_INCREMENT PRIMARY KEY,
    equipo_id   BIGINT      NOT NULL,
    jugador_id  BIGINT      NOT NULL,
    rol         VARCHAR(10) NOT NULL,
    orden       INT         NOT NULL,
    CONSTRAINT uk_equipo_jugador UNIQUE (equipo_id, jugador_id),
    CONSTRAINT ck_equipo_jugador_rol CHECK (rol IN ('TITULAR', 'SUPLENTE')),
    CONSTRAINT fk_ej_equipo  FOREIGN KEY (equipo_id)  REFERENCES equipo (id)  ON DELETE CASCADE,
    CONSTRAINT fk_ej_jugador FOREIGN KEY (jugador_id) REFERENCES jugador (id) ON DELETE CASCADE
);

-- Fixture del torneo: partidos todos contra todos entre los equipos sorteados.
-- Se juegan el sábado y el domingo de la semana. Goles en NULL = partido sin jugar.
-- La tabla de posiciones NO se guarda: se calcula a partir de estos resultados.
-- (Nota MySQL: no se agrega un CHECK local <> visitante porque MySQL no permite
--  CHECK sobre columnas con FK ON DELETE CASCADE, lo valida el código Java.)
CREATE TABLE IF NOT EXISTS partido (
    id                   BIGINT      AUTO_INCREMENT PRIMARY KEY,
    torneo_id            BIGINT      NOT NULL,
    jornada              INT         NOT NULL,
    dia                  VARCHAR(10) NOT NULL,
    fecha                DATE        NOT NULL,
    orden                INT         NOT NULL,
    equipo_local_id      BIGINT      NOT NULL,
    equipo_visitante_id  BIGINT      NOT NULL,
    goles_local          INT         NULL,
    goles_visitante      INT         NULL,
    CONSTRAINT uk_partido_cruce UNIQUE (torneo_id, equipo_local_id, equipo_visitante_id),
    CONSTRAINT ck_partido_dia   CHECK (dia IN ('SABADO', 'DOMINGO')),
    CONSTRAINT ck_partido_goles CHECK ((goles_local IS NULL AND goles_visitante IS NULL)
                                    OR (goles_local >= 0 AND goles_visitante >= 0)),
    CONSTRAINT fk_partido_torneo    FOREIGN KEY (torneo_id)           REFERENCES torneo (id) ON DELETE CASCADE,
    CONSTRAINT fk_partido_local     FOREIGN KEY (equipo_local_id)     REFERENCES equipo (id) ON DELETE CASCADE,
    CONSTRAINT fk_partido_visitante FOREIGN KEY (equipo_visitante_id) REFERENCES equipo (id) ON DELETE CASCADE
);
