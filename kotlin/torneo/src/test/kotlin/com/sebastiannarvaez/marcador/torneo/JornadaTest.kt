package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JornadaTest {
    @Test
    fun losDosPartidosDevuelvenSuResultado() = runBlocking {
        val (uno, otro) = jugarJornada(Ejemplo.rayoContraToros, Ejemplo.lobosContraAguilas, msPorMinuto = 0)
        assertEquals(2 to 1, uno.golesLocal to uno.golesVisitante)
        assertEquals(1 to 2, otro.golesLocal to otro.golesVisitante)
    }

    @Test
    fun losPartidosJueganALaVezYNoUnoDetrasDeOtro() = runBlocking {
        val ms = measureTimeMillis {
            jugarJornada(Ejemplo.rayoContraToros, Ejemplo.lobosContraAguilas, msPorMinuto = 10)
        }
        // Uno solo dura algo más de 1 s (90 esperas de 10 ms y un poco de más); uno detrás de otro serían más de 2 s.
        assertTrue(ms < 1500, "Tardó $ms ms: parece que no juegan a la vez")
    }

    @Test
    fun lasEstadisticasCuentanCadaTipoDeEvento() = runBlocking {
        assertEquals(Estadisticas(goles = 3, tarjetas = 1, cambios = 1), estadisticasDe(Ejemplo.rayoContraToros, msPorConsulta = 0))
    }

    @Test
    fun lasConsultasDeEstadisticasVanEnParalelo() = runBlocking {
        val ms = measureTimeMillis { estadisticasDe(Ejemplo.rayoContraToros, msPorConsulta = 100) }
        // En serie serían ~300 ms.
        assertTrue(ms < 250, "Tardó $ms ms: las consultas no van en paralelo")
    }
}
