package com.sebastiannarvaez.marcador.ui

import com.sebastiannarvaez.marcador.domain.ErrorDeRed
import com.sebastiannarvaez.marcador.domain.FalloDeRed

// El texto que ve el usuario para cada fallo. Vive en la UI: el dominio dice QUE paso
// (ErrorDeRed) y la pantalla decide COMO contarlo. `when` exhaustivo sobre el sealed.
fun Throwable.mensajeParaElUsuario(): String = when (this) {
    is FalloDeRed -> when (val error = error) {
        ErrorDeRed.SinConexion -> "No hay conexión"
        is ErrorDeRed.Servidor -> "El servidor falló (código ${error.codigo}); prueba más tarde"
    }
    else -> message ?: "Algo salió mal"
}
