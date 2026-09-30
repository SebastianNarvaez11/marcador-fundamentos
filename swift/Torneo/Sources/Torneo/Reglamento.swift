// Un protocolo dice QUÉ se puede hacer, sin decir cómo (la `interface` de
// Kotlin). Quien lo adopte rellena lo que falta. Un protocolo lo pueden adoptar
// structs, enums y clases; una interfaz de Kotlin solo clases y objetos.
public protocol Reglamento: Sendable {
    var puntosPorVictoria: Int { get }
    var puntosPorEmpate: Int { get }

    // Estos dos también son REQUISITOS del protocolo, pero llevan implementación
    // por defecto (en la extensión de abajo): quien adopte el protocolo puede no
    // escribirlos y, si los escribe, se usa el suyo.
    var puntosPorDerrota: Int { get }
    func puntosPor(golesAFavor: Int, golesEnContra: Int) -> Int
}

// Implementación por defecto: una `extension` del protocolo. Es la forma de
// Swift de tener un método con cuerpo en la «interfaz» (Kotlin lo escribe
// directamente dentro de `interface Reglamento`).
extension Reglamento {
    public var puntosPorDerrota: Int { 0 }

    public func puntosPor(golesAFavor: Int, golesEnContra: Int) -> Int {
        switch (golesAFavor, golesEnContra) {
        case let (aFavor, enContra) where aFavor > enContra: puntosPorVictoria
        case let (aFavor, enContra) where aFavor == enContra: puntosPorEmpate
        default: puntosPorDerrota
        }
    }

    // OJO: esto NO es un requisito del protocolo (no está declarado arriba). Si un
    // tipo escribe su propio `resumen()` y se llama a través de `any Reglamento`,
    // se ejecuta ESTE, no el del tipo. Solo los requisitos se despachan
    // dinámicamente. Lo comprueba una prueba (`ReglamentoTests`).
    public func resumen() -> String {
        "\(puntosPorVictoria)/\(puntosPorEmpate)/\(puntosPorDerrota)"
    }
}

// Kotlin usa `object ReglamentoLiga`: una sola instancia. En Swift no existe
// `object`: un `struct` sin datos que cambian hace el mismo papel, y da igual
// cuántas copias haya porque todas son iguales.
public struct ReglamentoLiga: Reglamento, CustomStringConvertible {
    public let puntosPorVictoria = 3
    public let puntosPorEmpate = 1
    public let nombre = "Liga"

    public init() {}

    public var description: String { "Reglamento \(nombre) (\(resumen()))" }
}

// El equivalente de la clase abstracta `ReglamentoPorPuntos` de Kotlin: en
// Swift no hay clases abstractas. Se usa un protocolo (o un protocolo con
// extensión) y, para compartir estado, un struct que se pasa por composición.
public struct ReglamentoPersonalizado: Reglamento {
    public let puntosPorVictoria: Int
    public let puntosPorEmpate: Int

    public init(puntosPorVictoria: Int, puntosPorEmpate: Int) {
        self.puntosPorVictoria = puntosPorVictoria
        self.puntosPorEmpate = puntosPorEmpate
    }
}
