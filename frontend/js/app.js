/* ==========================================================
   Torneo de Fútbol 5 — lógica del frontend (JavaScript vainilla)
   Se comunica con el backend Spring Boot mediante fetch().
   ========================================================== */

// Si la página la sirve Spring Boot (http://localhost:8080) usamos rutas relativas.
// Si se abre con doble clic (file://) o con Live Server de VS Code (puerto 5500),
// apuntamos al backend en localhost:8080.
const API = (location.protocol === 'file:' || location.port === '5500')
    ? 'http://localhost:8080/api'
    : '/api';

// Versión del backend con la que funciona esta página (ver VersionController.java)
const VERSION_ESPERADA = '1.3';

const POSICIONES = {
    ARQUERO: 'Arquero',
    DEFENSOR: 'Defensor',
    DELANTERO: 'Delantero'
};

// Color de la pechera según el nombre del equipo que asigna el backend
const PECHERAS = {
    Rojo: '#D7263D', Azul: '#1F5FBF', Verde: '#3DBB63', Amarillo: '#F2C230',
    Negro: '#1A1A1A', Blanco: '#FFFFFF', Naranja: '#F07A1A', Violeta: '#7B3FB5',
    Celeste: '#6EC1E4', Gris: '#8A8F94', 'Bordó': '#7A1F2B', Rosa: '#E86FA4'
};

let torneo = null;          // último estado del torneo recibido
let ultimoInscriptoId = null;
let fixture = null;         // último fixture recibido
let diaActivo = 'SABADO';   // pestaña visible del fixture

const DIAS = { SABADO: 'Sábado', DOMINGO: 'Domingo' };

// ---------------------------------------------------------------
//  Utilidades
// ---------------------------------------------------------------
const $ = (id) => document.getElementById(id);

/** Evita inyectar HTML con los nombres que escriben los usuarios. */
function escapar(texto) {
    return String(texto ?? '')
        .replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;').replaceAll("'", '&#39;');
}

/**
 * Envoltorio de fetch: agrega JSON, interpreta errores del backend
 * (formato { mensaje, detalles }) y detecta si el servidor está caído.
 */
async function api(ruta, opciones = {}) {
    let respuesta;
    try {
        respuesta = await fetch(API + ruta, {
            headers: { 'Content-Type': 'application/json' },
            ...opciones
        });
    } catch (e) {
        $('aviso-conexion').hidden = false;
        throw new Error('No hay conexión con el servidor.');
    }
    $('aviso-conexion').hidden = true;

    if (respuesta.status === 204) return null;
    const datos = await respuesta.json().catch(() => null);

    if (!respuesta.ok) {
        const error = new Error(datos?.mensaje || `Error ${respuesta.status}`);
        error.detalles = datos?.detalles || [];
        error.status = respuesta.status;
        throw error;
    }
    return datos;
}

function mostrarMensaje(elemento, texto, tipo, detalles = []) {
    elemento.className = `mensaje ${elemento.classList.contains('mensaje--admin') ? 'mensaje--admin ' : ''}mensaje--${tipo}`;
    elemento.innerHTML = escapar(texto) +
        (detalles.length ? `<ul>${detalles.map(d => `<li>${escapar(d)}</li>`).join('')}</ul>` : '');
}

function formatearFecha(iso) {
    const [a, m, d] = iso.split('-');
    return `${d}/${m}/${a}`;
}

function plural(n, singular, pluralTexto) {
    return `${n} ${n === 1 ? singular : pluralTexto}`;
}

/** Elige texto blanco o negro según lo claro que sea el color de la pechera. */
function tintaSobre(hex) {
    const n = parseInt(hex.slice(1), 16);
    const r = (n >> 16) & 255, g = (n >> 8) & 255, b = n & 255;
    return (0.299 * r + 0.587 * g + 0.114 * b) > 160 ? '#13211A' : '#FFFFFF';
}

