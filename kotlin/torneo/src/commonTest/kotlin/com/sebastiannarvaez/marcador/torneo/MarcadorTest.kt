package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class MarcadorTest {
    private val partidoUno = PartidoEnVivo(Ejemplo.rayoContraToros, msPorMinuto = 1)
    private val partidoDos = PartidoEnVivo(Ejemplo.lobosContraAguilas, msPorMinuto = 1)

    @Test
    fun elMarcadorSoloCambiaConLosGoles() = runBlocking {
        assertEquals(
            listOf("0-0", "1-0", "1-1", "2-1"),
            partidoUno.marcadores().toList().map { it.toString() },
        )
    }

    @Test
    fun elMarcadorFinalCoincideConElDelPartido() = runBlocking {
        assertEquals(Ejemplo.lobosContraAguilas.marcador(), partidoDos.marcadores().toList().last())
    }

    @Test
    fun laJornadaCombinaLosDosMarcadores() = runBlocking {
        val ultimo = marcadoresDeLaJornada(partidoUno, partidoDos).toList().last()
        assertEquals(MarcadoresDeJornada(Marcador(2, 1), Marcador(1, 2)), ultimo)
        assertEquals(6, ultimo.golesTotales)
    }

    @Test
    fun laJornadaEmiteAlPrincipioConLosDosACero() = runBlocking {
        assertEquals(MarcadoresDeJornada(Marcador(0, 0), Marcador(0, 0)), marcadoresDeLaJornada(partidoUno, partidoDos).first())
    }

    @Test
    fun flatMapLatestDejaDeSeguirAlPartidoAnterior() = runBlocking {
        val elegidos = flow {
            emit(PartidoEnVivo(Ejemplo.rayoContraToros, msPorMinuto = 5)) // el gol del 12 llegaría a los ~60 ms
            delay(30)                                                    // cambia de partido antes de verlo
            emit(PartidoEnVivo(Ejemplo.lobosContraAguilas, msPorMinuto = 1))
        }
        val marcadores = seguirPartido(elegidos).toList()
        // 0-0 del primero, 0-0 del segundo y los goles del segundo: nunca el 1-0 del primero.
        assertEquals(listOf("0-0", "0-0", "1-0", "1-1", "1-2"), marcadores.map { it.toString() })
    }

    @Test
    fun debounceSoloBuscaLoUltimoQueSeEscribio() = runBlocking {
        val teclado = flow {
            for (texto in listOf("r", "ra", "ray")) { emit(texto); delay(10) }
            delay(200)
        }
        val busquedas = buscarEquipos(teclado, listOf(Ejemplo.rayo, Ejemplo.toros), pausaMs = 100).toList()
        assertEquals(listOf(listOf(Ejemplo.rayo)), busquedas)
    }
}
