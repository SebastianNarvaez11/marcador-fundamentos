package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CancelacionTest {
    @Test
    fun elRelojSeParaAlPitarElFinal() = runBlocking {
        val minutos = mutableListOf<Int>()
        val final = jugarConCronometro(Ejemplo.rayoContraToros, msPorMinuto = 1) { minutos += it }
        assertEquals(2, final.golesLocal)
        val alPitar = minutos.size
        assertTrue(alPitar > 0)
        delay(30)
        assertEquals(alPitar, minutos.size, "El reloj siguió corriendo tras el final")
    }

    @Test
    fun cancelarElTransmisorCancelaTodosSusPartidos() = runBlocking {
        val transmisor = Transmisor()
        val terminados = mutableListOf<Partido>()
        val trabajos = listOf(Ejemplo.rayoContraToros, Ejemplo.lobosContraAguilas).map {
            transmisor.transmitir(it, msPorMinuto = 20) { partido -> terminados += partido }
        }
        delay(100)
        transmisor.pitarElFinal()
        trabajos.forEach { it.join() }
        assertTrue(trabajos.all { it.isCancelled })
        assertTrue(terminados.isEmpty())
        assertFalse(transmisor.activo)
    }

    @Test
    fun unBucleDeCalculoSeCancelaPorqueMiraEnsureActive() = runBlocking {
        val calculo = launch(Dispatchers.Default) { probabilidadDeVictoria(Ejemplo.rayoContraToros, simulaciones = Int.MAX_VALUE) }
        delay(50)
        val ms = measureTimeMillis { calculo.cancelAndJoin() }
        assertTrue(ms < 1_000, "Tardó $ms ms en cancelarse")
        assertTrue(calculo.isCancelled)
    }

    @Test
    fun laProbabilidadEstaEntreCeroYUno() = runBlocking {
        val p = probabilidadDeVictoria(Ejemplo.rayoContraToros, simulaciones = 10_000)
        assertTrue(p in 0.0..1.0)
    }

    @Test
    fun intentarDevuelveElErrorComoValor() = runBlocking {
        val resultado = intentar { error("boom") }
        assertEquals("boom", resultado.exceptionOrNull()?.message)
        assertEquals(7, intentar { 7 }.getOrNull())
    }

    @Test
    fun intentarNoTragaLaCancelacion() = runBlocking {
        val alcanzado = CompletableDeferred<Boolean>()
        val trabajo = launch {
            intentar { delay(10_000) }
            alcanzado.complete(true) // solo se llegaría aquí si se tragara la cancelación
        }
        delay(20)
        trabajo.cancelAndJoin()
        assertFalse(alcanzado.isCompleted)
        assertTrue(trabajo.isCancelled)
    }
}