function iniciales(nombre, apellido) {
    return (nombre[0] + apellido[0]).toUpperCase();
}

/** Color de pechera de un equipo a partir de su nombre ("Equipo Rojo" -> rojo). */
function colorEquipo(nombre) {
    return PECHERAS[String(nombre).replace('Equipo ', '')] || '#E3F23B';
}

function formatearDiaMes(iso) {
    const [, m, d] = iso.split('-');
    return `${d}/${m}`;
}

/** Nombre del equipo con su punto de color. Con corto=true muestra solo el color ("Rojo"). */
function chipEquipo(equipo, corto = false) {
    const nombre = corto ? equipo.nombre.replace('Equipo ', '') : equipo.nombre;
    return `<span class="punto" style="--bib:${colorEquipo(equipo.nombre)}" aria-hidden="true"></span>${escapar(nombre)}`;
}

// ---------------------------------------------------------------
//  Torneo (encabezado + panel del organizador)
// ---------------------------------------------------------------
async function cargarTorneo() {
    torneo = await api('/torneos/actual');

    $('semana').textContent =
        `Semana del ${formatearFecha(torneo.fechaInicio)} al ${formatearFecha(torneo.fechaFin)}`;

    const n = torneo.cantidadInscriptos;
    const cupoLleno = n >= torneo.cupoMaximo;
    const dias = `el sábado ${formatearDiaMes(torneo.fechaSabado)} y el domingo ${formatearDiaMes(torneo.fechaDomingo)}`;
    let estado;
    if (torneo.estado === 'SORTEADO') {
        estado = `<strong>Equipos sorteados.</strong> ${plural(torneo.equiposPosibles, 'equipo', 'equipos')} juegan todos contra todos ${dias}.`;
    } else if (cupoLleno) {
        estado = `<strong>Cupo completo.</strong> ${n} anotados: ${torneo.maximoEquipos} equipos con un suplente cada uno. Se juega ${dias}.`;
    } else if (n < torneo.minimoJugadores) {
        const faltan = torneo.minimoJugadores - n;
        estado = `${plural(n, 'anotado', 'anotados')}. Faltan <strong>${faltan}</strong> para armar ${torneo.minimoJugadores / 5} equipos.`;
    } else {
        const suplentes = torneo.suplentesPosibles
            ? ` y ${plural(torneo.suplentesPosibles, 'suplente', 'suplentes')}` : '';
        estado = `${n} de ${torneo.cupoMaximo} lugares ocupados. Alcanza para <strong>${plural(torneo.equiposPosibles, 'equipo', 'equipos')}</strong>${suplentes}.`;
    }
    $('estado-torneo').innerHTML = estado;

    const sorteado = torneo.estado === 'SORTEADO';
    const formulario = $('form-inscripcion');
    formulario.querySelectorAll('input, button').forEach(el => el.disabled = sorteado || cupoLleno);
    $('ayuda-form').textContent = sorteado
        ? 'Los equipos de esta semana ya se sortearon. Las inscripciones vuelven a abrirse si el organizador anula el sorteo.'
        : cupoLleno
            ? `El cupo de ${torneo.cupoMaximo} jugadores está completo (${torneo.maximoEquipos} equipos con suplente).`
            : 'Completá tus datos. Si ya jugaste otra semana, usá el mismo email.';

    $('boton-sortear').textContent = sorteado ? 'Volver a sortear' : 'Sortear equipos';
    $('boton-sortear').disabled = n < torneo.minimoJugadores;
    $('boton-anular').hidden = !sorteado;
    $('boton-demo').disabled = sorteado || cupoLleno;
    $('cantidad-demo').disabled = sorteado || cupoLleno;
    // Se puede vaciar la lista siempre que haya anotados (si ya se sorteó, primero se anula el sorteo)
    $('boton-borrar-todos').disabled = n === 0;
}

