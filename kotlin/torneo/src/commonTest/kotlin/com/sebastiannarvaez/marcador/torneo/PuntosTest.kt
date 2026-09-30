package com.sebastiannarvaez.marcador.torneo

import kotlin.test.Test
import kotlin.test.assertEquals

class PuntosTest {
    @Test
    fun victoriaValeTresPuntos() = assertEquals(3, puntosPor(2, 1))

    @Test
    fun empateValeUnPunto() = assertEquals(1, puntosPor(0, 0))

    @Test
    fun derrotaNoDaPuntos() = assertEquals(0, puntosPor(0, 3))

    @Test
    fun encabezadoUsaElPrefijoPorDefecto() =
        assertEquals("Jornada 2 de 5", encabezadoDeJornada(2, 5))

    @Test
    fun encabezadoAceptaPrefijoConNombre() =
        assertEquals("Fecha 1 de 3", encabezadoDeJornada(1, 3, prefijo = "Fecha"))
}
