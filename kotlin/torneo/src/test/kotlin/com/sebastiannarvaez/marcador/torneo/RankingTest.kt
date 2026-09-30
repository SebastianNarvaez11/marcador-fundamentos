package com.sebastiannarvaez.marcador.torneo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RankingTest {
    // El mismo Ranking sirve para Strings puntuados por su longitud…
    private val palabras = Ranking(listOf("gol", "penalti", "fuera", "tarjeta"), puntos = { it.length })

    @Test
    fun ordenaDeMasAMenosPuntos() {
        // penalti y tarjeta empatan a 7: sin desempate, conservan el orden de entrada.
        assertEquals(listOf("penalti", "tarjeta", "fuera", "gol"), palabras.ordenados)
    }

    @Test
    fun elDesempateOrdenaLosEmpatados() {
        val ranking = Ranking(
            elementos = listOf("penalti", "tarjeta"),
            puntos = { it.length },
            desempate = compareBy { it },
        )
        assertEquals(listOf("penalti", "tarjeta"), ranking.ordenados)
    }

    @Test
    fun losEmpatadosCompartenPuesto() {
        assertEquals(1, palabras.puesto("penalti"))
        assertEquals(1, palabras.puesto("tarjeta"))
        assertEquals(3, palabras.puesto("fuera"))
        assertEquals(4, palabras.puesto("gol"))
    }

    @Test
    fun puestoDeUnElementoAusenteEsNull() = assertNull(palabras.puesto("corner"))

    @Test
    fun topCortaLaLista() = assertEquals(2, palabras.top(2).size)

    // …y para Ints puntuados por sí mismos: T se decide en cada uso.
    @Test
    fun sirveParaCualquierTipo() {
        val numeros = Ranking(listOf(3, 9, 5), puntos = { it })
        assertEquals(listOf(9, 5, 3), numeros.toList())
        assertEquals(9, numeros.puntosDe(9))
    }
}