// ---------------------------------------------------------------
//  Inscriptos
// ---------------------------------------------------------------
async function cargarInscriptos() {
    const inscriptos = await api('/inscripciones');
    const sorteado = torneo?.estado === 'SORTEADO';

    $('tabla-inscriptos').innerHTML = inscriptos.map((j, i) => `
        <tr class="${j.inscripcionId === ultimoInscriptoId ? 'nuevo' : ''}">
            <td class="col-num">${i + 1}</td>
            <td>
                ${escapar(j.nombre)} ${escapar(j.apellido)}
                <span class="jugador-email">${escapar(j.email)}</span>
            </td>
            <td><span class="etiqueta etiqueta--${j.posicion}">${POSICIONES[j.posicion]}</span></td>
            <td class="col-acciones">
                ${sorteado ? '' : `<button type="button" class="boton-baja" data-id="${j.inscripcionId}"
                    data-nombre="${escapar(j.nombre + ' ' + j.apellido)}">Quitar</button>`}
            </td>
        </tr>`).join('');

    $('sin-inscriptos').hidden = inscriptos.length > 0;

    const cuenta = { ARQUERO: 0, DEFENSOR: 0, DELANTERO: 0 };
    inscriptos.forEach(j => cuenta[j.posicion]++);
    $('resumen-posiciones').textContent = inscriptos.length
        ? `${plural(cuenta.ARQUERO, 'arquero', 'arqueros')}, ${plural(cuenta.DEFENSOR, 'defensor', 'defensores')}, ${plural(cuenta.DELANTERO, 'delantero', 'delanteros')}`
        : '';
}

async function inscribir(evento) {
    evento.preventDefault();
    const form = evento.target;
    const mensaje = $('mensaje-form');

    const datos = {
        nombre: form.nombre.value.trim(),
        apellido: form.apellido.value.trim(),
        email: form.email.value.trim(),
        posicion: form.posicion.value || null
    };

    // Validación rápida en el navegador (el backend vuelve a validar todo)
    const faltantes = [];
    form.nombre.setAttribute('aria-invalid', !datos.nombre);
    form.apellido.setAttribute('aria-invalid', !datos.apellido);
    form.email.setAttribute('aria-invalid', !form.email.checkValidity() || !datos.email);
    if (!datos.nombre) faltantes.push('Escribí tu nombre.');
    if (!datos.apellido) faltantes.push('Escribí tu apellido.');
    if (!datos.email || !form.email.checkValidity()) faltantes.push('Escribí un email válido.');
    if (!datos.posicion) faltantes.push('Elegí una posición.');
    if (faltantes.length) {
        mostrarMensaje(mensaje, 'Faltan datos para anotarte:', 'error', faltantes);
        return;
    }

    $('boton-inscribir').disabled = true;
    try {
        const inscripto = await api('/inscripciones', { method: 'POST', body: JSON.stringify(datos) });
        ultimoInscriptoId = inscripto.inscripcionId;
        mostrarMensaje(mensaje, `Listo, ${inscripto.nombre}: quedaste anotado como ${POSICIONES[inscripto.posicion].toLowerCase()}.`, 'ok');
        form.reset();
        await refrescar();
    } catch (e) {
        mostrarMensaje(mensaje, e.message, 'error', e.detalles);
    } finally {
        $('boton-inscribir').disabled = torneo?.estado === 'SORTEADO'
            || torneo?.cantidadInscriptos >= torneo?.cupoMaximo;
    }
}

async function darDeBaja(id, nombre) {
    if (!confirm(`¿Quitar a ${nombre} del torneo de esta semana?`)) return;
    try {
        await api(`/inscripciones/${id}`, { method: 'DELETE' });
        mostrarMensaje($('mensaje-form'), `${nombre} ya no está anotado.`, 'ok');
        await refrescar();
    } catch (e) {
        mostrarMensaje($('mensaje-form'), e.message, 'error');
    }
}

