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
        let viewModel = PartidoViewModel(partidoId: 99, repositorio: repositorio(), partidoEnJuego: PartidoEnJuego(), msPorMinuto: 0)
        #expect(viewModel.uiState == .error("No existe el partido n.º 99"))
    }

    @Test func unGolAManoSeVeYSeGuardaEnElRepositorio() {
        let repo = repositorio()
        let viewModel = PartidoViewModel(partidoId: 1, repositorio: repo, partidoEnJuego: PartidoEnJuego(), msPorMinuto: 0)
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
        _ = PartidoViewModel(partidoId: 1, repositorio: repo, partidoEnJuego: PartidoEnJuego(), msPorMinuto: 0)
        #expect(repo.consultas == [1])
    }

    // Volver a abrir el partido que se está jugando REUTILIZA su directo: no empieza de cero.
    @Test func otraPantallaDelMismoPartidoReutilizaElDirecto() {
        let enJuego = PartidoEnJuego()
        let primera = PartidoViewModel(partidoId: 1, repositorio: repositorio(), partidoEnJuego: enJuego, msPorMinuto: 60_000)
        primera.empezar()
        let segunda = PartidoViewModel(partidoId: 1, repositorio: repositorio(), partidoEnJuego: enJuego, msPorMinuto: 60_000)
        #expect(segunda.directo === primera.directo)
    }

    // UNO A LA VEZ: la pantalla de otro partido ve el aviso y su «Empezar» no hace nada.
    @Test func otroPartidoVeElAvisoYNoEmpieza() {
        let repo = RepositorioFalso(partidos: [
            PartidoDeLista(id: 1, partido: Ejemplo.rayoContraToros),
            PartidoDeLista(id: 2, partido: Ejemplo.rayoContraToros),
        ])
        let enJuego = PartidoEnJuego()
        PartidoViewModel(partidoId: 1, repositorio: repo, partidoEnJuego: enJuego, msPorMinuto: 60_000).empezar()
        let segundo = PartidoViewModel(partidoId: 2, repositorio: repo, partidoEnJuego: enJuego, msPorMinuto: 60_000)
        guard case let .exito(datos) = segundo.uiState else {
            Issue.record("Se esperaba .exito")
            return
        }
        #expect(datos.otroEnJuego == 1)
        segundo.empezar()
        #expect(enJuego.directo?.partidoId == 1)
    }

    // Mientras se juega, la duración no cambia; sin empezar, sí (un directo nuevo).
    @Test func laDuracionSoloCambiaSiNoSeJuega() {
        let enJuego = PartidoEnJuego()
        let viewModel = PartidoViewModel(partidoId: 1, repositorio: repositorio(), partidoEnJuego: enJuego, msPorMinuto: 60_000)
        viewModel.cambiarDuracion(60)
        #expect(viewModel.directo?.duracion == 60)
        viewModel.empezar()
        viewModel.cambiarDuracion(90)
        #expect(viewModel.directo?.duracion == 60)
    }
}
