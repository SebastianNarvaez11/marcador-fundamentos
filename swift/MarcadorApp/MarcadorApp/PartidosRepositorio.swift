import Foundation
import Observation
import Torneo

// f75 · EL REPOSITORIO: la ÚNICA puerta a los datos (gemelo de `PartidosRepository` de Kotlin, f43).
//
// Es un PROTOCOLO: quien lo usa (un ViewModel) dice QUÉ necesita («dame el partido 3», «guarda este
// gol») y no sabe de dónde salen los datos. Hoy son una lista en memoria; en f77 se guardarán en un
// JSON; en las pruebas (f79) será un falso. El ViewModel no cambia en ninguno de los tres casos.
protocol PartidosRepositorio: AnyObject {
    var equipos: [Equipo] { get }
    var partidos: [PartidoDeLista] { get }
    func partido(id: Int) -> Partido?
    func registrarGol(partidoId: Int, gol: EventoDePartido)
}

// La implementación real de esta lección: los partidos de ejemplo, en memoria.
//
// `@Observable`: como es la fuente de la verdad, las pantallas que LEEN `partidos` se redibujan solas
// cuando alguien registra un gol (la lista muestra el marcador nuevo sin que nadie la avise).
@Observable
final class RepositorioEnMemoria: PartidosRepositorio {
    let equipos: [Equipo]
    private(set) var partidos: [PartidoDeLista]

    init(equipos: [Equipo] = DatosDeEjemplo.torneo.equipos, partidos: [PartidoDeLista] = DatosDeEjemplo.partidos) {
        self.equipos = equipos
        self.partidos = partidos
    }

    func partido(id: Int) -> Partido? {
        partidos.first { $0.id == id }?.partido
    }

    // `Partido` es un valor: se sustituye por una copia con el gol añadido (`registrando`).
    func registrarGol(partidoId: Int, gol: EventoDePartido) {
        guard let indice = partidos.firstIndex(where: { $0.id == partidoId }) else { return }
        partidos[indice] = PartidoDeLista(id: partidoId, partido: partidos[indice].partido.registrando(gol))
    }
}