// ---------------------------------------------------------------
//  Sorteo y equipos
// ---------------------------------------------------------------
async function sortear() {
    if (torneo?.estado === 'SORTEADO'
        && !confirm('Se van a reemplazar los equipos y el fixture por un sorteo nuevo. Se pierden los resultados cargados. ¿Seguir?')) {
        return;
    }
    const boton = $('boton-sortear');
    boton.disabled = true;
    try {
        const resultado = await api('/torneos/actual/sorteo', { method: 'POST' });
        const n = resultado.equipos.length;
        mostrarMensaje($('mensaje-admin'), `Sorteo hecho: ${plural(n, 'equipo', 'equipos')} y ${plural(n * (n - 1) / 2, 'partido', 'partidos')} en el fixture.`, 'ok');
        diaActivo = 'SABADO';
        await cargarTorneo();
        await Promise.all([cargarInscriptos(), cargarFixture(), cargarTabla()]);
        dibujarEquipos(resultado, true);
        $('equipos').scrollIntoView();
    } catch (e) {
        mostrarMensaje($('mensaje-admin'), e.message, 'error');
        boton.disabled = false;
    }
}

async function anularSorteo() {
    if (!confirm('Se borran los equipos, el fixture y los resultados, y se vuelven a abrir las inscripciones. ¿Seguir?')) return;
    try {
        await api('/torneos/actual/sorteo', { method: 'DELETE' });
        mostrarMensaje($('mensaje-admin'), 'Sorteo anulado. Las inscripciones están abiertas de nuevo.', 'ok');
        await refrescar();
    } catch (e) {
        mostrarMensaje($('mensaje-admin'), e.message, 'error');
    }
}

/** Borra todos los anotados de la semana con una sola llamada a la API. */
async function borrarTodos() {
    const n = torneo?.cantidadInscriptos ?? 0;
    const sorteado = torneo?.estado === 'SORTEADO';
    const pregunta = sorteado
        ? `Ya hay un sorteo hecho. Se van a borrar los equipos, el fixture, los resultados y los ${n} anotados. No se puede deshacer. ¿Seguir?`
        : `Se van a borrar los ${n} anotados de esta semana. No se puede deshacer. ¿Seguir?`;
    if (!confirm(pregunta)) return;
    try {
        if (sorteado) await api('/torneos/actual/sorteo', { method: 'DELETE' });
        const r = await api('/inscripciones', { method: 'DELETE' });
        mostrarMensaje($('mensaje-admin'), `Se borraron ${plural(r.borrados, 'anotado', 'anotados')}.`, 'ok');
        $('mensaje-form').textContent = '';
        ultimoInscriptoId = null;
        await refrescar();
    } catch (e) {
        mostrarMensaje($('mensaje-admin'), e.message, 'error');
    }
}

async function cargarDemo() {
    const cantidad = parseInt($('cantidad-demo').value, 10) || 23;
    try {
        const r = await api(`/admin/jugadores-demo?cantidad=${cantidad}`, { method: 'POST' });
        mostrarMensaje($('mensaje-admin'), `Se anotaron ${plural(r.cargados, 'jugador', 'jugadores')} de prueba.`, 'ok');
        await refrescar();
    } catch (e) {
        mostrarMensaje($('mensaje-admin'), e.message, 'error');
    }
}

async function cargarEquipos() {
    const resultado = await api('/torneos/actual/equipos');
    dibujarEquipos(resultado, false);
}

