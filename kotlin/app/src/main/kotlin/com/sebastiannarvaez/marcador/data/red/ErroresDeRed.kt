package com.sebastiannarvaez.marcador.data.red

import com.sebastiannarvaez.marcador.domain.ErrorDeRed
import com.sebastiannarvaez.marcador.domain.FalloDeRed
import retrofit2.HttpException
import java.io.IOException

// De las excepciones de la red al error de dominio, en UN sitio:
//   HttpException -> hubo respuesta con codigo de error (4xx, 5xx): Servidor(codigo)
//   IOException   -> no hubo respuesta (sin red, host desconocido, timeout): SinConexion
//   lo demas      -> tal cual (un fallo de programacion no se disfraza de «sin red»).
fun Throwable.aErrorDeRed(): Throwable = when (this) {
    is HttpException -> FalloDeRed(ErrorDeRed.Servidor(code()))
    is IOException -> FalloDeRed(ErrorDeRed.SinConexion)
    else -> this
}
