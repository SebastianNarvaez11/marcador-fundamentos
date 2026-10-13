import Testing
import Torneo
@testable import MarcadorApp

// Pruebas del ViewModel con el repositorio en memoria (en las otras pruebas, un falso).
// `@MainActor`: el ViewModel lo es, y el target de pruebas no tiene el aislamiento por defecto.
@MainActor
struct PartidoViewModelTests {

    // El repositorio es un FALSO (ver `Dobles.swift`): sin disco y con los goles anotados.
    private func repositorio() -> RepositorioFalso { RepositorioFalso() }

    @Test func unPartidoQueNoExisteDaUnEstadoDeError() {
        let viewModel = PartidoViewModel(partidoId: 99, repositorio: repositorio(), msPorMinuto: 0)
        #expect(viewModel.uiState == .error("No existe el partido n.º 99"))
    }

    @Test func unGolAManoSeVeYSeGuardaEnElRepositorio() {
        let repo = repositorio()
        let viewModel = PartidoViewModel(partidoId: 1, repositorio: repo, msPorMinuto: 0)
        viewModel.alGol(.visitante)
        #expect(viewModel.directo?.marcador == Marcador(local: 0, visitante: 1))
        // El falso anotó UN gol, del equipo visitante, con el jugador que tocaba por turno
        // (Toros lleva 1 gol de 2 jugadores: le toca el índice 1, Sofía).
        #expect(repo.golesRegistrados.count == 1)
        #expect(repo.golesRegistrados.first?.partidoId == 1)
        guard case let .gol(minuto, jugador, equipo)? = repo.golesRegistrados.first?.gol else {
            Issue.record("Se esperaba un gol")
            return
        }
        #expect(minuto == 0)
        #expect(jugador.nombre == "Sofía")
        #expect(equipo == Ejemplo.toros)
    }

    // El ViewModel pregunta por SU partido, no por otro.
    @Test func elViewModelPideAlRepositorioSuPartido() {
        let repo = repositorio()
        _ = PartidoViewModel(partidoId: 1, repositorio: repo, msPorMinuto: 0)
        #expect(repo.consultas == [1])
    }

}