/** Dibuja cada equipo como una mini-cancha con sus titulares y el banco de suplentes. */
function dibujarEquipos(resultado, animar) {
    const { equipos, advertencias } = resultado;
    $('sin-equipos').hidden = equipos.length > 0;
    $('advertencias').innerHTML = advertencias.map(a => `<li>${escapar(a)}</li>`).join('');

    const grilla = $('grilla-equipos');
    grilla.classList.toggle('recien-sorteado', animar);
    let orden = 0; // para escalonar la animación de entrada

    const ficha = (j, esArquero) => `
        <div class="ficha ${esArquero ? 'ficha--arquero' : ''}" style="--i:${orden++}"
             title="${escapar(j.nombre + ' ' + j.apellido)} (${POSICIONES[j.posicion]})">
            <span class="ficha__camiseta" aria-hidden="true">${escapar(iniciales(j.nombre, j.apellido))}</span>
            <span class="ficha__nombre">${escapar(j.nombre)} ${escapar(j.apellido[0])}.</span>
        </div>`;

    grilla.innerHTML = equipos.map(eq => {
        const color = eq.nombre.replace('Equipo ', '');
        const bib = PECHERAS[color] || '#E3F23B';

        // El primer arquero titular va al arco; un segundo arquero (caso raro) juega en defensa.
        const titulares = [...eq.titulares];
        const iArquero = titulares.findIndex(j => j.posicion === 'ARQUERO');
        const arquero = iArquero >= 0 ? titulares.splice(iArquero, 1)[0] : null;
        const delanteros = titulares.filter(j => j.posicion === 'DELANTERO');
        const defensa = titulares.filter(j => j.posicion !== 'DELANTERO');

        const nombresTitulares = eq.titulares.map(j => `${j.nombre} ${j.apellido}`).join(', ');

        return `
        <article class="equipo" style="--bib:${bib}; --bib-tinta:${tintaSobre(bib)}">
            <header class="equipo__cabecera">
                <h3>${escapar(eq.nombre)}</h3>
                <p>${eq.suplentes.length ? plural(eq.suplentes.length, 'suplente', 'suplentes') : 'Sin suplentes'}</p>
            </header>
            <div class="cancha" role="img" aria-label="Titulares de ${escapar(eq.nombre)}: ${escapar(nombresTitulares)}">
                <div class="linea">${delanteros.map(j => ficha(j, false)).join('')}</div>
                <div class="linea">${defensa.map(j => ficha(j, false)).join('')}</div>
                <div class="linea">${arquero ? ficha(arquero, true) : '<span class="sin-arquero">Sin arquero</span>'}</div>
            </div>
            <div class="banco">
                <h4>Banco</h4>
                ${eq.suplentes.length
                    ? `<ul>${eq.suplentes.map(j => `<li>${escapar(j.nombre)} ${escapar(j.apellido)} <span class="etiqueta etiqueta--${j.posicion}">${POSICIONES[j.posicion]}</span></li>`).join('')}</ul>`
                    : '<p class="banco--vacio">Nadie en el banco.</p>'}
            </div>
        </article>`;
    }).join('');
}

