import Foundation
import Observation

// LO QUE VE LA PANTALLA DE ÁRBITROS. Ya no es un `enum`: con dos fuentes se pueden dar VARIAS cosas
// a la vez (la lista guardada, la ruedecita de refrescar y un aviso de que la red falló).
struct ArbitrosUiState: Equatable {
    var arbitros: [Arbitro] = []
    var refrescando = false
    var error: String?
}

@MainActor
@Observable
final class ArbitrosViewModel {
    private(set) var refrescando = false
    private(set) var error: String?

    @ObservationIgnored private let repositorio: any ArbitrosRepositorio

    init(repositorio: any ArbitrosRepositorio) {
        self.repositorio = repositorio
    }

    // La lista sale del repositorio (lo guardado); el ViewModel solo añade si está refrescando y el aviso.
    var uiState: ArbitrosUiState {
        ArbitrosUiState(arbitros: repositorio.arbitros, refrescando: refrescando, error: error)
    }

    // Al abrir la pantalla (`.task`), al tirar hacia abajo (`.refreshable`) y con «Actualizar».
    func refrescar() async {
        refrescando = true
        error = nil
        do {
            try await repositorio.refrescar()
        } catch {
            // `ErrorDeRed` es `LocalizedError`: «No hay conexión» o «El servidor respondió…».
            self.error = error.localizedDescription
            Registro.anotar("árbitros: no se pudieron refrescar (\(error))")
        }
        refrescando = false
    }
}
