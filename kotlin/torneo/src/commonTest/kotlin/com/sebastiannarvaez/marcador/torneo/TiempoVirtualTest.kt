package com.sebastiannarvaez.marcador.torneo

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.time.measureTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TiempoVirtualTest {
    private val guion = Ejemplo.rayoContraToros
    private val minuto = 60_000L // un minuto de juego = un minuto REAL

    @Test
    fun unPartidoDe90MinutosSeJuegaEnMilisegundos() = runTest {
        val real = measureTime {
            val final = jugarPartido(guion, msPorMinuto = minuto, dispatcher = StandardTestDispatcher(testScheduler))
            assertEquals(2, final.golesLocal)
            assertEquals(1, final.golesVisitante)
        }.inWholeMilliseconds
        assertEquals(90 * minuto, currentTime, "El reloj virtual debería marcar 90 minutos")
        assertTrue(real < 2_000, "Tardó $real ms de reloj real")
    }

    @Test
    fun elDispatcherEstandarSoloAvanzaCuandoSeLeDice() = runTest {
        val enVivo = PartidoEnVivo(guion, minuto, StandardTestDispatcher(testScheduler))
        val vistos = mutableListOf<EventoDePartido>()
        backgroundScope.launch { enVivo.eventos().toList(vistos) }

        // La corrutina se lanzó, pero un StandardTestDispatcher no la ejecuta hasta
        // que el test suspende o llama a runCurrent().
        assertEquals(0, vistos.size)

        advanceTimeBy(12 * minuto - 1)  // un milisegundo antes del gol del 12
        runCurrent()
        assertEquals(0, vistos.size)

        advanceTimeBy(1)                // en el 12: gol
        runCurrent()
        assertEquals(listOf(12), vistos.map { it.minuto })

        // Hasta el final. Ojo: aquí NO vale advanceUntilIdle(): lo que lanza el
        // backgroundScope es trabajo «de fondo» y advanceUntilIdle() no espera por él.
        advanceTimeBy(90 * minuto)
        runCurrent()
        assertEquals(5, vistos.size)
    }

    @Test
    fun elDispatcherUnconfinedEjecutaSinEsperar() = runTest {
        val enVivo = PartidoEnVivo(guion, minuto, StandardTestDispatcher(testScheduler))
        val marcadores = mutableListOf<Marcador>()
        // Unconfined: la corrutina corre YA, sin esperar a runCurrent().
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { enVivo.marcador.toList(marcadores) }
        assertEquals(listOf(Marcador(0, 0)), marcadores)
        enVivo.iniciar(backgroundScope)
        advanceTimeBy(90 * minuto)
        runCurrent()
        assertEquals(listOf("0-0", "1-0", "1-1", "2-1"), marcadores.map { it.toString() })
    }

    @Test
    fun turbineProbandoElFlowFrioEventoAEvento() = runTest {
        PartidoEnVivo(guion, minuto, StandardTestDispatcher(testScheduler)).eventos().test {
            for (esperado in guion.eventos) assertEquals(esperado, awaitItem())
            awaitComplete()
        }
        assertEquals(90 * minuto, currentTime)
    }

    @Test
    fun turbineProbandoElStateFlow() = runTest {
        val enVivo = PartidoEnVivo(guion, minuto, StandardTestDispatcher(testScheduler))
        enVivo.marcador.test {
            assertEquals(Marcador(0, 0), awaitItem())   // el valor actual llega primero
            enVivo.iniciar(backgroundScope)
            assertEquals(Marcador(1, 0), awaitItem())
            assertEquals(Marcador(1, 1), awaitItem())
            assertEquals(Marcador(2, 1), awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun turbineProbandoLosMarcadores() = runTest {
        PartidoEnVivo(guion, minuto, StandardTestDispatcher(testScheduler)).marcadores().test {
            assertEquals(Marcador(0, 0), awaitItem())
            assertEquals(Marcador(1, 0), awaitItem())
            assertEquals(12 * minuto, currentTime)
            assertEquals(Marcador(1, 1), awaitItem())
            assertEquals(Marcador(2, 1), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun turbineProbandoElSharedFlowDeGoles() = runTest {
        val enVivo = PartidoEnVivo(guion, minuto, StandardTestDispatcher(testScheduler))
        enVivo.goles.test {
            enVivo.iniciar(backgroundScope)
            assertEquals(listOf(12, 55, 80), List(3) { awaitItem().minuto })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun laJornadaConWhileSubscribedEnTiempoVirtual() = runTest {
        val despachador = StandardTestDispatcher(testScheduler)
        val jornada = Jornada(
            PartidoEnVivo(guion, minuto, despachador),
            PartidoEnVivo(Ejemplo.lobosContraAguilas, minuto, despachador),
            backgroundScope,
        )
        jornada.marcadores.test {
            assertEquals(MarcadoresDeJornada(Marcador(0, 0), Marcador(0, 0)), awaitItem())
            jornada.iniciar()
            var ultimo = awaitItem()
            while (ultimo.golesTotales < 6) ultimo = awaitItem()
            assertEquals(MarcadoresDeJornada(Marcador(2, 1), Marcador(1, 2)), ultimo)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun elRelojSeParaAlPitarElFinalEnTiempoVirtual() = runTest {
        val minutos = mutableListOf<Int>()
        val final = jugarConCronometro(guion, minuto, StandardTestDispatcher(testScheduler)) { minutos += it }
        assertEquals(2, final.golesLocal)
        val alPitar = minutos.size
        assertTrue(alPitar in 89..90, "El reloj marcó $alPitar minutos")
        delay(10 * minuto)
        assertEquals(alPitar, minutos.size, "El reloj siguió tras el final")
    }

    @Test
    fun elTransmisorSeCancelaAMitadDelPartidoConElRelojVirtual() = runTest {
        val terminados = mutableListOf<Partido>()
        val transmisor = Transmisor(dispatcher = StandardTestDispatcher(testScheduler))
        val trabajo: Job = transmisor.transmitir(guion, minuto) { terminados += it }
        advanceTimeBy(45 * minuto)
        runCurrent()
        transmisor.pitarElFinal()
        trabajo.cancelAndJoin()
        assertTrue(terminados.isEmpty())
        assertEquals(45 * minuto, currentTime)
    }
}