// ---------------------------------------------------------------
//  Tabla de posiciones (3 puntos victoria, 1 empate, 0 derrota)
// ---------------------------------------------------------------
async function cargarTabla() {
    const tabla = await api('/torneos/actual/tabla');
    const hay = tabla.filas.length > 0;
    $('sin-tabla').hidden = hay;
    $('reglas-tabla').hidden = !hay;
    $('progreso-tabla').textContent = hay
        ? (tabla.finalizado ? 'Torneo terminado' : `Tras ${tabla.partidosJugados} de ${tabla.partidosTotales} partidos`)
        : '';

    // Aviso de campeón o de empate en el primer puesto
    let aviso = '';
    if (tabla.campeon) {
        const pts = tabla.filas[0].pts;
        aviso = `<p class="campeon" style="--bib:${colorEquipo(tabla.campeon.nombre)}">
                    <span class="campeon__titulo">Campeón: ${escapar(tabla.campeon.nombre)}</span>
                    <span>${plural(pts, 'punto', 'puntos')} en ${tabla.filas[0].pj} partidos.</span></p>`;
    } else if (tabla.empateEnPrimerPuesto) {
        const igualados = [tabla.filas[0]];
        for (let i = 1; i < tabla.filas.length && tabla.filas[i].empatadoConAnterior; i++) igualados.push(tabla.filas[i]);
        aviso = `<p class="campeon campeon--sorteo">
                    <span class="campeon__titulo">Primer puesto igualado</span>
                    <span>${igualados.map(f => escapar(f.equipo.nombre)).join(' y ')} terminaron iguales en puntos, diferencia de gol y goles a favor. El campeón se define por sorteo.</span></p>`;
    }
    $('campeon').innerHTML = aviso;

    if (!hay) { $('contenedor-tabla').innerHTML = ''; return; }
    const hayIgualdades = tabla.filas.some(f => f.empatadoConAnterior);
    const signo = (n) => n > 0 ? `+${n}` : String(n);

    $('contenedor-tabla').innerHTML = `
        <table class="tabla-pos">
            <thead><tr>
                <th scope="col" class="num">#</th>
                <th scope="col" class="tabla-pos__equipo">Equipo</th>
                <th scope="col" class="num"><abbr title="Partidos jugados">PJ</abbr></th>
                <th scope="col" class="num"><abbr title="Ganados">PG</abbr></th>
                <th scope="col" class="num"><abbr title="Empatados">PE</abbr></th>
                <th scope="col" class="num"><abbr title="Perdidos">PP</abbr></th>
                <th scope="col" class="num opcional"><abbr title="Goles a favor">GF</abbr></th>
                <th scope="col" class="num opcional"><abbr title="Goles en contra">GC</abbr></th>
                <th scope="col" class="num"><abbr title="Diferencia de gol">DG</abbr></th>
                <th scope="col" class="num tabla-pos__pts"><abbr title="Puntos">Pts</abbr></th>
            </tr></thead>
            <tbody>
            ${tabla.filas.map(f => `
                <tr class="${f.posicion === 1 && tabla.partidosJugados > 0 ? 'lider' : ''}">
                    <td class="num">${f.posicion}${f.empatadoConAnterior ? '<sup title="Igualado con el de arriba: se define por sorteo">*</sup>' : ''}</td>
                    <th scope="row" class="tabla-pos__equipo">${chipEquipo(f.equipo)}</th>
                    <td class="num">${f.pj}</td>
                    <td class="num">${f.pg}</td>
                    <td class="num">${f.pe}</td>
                    <td class="num">${f.pp}</td>
                    <td class="num opcional">${f.gf}</td>
                    <td class="num opcional">${f.gc}</td>
                    <td class="num">${signo(f.dg)}</td>
                    <td class="num tabla-pos__pts">${f.pts}</td>
                </tr>`).join('')}
            </tbody>
        </table>
        ${hayIgualdades ? '<p class="nota">* Igualado en puntos, diferencia de gol y goles a favor con el equipo de arriba: ese orden se define por sorteo.</p>' : ''}`;
}

// ---------------------------------------------------------------
//  Fixture y carga de resultados
// ---------------------------------------------------------------
async function cargarFixture() {
    fixture = await api('/torneos/actual/fixture');
    dibujarFixture();
}

function dibujarFixture() {
    const hay = fixture.jornadas.length > 0;
    $('fixture-contenido').hidden = !hay;
    $('sin-fixture').hidden = hay;
    $('progreso-fixture').textContent = hay
        ? `${fixture.partidosJugados} de ${fixture.partidosTotales} partidos jugados` : '';
    if (!hay) return;

    // Pestañas Sábado / Domingo (con pocos equipos puede que el domingo no tenga partidos)
    const porDia = { SABADO: [], DOMINGO: [] };
    fixture.jornadas.forEach(j => porDia[j.dia].push(j));
    if (porDia[diaActivo].length === 0) diaActivo = 'SABADO';

    $('pestanas-dias').innerHTML = ['SABADO', 'DOMINGO'].filter(d => porDia[d].length).map(d => {
        const fecha = d === 'SABADO' ? fixture.fechaSabado : fixture.fechaDomingo;
        const partidos = porDia[d].flatMap(j => j.partidos);
        const jugados = partidos.filter(p => p.jugado).length;
        return `<button type="button" role="tab" class="pestana" data-dia="${d}"
                    aria-selected="${d === diaActivo}" tabindex="${d === diaActivo ? 0 : -1}">
                    <span class="pestana__dia">${DIAS[d]} ${formatearDiaMes(fecha)}</span>
                    <span class="pestana__detalle">${plural(porDia[d].length, 'fecha', 'fechas')}, ${jugados} de ${partidos.length} jugados</span>
                </button>`;
    }).join('');

    $('panel-fixture').innerHTML = porDia[diaActivo].map(j => `
        <section class="jornada" aria-label="Fecha ${j.numero}">
            <h3>Fecha ${j.numero}</h3>
            ${j.partidos.map(dibujarPartido).join('')}
            ${j.libre ? `<p class="jornada__libre">Libre: ${chipEquipo(j.libre)}</p>` : ''}
        </section>`).join('');
}

