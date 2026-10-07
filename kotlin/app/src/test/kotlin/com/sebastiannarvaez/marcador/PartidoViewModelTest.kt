package com.sebastiannarvaez.marcador

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.sebastiannarvaez.marcador.domain.Lado
import com.sebastiannarvaez.marcador.domain.PartidoDeLista
import com.sebastiannarvaez.marcador.domain.RegistrarGol
import com.sebastiannarvaez.marcador.torneo.Ejemplo
import com.sebastiannarvaez.marcador.torneo.Gol
import com.sebastiannarvaez.marcador.torneo.Marcador
import com.sebastiannarvaez.marcador.ui.MarcadorEvento
import com.sebastiannarvaez.marcador.ui.MarcadorUiState
import com.sebastiannarvaez.marcador.ui.PartidoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// TESTS DEL VIEWMODEL
//
// Tres piezas:
//  1. `Dispatchers.setMain(testDispatcher)`: viewModelScope usa Dispatchers.Main.immediate,
//     que en la JVM no existe (no hay hilo principal de Android). Se sustituye por uno de
//     pruebas y se RESTAURA en @AfterTest.
//  2. `runTest` + el mismo scheduler: el tiempo es VIRTUAL. Un partido de 90 minutos
//     de 250 ms cada uno se «juega» en milisegundos reales con `advanceTimeBy`.
//  3. Turbine (`flow.test { awaitItem() }`): recoge un Flow y deja hacer asserts sobre cada
//     emision en orden, sin `delay` ni carreras.
@OptIn(ExperimentalCoroutinesApi::class)
class PartidoViewModelTest {

    private val despachador = StandardTestDispatcher()

    @BeforeTest
    fun antes() = Dispatchers.setMain(despachador)

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    private fun TestScope.crear(
        repo: FakePartidosRepository = FakePartidosRepository(),
        preferencias: FakePreferencias = FakePreferencias(),
        partidoInicial: Int = 1,
    ) = PartidoViewModel(SavedStateHandle(), repo, RegistrarGol(repo), preferencias, partidoInicial)

    @Test
    fun pasaDeCargandoAExito() = runTest {
        val viewModel = crear()
        viewModel.uiState.test {
            assertEquals(MarcadorUiState.Cargando, awaitItem())
            val exito = awaitItem() as MarcadorUiState.Exito
            assertEquals(1, exito.partidoId)
            assertEquals(Marcador(0, 0), exito.marcador)
            assertEquals(90, exito.duracion)
        }
    }

    @Test
    fun unPartidoQueNoExisteSaleComoError() = runTest {
        // Ahora el id llega en la NavKey (aqui, `partidoInicial`): abrir el 99 es el error.
        val viewModel = crear(partidoInicial = 99)
        viewModel.uiState.test {
            assertEquals(MarcadorUiState.Cargando, awaitItem())
            assertEquals(MarcadorUiState.Error("No existe el partido n.º 99"), awaitItem())
        }
    }

    @Test
    fun unGolAManoSeVeYSeGuardaEnElRepositorio() = runTest {
        val repo = FakePartidosRepository()
        val viewModel = crear(repo)
        viewModel.uiState.test {
            skipItems(2)
            viewModel.alEvento(MarcadorEvento.Gol(Lado.LOCAL))
            // El estado cambia (sale por la pantalla)...
            assertEquals(Marcador(1, 0), (awaitItem() as MarcadorUiState.Exito).marcador)
            // ...y, ademas, el caso de uso lo guardo: se comprueba el RESULTADO en el fake,
            // no que se haya «llamado» a nada.
            advanceUntilIdle()
            val guardados = repo.partido(1)!!.eventos.filterIsInstance<Gol>()
            assertEquals(listOf(Ejemplo.rayo), guardados.map { it.equipo })
        }
    }

    @Test
    fun unPartidoDe90MinutosSeJuegaEnTiempoVirtual() = runTest {
        val viewModel = crear()
        viewModel.uiState.test {
            skipItems(2)
            viewModel.alEvento(MarcadorEvento.Empezar)
            runCurrent()
            assertEquals(true, (expectMostRecentItem() as MarcadorUiState.Exito).corriendo)
            // 90 minutos x 250 ms = 22,5 s VIRTUALES; tarda milisegundos de verdad.
            advanceTimeBy(90 * PartidoViewModel.MS_POR_MINUTO + 1)
            runCurrent()
            val final = expectMostRecentItem() as MarcadorUiState.Exito
            assertEquals(90, final.minuto)
            assertEquals(false, final.corriendo)
            assertEquals("minuto 90 con 0-0", final.ultimoAviso)
        }
    }

    // El aviso de cada 15' sale con el MISMO marcador que la pantalla: un solo reloj aplica los
    // goles del guion y calcula el aviso. Con goles justo en los minutos 15 y 30.
    @Test
    fun elAvisoDeCada15MinutosCoincideConElMarcador() = runTest {
        val repo = FakePartidosRepository(listOf(PartidoDeLista(1, partidoConGoles())))
        val viewModel = crear(repo)
        viewModel.uiState.test {
            skipItems(2)
            viewModel.alEvento(MarcadorEvento.Empezar)
            val esperado = listOf(15 to "1-0", 30 to "1-1", 45 to "1-1", 60 to "2-1", 75 to "2-1", 90 to "2-1")
            var transcurrido = 0L
            for ((minuto, marcador) in esperado) {
                val hasta = minuto * PartidoViewModel.MS_POR_MINUTO + 1
                advanceTimeBy(hasta - transcurrido)
                transcurrido = hasta
                runCurrent()
                val estado = expectMostRecentItem() as MarcadorUiState.Exito
                assertEquals(minuto, estado.minuto)
                assertEquals("minuto $minuto con $marcador", estado.ultimoAviso)
                assertEquals(marcador, estado.marcador.toString())
            }
        }
    }

    @Test
    fun cambiarLaDuracionLaGuardaYRecargaElPartido() = runTest {
        val preferencias = FakePreferencias()
        val viewModel = crear(preferencias = preferencias)
        viewModel.uiState.test {
            skipItems(2)
            viewModel.alEvento(MarcadorEvento.CambiarDuracion(60))
            advanceUntilIdle()
            assertEquals(60, preferencias.duracion.value)
            assertEquals(60, (expectMostRecentItem() as MarcadorUiState.Exito).duracion)
        }
    }

    @Test
    fun elGolDelCasoDeUsoLoMarcaElJugadorPorTurno() = runTest {
        val repo = FakePartidosRepository()
        val registrar = RegistrarGol(repo)
        val primero = registrar(1, Lado.LOCAL, 10).getOrThrow()
        val segundo = registrar(1, Lado.LOCAL, 20).getOrThrow()
        assertEquals(Ejemplo.rayo.plantilla[0], primero.jugador)
        assertEquals(Ejemplo.rayo.plantilla[1 % Ejemplo.rayo.plantilla.size], segundo.jugador)
        assertIs<Result<*>>(registrar(99, Lado.LOCAL, 1))
        assertEquals(true, registrar(99, Lado.LOCAL, 1).isFailure)
    }
}
