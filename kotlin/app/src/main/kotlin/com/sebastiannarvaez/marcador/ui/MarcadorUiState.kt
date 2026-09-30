package com.sebastiannarvaez.marcador.ui

import com.sebastiannarvaez.marcador.domain.Lado
import com.sebastiannarvaez.marcador.torneo.Marcador
import com.sebastiannarvaez.marcador.torneo.Partido

// f41 · MODELAR EL UiState
//
// La pantalla no debe armar su estado a partir de cinco flujos sueltos: pueden llegar
// desincronizados (¿minuto nuevo con marcador viejo?) y admiten combinaciones que no
// existen («cargando» y «error» a la vez). Se modela UN objeto con TODO lo que la
// pantalla necesita, y que solo pueda ser lo que puede ser.
//
// sealed interface  -> las variantes son EXCLUYENTES y distintas entre si (cada una con
//                      sus datos): cargando, con datos, o con error. `when` exhaustivo.
// data class unica  -> mejor cuando los campos son independientes (un formulario con
//                      `cargando: Boolean` y `error: String?` que conviven con los datos:
//                      «recargando con los datos viejos a la vista»).
//
// Aqui la pantalla entera cambia segun el caso, asi que sealed. Lo que se deja fuera a
// proposito: nada de `partido: Partido?` con `null` para «aun no hay», porque obliga a
// comprobarlo en cada uso; en `Exito` el partido SIEMPRE existe.
sealed interface MarcadorUiState {
    data object Cargando : MarcadorUiState

    data class Exito(
        val partidoId: Int,
        val partido: Partido,
        val marcador: Marcador,
        val minuto: Int,
        val corriendo: Boolean,
        val ultimoAviso: String,
    ) : MarcadorUiState

    data class Error(val mensaje: String) : MarcadorUiState
}

// EVENTOS: lo que la pantalla PIDE, sin decir como se hace. El estado BAJA (UiState) y los
// eventos SUBEN (MarcadorEvento): flujo unidireccional. La pantalla no modifica nada, solo
// avisa; el ViewModel decide y publica un estado nuevo.
sealed interface MarcadorEvento {
    data object Empezar : MarcadorEvento
    data class Gol(val lado: Lado) : MarcadorEvento
    data class Elegir(val partidoId: Int) : MarcadorEvento
}
