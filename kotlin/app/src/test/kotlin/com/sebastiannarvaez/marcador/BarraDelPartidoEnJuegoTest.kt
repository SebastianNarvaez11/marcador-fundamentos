package com.sebastiannarvaez.marcador

import com.sebastiannarvaez.marcador.domain.PartidoEnJuego
import com.sebastiannarvaez.marcador.torneo.Marcador
import com.sebastiannarvaez.marcador.ui.textoDeLaBarra
import kotlin.test.Test
import kotlin.test.assertEquals

// El texto de la barra es una funcion normal: se prueba en la JVM, sin pintar nada.
class BarraDelPartidoEnJuegoTest {

    @Test
    fun enJuegoMuestraElMinuto() {
        val enJuego = PartidoEnJuego.EnJuego(1, "Rayo FC", "Toros", Marcador(2, 1), minuto = 34, terminado = false)
        assertEquals("Rayo FC 2–1 Toros · 34'", textoDeLaBarra(enJuego))
    }

    @Test
    fun alTerminarMuestraFinal() {
        val final = PartidoEnJuego.EnJuego(1, "Rayo FC", "Toros", Marcador(3, 2), minuto = 90, terminado = true)
        assertEquals("Final · Rayo FC 3–2 Toros", textoDeLaBarra(final))
    }
}
