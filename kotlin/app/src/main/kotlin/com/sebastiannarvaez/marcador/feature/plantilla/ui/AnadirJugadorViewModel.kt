package com.sebastiannarvaez.marcador.feature.plantilla.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sebastiannarvaez.marcador.feature.partidos.domain.PartidosRepository
import com.sebastiannarvaez.marcador.torneo.Jugador
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

// EL ESTADO DEL FORMULARIO VIVE AQUI, en `mutableStateOf` y no en un StateFlow.
//
// Un campo de texto necesita que su valor cambie EN EL ACTO, en la misma pasada en la que se
// pulsa la tecla. `mutableStateOf` se escribe y se lee sin corrutinas ni esperas; Compose
// repinta lo que lo leyo. Es la forma clásica de guardar el texto de un TextField.
@HiltViewModel(assistedFactory = AnadirJugadorViewModel.Fabrica::class)
class AnadirJugadorViewModel @AssistedInject constructor(
    private val repositorio: PartidosRepository,
    @Assisted val equipo: String,
) : ViewModel() {

    @AssistedFactory
    interface Fabrica {
        fun crear(equipo: String): AnadirJugadorViewModel
    }

    // `private set`: la pantalla lee el formulario, pero solo cambia llamando a las funciones de abajo.
    var formulario by mutableStateOf(FormularioJugador())
        private set

    // La plantilla GUARDADA, para las reglas «no repetido». Llega de Room como Flow y se copia a
    // un mutableStateOf: asi los errores (que la leen) se recalculan tambien cuando ella cambia.
    private var plantilla by mutableStateOf(emptyList<Jugador>())

    // Pasa a true cuando el jugador ya esta guardado: la pantalla lo ve y vuelve atras.
    var guardado by mutableStateOf(false)
        private set

    private var guardando = false

    init {
        viewModelScope.launch {
            repositorio.observarPlantilla(equipo).collect { plantilla = it }
        }
    }

    // Lo que ve la pantalla: el error SOLO si el campo esta tocado; si no, null.
    val errorNombre: ErrorNombre?
        get() = if (formulario.nombreTocado) formulario.errorNombre(plantilla) else null

    val errorDorsal: ErrorDorsal?
        get() = if (formulario.dorsalTocado) formulario.errorDorsal(plantilla) else null

    // El boton «Guardar» solo se activa con el formulario valido, tocado o no.
    val sePuedeGuardar: Boolean
        get() = formulario.esValido(plantilla)

    fun alEscribirNombre(texto: String) {
        formulario = formulario.copy(nombre = texto, nombreTocado = true)
    }

    fun alEscribirDorsal(texto: String) {
        formulario = formulario.copy(dorsal = texto, dorsalTocado = true)
    }

    fun guardar() {
        if (!sePuedeGuardar || guardando) return
        guardando = true
        viewModelScope.launch {
            repositorio.anadirJugador(equipo, formulario.aJugador())
            guardado = true
        }
    }
}
