package com.sebastiannarvaez.marcador.domain

// Lo que le puede pasar a una peticion, dicho en el idioma de la app. La pantalla no
// conoce `HttpException` ni `IOException` (son de Retrofit y de Java): conoce esto.
sealed interface ErrorDeRed {
    // No hubo respuesta: sin red, el servidor no existe o tardo demasiado.
    data object SinConexion : ErrorDeRed

    // Hubo respuesta, pero con un codigo de error (404, 500...).
    data class Servidor(val codigo: Int) : ErrorDeRed
}

// `Result.failure` necesita un Throwable: esta excepcion lleva dentro el ErrorDeRed.
class FalloDeRed(val error: ErrorDeRed) : Exception(error.toString())
