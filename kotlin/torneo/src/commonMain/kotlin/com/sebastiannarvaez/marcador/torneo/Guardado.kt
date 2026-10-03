package com.sebastiannarvaez.marcador.torneo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// El JSON no habla de objetos con referencias (un Gol apunta a un Equipo), sino
// de árboles de datos. Por eso el fichero usa unas clases «guardadas» aparte,
// donde cada equipo o jugador se nombra por su nombre, y el dominio se
// reconstruye al cargar. @Serializable le pide al plugin que genere el código
// de conversión de cada clase.

@Serializable
data class JugadorGuardado(val nombre: String, val dorsal: Int? = null)

@Serializable
data class EquipoGuardado(
    val nombre: String,
    val plantilla: List<JugadorGuardado>,
    val capitan: String? = null,
)

// Una interfaz sellada serializable: cada variante se identifica en el JSON
// con el campo «tipo», cuyo valor sale de @SerialName.
@Serializable
sealed interface EventoGuardado {
    val minuto: Int
}

@Serializable
@SerialName("gol")
data class GolGuardado(override val minuto: Int, val jugador: String, val equipo: String) : EventoGuardado

@Serializable
@SerialName("tarjeta")
data class TarjetaGuardada(override val minuto: Int, val jugador: String, val color: ColorDeTarjeta) : EventoGuardado

@Serializable
@SerialName("cambio")
data class CambioGuardado(override val minuto: Int, val sale: String, val entra: String) : EventoGuardado

@Serializable
data class PartidoGuardado(val local: String, val visitante: String, val eventos: List<EventoGuardado>)

@Serializable
data class TorneoGuardado(
    val nombre: String,
    val equipos: List<EquipoGuardado>,
    val partidos: List<PartidoGuardado>,
)

// ColorDeTarjeta viaja como texto («AMARILLA», «ROJA»): basta con anotar el
// enum con @Serializable en EventoDePartido.kt.

private val json = Json {
    // Sangrías y saltos de línea, para poder leer el fichero a ojo.
    prettyPrint = true
    // El campo que distingue las variantes de EventoGuardado.
    classDiscriminator = "tipo"
    // Al leer, ignora campos que no conozca en vez de fallar (versiones futuras).
    ignoreUnknownKeys = true
}

// ---- Dominio -> guardado ----

private fun Equipo.aGuardado() = EquipoGuardado(
    nombre = nombre,
    plantilla = plantilla.map { JugadorGuardado(it.nombre, it.dorsal) },
    capitan = capitan?.nombre,
)

// `when` exhaustivo sobre el evento del dominio: si aparece uno nuevo, esto no compila.
private fun EventoDePartido.aGuardado(): EventoGuardado = when (this) {
    is Gol -> GolGuardado(minuto, jugador.nombre, equipo.nombre)
    is Tarjeta -> TarjetaGuardada(minuto, jugador.nombre, color)
    is Cambio -> CambioGuardado(minuto, sale.nombre, entra.nombre)
}

fun Torneo.aJson(): String = json.encodeToString(
    TorneoGuardado(
        nombre = nombre,
        equipos = equipos.map { it.aGuardado() },
        partidos = partidos.map { partido ->
            PartidoGuardado(partido.local.nombre, partido.visitante.nombre, partido.eventos.map { it.aGuardado() })
        },
    ),
)

// ---- Guardado -> dominio ----

private fun EquipoGuardado.aEquipo(): Equipo {
    val jugadores = plantilla.map { Jugador(it.nombre, it.dorsal) }
    // Se guarda en una `val` con otro nombre: dentro de `apply`, `this` es el
    // Equipo nuevo, y su `capitan` (un Jugador?) taparía al `capitan` de EquipoGuardado (un String?).
    val nombreDelCapitan = capitan
    return Equipo(nombre, jugadores).apply {
        nombreDelCapitan?.let { buscado -> nombrarCapitan(jugadores.first { it.nombre == buscado }) }
    }
}

// Las referencias por nombre se resuelven aquí: si falta un equipo o un jugador,
// `first` o `getValue` lanzan y el Result de `torneoDesdeJson` lo cuenta.
private fun PartidoGuardado.aPartido(equiposPorNombre: Map<String, Equipo>): Partido {
    val local = equiposPorNombre.getValue(local)
    val visitante = equiposPorNombre.getValue(visitante)
    val jugadores = local.plantilla + visitante.plantilla
    fun jugador(nombre: String): Jugador = jugadores.first { it.nombre == nombre }

    return Partido(
        local = local,
        visitante = visitante,
        eventos = eventos.map { evento ->
            when (evento) {
                is GolGuardado -> Gol(evento.minuto, jugador(evento.jugador), equiposPorNombre.getValue(evento.equipo))
                is TarjetaGuardada -> Tarjeta(evento.minuto, jugador(evento.jugador), evento.color)
                is CambioGuardado -> Cambio(evento.minuto, jugador(evento.sale), jugador(evento.entra))
            }
        },
    )
}

// Leer un JSON puede fallar de muchas formas (texto roto, equipo que no existe…):
// el resultado es un Result, no una excepción suelta.
fun torneoDesdeJson(texto: String): Result<Torneo> = runCatching {
    val guardado = json.decodeFromString<TorneoGuardado>(texto)
    val equipos = guardado.equipos.map { it.aEquipo() }
    val porNombre = equipos.associateBy { it.nombre }
    Torneo(guardado.nombre, equipos).apply {
        guardado.partidos.forEach { registrar(it.aPartido(porNombre)).getOrThrow() }
    }
}

// Antes recibian un java.io.File; ahora una ruta y `leerTexto`/`escribirTexto` (Ficheros.kt).
fun Torneo.guardar(ruta: String): Result<Unit> = runCatching { escribirTexto(ruta, aJson()) }

fun cargarTorneo(ruta: String): Result<Torneo> = runCatching { leerTexto(ruta) }.mapCatching {
    torneoDesdeJson(it).getOrThrow()
}
