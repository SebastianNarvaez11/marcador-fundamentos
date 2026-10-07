package com.sebastiannarvaez.marcador.ui

import com.sebastiannarvaez.marcador.domain.Arbitro

// Tres estados excluyentes: la pantalla entera cambia segun el caso, asi que sealed.
// Esta vez el `Cargando` es de verdad: la red tarda.
sealed interface ArbitrosUiState {
    data object Cargando : ArbitrosUiState
    data class Exito(val arbitros: List<Arbitro>) : ArbitrosUiState
    data class Error(val mensaje: String) : ArbitrosUiState
}
