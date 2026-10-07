import Foundation
import Observation

// LO QUE VE LA PANTALLA DE ÁRBITROS. Esta vez «cargando» es de verdad: la red tarda.
enum ArbitrosUiState: Equatable {
    case cargando
    case error(String)
    case exito([Arbitro])
}

@MainActor
@Observable
final class ArbitrosViewModel {
    private(set) var uiState: ArbitrosUiState = .cargando

    @ObservationIgnored private let repositorio: any ArbitrosRepositorio

    init(repositorio: any ArbitrosRepositorio) {
        self.repositorio = repositorio
    }

    // La lanza la vista con `.task` al aparecer.
    func cargar() async {
        uiState = .cargando
        do {
            let arbitros = try await repositorio.arbitros()
            uiState = .exito(arbitros)
        } catch {
            Registro.anotar("árbitros: no se pudieron cargar (\(error))")
            uiState = .error("No hay conexión")
        }
    }

    // El botón «Reintentar»: vuelve a pedirlos.
    func reintentar() async {
        await cargar()
    }
}
