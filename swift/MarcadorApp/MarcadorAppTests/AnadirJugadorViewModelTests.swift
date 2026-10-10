import Testing
import Torneo
@testable import MarcadorApp

// El ViewModel del formulario con el repositorio falso (Rayo FC: Ana #9, Luis y Marta #1).
@MainActor
struct AnadirJugadorViewModelTests {

    @Test func alAbrirNoSeVeNingunErrorYNoSePuedeGuardar() {
        let viewModel = AnadirJugadorViewModel(equipo: "Rayo FC", repositorio: RepositorioFalso())
        #expect(viewModel.errorNombre == nil)
        #expect(viewModel.errorDorsal == nil)
        #expect(!viewModel.sePuedeGuardar)
    }

    @Test func unDorsalDeLaPlantillaGuardadaEsUnError() {
        let viewModel = AnadirJugadorViewModel(equipo: "Rayo FC", repositorio: RepositorioFalso())
        viewModel.formulario.nombre = "Bea"
        viewModel.formulario.dorsal = "9"
        #expect(viewModel.errorDorsal == .repetido)
        #expect(!viewModel.guardar())
    }

    @Test func guardarUnJugadorValidoLoAnadeAlEquipo() {
        let repositorio = RepositorioFalso()
        let viewModel = AnadirJugadorViewModel(equipo: "Rayo FC", repositorio: repositorio)
        viewModel.formulario.nombre = "Bea"
        viewModel.formulario.dorsal = "7"
        #expect(viewModel.guardar())
        #expect(repositorio.jugadoresAnadidos.map(\.jugador) == [Jugador(nombre: "Bea", dorsal: 7)])
        // Ya está en la plantilla: el mismo nombre otra vez es un error.
        #expect(viewModel.errorNombre == .repetido)
    }
}
