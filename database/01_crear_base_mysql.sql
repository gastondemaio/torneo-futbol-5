-- =====================================================================
--  Solo para MySQL: crear la base y un usuario propio (opcional).
--  Ejecutar como root:  mysql -u root -p < database/01_crear_base_mysql.sql
--  Si usás H2 (configuración por defecto) NO hace falta este archivo.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS futbol5db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

-- Usuario opcional (si lo usás, cambiá usuario/clave en application-mysql.properties)
CREATE USER IF NOT EXISTS 'futbol5'@'localhost' IDENTIFIED BY 'futbol5';
GRANT ALL PRIVILEGES ON futbol5db.* TO 'futbol5'@'localhost';
FLUSH PRIVILEGES;
