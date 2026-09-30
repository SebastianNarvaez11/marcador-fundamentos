package com.sebastiannarvaez.marcador.torneo

// `enum class`: un conjunto cerrado de valores, todos con el mismo aspecto.
// Cada constante puede llevar datos (aquí, la etiqueta para mostrar).
enum class ColorDeTarjeta(val etiqueta: String) {
    AMARILLA("Amarilla"),
    ROJA("Roja"),
}

// `sealed interface`: los únicos implementadores posibles están en este mismo
// paquete y módulo. Por eso el compilador sabe cuáles son TODOS y puede
// exigir que un `when` los cubra sin `else`.
// A diferencia de un enum, cada variante puede llevar datos distintos.
sealed interface EventoDePartido {
    val minuto: Int
}

data class Gol(
    override val minuto: Int,
    val jugador: Jugador,
    val equipo: Equipo,
) : EventoDePartido

data class Tarjeta(
    override val minuto: Int,
    val jugador: Jugador,
    val color: ColorDeTarjeta,
) : EventoDePartido

data class Cambio(
    override val minuto: Int,
    val sale: Jugador,
    val entra: Jugador,
) : EventoDePartido

// `when` como expresión sobre un tipo sellado: sin `else`. Si mañana se añade
// una variante nueva, esta función deja de compilar y avisa de dónde falta.
fun describir(evento: EventoDePartido): String = when (evento) {
    is Gol -> "${evento.minuto}' Gol de ${evento.jugador.nombre} (${evento.equipo.nombre})"
    is Tarjeta -> "${evento.minuto}' Tarjeta ${evento.color.etiqueta.lowercase()} para ${evento.jugador.nombre}"
    is Cambio -> "${evento.minuto}' Cambio: sale ${evento.sale.nombre}, entra ${evento.entra.nombre}"
}
