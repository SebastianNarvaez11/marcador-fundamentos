import Foundation
import Observation
import Torneo

// LO QUE VE LA PANTALLA DE LA CRÓNICA.
struct CronicaUiState: Equatable {
    var cronica: Cronica?        // nil: el partido no existe
    var publicando = false
    var mensaje: String?         // «Crónica publicada con el n.º 101», o el error
}

// OJO: este ViewModel hace DEMASIADO, a propósito. Lee el partido de un repositorio, decide cómo se
// escribe la crónica y la publica con otro. Más adelante esa regla se irá a un caso de uso.
@MainActor
@Observable
final class CronicaViewModel {
    private(set) var uiState: CronicaUiState

    @ObservationIgnored private let cronicas: any CronicasRepositorio

    init(partidoId: Int, partidos: any PartidosRepositorio, cronicas: any CronicasRepositorio) {
        self.cronicas = cronicas
        if let partido = partidos.partido(id: partidoId) {
            uiState = CronicaUiState(cronica: Self.redactar(partido))
        } else {
            uiState = CronicaUiState(mensaje: "No existe el partido \(partidoId)")
        }
    }

    // La regla: el título es el resultado y el texto, una línea por gol («12' Ana (Rayo FC)»).
    private static func redactar(_ partido: Partido) -> Cronica {
        var lineas: [String] = []
        for evento in partido.eventos {
            if case let .gol(minuto, jugador, equipo) = evento {
                lineas.append("\(minuto)' \(jugador.nombre) (\(equipo.nombre))")
            }
        }
        return Cronica(
            titulo: "\(partido.local.nombre) \(partido.marcador()) \(partido.visitante.nombre)",
            texto: lineas.isEmpty ? "Sin goles" : lineas.joined(separator: "\n")
        )
    }

    // El botón «Publicar».
    func publicar() async {
        guard let cronica = uiState.cronica else { return }
        uiState.publicando = true
        uiState.mensaje = nil
        do {
            let id = try await cronicas.publicar(titulo: cronica.titulo, texto: cronica.texto)
            uiState.mensaje = "Crónica publicada con el n.º \(id)"
        } catch {
            // `ErrorDeRed` es `LocalizedError`: aquí sale «No hay conexión» o «El servidor respondió…».
            uiState.mensaje = error.localizedDescription
        }
        uiState.publicando = false
        Registro.anotar("crónica: \(uiState.mensaje ?? "")")
    }
}
