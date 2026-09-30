import Testing
import Torneo
@testable import MarcadorApp

// f75 · Pruebas del ViewModel con el repositorio en memoria (en f79 se sustituye por un falso).
// `@MainActor`: el ViewModel lo es, y el target de pruebas no tiene el aislamiento por defecto.
@MainActor
struct PartidoViewModelTests {

    private func repositorio() -> RepositorioEnMemoria {
        RepositorioEnMemoria(
            equipos: [Ejemplo.rayo, Ejemplo.toros],
            partidos: [PartidoDeLista(id: 1, partido: Ejemplo.rayoContraToros)]
        )
    }

    @Test func jugarAplicaLosGolesDelGuionYActualizaElEstado() async {
        let viewModel = PartidoViewModel(partidoId: 1, repositorio: repositorio(), msPorMinuto: 0)
        await viewModel.jugar(duracion: 90)
        guard case let .exito(datos) = viewModel.uiState else {
            Issue.record("Se esperaba .exito y salió \(viewModel.uiState)")
            return
        }
        #expect(datos.marcador == Marcador(local: 2, visitante: 1))
        #expect(datos.minuto == 90)
        #expect(!datos.corriendo)
        #expect(datos.ultimoAviso == "minuto 90 con 2-1")
    }

    @Test func unPartidoDeSesentaMinutosIgnoraElGolDelMinutoOchenta() async {
        let viewModel = PartidoViewModel(partidoId: 1, repositorio: repositorio(), msPorMinuto: 0)
        await viewModel.jugar(duracion: 60)
        #expect(viewModel.marcador == Marcador(local: 1, visitante: 1))
    }

    @Test func unPartidoQueNoExisteDaUnEstadoDeError() {
        let viewModel = PartidoViewModel(partidoId: 99, repositorio: repositorio(), msPorMinuto: 0)
        #expect(viewModel.uiState == .error("No existe el partido n.º 99"))
    }

    @Test func unGolAManoSeVeYSeGuardaEnElRepositorio() {
        let repo = repositorio()
        let viewModel = PartidoViewModel(partidoId: 1, repositorio: repo, msPorMinuto: 0)
        viewModel.alGol(.visitante)
        #expect(viewModel.marcador == Marcador(local: 0, visitante: 1))
        // El repositorio ya tiene un gol más (el 2-1 de ejemplo pasa a 2-2).
        #expect(repo.partido(id: 1)?.marcador() == Marcador(local: 2, visitante: 2))
    }

    @Test func cancelarLaTareaDetieneElPartido() async {
        let viewModel = PartidoViewModel(partidoId: 1, repositorio: repositorio(), msPorMinuto: 20)
        let tarea = Task { await viewModel.jugar(duracion: 90) }
        try? await Task.sleep(for: .milliseconds(150))
        tarea.cancel()
        await tarea.value
        #expect(viewModel.minuto < 90)
        #expect(!viewModel.corriendo)
    }

    // f75 · LA PRUEBA DEL CICLO. Con `[weak self]` el ViewModel se libera en cuanto se suelta...
    @Test func elViewModelSeLiberaAlSoltarlo() async {
        weak var referencia: PartidoViewModel?
        do {
            let viewModel = PartidoViewModel(partidoId: 1, repositorio: repositorio(), msPorMinuto: 0, reintroducirElCiclo: false)
            referencia = viewModel
            await viewModel.jugar(duracion: 3)
        }
        #expect(referencia == nil)
    }

    // ...y con el ciclo reintroducido, NO: `referencia` sigue viva aunque nadie tenga el ViewModel.
    // (Este objeto se queda en memoria hasta que termina el proceso de pruebas: es la fuga.)
    @Test func conElCicloReintroducidoElViewModelSeQuedaEnMemoria() async {
        weak var referencia: PartidoViewModel?
        do {
            let viewModel = PartidoViewModel(partidoId: 1, repositorio: repositorio(), msPorMinuto: 0, reintroducirElCiclo: true)
            referencia = viewModel
            await viewModel.jugar(duracion: 3)
        }
        #expect(referencia != nil)
    }
}
