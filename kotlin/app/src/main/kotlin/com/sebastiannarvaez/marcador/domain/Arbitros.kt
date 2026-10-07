package com.sebastiannarvaez.marcador.domain

// Un arbitro de la liga, como lo usa la app: en espanol y solo con lo que se pinta.
// No es el JSON (eso es `ArbitroDto`, en data/red): si manana el servidor cambia un
// nombre de campo, cambia el DTO y su traduccion, y esta clase ni se entera.
data class Arbitro(val id: Int, val nombre: String, val ciudad: String)

// El repositorio de arbitros: la pantalla pide la lista y no sabe que viene de la red.
// `suspend` porque es UNA peticion que tarda y termina (no un flujo que cambia solo).
interface ArbitrosRepository {
    suspend fun arbitros(): List<Arbitro>
}
