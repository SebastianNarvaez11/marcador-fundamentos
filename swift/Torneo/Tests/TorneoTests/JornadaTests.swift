import Testing
@testable import Torneo

struct JornadaTests {
    @Test func dosPartidosConAsyncLetDuranLoQueElMasLargo() async throws {
        let reloj = ContinuousClock()
        var jugados: (Partido, Partido)?
        let duracion = try await reloj.measure {
            jugados = try await jugarJornada(Ejemplo.rayoContraToros, Ejemplo.lobosContraAguilas, msPorMinuto: 5)
        }
        let (uno, otro) = try #require(jugados)
        #expect(uno.golesLocal == 2 && uno.golesVisitante == 1)
        #expect(otro.golesLocal == 1 && otro.golesVisitante == 2)
        // Uno solo son ~450 ms; los dos seguidos serían ~900 ms. A la vez, bastante menos.
        let ms = milisegundos(duracion)
        #expect(ms >= 450)
        #expect(ms < 800)
    }

    @Test func estadisticasEnParalelo() async throws {
        let reloj = ContinuousClock()
        var estadisticas: Estadisticas?
        let duracion = try await reloj.measure {
            estadisticas = try await estadisticasDe(Ejemplo.rayoContraToros, msPorConsulta: 100)
        }
        #expect(estadisticas == Estadisticas(goles: 3, tarjetas: 1, cambios: 1))
        // Tres consultas de 100 ms a la vez: ~100 ms, no ~300 ms.
        #expect(milisegundos(duracion) < 250)
    }

    @Test func taskGroupDevuelveLosPartidosEnElOrdenOriginal() async throws {
        // El segundo es más corto en tiempo real, pero tiene que salir segundo.
        let jugados = try await jugarJornada([Ejemplo.rayoContraToros, Ejemplo.lobosContraAguilas, Ejemplo.rayoContraToros], msPorMinuto: 1)
        #expect(jugados.count == 3)
        #expect(jugados.map(\.local.nombre) == ["Rayo FC", "Lobos", "Rayo FC"])
    }

    @Test func taskGroupConLaListaVacia() async throws {
        #expect(try await jugarJornada([], msPorMinuto: 1).isEmpty)
    }

    @Test func siUnaTareaHijaFallaElGrupoCancelaALasDemas() async {
        let reloj = ContinuousClock()
        let inicio = reloj.now
        do {
            _ = try await withThrowingTaskGroup(of: Partido.self) { grupo in
                grupo.addTask { try await jugarConApagon(Ejemplo.lobosContraAguilas, msPorMinuto: 5, minutoDelApagon: 10) }
                grupo.addTask { try await jugarPartido(Ejemplo.rayoContraToros, msPorMinuto: 5) }   // ~450 ms
                return try await grupo.next()
            }
            Issue.record("Debía lanzar SuspendidoPorApagon")
        } catch {
            #expect(error as? SuspendidoPorApagon == SuspendidoPorApagon(minuto: 10))
        }
        // El apagón llega a los 50 ms; si la hermana no se cancelara, tardaría ~450 ms.
        #expect(milisegundos(inicio.duration(to: reloj.now)) < 300)
    }

    @Test func laJornadaSupervisadaAguantaElFallo() async throws {
        let resultados = try await jugarJornadaSupervisada([Ejemplo.rayoContraToros, Ejemplo.lobosContraAguilas]) { partido in
            if partido.local == Ejemplo.lobos {
                try await jugarConApagon(partido, msPorMinuto: 1, minutoDelApagon: 30)
            } else {
                try await jugarPartido(partido, msPorMinuto: 1)
            }
        }
        #expect(resultados.count == 2)
        if case let .success(partido) = resultados[0] {
            #expect(partido.golesLocal == 2)
        } else {
            Issue.record("El primero debía terminar")
        }
        if case let .failure(error) = resultados[1] {
            #expect(error as? SuspendidoPorApagon == SuspendidoPorApagon(minuto: 30))
        } else {
            Issue.record("El segundo debía fallar")
        }
    }

    @Test func laCancelacionNoSeConvierteEnResultado() async {
        let tarea = Task {
            try await jugarJornadaSupervisada([Ejemplo.rayoContraToros]) { try await jugarPartido($0, msPorMinuto: 10) }
        }
        try? await Task.sleep(for: .milliseconds(50))
        tarea.cancel()
        let resultado = await tarea.result
        if case let .failure(error) = resultado {
            #expect(error is CancellationError)
        } else {
            Issue.record("Debía propagar la CancellationError")
        }
    }
}
