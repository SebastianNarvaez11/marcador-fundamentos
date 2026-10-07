package com.sebastiannarvaez.marcador.domain

import kotlinx.coroutines.flow.Flow

// Un arbitro de la liga, como lo usa la app: en espanol y solo con lo que se pinta.
// No es el JSON (eso es `ArbitroDto`, en data/red): si manana el servidor cambia un
// nombre de campo, cambia el DTO y su traduccion, y esta clase ni se entera.
data class Arbitro(val id: Int, val nombre: String, val ciudad: String)

// DOS FUENTES, UNA VERDAD. La pantalla OBSERVA lo guardado (Flow: llega al instante y
// tambien sin red) y pide REFRESCAR, que trae la lista de la red y la guarda. Nunca pinta
// la respuesta de la red directamente: si refrescar falla, lo guardado sigue ahi.
interface ArbitrosRepository {
    fun observarArbitros(): Flow<List<Arbitro>>

    suspend fun refrescar(): Result<Unit>
}