function dibujarPartido(p) {
    const gl = p.golesLocal ?? '';
    const gv = p.golesVisitante ?? '';
    const ganaLocal = p.jugado && p.golesLocal > p.golesVisitante;
    const ganaVisitante = p.jugado && p.golesVisitante > p.golesLocal;
    return `
        <form class="partido ${p.jugado ? 'partido--jugado' : ''}" data-id="${p.id}" novalidate
              aria-label="${escapar(p.local.nombre)} contra ${escapar(p.visitante.nombre)}">
            <span class="partido__local ${ganaLocal ? 'gana' : ''}">${escapar(p.local.nombre.replace('Equipo ', ''))}<span class="punto" style="--bib:${colorEquipo(p.local.nombre)}" aria-hidden="true"></span></span>
            <input class="partido__gl" name="golesLocal" type="number" min="0" max="99" inputmode="numeric"
                   value="${gl}" aria-label="Goles de ${escapar(p.local.nombre)}">
            <span class="partido__guion" aria-hidden="true">–</span>
            <input class="partido__gv" name="golesVisitante" type="number" min="0" max="99" inputmode="numeric"
                   value="${gv}" aria-label="Goles de ${escapar(p.visitante.nombre)}">
            <span class="partido__visitante ${ganaVisitante ? 'gana' : ''}">${chipEquipo(p.visitante, true)}</span>
            <span class="partido__acciones">
                <button type="submit" class="boton-chico">Guardar</button>
                ${p.jugado ? `<button type="button" class="boton-baja" data-borrar="${p.id}">Borrar</button>` : ''}
            </span>
        </form>`;
}

async function guardarResultado(form) {
    const mensaje = $('mensaje-fixture');
    const gl = form.golesLocal.value.trim();
    const gv = form.golesVisitante.value.trim();
    const valido = (v) => /^\d{1,2}$/.test(v);
    if (!valido(gl) || !valido(gv)) {
        mostrarMensaje(mensaje, 'Escribí los goles de los dos equipos (números de 0 a 99).', 'error');
        (valido(gl) ? form.golesVisitante : form.golesLocal).focus();
        return;
    }
    const boton = form.querySelector('[type="submit"]');
    boton.disabled = true;
    try {
        const p = await api(`/partidos/${form.dataset.id}/resultado`, {
            method: 'PUT',
            body: JSON.stringify({ golesLocal: Number(gl), golesVisitante: Number(gv) })
        });
        mostrarMensaje(mensaje, `Resultado guardado: ${p.local.nombre} ${p.golesLocal}, ${p.visitante.nombre} ${p.golesVisitante}.`, 'ok');
        await Promise.all([cargarFixture(), cargarTabla()]);
    } catch (e) {
        mostrarMensaje(mensaje, e.message, 'error', e.detalles);
        boton.disabled = false;
    }
}

