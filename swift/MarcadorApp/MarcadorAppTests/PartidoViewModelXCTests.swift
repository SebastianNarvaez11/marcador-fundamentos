import XCTest
import Torneo
@testable import MarcadorApp

// LAS MISMAS PRUEBAS, EN XCTEST (el marco anterior a Swift Testing, de 2014).
//
// Es lo que vas a encontrar en casi todo el código que ya existe, y sigue siendo OBLIGATORIO para
// las pruebas de UI y de rendimiento. Diferencias con Swift Testing (`PartidoViewModelTests` y `DirectoTests`):
//
//   Swift Testing                     XCTest
//   struct + @Test                    clase que hereda de XCTestCase; el método debe EMPEZAR por `test`
//   #expect(a == b)                   XCTAssertEqual(a, b)   (una función por tipo de comprobación)
//   Issue.record("…")                 XCTFail("…")
//   init() / deinit                   setUp() / tearDown()   (una instancia NUEVA por prueba, en los dos)
//   @Test(arguments: […])             no hay: se escribe un bucle o varias pruebas
//   se ejecutan en paralelo           en serie por defecto
//
// `@MainActor` en la clase: el ViewModel lo es (en XCTest, además, `setUp` y los `test…` heredan el aislamiento de la clase).
@MainActor
final class PartidoViewModelXCTests: XCTestCase {
    var repositorio: RepositorioFalso!

    override func setUp() async throws {
        repositorio = RepositorioFalso()
    }

    func testJugarAplicaLosGolesDelGuion() async {
        let directo = Directo(partidoId: 1, partido: Ejemplo.rayoContraToros, duracion: 90, msPorMinuto: 0)
        await directo.jugar()
        XCTAssertEqual(directo.marcador, Marcador(local: 2, visitante: 1))
        XCTAssertEqual(directo.minuto, 90)
        XCTAssertFalse(directo.corriendo)
    }

    func testUnPartidoQueNoExisteDaUnEstadoDeError() {
        let viewModel = PartidoViewModel(partidoId: 99, repositorio: repositorio, partidoEnJuego: PartidoEnJuego(), msPorMinuto: 0)
        XCTAssertEqual(viewModel.uiState, .error("No existe el partido n.º 99"))
    }

    func testElViewModelSeLiberaAlSoltarlo() async {
        weak var referencia: PartidoViewModel?
        do {
            let viewModel = PartidoViewModel(partidoId: 1, repositorio: repositorio, partidoEnJuego: PartidoEnJuego(), msPorMinuto: 0)
            referencia = viewModel
        }
        XCTAssertNil(referencia)
    }

    // XCTest trae MEDICIÓN de rendimiento (`measure`), que Swift Testing no tiene: ejecuta el bloque
    // varias veces y compara con una línea base. Aquí, jugar 90 minutos sin esperas.
    func testRendimientoDeJugarUnPartidoEntero() {
        measure {
            let directo = Directo(partidoId: 1, partido: Ejemplo.rayoContraToros, duracion: 90, msPorMinuto: 0)
            let terminado = expectation(description: "el partido termina")
            Task { @MainActor in
                await directo.jugar()
                terminado.fulfill()
            }
            wait(for: [terminado], timeout: 10)
        }
    }
}
