import XCTest
import Torneo
@testable import MarcadorApp

// f79 · LAS MISMAS PRUEBAS, EN XCTEST (el marco anterior a Swift Testing, de 2014).
//
// Es lo que vas a encontrar en casi todo el código que ya existe, y sigue siendo OBLIGATORIO para
// las pruebas de UI y de rendimiento. Diferencias con Swift Testing (`PartidoViewModelTests`):
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
        let viewModel = PartidoViewModel(partidoId: 1, repositorio: repositorio, msPorMinuto: 0)
        await viewModel.jugar(duracion: 90)
        XCTAssertEqual(viewModel.marcador, Marcador(local: 2, visitante: 1))
        XCTAssertEqual(viewModel.minuto, 90)
        XCTAssertFalse(viewModel.corriendo)
    }

    func testUnPartidoQueNoExisteDaUnEstadoDeError() {
        let viewModel = PartidoViewModel(partidoId: 99, repositorio: repositorio, msPorMinuto: 0)
        XCTAssertEqual(viewModel.uiState, .error("No existe el partido n.º 99"))
    }

    func testElViewModelSeLiberaAlSoltarlo() async {
        weak var referencia: PartidoViewModel?
        do {
            let viewModel = PartidoViewModel(partidoId: 1, repositorio: repositorio, msPorMinuto: 0, reintroducirElCiclo: false)
            referencia = viewModel
            await viewModel.jugar(duracion: 3)
        }
        XCTAssertNil(referencia)
    }

    // XCTest trae MEDICIÓN de rendimiento (`measure`), que Swift Testing no tiene: ejecuta el bloque
    // varias veces y compara con una línea base. Aquí, jugar 90 minutos sin esperas.
    func testRendimientoDeJugarUnPartidoEntero() {
        measure {
            let viewModel = PartidoViewModel(partidoId: 1, repositorio: repositorio, msPorMinuto: 0)
            let terminado = expectation(description: "el partido termina")
            Task { @MainActor in
                await viewModel.jugar(duracion: 90)
                terminado.fulfill()
            }
            wait(for: [terminado], timeout: 10)
        }
    }
}