async function borrarResultado(id) {
    try {
        const p = await api(`/partidos/${id}/resultado`, { method: 'DELETE' });
        mostrarMensaje($('mensaje-fixture'), `Se borró el resultado de ${p.local.nombre} contra ${p.visitante.nombre}.`, 'ok');
        await Promise.all([cargarFixture(), cargarTabla()]);
    } catch (e) {
        mostrarMensaje($('mensaje-fixture'), e.message, 'error');
    }
}

function cambiarDia(dia) {
    diaActivo = dia;
    dibujarFixture();
    document.querySelector(`.pestana[data-dia="${dia}"]`)?.focus();
}

// ---------------------------------------------------------------
//  Arranque
// ---------------------------------------------------------------
/**
 * Confirma que el servidor levantado es la misma versión que esta página.
 * Si no (por ejemplo quedó corriendo una copia vieja del proyecto), lo avisa arriba de todo.
 */
async function verificarVersion() {
    let version = null;
    try {
        version = (await api('/version')).version;
    } catch (e) {
        if (e.status === undefined) return;   // sin conexión: ya lo muestra el otro aviso
    }
    const aviso = $('aviso-version');
    aviso.hidden = version === VERSION_ESPERADA;
    if (!aviso.hidden) {
        aviso.innerHTML = `El servidor que está corriendo es ${version ? 'la versión ' + escapar(version) : 'una versión anterior'} del proyecto y esta página es la ${VERSION_ESPERADA}.
            Por eso algunas funciones (borrar anotados, fixture, tabla) no responden.
            Detené el servidor y levantalo desde la carpeta de la versión nueva con <code>mvn clean package</code> y <code>mvn spring-boot:run</code>.`;
    }
}

async function refrescar() {
    try {
        await cargarTorneo();
    } catch (e) {
        console.error(e);
        return;
    }
    // allSettled: si una sección falla, las demás se muestran igual
    const resultados = await Promise.allSettled([cargarInscriptos(), cargarEquipos(), cargarFixture(), cargarTabla()]);
    const fallo = resultados.find(r => r.status === 'rejected');
    if (fallo) {
        console.error(fallo.reason);
        mostrarMensaje($('mensaje-admin'), `No se pudo cargar toda la página: ${fallo.reason.message}`, 'error');
    }
}

document.addEventListener('DOMContentLoaded', () => {
    $('form-inscripcion').addEventListener('submit', inscribir);
    $('boton-sortear').addEventListener('click', sortear);
    $('boton-anular').addEventListener('click', anularSorteo);
    $('boton-demo').addEventListener('click', cargarDemo);
    $('boton-borrar-todos').addEventListener('click', borrarTodos);

    // Delegación de eventos: un solo listener para todos los botones "Quitar"
    $('tabla-inscriptos').addEventListener('click', (e) => {
        const boton = e.target.closest('.boton-baja');
        if (boton) darDeBaja(boton.dataset.id, boton.dataset.nombre);
    });

    // Fixture: guardar con el botón o con Enter dentro de los goles
    $('panel-fixture').addEventListener('submit', (e) => {
        e.preventDefault();
        guardarResultado(e.target);
    });
    $('panel-fixture').addEventListener('click', (e) => {
        const boton = e.target.closest('[data-borrar]');
        if (boton) borrarResultado(boton.dataset.borrar);
    });

    // Pestañas Sábado / Domingo (clic y flechas del teclado)
    $('pestanas-dias').addEventListener('click', (e) => {
        const pestana = e.target.closest('.pestana');
        if (pestana) cambiarDia(pestana.dataset.dia);
    });
    $('pestanas-dias').addEventListener('keydown', (e) => {
        if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return;
        const otras = [...document.querySelectorAll('.pestana')].map(b => b.dataset.dia);
        const i = otras.indexOf(diaActivo);
        const siguiente = otras[(i + (e.key === 'ArrowRight' ? 1 : otras.length - 1)) % otras.length];
        if (siguiente && siguiente !== diaActivo) cambiarDia(siguiente);
    });

    verificarVersion();
    refrescar();
});
