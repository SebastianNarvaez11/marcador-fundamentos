package com.sebastiannarvaez.marcador

import app.cash.turbine.test
import com.sebastiannarvaez.marcador.domain.Arbitro
import com.sebastiannarvaez.marcador.domain.ErrorDeRed
import com.sebastiannarvaez.marcador.domain.FalloDeRed
import com.sebastiannarvaez.marcador.ui.ArbitrosUiState
import com.sebastiannarvaez.marcador.ui.ArbitrosViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

// Las mismas tres piezas que PartidoViewModelTest: setMain, runTest y Turbine. La red no
// se toca: el ViewModel recibe un repositorio falso.
@OptIn(ExperimentalCoroutinesApi::class)
class ArbitrosViewModelTest {

    private val despachador = StandardTestDispatcher()

    @BeforeTest
    fun antes() = Dispatchers.setMain(despachador)

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    private val leanne = Arbitro(1, "Leanne Graham", "Gwenborough")

    @Test
    fun alAbrirRefrescaYPintaLoDeLaRed() = runTest {
        val viewModel = ArbitrosViewModel(FakeArbitrosRepository())
        viewModel.uiState.test {
            assertEquals(ArbitrosUiState(refrescando = true), awaitItem())
            assertEquals(ArbitrosUiState(arbitros = listOf(leanne)), awaitItem())
        }
    }

    @Test
    fun sinRedConservaLoGuardadoYAvisa() = runTest {
        val repositorio = FakeArbitrosRepository(guardados = listOf(leanne), fallo = FalloDeRed(ErrorDeRed.SinConexion))
        val viewModel = ArbitrosViewModel(repositorio)
        viewModel.uiState.test {
            assertEquals(ArbitrosUiState(refrescando = true), awaitItem())
            // La lista NO se vacia: lo guardado sigue y el error va al lado.
            assertEquals(ArbitrosUiState(arbitros = listOf(leanne), error = "No hay conexión"), awaitItem())
        }
    }
}
