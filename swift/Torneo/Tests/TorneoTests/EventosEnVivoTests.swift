import Testing
@testable import Torneo

struct EventosEnVivoTests {
    private func recoger<S: AsyncSequence>(_ secuencia: S) async throws -> [S.Element] {
        var recogidos: [S.Element] = []
        for try await elemento in secuencia { recogidos.append(elemento) }
        return recogidos
    }

    @Test func elStreamEmiteTodosLosEventosDelGuionEnOrden() async throws {
        let enVivo = PartidoEnVivo(guion: Ejemplo.rayoContraToros, msPorMinuto: 1)
        let eventos = try await recoger(enVivo.eventos)
        #expect(eventos == Ejemplo.rayoContraToros.eventos)
    }

    @Test func crearElStreamNoArrancaElPartido() async throws {
        let reloj = ContinuousClock()
        let inicio = reloj.now
        let enVivo = PartidoEnVivo(guion: Ejemplo.rayoContraToros, msPorMinuto: 10)
        _ = enVivo.eventos   // frío: no hace nada hasta recorrerlo
        _ = enVivo.narracion
        #expect(milisegundos(inicio.duration(to: reloj.now)) < 50)
    }

    @Test func cadaRecorridoEsUnPartidoNuevo() async throws {
        let enVivo = PartidoEnVivo(guion: Ejemplo.rayoContraToros, msPorMinuto: 1)
        let primero = try await recoger(enVivo.eventos)
        let segundo = try await recoger(enVivo.eventos)
        #expect(primero.count == 5)
        #expect(segundo.count == 5)
    }

    @Test func narracionYFiltroDeGoles() async throws {
        let enVivo = PartidoEnVivo(guion: Ejemplo.rayoContraToros, msPorMinuto: 1)
        let lineas = try await recoger(enVivo.narracion)
        #expect(lineas.first == "12' Gol de Ana (Rayo FC)")
        #expect(lineas.count == 5)
        let goles = try await recoger(enVivo.golesEnFrio)
        #expect(goles.count == 3)
    }

    @Test func primerGolCancelaElResto() async {
        var terminado = false
        let enVivo = PartidoEnVivo(guion: Ejemplo.rayoContraToros, msPorMinuto: 5, alTerminarLaEmision: { terminado = true })
        let reloj = ContinuousClock()
        let inicio = reloj.now
        let gol = await enVivo.primerGol()
        let ms = milisegundos(inicio.duration(to: reloj.now))
        #expect(gol == Ejemplo.rayoContraToros.eventos[0])
        #expect(ms < 300)   // el partido entero son ~450 ms
        // El productor se cancela: `onTermination` corre casi al instante.
        for _ in 0..<50 where !terminado { try? await Task.sleep(for: .milliseconds(10)) }
        #expect(terminado)
    }

    @Test func cancelarAlConsumidorCancelaAlProductor() async {
        var terminado = false
        let enVivo = PartidoEnVivo(guion: Ejemplo.rayoContraToros, msPorMinuto: 10, alTerminarLaEmision: { terminado = true })
        let consumidor = Task { () -> Int in
            var vistos = 0
            for await _ in enVivo.eventos { vistos += 1 }
            return vistos
        }
        try? await Task.sleep(for: .milliseconds(200))   // minuto ~20: solo el gol del 12
        consumidor.cancel()
        let vistos = await consumidor.value
        #expect(vistos < 5)
        for _ in 0..<50 where !terminado { try? await Task.sleep(for: .milliseconds(10)) }
        #expect(terminado)
    }

    @Test func losMarcadoresEmpiezanEnCeroYSoloCambianConUnGol() async throws {
        let enVivo = PartidoEnVivo(guion: Ejemplo.rayoContraToros, msPorMinuto: 1)
        let marcadores = try await recoger(enVivo.marcadores)
        #expect(marcadores.map(\.description) == ["0-0", "1-0", "1-1", "2-1"])
    }

    @Test func marcadorDespuesDeUnEvento() {
        let base = Marcador(local: 0, visitante: 0)
        let partido = Ejemplo.rayoContraToros
        #expect(base.despuesDe(.gol(minuto: 1, jugador: Ejemplo.ana, equipo: Ejemplo.rayo), en: partido) == Marcador(local: 1, visitante: 0))
        #expect(base.despuesDe(.gol(minuto: 1, jugador: Ejemplo.ivan, equipo: Ejemplo.toros), en: partido) == Marcador(local: 0, visitante: 1))
        #expect(base.despuesDe(.tarjeta(minuto: 2, jugador: Ejemplo.ivan, color: .roja), en: partido) == base)
        #expect(partido.marcador().description == "2-1")
        #expect(Marcador(local: 2, visitante: 1).goles == 3)
    }
}
