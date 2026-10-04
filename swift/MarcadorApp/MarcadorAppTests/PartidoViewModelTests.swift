import Testing
import Torneo
@testable import MarcadorApp

// Pruebas del ViewModel con el repositorio en memoria (en las otras pruebas, un falso).
// `@MainActor`: el ViewModel lo es, y el target de pruebas no tiene el aislamiento por defecto.
@MainActor
struct PartidoViewModelTests {

    // El repositorio es un FALSO (ver `Dobles.swift`): sin disco y con los goles anotados.
    private func repositorio() -> RepositorioFalso { RepositorioFalso() }

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

    @Test func cancelarLaTareaDetieneElPartido() async {
        let viewModel = PartidoViewModel(partidoId: 1, repositorio: repositorio(), msPorMinuto: 20)
        let tarea = Task { await viewModel.jugar(duracion: 90) }
        try? await Task.sleep(for: .milliseconds(150))
        tarea.cancel()
        await tarea.value
        #expect(viewModel.minuto < 90)
        #expect(!viewModel.corriendo)
    }

    // LA PRUEBA DEL CICLO. Con `[weak self]` el ViewModel se libera en cuanto se suelta...
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

    // Cada gol del guion avisa: goles de Ana (12), Iván (55) y Ana (80).
    @Test func cadaGolDelGuionAvisaAlNotificador() async {
        let espia = NotificadorEspia()
        let viewModel = PartidoViewModel(partidoId: 1, repositorio: repositorio(), notificador: espia, msPorMinuto: 0)
        await viewModel.jugar(duracion: 90)
        #expect(espia.avisos.map(\.minuto) == [12, 55, 80])
        #expect(espia.avisos.map(\.jugador) == ["Ana", "Iván", "Ana"])
        #expect(espia.avisos.first?.equipo == "Rayo FC")
    }

    // Los goles «a mano» no avisan, y un partido de 60 minutos no avisa del gol del minuto 80.
    @Test func losGolesAManoNoAvisanYLosPosterioresALaDuracionTampoco() async {
        let espia = NotificadorEspia()
        let viewModel = PartidoViewModel(partidoId: 1, repositorio: repositorio(), notificador: espia, msPorMinuto: 0)
        viewModel.alGol(.local)
        #expect(espia.avisos.isEmpty)
        await viewModel.jugar(duracion: 60)
        #expect(espia.avisos.map(\.minuto) == [12, 55])
    }
}
