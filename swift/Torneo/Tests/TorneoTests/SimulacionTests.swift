import Testing
@testable import Torneo

struct SimulacionTests {
    // `async throws`: las pruebas también pueden ser asíncronas.
    @Test func jugarPartidoReproduceElGuion() async throws {
        let final = try await jugarPartido(Ejemplo.rayoContraToros, msPorMinuto: 1)
        #expect(final.eventos == Ejemplo.rayoContraToros.eventos)
        #expect(final.golesLocal == 2)
        #expect(final.golesVisitante == 1)
        #expect(final.resultado().titular == "Gana Rayo FC")
    }

    @Test func unMinutoSonLosMilisegundosPedidos() async throws {
        let reloj = ContinuousClock()
        let duracion = try await reloj.measure {
            _ = try await jugarPartido(Ejemplo.rayoContraToros, msPorMinuto: 5)
        }
        // 90 minutos de 5 ms = 450 ms como mínimo (las esperas nunca son más cortas).
        #expect(milisegundos(duracion) >= 450)
    }

    @Test func unEventoDelMinutoCeroSeRegistraAlPrincipio() async throws {
        let ana = Ejemplo.ana
        let guion = Partido(local: Ejemplo.rayo, visitante: Ejemplo.toros, eventos: [.gol(minuto: 0, jugador: ana, equipo: Ejemplo.rayo)])
        let final = try await jugarPartido(guion, msPorMinuto: 1)
        #expect(final.golesLocal == 1)
    }

    @Test func unEventoDespuesDelNoventaAlargaElPartido() async throws {
        let guion = Partido(local: Ejemplo.rayo, visitante: Ejemplo.toros, eventos: [.gol(minuto: 93, jugador: Ejemplo.ana, equipo: Ejemplo.rayo)])
        let final = try await jugarPartido(guion, msPorMinuto: 1)
        #expect(final.eventos.count == 1)
    }

    @Test func cancelarLaTareaCortaElPartido() async {
        let reloj = ContinuousClock()
        let inicio = reloj.now
        let tarea = Task { try await jugarPartido(Ejemplo.rayoContraToros, msPorMinuto: 10) }
        try? await Task.sleep(for: .milliseconds(50))
        tarea.cancel()
        let resultado = await tarea.result
        let duracion = milisegundos(inicio.duration(to: reloj.now))
        if case let .failure(error) = resultado {
            #expect(error is CancellationError)
        } else {
            Issue.record("Debía fallar con CancellationError")
        }
        // El partido entero dura ~1000 ms; cancelado a los 50 ms tiene que acabar mucho antes.
        #expect(duracion < 500)
    }

    @Test func unBucleSinEsperasSoloSeCancelaSiMiraCheckCancellation() async {
        let reloj = ContinuousClock()
        let inicio = reloj.now
        let tarea = Task { try await probabilidadDeVictoria(Ejemplo.rayoContraToros, simulaciones: 2_000_000_000) }
        try? await Task.sleep(for: .milliseconds(100))
        tarea.cancel()
        let resultado = await tarea.result
        let duracion = milisegundos(inicio.duration(to: reloj.now))
        if case let .failure(error) = resultado {
            #expect(error is CancellationError)
        } else {
            Issue.record("Debía cancelarse")
        }
        #expect(duracion < 3_000)
    }

    @Test func laProbabilidadEstaEntreCeroYUno() async throws {
        let probabilidad = try await probabilidadDeVictoria(Ejemplo.rayoContraToros, simulaciones: 10_000)
        #expect((0.0...1.0).contains(probabilidad))
        #expect(probabilidad > 0.5)   // el local ganaba 2-1: fuerza 3 contra 2
    }
}
