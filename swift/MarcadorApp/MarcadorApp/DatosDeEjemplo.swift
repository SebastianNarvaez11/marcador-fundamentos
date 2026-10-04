import Torneo

// La lista necesita un identificador ESTABLE para cada fila. `Partido` no tiene id (dos
// partidos pueden repetir equipos), así que se envuelve: es el `PartidoDeLista` de Kotlin.
//
// `Identifiable` es un protocolo con UNA exigencia: una propiedad `id` que sea `Hashable`.
// Con él, `List(partidos)` y `ForEach(partidos)` saben identificar cada fila sin que se lo digas.
struct PartidoDeLista: Identifiable {
    let id: Int
    let partido: Partido
}

// Datos de ejemplo: un torneo de verdad de `Torneo` (el que valida los partidos al registrarlos)
// con tres vueltas de todos contra todos: 18 partidos con marcadores repartidos de forma
// determinista. Son EXACTAMENTE los mismos que `DatosDeEjemplo.kt` de la app de Android.
enum DatosDeEjemplo {
    static let torneo: Torneo = {
        let torneo = Torneo(nombre: "Copa Barrio", equipos: [Ejemplo.rayo, Ejemplo.toros, Ejemplo.lobos, Ejemplo.aguilas])
        let equipos = torneo.equipos
        var semilla = 0
        for vuelta in 0..<3 {
            for i in equipos.indices {
                for j in (i + 1)..<equipos.count {
                    semilla += 1
                    // Alternar quién es local en cada vuelta.
                    let (local, visitante) = vuelta % 2 == 0 ? (equipos[i], equipos[j]) : (equipos[j], equipos[i])
                    let golesLocal = (semilla * 3 + vuelta) % 4
                    let golesVisitante = (semilla * 5 + i) % 3
                    let eventos = goles(de: local, cantidad: golesLocal) + goles(de: visitante, cantidad: golesVisitante)
                    // `try!`: son datos escritos a mano; si estuvieran mal, se vería al primer arranque.
                    try! torneo.registrar(Partido(local: local, visitante: visitante, eventos: eventos))
                }
            }
        }
        return torneo
    }()

    // Los goles los meten los jugadores de la plantilla por turnos (`Torneo.registrar` exige que
    // el goleador juegue en el equipo).
    private static func goles(de equipo: Equipo, cantidad: Int) -> [EventoDePartido] {
        (0..<cantidad).map { n in
            .gol(minuto: 10 + n * 25, jugador: equipo.plantilla[(n + cantidad) % equipo.plantilla.count], equipo: equipo)
        }
    }

    static let partidos: [PartidoDeLista] = torneo.partidos.enumerated().map { indice, partido in
        PartidoDeLista(id: indice + 1, partido: partido)
    }
}
