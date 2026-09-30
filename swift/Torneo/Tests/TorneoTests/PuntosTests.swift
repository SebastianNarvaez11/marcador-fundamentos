import Testing
@testable import Torneo

// Swift Testing: cada función con `@Test` es una prueba y `#expect` comprueba
// una condición. No hay clases ni `assertEquals`: se escribe la expresión.
struct PuntosTests {
    @Test func victoriaDaTresPuntos() {
        #expect(puntosPor(golesAFavor: 2, golesEnContra: 1) == 3)
    }

    @Test func empateDaUnPunto() {
        #expect(puntosPor(golesAFavor: 1, golesEnContra: 1) == 1)
        #expect(puntosPor(golesAFavor: 0, golesEnContra: 0) == 1)
    }

    @Test func derrotaNoDaPuntos() {
        #expect(puntosPor(golesAFavor: 0, golesEnContra: 3) == 0)
    }

    // Una prueba parametrizada: una ejecución por cada tupla de `arguments`.
    @Test(arguments: [(3, 0, 3), (2, 2, 1), (0, 1, 0), (5, 4, 3)])
    func tablaDePuntos(golesAFavor: Int, golesEnContra: Int, esperados: Int) {
        #expect(puntosPor(golesAFavor: golesAFavor, golesEnContra: golesEnContra) == esperados)
    }
}
