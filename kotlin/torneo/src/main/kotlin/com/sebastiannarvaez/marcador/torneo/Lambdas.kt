package com.sebastiannarvaez.marcador.torneo

// Una lambda guardada en una `val`, con su tipo función explícito.
// Con un solo parámetro, la lambda lo llama `it` si no se le da nombre.
val esGol: (EventoDePartido) -> Boolean = { it is Gol }

// Con nombre de parámetro explícito y varios parámetros.
val esDelMinutoOAntes: (EventoDePartido, Int) -> Boolean = { evento, limite -> evento.minuto <= limite }

// Función de orden superior propia: ejecuta `accion` tantas veces como se pida.
fun repetir(veces: Int, accion: (Int) -> Unit) {
    for (i in 1..veces) accion(i)
}

// Una función puede DEVOLVER otra. El resultado «recuerda» `minuto` (captura).
fun antesDelMinuto(minuto: Int): (EventoDePartido) -> Boolean = { it.minuto < minuto }
