package com.sebastiannarvaez.marcador.domain

import com.sebastiannarvaez.marcador.torneo.Gol
import com.sebastiannarvaez.marcador.torneo.intentar

// f43 · CASO DE USO: una accion del usuario con sentido para el negocio.
//
// Un caso de uso es una clase con UNA operacion (`invoke`, asi se llama como una funcion:
// `registrarGol(1, Lado.LOCAL, 30)`). Vale la pena cuando hay REGLA: aqui, «quien marca».
// Si solo reenviara la llamada al repositorio, sobraria: el ViewModel lo llamaria directo.
//
// Regla: el gol a mano no dice quien lo marco, asi que lo marca el jugador de la
// plantilla que toca por turno (los goles que ya lleva el equipo, modulo el tamano).
// Los errores esperables (no existe el partido, el equipo no tiene jugadores) salen como
// `Result`, no como excepcion (f13). Se atrapan con `intentar` (F2, Cronometro.kt): como
// `runCatching`, pero RELANZA la CancellationException. Esta funcion es `suspend`: si la
// tragara, un gol de una pantalla ya cerrada se seguiria registrando como si nada.
class RegistrarGol(private val repositorio: PartidosRepository) {

    suspend operator fun invoke(partidoId: Int, lado: Lado, minuto: Int): Result<Gol> = intentar {
        val partido = repositorio.partido(partidoId) ?: error("No existe el partido $partidoId")
        val equipo = if (lado == Lado.LOCAL) partido.local else partido.visitante
        check(equipo.plantilla.isNotEmpty()) { "${equipo.nombre} no tiene jugadores" }
        val jugador = equipo.plantilla[partido.golesDe(equipo) % equipo.plantilla.size]
        Gol(minuto, jugador, equipo).also { repositorio.registrarGol(partidoId, it) }
    }
}
