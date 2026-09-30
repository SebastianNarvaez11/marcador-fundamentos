import Foundation

// `Int?` es un Int que puede ser nil (en Kotlin, null): en el amateur hay
// jugadores sin dorsal. Un opcional NO es un Int: es una caja que puede estar
// vacía, y el compilador no deja usarla como número sin abrirla antes.
//
// Es un `struct` (tipo de valor); en f52 se cuenta qué cambia frente a `class`.
// `Equatable` y `Hashable` los sintetiza el compilador (en Kotlin, `data class`).
// `Sendable`: se puede pasar entre tareas sin riesgo (Swift 6, f65). Un struct con
// solo propiedades `let` de tipos Sendable lo es; en un tipo `public` hay que decirlo.
public struct Jugador: Equatable, Hashable, Sendable {
    public let nombre: String
    public let dorsal: Int?

    // El inicializador de un struct público hay que escribirlo: el automático
    // es `internal`. `dorsal: Int? = nil` permite omitir el dorsal.
    public init(nombre: String, dorsal: Int? = nil) {
        self.nombre = nombre
        self.dorsal = dorsal
    }

    // `if let` abre la caja: el bloque solo corre si hay valor, y dentro
    // `numero` ya es un Int normal. `??` es el elvis de Kotlin (`?:`).
    public func dorsalOGuion() -> String {
        if let numero = dorsal {
            return "#\(numero)"
        }
        return "-"
    }

    // Lo mismo con optional chaining y `??`, en una línea. `map` sobre un
    // opcional aplica la función solo si hay valor (el `?.let` de Kotlin).
    public func dorsalOGuionCorto() -> String {
        dorsal.map { "#\($0)" } ?? "-"
    }

    // `!` promete «esto no es nil». Si mientes, el programa se detiene:
    // «Fatal error: Unexpectedly found nil while unwrapping an Optional value».
    // Existe para que la lección lea el fallo; no lo uses en código real.
    public func dorsalForzado() -> Int {
        dorsal!
    }

    // `if let` con comparación: tras abrir la caja, `numero` es un Int.
    public func esPortero() -> Bool {
        if let numero = dorsal {
            return numero == 1
        }
        return false
    }

    // Optional chaining: `?.` encadena; si algo es nil, todo el resultado es nil.
    // El operador ternario `? :` devuelve nil si el nombre está vacío.
    public func longitudDelNombre() -> Int? {
        nombre.isEmpty ? nil : nombre.count
    }
}

// `guard let` abre la caja Y sale de la función si está vacía: lo que sigue
// ya trabaja con `jugador` como Jugador, sin un nivel más de llaves.
// (El smart cast de Kotlin no existe: en Swift se crea una constante nueva.)
public func presentar(_ jugador: Jugador?) -> String {
    guard let jugador else { return "Sin jugador" }
    return "\(jugador.nombre) (\(jugador.dorsalOGuion()))"
}

// `trimmingCharacters` NO es de Swift sino de Foundation: sin `import Foundation`
// arriba, el compilador dice «value of type 'String' has no member 'trimmingCharacters'».
// `Int(texto)` es un inicializador que puede fallar: devuelve `Int?`, nil si el
// texto no es un número. Es el equivalente de `toIntOrNull()` en Kotlin, y por
// eso aquí no hace falta un `try` que atrape nada.
public func dorsalDesdeTexto(_ texto: String) -> Int? {
    Int(texto.trimmingCharacters(in: .whitespaces))
}
