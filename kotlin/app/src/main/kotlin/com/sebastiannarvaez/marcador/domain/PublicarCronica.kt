package com.sebastiannarvaez.marcador.domain

import com.sebastiannarvaez.marcador.resumen
import com.sebastiannarvaez.marcador.torneo.Gol
import com.sebastiannarvaez.marcador.torneo.intentar

// CASO DE USO CON DOS REPOSITORIOS. Aqui SI vale la pena:
//   - hay una REGLA de negocio: como se escribe una cronica (titulo = el resumen del
//     partido; texto = una linea por gol, en orden, o «Sin goles»);
//   - JUNTA dos repositorios: los partidos (Room) y las cronicas (la red);
//   - la regla se prueba sin pantalla (PublicarCronicaTest).
// Direccion: ViewModel -> caso de uso -> repositorios. Nunca al reves.
//
// Contraejemplo: la pantalla de arbitros llama a ArbitrosRepository DIRECTO. Un
// `ObtenerArbitros` que solo reenviara `observarArbitros()` seria pura ceremonia.
class PublicarCronica(
    private val partidos: PartidosRepository,
    private val cronicas: CronicasRepository,
) {
    suspend operator fun invoke(partidoId: Int): Result<CronicaPublicada> = intentar {
        val partido = partidos.partido(partidoId) ?: error("No existe el partido $partidoId")
        val titulo = partido.resumen()
        val goles = partido.eventos.filterIsInstance<Gol>().sortedBy { it.minuto }
        val texto = if (goles.isEmpty()) "Sin goles" else goles.joinToString("\n") { "${it.minuto}' ${it.jugador.nombre} (${it.equipo.nombre})" }
        // getOrThrow: si la red fallo, el FalloDeRed sale tal cual en el Result de fuera.
        CronicaPublicada(cronicas.publicar(titulo, texto).getOrThrow(), titulo, texto)
    }
}
