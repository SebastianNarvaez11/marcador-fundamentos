import Foundation
import Observation
import Synchronization
import Testing
import Torneo
@testable import MarcadorApp

// Un objeto `@Observable` mínimo para la prueba de observación.
@Observable final class Contador { var valor = 0 }

// El modelo es de la app y la app aísla todo a MainActor por defecto; el target de pruebas NO:
// hay que decirlo a mano.
@MainActor
struct MarcadorAppTests {

    // El asistente de Xcode genera un `example()` vacío. Éste comprueba lo mismo que
    // importa aquí: que el paquete `Torneo` llega hasta el target de pruebas de la app.
    @Test func elPaqueteTorneoLlegaALaApp() {
        let partido = Ejemplo.rayoContraToros
        #expect(partido.marcador() == Marcador(local: 2, visitante: 1))
    }

    // Un gol no modifica el marcador de antes: devuelve otro, y el de antes sigue igual.
    @Test func sumarUnGolCreaOtroMarcador() {
        let partido = Ejemplo.rayoContraToros
        let antes = Marcador(local: 0, visitante: 0)
        // Un gol del Rayo FC (el local) en el minuto 12.
        let gol = EventoDePartido.gol(minuto: 12, jugador: Ejemplo.ana, equipo: partido.local)
        let despues = antes.despuesDe(gol, en: partido)
        #expect(antes == Marcador(local: 0, visitante: 0))     // el de antes no cambió
        #expect(despues == Marcador(local: 1, visitante: 0))   // el nuevo lleva el gol
        #expect(despues != antes)                              // y son dos marcadores distintos
    }

    // Los datos de ejemplo son los mismos que en Android: 18 partidos con id único.
    @Test func losDatosDeEjemploTienenDieciochoPartidosConIdUnico() {
        let partidos = DatosDeEjemplo.partidos
        #expect(partidos.count == 18)
        #expect(Set(partidos.map(\.id)).count == 18)
        #expect(partidos.first?.id == 1)
    }

    // `@Observable` avisa cuando cambia lo que se LEYÓ dentro de `withObservationTracking`.
    @Test func observableAvisaSoloDeLoQueSeLee() {
        let contador = Contador()
        // `Mutex`: el `onChange` es @Sendable y no puede mutar una `var` capturada.
        let avisos = Mutex(0)
        withObservationTracking {
            _ = contador.valor
        } onChange: {
            avisos.withLock { $0 += 1 }
        }
        contador.valor = 60
        #expect(avisos.withLock { $0 } == 1)
        #expect(contador.valor == 60)
    }

    // Los goleadores se calculan de los partidos: la suma de sus goles es la de todos los partidos.
    @Test func losGoleadoresSumanLosGolesDeTodosLosPartidos() {
        let repositorio = RepositorioDePartidos()
        let golesTotales = repositorio.partidos.reduce(0) { $0 + $1.partido.golesLocal + $1.partido.golesVisitante }
        let golesDeGoleadores = repositorio.goleadores().reduce(0) { $0 + $1.goles }
        #expect(golesDeGoleadores == golesTotales)
        // Ordenados de más a menos goles.
        let goles = repositorio.goleadores().map(\.goles)
        #expect(goles == goles.sorted(by: >))
    }

    // El torneo se guarda como JSON en un fichero y se vuelve a leer igual.
    @Test func elTorneoSobreviveAGuardarseYCargarse() throws {
        let carpeta = FileManager.default.temporaryDirectory.appending(path: UUID().uuidString)
        try FileManager.default.createDirectory(at: carpeta, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: carpeta) }
        let almacen = AlmacenDelTorneo(url: carpeta.appending(path: "torneo.json"))
        #expect(!almacen.existe)

        let repositorio = RepositorioDePartidos(almacen: almacen)
        repositorio.registrarGol(partidoId: 1, gol: .gol(minuto: 5, jugador: Ejemplo.ana, equipo: Ejemplo.rayo))
        #expect(almacen.existe)
        let esperado = repositorio.partido(id: 1)?.marcador()

        // Otro repositorio, que arranca leyendo el fichero: mismo partido, mismo marcador.
        let recargado = RepositorioDePartidos(almacen: almacen)
        #expect(recargado.partidos.count == 18)
        #expect(recargado.partido(id: 1)?.marcador() == esperado)
    }

    // Un JSON roto no impide arrancar: se usan los datos de ejemplo.
    @Test func unJsonRotoNoImpideArrancar() throws {
        let url = FileManager.default.temporaryDirectory.appending(path: "\(UUID().uuidString).json")
        try Data("esto no es json".utf8).write(to: url)
        defer { try? FileManager.default.removeItem(at: url) }
        let repositorio = RepositorioDePartidos(almacen: AlmacenDelTorneo(url: url))
        #expect(repositorio.partidos.count == 18)
    }
}
