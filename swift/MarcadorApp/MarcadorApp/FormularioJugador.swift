import Foundation
import Torneo

// EL FORMULARIO «AÑADIR JUGADOR», COMO DATOS (gemelo de `FormularioJugador.kt`).
//
// Un `struct`: lo que el usuario ESCRIBIÓ, tal cual, y qué campos ha TOCADO. Los errores no se
// guardan: se CALCULAN a partir del texto y de la plantilla guardada.
struct FormularioJugador: Equatable {
    static let maximoNombre = 25

    // `didSet` corre cada vez que se escribe en la propiedad; `oldValue` es el valor de antes. Solo
    // si el texto cambió de verdad el campo queda tocado. Así el `TextField` puede escribir
    // DIRECTAMENTE aquí (`$viewModel.formulario.nombre`).
    var nombre = "" { didSet { if nombre != oldValue { nombreTocado = true } } }
    var dorsal = "" { didSet { if dorsal != oldValue { dorsalTocado = true } } }
    private(set) var nombreTocado = false
    private(set) var dorsalTocado = false

    func errorNombre(en plantilla: [Jugador]) -> ErrorNombre? {
        let limpio = nombre.trimmingCharacters(in: .whitespaces)
        if limpio.isEmpty { return .vacio }
        if limpio.count > Self.maximoNombre { return .largo }
        if plantilla.contains(where: { $0.nombre.caseInsensitiveCompare(limpio) == .orderedSame }) { return .repetido }
        return nil
    }

    func errorDorsal(en plantilla: [Jugador]) -> ErrorDorsal? {
        if dorsal.trimmingCharacters(in: .whitespaces).isEmpty { return nil }   // opcional
        guard let numero = dorsalDesdeTexto(dorsal) else { return .noEsNumero }
        if !(1...99).contains(numero) { return .fueraDeRango }
        if plantilla.contains(where: { $0.dorsal == numero }) { return .repetido }
        return nil
    }

    func esValido(en plantilla: [Jugador]) -> Bool {
        errorNombre(en: plantilla) == nil && errorDorsal(en: plantilla) == nil
    }

    func aJugador() -> Jugador {
        Jugador(nombre: nombre.trimmingCharacters(in: .whitespaces), dorsal: dorsalDesdeTexto(dorsal))
    }
}

// Un `enum` por campo; la vista elige el texto de cada caso.
enum ErrorNombre: Equatable { case vacio, largo, repetido }
enum ErrorDorsal: Equatable { case noEsNumero, fueraDeRango, repetido }
