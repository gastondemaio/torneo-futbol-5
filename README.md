# Torneo semanal de Fútbol 5 — MVP

Sistema web para anotarse al torneo de la semana, sortear hasta 8 equipos de 5 al azar
(equilibrados por posición, con suplentes) y jugar una liga todos contra todos el sábado y el
domingo, con carga de resultados y tabla de posiciones (3 puntos por victoria, 1 por empate).

- **Backend:** Java 17+, Spring Boot 3.5 (Web, Data JPA/Hibernate, Validation), Maven
- **Base de datos:** H2 en archivo (por defecto, no hay que instalar nada) o MySQL 8 (perfil `mysql`)
- **Frontend:** HTML + CSS + JavaScript vainilla con `fetch()`

---

## 1. Requisitos

| Herramienta | Versión | Cómo verificar |
|---|---|---|
| JDK | 17 o superior (17 o 21 recomendados) | `java -version` |
| Maven | 3.9 o superior | `mvn -v` |
| MySQL (opcional) | 8.0.16 o superior | `mysql --version` |

Si `mvn` no se reconoce: descargá Maven de https://maven.apache.org/download.cgi, descomprimilo
y agregá su carpeta `bin` a la variable `PATH`. Otra opción: abrir el proyecto en IntelliJ IDEA,
que trae Maven incluido (panel *Maven* → `Plugins` → `spring-boot` → `spring-boot:run`).

---

## 2. Ejecutar en local (paso a paso)

Todos los comandos se ejecutan **desde la carpeta raíz del proyecto** (donde está `pom.xml`).
Es importante: el servidor busca el frontend en `./frontend` y guarda la base H2 en `./data`.

### Paso 1 — Abrir una consola en la carpeta del proyecto

```bash
# Windows (CMD o PowerShell)
cd C:\ruta\a\futbol5-torneo

# Linux / macOS
cd ~/ruta/a/futbol5-torneo
```

### Paso 2 — Compilar y correr los tests

```bash
mvn clean package
```

La primera vez Maven descarga las dependencias (tarda unos minutos). Al final tiene que decir
`BUILD SUCCESS` y, sumando las tres clases de test, `Tests run: 20, Failures: 0`.

### Paso 3 — Levantar el servidor

```bash
mvn spring-boot:run
```

Cuando en la consola aparece `Tomcat started on port 8080` y `Started Futbol5Application`, el
servidor está listo. Para detenerlo: **Ctrl + C**.

Alternativa con el `.jar` generado en el paso 2 (también desde la raíz del proyecto):

```bash
java -jar target/futbol5-torneo-1.0.0.jar
```

### Paso 4 — Abrir el frontend

Abrí en el navegador: **http://localhost:8080**

Spring Boot sirve los archivos de la carpeta `frontend/` (`index.html`, `css/`, `js/`) en esa
dirección, así que no hay problemas de CORS. Es la forma recomendada.

Otras formas que también funcionan (con el servidor levantado):
- Doble clic en `frontend/index.html` (se abre como `file://`; el JS llama a `http://localhost:8080/api`).
- Live Server de VS Code sobre `frontend/index.html` (puerto 5500).

### Paso 5 — Probar el flujo completo

1. Completá el formulario y anotate.
2. En el panel **Organizador**, cargá 23 jugadores de prueba con el botón **Cargar**.
3. Tocá **Sortear equipos**: con 24 anotados se arman 4 equipos de 5 y quedan 4 suplentes.
   Se genera el fixture: 6 partidos (fechas 1 y 2 el sábado, fecha 3 el domingo).
4. En **Fixture**, escribí los goles de un partido y tocá **Guardar** (o Enter). La **Tabla de
   posiciones** se actualiza sola. Cuando se juegan todos los partidos aparece el campeón.
5. **Volver a sortear** reemplaza equipos, fixture y resultados; **Anular sorteo** reabre las inscripciones.
6. **Borrar anotados** vacía la lista de la semana de una sola vez (solo antes de sortear).
7. Para ver el torneo completo: cargá 48 jugadores de prueba (cupo lleno) y sorteá: 8 equipos, 28 partidos.

---

## 3. Usar MySQL en lugar de H2

1. Con MySQL corriendo, crear la base (opcional: Spring también la crea por la URL con `createDatabaseIfNotExist=true`):
   ```bash
   mysql -u root -p < database/01_crear_base_mysql.sql
   ```
2. Revisar usuario y contraseña en `src/main/resources/application-mysql.properties`
   (por defecto `root` / `root`).
