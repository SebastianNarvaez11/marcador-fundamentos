import Testing
@testable import MarcadorApp

// El ViewModel de los árbitros con un repositorio FALSO: sin red, sin disco y sin servidor.
@MainActor
struct ArbitrosViewModelTests {
    private let leanne = Arbitro(id: 1, nombre: "Leanne Graham", ciudad: "Gwenborough")
    private let ervin = Arbitro(id: 2, nombre: "Ervin Howell", ciudad: "Wisokyburgh")

    @Test func pintaLoGuardadoYAlRefrescarLoNuevo() async {
        let repositorio = ArbitrosRepositorioFalso(guardados: [leanne])
        repositorio.alRefrescar = .success([leanne, ervin])
        let viewModel = ArbitrosViewModel(repositorio: repositorio)
        // Antes de ir a la red ya se ve lo guardado.
        #expect(viewModel.uiState == ArbitrosUiState(arbitros: [leanne]))
        await viewModel.refrescar()
        #expect(viewModel.uiState == ArbitrosUiState(arbitros: [leanne, ervin]))
    }

    @Test func sinRedConservaLaListaYMuestraElAviso() async {
        let repositorio = ArbitrosRepositorioFalso(guardados: [leanne])
        repositorio.alRefrescar = .failure(.sinConexion)
        let viewModel = ArbitrosViewModel(repositorio: repositorio)
        await viewModel.refrescar()
        #expect(viewModel.uiState == ArbitrosUiState(arbitros: [leanne], refrescando: false, error: "No hay conexión"))
    }
}
