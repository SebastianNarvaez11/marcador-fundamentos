package com.sebastiannarvaez.marcador.domain

import com.sebastiannarvaez.marcador.di.AlcanceDeApp
import com.sebastiannarvaez.marcador.torneo.Gol
import com.sebastiannarvaez.marcador.torneo.intentar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import javax.inject.Inject

// CASO DE USO: una accion del usuario con sentido para el negocio.
//
// Un caso de uso es una clase con UNA operacion (`invoke`, asi se llama como una funcion:
// `registrarGol(1, Lado.LOCAL, 30)`). Vale la pena cuando hay REGLA: aqui, «quien marca».
// Si solo reenviara la llamada al repositorio, sobraria: el ViewModel lo llamaria directo.
//
// Regla: el gol a mano no dice quien lo marco, asi que lo marca el jugador de la
// plantilla que toca por turno (los goles que ya lleva el equipo, modulo el tamano).
// Los errores esperables (no existe el partido, el equipo no tiene jugadores) salen como
// `Result`, no como excepcion. Se atrapan con `intentar` (Cronometro.kt): como
// `runCatching`, pero RELANZA la CancellationException.
//
// UN GOL NO SE QUEDA A MEDIAS. Quien llama es la pantalla del partido, desde viewModelScope; si
// se cierra justo despues del gol, ese alcance se cancela y el gol se perderia a mitad de
// guardar. Por eso el trabajo se lanza en el ALCANCE DE LA APP (`alcance.async`) y aqui solo
// se ESPERA su resultado (`await`): si cancelan al que espera, el guardado sigue.
//
// @Inject constructor: le dice a Hilt «asi se construye esta clase; dame lo que pide entre
// parentesis». Hilt busca un PartidosRepository y el alcance con la etiqueta @AlcanceDeApp.
class RegistrarGol @Inject constructor(
    private val repositorio: PartidosRepository,
    @AlcanceDeApp private val alcance: CoroutineScope,
) {

    suspend operator fun invoke(partidoId: Int, lado: Lado, minuto: Int): Result<Gol> = alcance.async {
        intentar {
            val partido = repositorio.partido(partidoId) ?: error("No existe el partido $partidoId")
            val equipo = if (lado == Lado.LOCAL) partido.local else partido.visitante
            check(equipo.plantilla.isNotEmpty()) { "${equipo.nombre} no tiene jugadores" }
            val jugador = equipo.plantilla[partido.golesDe(equipo) % equipo.plantilla.size]
            Gol(minuto, jugador, equipo).also { repositorio.registrarGol(partidoId, it) }
        }
    }.await()
}
