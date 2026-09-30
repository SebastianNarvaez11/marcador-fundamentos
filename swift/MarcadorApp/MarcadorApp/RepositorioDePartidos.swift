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

// La implementación real: los partidos de ejemplo, y (f77) guardados en un JSON de `Documents`.
//
// Al arrancar, si ya hay un `torneo.json`, se parte de él; si no, de los datos de ejemplo. Cada gol
// registrado se vuelve a guardar. Con `almacen: nil` (las pruebas) vive solo en memoria.
//
// `@Observable`: como es la fuente de la verdad, las pantallas que LEEN `partidos` se redibujan solas
// cuando alguien registra un gol (la lista muestra el marcador nuevo sin que nadie la avise).
@Observable
final class RepositorioDePartidos: PartidosRepositorio {
    let equipos: [Equipo]
    private(set) var partidos: [PartidoDeLista]

    @ObservationIgnored private let almacen: AlmacenDelTorneo?

    init(
        equipos: [Equipo] = DatosDeEjemplo.torneo.equipos,
        partidos: [PartidoDeLista] = DatosDeEjemplo.partidos,
        almacen: AlmacenDelTorneo? = nil
    ) {
        self.almacen = almacen
        if let almacen, almacen.existe {
            do {
                let guardado = try almacen.cargar()
                self.equipos = guardado.equipos
                self.partidos = guardado.partidos.enumerated().map { PartidoDeLista(id: $0 + 1, partido: $1) }
                Registro.anotar("torneo CARGADO de \(almacen.url.lastPathComponent): \(guardado.partidos.count) partidos")
                return
            } catch {
                // Un JSON roto no debe impedir arrancar: se avisa y se parte de cero.
                Registro.anotar("torneo.json ILEGIBLE (\(error)); se usan los datos de ejemplo")
            }
        }
        self.equipos = equipos
        self.partidos = partidos
    }

    // Las tablas y los goleadores no se guardan: se CALCULAN de los partidos. Aquí se arma un `Torneo`
    // con ellos, que valida cada partido (un gol de alguien ajeno lanzaría un error).
    func torneo() throws -> Torneo {
        let torneo = Torneo(nombre: "Copa Barrio", equipos: equipos)
        for item in partidos { try torneo.registrar(item.partido) }
        return torneo
    }

    private func guardar() {
        guard let almacen else { return }
        do {
            try almacen.guardar(try torneo())
        } catch {
            Registro.anotar("no se pudo guardar el torneo: \(error)")
        }
    }

    func partido(id: Int) -> Partido? {
        partidos.first { $0.id == id }?.partido
    }

    // `Partido` es un valor: se sustituye por una copia con el gol añadido (`registrando`).
    func registrarGol(partidoId: Int, gol: EventoDePartido) {
        guard let indice = partidos.firstIndex(where: { $0.id == partidoId }) else { return }
        partidos[indice] = PartidoDeLista(id: partidoId, partido: partidos[indice].partido.registrando(gol))
        guardar()
    }
}
