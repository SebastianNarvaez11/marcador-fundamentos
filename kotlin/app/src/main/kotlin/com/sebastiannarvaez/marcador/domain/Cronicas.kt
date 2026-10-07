package com.sebastiannarvaez.marcador.domain

// La cronica tal como quedo publicada: el numero que dio el servidor y lo enviado.
data class CronicaPublicada(val numero: Int, val titulo: String, val texto: String)

// Publicar es UNA accion que termina: `suspend`. El fallo esperable (sin red, error del
// servidor) sale en el Result como FalloDeRed, no como excepcion.
interface CronicasRepository {
    suspend fun publicar(titulo: String, texto: String): Result<Int>
}
