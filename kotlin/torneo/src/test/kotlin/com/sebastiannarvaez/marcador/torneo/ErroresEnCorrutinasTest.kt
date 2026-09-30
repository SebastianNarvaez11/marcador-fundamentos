package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ErroresEnCorrutinasTest {
    private val roto = Ejemplo.lobosContraAguilas
    private val sano = Ejemplo.rayoContraToros

    private suspend fun jugarSalvoElRoto(partido: Partido): Partido =
        if (partido === roto) jugarConApagon(partido, msPorMinuto = 1, minutoDelApagon = 30)
        else jugarPartido(partido, msPorMinuto = 1)

    @Test
    fun unPartidoQueFallaNoTumbaAlOtro() = runBlocking {
        val resultados = jugarJornadaSupervisada(listOf(sano, roto), ::jugarSalvoElRoto)
        assertTrue(resultados[0].isSuccess)
        assertEquals(2, resultados[0].getOrThrow().golesLocal)
        val error = resultados[1].exceptionOrNull()
        assertIs<SuspendidoPorApagon>(error)
        assertEquals(30, error.minuto)
    }

    @Test
    fun conCoroutineScopeElFalloSiTumbaALaHermana() = runBlocking {
        lateinit var hermana: Deferred<Partido>
        assertFailsWith<SuspendidoPorApagon> {
            coroutineScope {
                hermana = async { jugarPartido(sano, msPorMinuto = 5) }
                async { jugarConApagon(roto, msPorMinuto = 5, minutoDelApagon = 10) }.await()
            }
        }
        assertTrue(hermana.isCancelled)
    }

    @Test
    fun unTryCatchAlrededorDeLaunchNoAtrapaElError() = runBlocking {
        val errores = CopyOnWriteArrayList<Throwable>()
        val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e -> errores += e })
        var atrapadoAqui = false
        try {
            alcance.launch { jugarConApagon(roto, msPorMinuto = 1, minutoDelApagon = 5) }.join()
        } catch (e: Exception) {
            atrapadoAqui = true
        }
        // `launch` no relanza en quien lo llama: el error va al handler.
        assertTrue(!atrapadoAqui)
        assertEquals(1, errores.size)
        assertIs<SuspendidoPorApagon>(errores.single())
    }

    @Test
    fun elTransmisorSigueConLosDemasCuandoUnPartidoFalla() = runBlocking {
        val fallos = CopyOnWriteArrayList<Throwable>()
        val terminados = CopyOnWriteArrayList<Partido>()
        val transmisor = Transmisor(alFallar = { fallos += it }, jugar = { p, ms ->
            if (p === roto) jugarConApagon(p, ms, minutoDelApagon = 20) else jugarPartido(p, ms)
        })
        val trabajos = listOf(roto, sano).map { transmisor.transmitir(it, msPorMinuto = 2) { p -> terminados += p } }
        trabajos.forEach { it.join() }
        assertEquals(1, fallos.size)
        assertEquals(listOf(sano.local), terminados.map { it.local })
        assertTrue(transmisor.activo, "El fallo de un partido no debe cerrar el transmisor")
        transmisor.pitarElFinal()
    }

    @Test
    fun cancelarLaJornadaSupervisadaNoSeConvierteEnResultado() = runBlocking {
        val trabajo = launch { jugarJornadaSupervisada(listOf(sano, roto)) { delay(10_000); it } }
        delay(20)
        trabajo.cancelAndJoin()
        assertTrue(trabajo.isCancelled)
    }
}
