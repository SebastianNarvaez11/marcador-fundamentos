import Testing
@testable import Torneo

struct JugadorTests {
    let ana = Jugador(nombre: "Ana", dorsal: 9)
    let luis = Jugador(nombre: "Luis")

    @Test func dorsalOGuion() {
        #expect(ana.dorsalOGuion() == "#9")
        #expect(luis.dorsalOGuion() == "-")
        #expect(ana.dorsalOGuionCorto() == "#9")
        #expect(luis.dorsalOGuionCorto() == "-")
    }

    @Test func esPortero() {
        #expect(Jugador(nombre: "Marta", dorsal: 1).esPortero())
        #expect(!ana.esPortero())
        #expect(!luis.esPortero())
    }

    @Test func longitudDelNombreEsNilConNombreVacio() {
        #expect(ana.longitudDelNombre() == 3)
        #expect(Jugador(nombre: "").longitudDelNombre() == nil)
    }

    @Test func presentarConGuardLet() {
        #expect(presentar(ana) == "Ana (#9)")
        #expect(presentar(luis) == "Luis (-)")
        #expect(presentar(nil) == "Sin jugador")
    }

    @Test func dorsalDesdeTextoDevuelveNilSiNoEsNumero() {
        #expect(dorsalDesdeTexto("10") == 10)
        #expect(dorsalDesdeTexto(" 7 ") == 7)
        #expect(dorsalDesdeTexto("diez") == nil)
        #expect(dorsalDesdeTexto("") == nil)
    }

    @Test func dorsalForzadoConValor() {
        #expect(ana.dorsalForzado() == 9)
    }

    // Igual que en Kotlin, dos jugadores con los mismos datos son iguales.
    @Test func igualdadPorValor() {
        #expect(ana == Jugador(nombre: "Ana", dorsal: 9))
        #expect(ana != Jugador(nombre: "Ana", dorsal: 10))
    }
}
