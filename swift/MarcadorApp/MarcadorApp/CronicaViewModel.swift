import Foundation
import Observation

// LO QUE VE LA PANTALLA DE LA CRÓNICA.
struct CronicaUiState: Equatable {
    var cronica: Cronica?        // nil: el partido no existe
    var publicando = false
    var mensaje: String?         // «Crónica publicada con el n.º 101», o el error
}

// Ahora el ViewModel solo hace lo suyo: enseñar la crónica y avisar al caso de uso.
// Cómo se escribe y con qué repositorios se publica lo sabe `PublicarCronica`.
@MainActor
@Observable
final class CronicaViewModel {
    private(set) var uiState: CronicaUiState

    @ObservationIgnored private let partidoId: Int
    @ObservationIgnored private let publicarCronica: PublicarCronica

    init(partidoId: Int, publicarCronica: PublicarCronica) {
        self.partidoId = partidoId
        self.publicarCronica = publicarCronica
        if let cronica = publicarCronica.redactar(partidoId: partidoId) {
            uiState = CronicaUiState(cronica: cronica)
        } else {
            uiState = CronicaUiState(mensaje: "No existe el partido \(partidoId)")
        }
    }

    // El botón «Publicar».
    func publicar() async {
        guard uiState.cronica != nil else { return }
        uiState.publicando = true
        uiState.mensaje = nil
        switch await publicarCronica(partidoId: partidoId) {
        case let .success(id):
            uiState.mensaje = "Crónica publicada con el n.º \(id)"
        case let .failure(fallo):
            uiState.mensaje = fallo.localizedDescription
        }
        uiState.publicando = false
        Registro.anotar("crónica: \(uiState.mensaje ?? "")")
    }
}
