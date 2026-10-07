package com.sebastiannarvaez.marcador.ui

import com.sebastiannarvaez.marcador.domain.Arbitro

// Ahora una data class y no un sealed: la lista guardada, «refrescando» y el error
// CONVIVEN. Es el caso «recargando con los datos viejos a la vista»: mientras llega la
// red se ve lo guardado, y si falla se avisa sin vaciar la lista.
data class ArbitrosUiState(
    val arbitros: List<Arbitro> = emptyList(),
    val refrescando: Boolean = false,
    val error: String? = null,
)
