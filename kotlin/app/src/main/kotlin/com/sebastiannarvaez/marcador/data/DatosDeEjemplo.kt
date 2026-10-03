package com.sebastiannarvaez.marcador.data

import com.sebastiannarvaez.marcador.domain.PartidoDeLista
import com.sebastiannarvaez.marcador.torneo.Ejemplo
import com.sebastiannarvaez.marcador.torneo.Equipo
import com.sebastiannarvaez.marcador.torneo.EventoDePartido
import com.sebastiannarvaez.marcador.torneo.Gol
import com.sebastiannarvaez.marcador.torneo.Partido
import com.sebastiannarvaez.marcador.torneo.Torneo

// DATOS DE EJEMPLO
//
// Esto es la capa de DATOS: de donde salen los partidos. Hoy son datos de ejemplo
// en memoria; despues se esconden detras de un repositorio y salen de Room.
//
// Un torneo de verdad de :torneo (el que valida los partidos al registrarlos) con
// tres vueltas de todos contra todos: 18 partidos, con marcadores repartidos de forma
// determinista. Hasta que llega Room no hay datos guardados: esto hace de «base de datos».
//
object DatosDeEjemplo {
    val torneo: Torneo = Torneo("Copa Barrio", listOf(Ejemplo.rayo, Ejemplo.toros, Ejemplo.lobos, Ejemplo.aguilas))
        .also { torneo ->
            val equipos = torneo.equipos
            var semilla = 0
            repeat(3) { vuelta ->
                for (i in equipos.indices) for (j in i + 1 until equipos.size) {
                    semilla++
                    // Alternar quien es local en cada vuelta.
                    val (local, visitante) = if (vuelta % 2 == 0) equipos[i] to equipos[j] else equipos[j] to equipos[i]
                    val golesLocal = (semilla * 3 + vuelta) % 4
                    val golesVisitante = (semilla * 5 + i) % 3
                    torneo.registrar(Partido(local, visitante, goles(local, golesLocal) + goles(visitante, golesVisitante))).getOrThrow()
                }
            }
        }

    // Los goles los meten los jugadores de la plantilla por turnos (Torneo.registrar
    // exige que el goleador juegue en el equipo).
    private fun goles(equipo: Equipo, cantidad: Int): List<EventoDePartido> =
        List(cantidad) { n -> Gol(minuto = 10 + n * 25, jugador = equipo.plantilla[(n + cantidad) % equipo.plantilla.size], equipo = equipo) }

    val partidos: List<PartidoDeLista> = torneo.partidos.mapIndexed { indice, partido -> PartidoDeLista(indice + 1, partido) }
}