3. Levantar con el perfil `mysql`:
   ```bash
   # Linux / macOS / CMD
   mvn spring-boot:run -Dspring-boot.run.profiles=mysql

   # PowerShell (requiere comillas)
   mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"

   # o con el jar
   java -jar target/futbol5-torneo-1.0.0.jar --spring.profiles.active=mysql
   ```

Las tablas las crea automáticamente `src/main/resources/schema.sql` al arrancar
(`CREATE TABLE IF NOT EXISTS`). El mismo script está en `database/02_crear_tablas.sql` por si
querés ejecutarlo a mano.

### Ver los datos de H2

Con el servidor levantado: **http://localhost:8080/h2-console**
- JDBC URL: `jdbc:h2:file:./data/futbol5db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE`
- Usuario: `sa` — Contraseña: vacía

Para empezar con la base vacía: detener el servidor y borrar la carpeta `data/`.

---

## 4. Estructura del proyecto

```
futbol5-torneo/
├── pom.xml                          Dependencias y build (Maven)
├── README.md                        Esta guía
├── database/
│   ├── 01_crear_base_mysql.sql      Crea la base y un usuario en MySQL
│   ├── 02_crear_tablas.sql          DDL de las 6 tablas (copia de schema.sql)
│   └── 03_consultas_utiles.sql      Consultas para revisar inscriptos y equipos
├── docs/
│   ├── modelo-entidad-relacion.md   DER, diccionario de datos y reglas de negocio
│   ├── der.png                      Diagrama entidad-relación (imagen)
│   └── der.dot                      Fuente del diagrama (Graphviz)
├── frontend/                        Lo sirve Spring Boot en http://localhost:8080
│   ├── index.html
│   ├── css/styles.css
│   └── js/app.js                    Lógica con fetch() contra la API
└── src/
    ├── main/java/com/torneo/futbol5/
    │   ├── Futbol5Application.java  Punto de entrada (main)
    │   ├── config/AppConfig.java    CORS, reloj con zona horaria, bean del sorteador
    │   ├── controller/              API REST (Inscripcion, Torneo, Partido, Admin)
    │   ├── dto/                     Objetos JSON de entrada/salida
    │   ├── exception/               Errores de negocio y manejo global → JSON
    │   ├── model/                   Entidades JPA: Jugador, Torneo, Inscripcion, Equipo, EquipoJugador, Partido
    │   ├── repository/              Interfaces Spring Data JPA
    │   ├── service/                 Reglas de negocio y transacciones
    │   ├── sorteo/
    │   │   ├── SorteadorEquipos.java   ★ Algoritmo de sorteo (comentado en detalle)
    │   │   └── EquipoSorteado.java
    │   ├── fixture/
    │   │   └── GeneradorFixture.java   ★ Fixture todos contra todos (método del círculo)
    │   └── tabla/
    │       └── CalculadoraTabla.java   ★ Tabla de posiciones: 3 / 1 / 0 y desempates
    ├── main/resources/
    │   ├── application.properties          Config por defecto (H2)
    │   ├── application-mysql.properties    Config del perfil mysql
    │   └── schema.sql                      Script de creación de tablas
    └── test/java/...                Tests JUnit 5: SorteadorEquiposTest, GeneradorFixtureTest,
                                     CalculadoraTablaTest
```

Arquitectura en capas: **Controller** (HTTP) → **Service** (reglas, transacciones) →
**Repository** (JPA) → **Base de datos**. Los algoritmos (sorteo, fixture y tabla) están en los
paquetes `sorteo`, `fixture` y `tabla`: no dependen de Spring ni de la base, por eso se testean sin
levantar nada.

---

## 5. API REST

