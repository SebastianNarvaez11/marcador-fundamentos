import Synchronization

// `class`: tipo de REFERENCIA. Dos variables pueden apuntar al MISMO equipo, y
// lo que cambie una lo ve la otra. `final` = nadie hereda (en Kotlin las clases
// ya son finales por defecto; en Swift hay que decirlo, y además es más rápido).
//
// Un `struct` habría copiado el equipo en cada asignación. Un equipo tiene
// IDENTIDAD (el Rayo FC es ese equipo, no uno «igual»), así que es una clase.
public final class Equipo: Sendable {
    public let nombre: String
    public let plantilla: [Jugador]

    // Cualquiera lee el capitán, pero solo esta clase lo cambia (el `private set`
    // de Kotlin). Hasta f64 era un `private(set) var`; para que `Equipo` pueda ser
    // `Sendable` (f65) el único dato que cambia vive dentro de un `Mutex`, un
    // cerrojo: solo una tarea a la vez ejecuta el bloque de `withLock`.
    private let capitanGuardado = Mutex<Jugador?>(nil)

    public var capitan: Jugador? { capitanGuardado.withLock { $0 } }

    // `init?` es un inicializador que puede FALLAR: devuelve `Equipo?`, nil si
    // los datos no valen. Es el `require` de Kotlin, pero sin excepción.
    // (f57 cuenta cuándo conviene `throws`: para saber POR QUÉ falló. Aquí basta
    // con `nil`, porque quien crea un equipo solo necesita saber si salió.)
    public init?(nombre: String, plantilla: [Jugador]) {
        guard !nombre.isEmpty else { return nil }
        let dorsales = plantilla.compactMap(\.dorsal)
        guard dorsales.count == Set(dorsales).count else { return nil }
        self.nombre = nombre
        // Los arrays son tipos de VALOR: esta asignación ya es una copia, y la
        // copia defensiva de Kotlin (`toList()`) aquí no hace falta.
        self.plantilla = plantilla
    }

    // Devuelve `false` si el jugador no es de este equipo (en Kotlin, `require`).
    @discardableResult
    public func nombrarCapitan(_ jugador: Jugador) -> Bool {
        guard plantilla.contains(jugador) else { return false }
        capitanGuardado.withLock { $0 = jugador }
        return true
    }

    public var cantidadDeJugadores: Int { plantilla.count }
}

// Una clase NO es Equatable por sí sola: `==` entre dos referencias no compila
// hasta que se diga qué significa. Aquí, «el mismo equipo» = la misma referencia
// (`===`), que es lo que hace Kotlin por defecto con `==` en una clase normal.
extension Equipo: Equatable, Hashable {
    public static func == (izquierda: Equipo, derecha: Equipo) -> Bool {
        izquierda === derecha
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(ObjectIdentifier(self))
    }
}

extension Equipo: CustomStringConvertible {
    public var description: String { "\(nombre) (\(cantidadDeJugadores) jugadores)" }
}
