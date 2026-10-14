import Testing
import Torneo
@testable import MarcadorApp

// Las reglas del formulario, sin vista ni ViewModel. La plantilla del Rayo FC: Ana #9, Luis sin
// dorsal y Marta #1.
@MainActor
struct FormularioJugadorTests {
    let rayo = Ejemplo.rayo.plantilla

    func formulario(nombre: String = "", dorsal: String = "") -> FormularioJugador {
        var formulario = FormularioJugador()
        formulario.nombre = nombre
        formulario.dorsal = dorsal
        return formulario
    }

    @Test func unNombreVacioOSoloConEspaciosEsUnError() {
        #expect(formulario(nombre: "").errorNombre(en: rayo) == .vacio)
        #expect(formulario(nombre: "   ").errorNombre(en: rayo) == .vacio)
    }

    @Test func unNombreDeMasDe25LetrasEsDemasiadoLargo() {
        #expect(formulario(nombre: String(repeating: "a", count: 26)).errorNombre(en: rayo) == .largo)
        #expect(formulario(nombre: String(repeating: "a", count: 25)).errorNombre(en: rayo) == nil)
    }

    @Test func unNombreQueYaEstaEnLaPlantillaEsUnError() {
        #expect(formulario(nombre: " ana ").errorNombre(en: rayo) == .repetido)
    }

    @Test func elDorsalEsOpcional() {
        #expect(formulario(nombre: "Bea").errorDorsal(en: rayo) == nil)
        #expect(formulario(nombre: "Bea").esValido(en: rayo))
    }

    @Test(arguments: [("diez", ErrorDorsal.noEsNumero), ("0", .fueraDeRango), ("100", .fueraDeRango), ("9", .repetido)])
    func losDorsalesQueNoValen(texto: String, error: ErrorDorsal) {
        #expect(formulario(nombre: "Bea", dorsal: texto).errorDorsal(en: rayo) == error)
    }

    @Test func escribirMarcaElCampoComoTocado() {
        var formulario = FormularioJugador()
        #expect(!formulario.nombreTocado)
        formulario.nombre = "B"
        #expect(formulario.nombreTocado)
        #expect(!formulario.dorsalTocado)
    }

    @Test func unFormularioValidoDaElJugadorLimpio() {
        #expect(formulario(nombre: "  Bea ", dorsal: " 7").aJugador() == Jugador(nombre: "Bea", dorsal: 7))
    }
}
