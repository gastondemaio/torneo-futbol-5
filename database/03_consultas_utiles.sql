-- =====================================================================
--  Consultas útiles para revisar los datos (MySQL Workbench o consola H2)
-- =====================================================================

-- Inscriptos del torneo de esta semana, en orden de llegada
SELECT i.fecha_inscripcion, j.nombre, j.apellido, j.email, j.posicion
  FROM inscripcion i
  JOIN jugador j ON j.id = i.jugador_id
  JOIN torneo  t ON t.id = i.torneo_id
 WHERE CURRENT_DATE BETWEEN t.fecha_inicio AND t.fecha_fin
 ORDER BY i.fecha_inscripcion;

-- Cantidad de inscriptos por posición en cada torneo
SELECT t.nombre, j.posicion, COUNT(*) AS cantidad
  FROM inscripcion i
  JOIN jugador j ON j.id = i.jugador_id
  JOIN torneo  t ON t.id = i.torneo_id
 GROUP BY t.nombre, j.posicion
 ORDER BY t.nombre, j.posicion;

-- Equipos sorteados con sus integrantes
SELECT t.nombre AS torneo, e.nombre AS equipo, ej.rol, ej.orden,
       j.nombre, j.apellido, j.posicion
  FROM equipo e
  JOIN torneo t          ON t.id = e.torneo_id
  JOIN equipo_jugador ej ON ej.equipo_id = e.id
  JOIN jugador j         ON j.id = ej.jugador_id
 ORDER BY t.fecha_inicio DESC, e.numero, ej.orden;

-- Historial: cuántas veces jugó cada persona
SELECT j.nombre, j.apellido, j.email, COUNT(i.id) AS torneos_jugados
  FROM jugador j
  LEFT JOIN inscripcion i ON i.jugador_id = j.id
 GROUP BY j.id, j.nombre, j.apellido, j.email
 ORDER BY torneos_jugados DESC;
