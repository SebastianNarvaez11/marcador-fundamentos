import Foundation
import Testing
@testable import MarcadorApp

// El ViewModel de los árbitros con un repositorio FALSO: sin red, sin esperas y sin servidor.
@MainActor
struct ArbitrosViewModelTests {
    private let leanne = Arbitro(id: 1, nombre: "Leanne Graham", ciudad: "Gwenborough")

    @Test func pasaDeCargandoAExito() async {
        let repositorio = ArbitrosRepositorioFalso(respuesta: .success([leanne]))
        let viewModel = ArbitrosViewModel(repositorio: repositorio)
        #expect(viewModel.uiState == .cargando)
        await viewModel.cargar()
        #expect(viewModel.uiState == .exito([leanne]))
    }

    @Test func sinRedSaleElErrorYReintentarVuelveACargar() async {
        let repositorio = ArbitrosRepositorioFalso(respuesta: .failure(URLError(.notConnectedToInternet)))
        let viewModel = ArbitrosViewModel(repositorio: repositorio)
        await viewModel.cargar()
        #expect(viewModel.uiState == .error("No hay conexión"))
        // Vuelve la red: «Reintentar» pide otra vez la lista.
        repositorio.respuesta = .success([leanne])
        await viewModel.reintentar()
        #expect(viewModel.uiState == .exito([leanne]))
        #expect(repositorio.llamadas == 2)
    }
}
