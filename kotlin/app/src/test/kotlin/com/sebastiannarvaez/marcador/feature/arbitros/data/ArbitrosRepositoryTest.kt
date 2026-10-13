package com.sebastiannarvaez.marcador.feature.arbitros.data

import com.sebastiannarvaez.marcador.core.FakeArbitroDao
import com.sebastiannarvaez.marcador.core.FakeLigaApi
import com.sebastiannarvaez.marcador.core.database.ArbitroEntity
import com.sebastiannarvaez.marcador.core.domain.ErrorDeRed
import com.sebastiannarvaez.marcador.core.domain.FalloDeRed
import com.sebastiannarvaez.marcador.feature.arbitros.domain.Arbitro
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

// Las dos fuentes, falsas: FakeLigaApi (la red) y FakeArbitroDao (la base).
class ArbitrosRepositoryTest {

    @Test
    fun refrescarGuardaYLaPantallaLoVe() = runTest {
        val repositorio = ArbitrosRepositoryDosFuentes(FakeLigaApi(), FakeArbitroDao())
        assertEquals(Result.success(Unit), repositorio.refrescar())
        assertEquals(listOf(Arbitro(1, "Leanne Graham", "Gwenborough")), repositorio.observarArbitros().first())
    }

    @Test
    fun sinRedConservaLoGuardadoYElErrorEsDeDominio() = runTest {
        val dao = FakeArbitroDao(listOf(ArbitroEntity(7, "Árbitro guardado", "Cali")))
        val repositorio = ArbitrosRepositoryDosFuentes(FakeLigaApi(fallo = IOException("sin red")), dao)

        val error = repositorio.refrescar().exceptionOrNull()

        assertEquals(ErrorDeRed.SinConexion, (error as FalloDeRed).error)
        assertEquals(listOf(Arbitro(7, "Árbitro guardado", "Cali")), repositorio.observarArbitros().first())
    }
}
