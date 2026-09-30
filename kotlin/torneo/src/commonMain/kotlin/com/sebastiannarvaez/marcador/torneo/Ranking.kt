package com.sebastiannarvaez.marcador.torneo

// typealias: un nombre nuevo para un tipo que ya existe. No crea un tipo distinto.
typealias Desempate<T> = Comparator<T>

// `<T>` es un parámetro de tipo: el Ranking sirve igual para Jugador (goleadores)
// que para FilaDePosicion (tabla), y el compilador sabe en cada caso qué es T.
// Al usarlo se escribe Ranking<Jugador>: el tipo real sustituye a T.
class Ranking<T>(
    elementos: Collection<T>,
    // Cómo se puntúa un elemento: cada uso lo decide (goles, puntos de la liga…).
    private val puntos: (T) -> Int,
    desempate: Desempate<T> = Comparator { _, _ -> 0 },
) : Iterable<T> {

    // De más a menos puntos y, a igualdad, según el desempate.
    val ordenados: List<T> = elementos.sortedWith(
        compareByDescending<T> { puntos(it) }.then(desempate),
    )

    fun puntosDe(elemento: T): Int = puntos(elemento)

    // Puesto con empates: dos con los mismos puntos comparten puesto (1, 1, 3…).
    // Devuelve null si el elemento no está en el ranking.
    fun puesto(elemento: T): Int? {
        if (elemento !in ordenados) return null
        val suyos = puntos(elemento)
        return ordenados.indexOfFirst { puntos(it) == suyos } + 1
    }

    fun top(cantidad: Int): List<T> = ordenados.take(cantidad)

    override fun iterator(): Iterator<T> = ordenados.iterator()
}
