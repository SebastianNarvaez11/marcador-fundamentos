import Testing
import Torneo
@testable import MarcadorApp

// El caso de uso, probado solo con el falso: sin ViewModel, sin pantalla, sin tiempo.
@MainActor
struct RegistrarGolTests {

    @Test func marcaElJugadorQueTocaPorTurno() throws {
        let repo = RepositorioFalso()
        let registrar = RegistrarGol(repositorio: repo)
        // Rayo lleva 2 goles y tiene 3 jugadores: toca el índice 2 (Marta).
        let gol = try registrar(partidoId: 1, lado: .local, minuto: 30).get()
        guard case let .gol(_, jugador, _) = gol else {
            Issue.record("Se esperaba un gol")
            return
        }
        #expect(jugador == Ejemplo.marta)
        #expect(repo.golesRegistrados.count == 1)
    }

    @Test func unPartidoQueNoExisteEsUnFalloEsperableNoUnaExcepcion() {
        let repo = RepositorioFalso()
        let resultado = RegistrarGol(repositorio: repo)(partidoId: 99, lado: .local, minuto: 1)
        #expect(resultado == .failure(.noExisteElPartido(99)))
        #expect(repo.golesRegistrados.isEmpty)
    }

    @Test func unEquipoSinJugadoresNoPuedeMarcar() {
        let vacio = Equipo(nombre: "Fantasmas", plantilla: [])!
        let partido = Partido(local: vacio, visitante: Ejemplo.toros)
        let repo = RepositorioFalso(partidos: [PartidoDeLista(id: 5, partido: partido)])
        let resultado = RegistrarGol(repositorio: repo)(partidoId: 5, lado: .local, minuto: 1)
        #expect(resultado == .failure(.equipoSinJugadores("Fantasmas")))
    }

    // Una prueba parametrizada: la MISMA prueba con varios datos (aparece como un caso por argumento).
    @Test(arguments: [Lado.local, Lado.visitante])
    func cadaLadoRegistraUnGolDeSuEquipo(lado: Lado) throws {
        let repo = RepositorioFalso()
        let gol = try RegistrarGol(repositorio: repo)(partidoId: 1, lado: lado, minuto: 10).get()
        guard case let .gol(_, _, equipo) = gol else {
            Issue.record("Se esperaba un gol")
            return
        }
        #expect(equipo == (lado == .local ? Ejemplo.rayo : Ejemplo.toros))
    }
}
