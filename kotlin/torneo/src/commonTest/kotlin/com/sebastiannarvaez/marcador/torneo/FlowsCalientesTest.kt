package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FlowsCalientesTest {
    private fun alcance() = CoroutineScope(Job() + Dispatchers.Default)

    @Test
    fun elStateFlowTieneValorDesdeElPrincipio() {
        val enVivo = PartidoEnVivo(Ejemplo.rayoContraToros)
        assertEquals(Marcador(0, 0), enVivo.marcador.value)
    }

    @Test
    fun elMarcadorLlegaAlResultadoFinal() = runBlocking {
        val alcance = alcance()
        val enVivo = PartidoEnVivo(Ejemplo.rayoContraToros, msPorMinuto = 1)
        enVivo.iniciar(alcance).join()
        assertEquals(Marcador(2, 1), enVivo.marcador.value)
        alcance.cancel()
    }

    @Test
    fun elStateFlowNoRepiteValoresIguales() = runBlocking {
        val alcance = alcance()
        val enVivo = PartidoEnVivo(Ejemplo.rayoContraToros, msPorMinuto = 2)
        val vistos = async(start = CoroutineStart.UNDISPATCHED) { enVivo.marcador.take(4).toList() }
        enVivo.iniciar(alcance)
        assertEquals(listOf("0-0", "1-0", "1-1", "2-1"), vistos.await().map { it.toString() })
        alcance.cancel()
    }

    @Test
    fun elSharedFlowEntregaCadaGolAlQueEstabaSuscrito() = runBlocking {
        val alcance = alcance()
        val enVivo = PartidoEnVivo(Ejemplo.rayoContraToros, msPorMinuto = 1)
        val recibidos = async(start = CoroutineStart.UNDISPATCHED) { enVivo.goles.take(3).toList() }
        enVivo.iniciar(alcance)
        assertEquals(listOf(12, 55, 80), recibidos.await().map { it.minuto })
        alcance.cancel()
    }

    @Test
    fun elSharedFlowNoRepiteLosGolesAlQueLlegaTarde() = runBlocking {
        val alcance = alcance()
        val enVivo = PartidoEnVivo(Ejemplo.rayoContraToros, msPorMinuto = 1)
        enVivo.iniciar(alcance).join()
        // Ya pasó todo: sin replay, el suscriptor tardío no recibe nada.
        assertNull(withTimeoutOrNull(100) { enVivo.goles.first() })
        // El StateFlow, en cambio, le da el estado actual al instante.
        assertEquals(Marcador(2, 1), enVivo.marcador.first())
        alcance.cancel()
    }

    @Test
    fun unPartidoNoSePuedeIniciarDosVeces() = runBlocking {
        val alcance = alcance()
        val enVivo = PartidoEnVivo(Ejemplo.rayoContraToros, msPorMinuto = 1)
        enVivo.iniciar(alcance)
        assertFailsWith<IllegalStateException> { enVivo.iniciar(alcance) }
        alcance.cancel()
    }

    @Test
    fun conWhileSubscribedLaJornadaNoSeCalculaSinEspectadores() = runBlocking {
        val alcance = alcance()
        val jornada = Jornada(PartidoEnVivo(Ejemplo.rayoContraToros, 1), PartidoEnVivo(Ejemplo.lobosContraAguilas, 1), alcance)
        jornada.iniciar().join()
        // Los partidos acabaron, pero nadie miraba: el combine ni arrancó.
        assertEquals(MarcadoresDeJornada(Marcador(0, 0), Marcador(0, 0)), jornada.marcadores.value)
        // En cuanto alguien mira, se reactiva y ve lo de ahora.
        val visto = withTimeoutOrNull(1_000) {
            jornada.marcadores.first { it.golesTotales == 6 }
        }
        assertEquals(MarcadoresDeJornada(Marcador(2, 1), Marcador(1, 2)), visto)
        alcance.cancel()
    }

    @Test
    fun laJornadaMezclaLosGolesDeLosDosPartidos() = runBlocking {
        val alcance = alcance()
        val jornada = Jornada(PartidoEnVivo(Ejemplo.rayoContraToros, 2), PartidoEnVivo(Ejemplo.lobosContraAguilas, 2), alcance)
        val goles = async(start = CoroutineStart.UNDISPATCHED) { jornada.goles.take(6).toList() }
        jornada.iniciar()
        assertEquals(3 + 3, goles.await().size)
        alcance.cancel()
    }

    @Test
    fun alPitarElFinalSePuedeCancelarTodo() = runBlocking {
        val alcance = alcance()
        val jornada = Jornada(PartidoEnVivo(Ejemplo.rayoContraToros, 1), PartidoEnVivo(Ejemplo.lobosContraAguilas, 1), alcance)
        val pantalla = alcance.launch { jornada.marcadores.collect { } }
        jornada.iniciar().join()
        pantalla.cancelAndJoin()
        alcance.coroutineContext[Job]!!.let { it.cancel(); it.join() }
        assertEquals(true, pantalla.isCancelled)
    }
}