Base: `http://localhost:8080/api`

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| GET | `/torneos/actual` | Torneo de la semana (se crea solo) con cantidad de inscriptos, equipos y suplentes posibles | 200 |
| GET | `/inscripciones` | Listado de inscriptos de la semana | 200 |
| POST | `/inscripciones` | Inscribir jugador | 201, 400 datos inválidos, 409 ya inscripto o inscripciones cerradas |
| DELETE | `/inscripciones/{id}` | Dar de baja una inscripción | 204, 404, 409 |
| DELETE | `/inscripciones` | Borrar todos los anotados de la semana (y los jugadores que quedan sin inscripciones) | 200 `{"borrados": n}`, 409 si ya se sorteó |
| POST | `/torneos/actual/sorteo` | Sortear o volver a sortear | 200, 422 menos de 10 inscriptos |
| GET | `/torneos/actual/equipos` | Equipos del sorteo y advertencias | 200 |
| GET | `/torneos/actual/fixture` | Partidos agrupados por fecha (sábado y domingo), con quién queda libre | 200 |
| GET | `/torneos/actual/tabla` | Tabla de posiciones, campeón si terminó | 200 |
| PUT | `/partidos/{id}/resultado` | Cargar o corregir goles: `{"golesLocal":3,"golesVisitante":1}` | 200, 400 goles inválidos, 404 |
| DELETE | `/partidos/{id}/resultado` | Borrar el resultado (vuelve a "sin jugar") | 200, 404 |
| DELETE | `/torneos/actual/sorteo` | Anular sorteo y reabrir inscripciones | 200 |
| POST | `/admin/jugadores-demo?cantidad=23` | Cargar jugadores ficticios (1 a 60) | 200 |

Ejemplo de inscripción (`POST /api/inscripciones`):

```json
{ "nombre": "Lionel", "apellido": "Messi", "email": "lio@ejemplo.com", "posicion": "DELANTERO" }
```

Los errores siempre vuelven como `{ "mensaje": "...", "detalles": ["..."] }`.

Probar desde consola con curl (Linux/macOS/Git Bash):

```bash
curl -X POST http://localhost:8080/api/inscripciones -H "Content-Type: application/json" \
     -d '{"nombre":"Ana","apellido":"Paz","email":"ana@ejemplo.com","posicion":"ARQUERO"}'
curl -X POST "http://localhost:8080/api/admin/jugadores-demo?cantidad=22"
curl -X POST http://localhost:8080/api/torneos/actual/sorteo
```

---

## 6. Reglas de negocio implementadas

- Un torneo por semana (lunes a domingo, hora de Argentina), creado automáticamente.
- El email identifica al jugador; no puede anotarse dos veces en la misma semana.
- Mínimo 10 inscriptos para sortear (2 equipos).
- Equipos = inscriptos ÷ 5, con un **máximo de 8 equipos por sorteo**. Cada equipo tiene **5 titulares**.
- **Suplentes:** los que no completan un equipo quedan como suplentes, repartidos parejo.
  Ejemplo: 23 inscriptos → 4 equipos + 3 suplentes.
- **Cupo:** 48 jugadores por semana (8 equipos con 1 suplente cada uno).
- **Formato:** liga todos contra todos a una rueda, el sábado y el domingo. Con 8 equipos son
  7 fechas y 28 partidos: fechas 1 a 4 el sábado, 5 a 7 el domingo.
- **Puntos:** victoria 3, empate 1, derrota 0. Gana el que más puntos suma al terminar.
- **Desempate** (como la tabla anual de la Liga Profesional, sin fair play): diferencia de gol,
  goles a favor y sorteo.
- Un arquero titular por equipo cuando alcanzan; defensores y delanteros repartidos parejo.
  Si un equipo queda sin arquero, se muestra una advertencia.
- Después del sorteo se cierran las inscripciones (se pueden reabrir anulando el sorteo).

Detalle completo y DER en [`docs/modelo-entidad-relacion.md`](docs/modelo-entidad-relacion.md).

---

## 7. Problemas frecuentes

| Síntoma | Solución |
|---|---|
| `Port 8080 was already in use` | Cerrar el otro programa, o usar otro puerto: `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` y abrir http://localhost:8081 |
| `'mvn' no se reconoce como un comando` | Instalar Maven y agregar su `bin` al PATH (ver sección 1) |
| `release version 17 not supported` | El JDK es viejo: instalar JDK 17 o 21 y configurar `JAVA_HOME` |
| La página abre pero dice "No hay conexión con el servidor" | El backend no está corriendo, o se abrió el HTML en otro puerto distinto de 8080/5500 |
| La página en http://localhost:8080 da 404 | El servidor se lanzó desde otra carpeta: ejecutarlo desde la raíz del proyecto |
| `Access denied for user 'root'` (MySQL) | Corregir usuario/clave en `application-mysql.properties` |

---

## 8. Mejoras para una próxima versión

- Autenticación para el panel del organizador (Spring Security).
- Nivel de juego por jugador para equilibrar también por habilidad.
- Horarios y canchas para cada partido del fixture.
- Goleadores y tarjetas (con tarjetas se puede sumar el desempate por fair play de la AFA).
- Notificación por email con el equipo asignado.
