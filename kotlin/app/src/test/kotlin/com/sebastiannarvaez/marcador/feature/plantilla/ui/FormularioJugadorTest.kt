package com.sebastiannarvaez.marcador.feature.plantilla.ui

import com.sebastiannarvaez.marcador.torneo.Ejemplo
import com.sebastiannarvaez.marcador.torneo.Jugador
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// LAS REGLAS DEL FORMULARIO, sin pantalla, sin ViewModel y sin corrutinas: un FormularioJugador
// y una plantilla. La del Rayo FC: Ana #9, Luis sin dorsal y Marta #1.
class FormularioJugadorTest {

    private val rayo = Ejemplo.rayo.plantilla

    @Test
    fun unNombreVacioOSoloConEspaciosEsUnError() {
        assertEquals(ErrorNombre.VACIO, FormularioJugador(nombre = "").errorNombre(rayo))
        assertEquals(ErrorNombre.VACIO, FormularioJugador(nombre = "   ").errorNombre(rayo))
    }

    @Test
    fun unNombreDeMasDe25LetrasEsDemasiadoLargo() {
        assertEquals(ErrorNombre.LARGO, FormularioJugador(nombre = "a".repeat(26)).errorNombre(rayo))
        assertNull(FormularioJugador(nombre = "a".repeat(25)).errorNombre(rayo))
    }

    @Test
    fun unNombreQueYaEstaEnLaPlantillaEsUnError() {
        assertEquals(ErrorNombre.REPETIDO, FormularioJugador(nombre = " ana ").errorNombre(rayo))
    }

    @Test
    fun elDorsalEsOpcional() {
        assertNull(FormularioJugador(nombre = "Bea", dorsal = "").errorDorsal(rayo))
        assertTrue(FormularioJugador(nombre = "Bea", dorsal = "").esValido(rayo))
    }

    @Test
    fun unDorsalQueNoEsUnNumeroDel1Al99EsUnError() {
        assertEquals(ErrorDorsal.NO_ES_NUMERO, FormularioJugador(dorsal = "diez").errorDorsal(rayo))
        assertEquals(ErrorDorsal.FUERA_DE_RANGO, FormularioJugador(dorsal = "0").errorDorsal(rayo))
        assertEquals(ErrorDorsal.FUERA_DE_RANGO, FormularioJugador(dorsal = "100").errorDorsal(rayo))
    }

    @Test
    fun unDorsalQueYaLlevaOtroJugadorEsUnError() {
        assertEquals(ErrorDorsal.REPETIDO, FormularioJugador(nombre = "Bea", dorsal = "9").errorDorsal(rayo))
        assertFalse(FormularioJugador(nombre = "Bea", dorsal = "9").esValido(rayo))
    }

    @Test
    fun unFormularioValidoDaElJugadorLimpio() {
        assertEquals(Jugador("Bea", 7), FormularioJugador(nombre = "  Bea ", dorsal = " 7").aJugador())
    }
}
