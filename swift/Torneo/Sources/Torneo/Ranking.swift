// Un protocolo con TIPO ASOCIADO: `Elemento` es un hueco que cada tipo que lo
// adopta rellena con el suyo. Es lo que Kotlin expresa con `interface X<T>`;
// en Swift los protocolos no llevan `<T>` sino `associatedtype`.
public protocol Clasificacion {
    associatedtype Elemento
    var ordenados: [Elemento] { get }
}

// `<T>` es un parámetro de tipo: el Ranking sirve igual para Jugador (goleadores)
// que para la fila de la tabla, y el compilador sabe en cada caso qué es T.
// Se usa como `Ranking<Jugador>`: el tipo real sustituye a T.
public struct Ranking<T>: Clasificacion {
    public typealias Elemento = T

    // Cómo se puntúa un elemento: cada uso lo decide (goles, puntos de la liga…).
    // Se GUARDA, por eso el parámetro del `init` es `@escaping`.
    private let puntos: (T) -> Int

    // De más a menos puntos y, a igualdad, según el desempate.
    public let ordenados: [T]

    // `desempate` es como el de `sorted(by:)`: «¿va `a` antes que `b`?».
    public init(
        elementos: [T],
        puntos: @escaping (T) -> Int,
        desempate: (T, T) -> Bool = { _, _ in false }
    ) {
        self.puntos = puntos
        self.ordenados = elementos.sorted { a, b in
            let puntosDeA = puntos(a)
            let puntosDeB = puntos(b)
            return puntosDeA != puntosDeB ? puntosDeA > puntosDeB : desempate(a, b)
        }
    }

    public func puntosDe(_ elemento: T) -> Int { puntos(elemento) }

    public func top(_ cantidad: Int) -> [T] { Array(ordenados.prefix(cantidad)) }
}

// Ranking es una `Sequence`: se puede recorrer con `for … in`. El tipo asociado
// `Element` de Sequence se deduce solo de `makeIterator()`.
extension Ranking: Sequence {
    public func makeIterator() -> IndexingIterator<[T]> { ordenados.makeIterator() }
}

// Extensión CONDICIONAL: este método solo existe cuando `T` es `Equatable`.
// Para saber si un elemento «está» hay que poder compararlo; con un `T`
// cualquiera no se puede (en Kotlin, `!in` usa `equals` de cualquier objeto).
extension Ranking where T: Equatable {
    // Puesto con empates: dos con los mismos puntos comparten puesto (1, 1, 3…).
    // Devuelve nil si el elemento no está en el ranking.
    public func puesto(de elemento: T) -> Int? {
        guard ordenados.contains(elemento) else { return nil }
        let suyos = puntos(elemento)
        guard let primero = ordenados.firstIndex(where: { puntos($0) == suyos }) else { return nil }
        return primero + 1
    }
}

// Una función genérica sobre el protocolo con tipo asociado. `C.Elemento` es el
// tipo que ese `C` haya elegido.
public func lider<C: Clasificacion>(_ clasificacion: C) -> C.Elemento? {
    clasificacion.ordenados.first
}

// `some Sequence<Jugador>`: «algún tipo concreto que es una secuencia de
// Jugador, que el compilador conoce». Es una función genérica escrita más corta.
public func nombres(de jugadores: some Sequence<Jugador>) -> [String] {
    jugadores.map(\.nombre)
}

// `any Clasificacion`: «cualquier cosa que sea una clasificación», con el tipo
// concreto BORRADO en tiempo de ejecución. Permite una lista de tipos distintos,
// a cambio de perder el tipo del `Elemento` y de un pequeño coste al usarlo.
public func cuantosPuestosHay(_ clasificaciones: [any Clasificacion]) -> Int {
    clasificaciones.reduce(0) { $0 + $1.ordenados.count }
}

// Tabla de goleadores: otro Ranking, esta vez de Jugador puntuado por sus goles.
extension Array where Element == Partido {
    public func goleadores() -> Ranking<Jugador> {
        // `reduce(into:)` construye un diccionario jugador -> goles.
        let goles = flatMap { $0.goleadores() }.reduce(into: [Jugador: Int]()) { acumulado, jugador in
            acumulado[jugador, default: 0] += 1
        }
        return Ranking(
            elementos: Array<Jugador>(goles.keys),
            puntos: { goles[$0] ?? 0 },
            desempate: { $0.nombre < $1.nombre }
        )
    }
}

extension Torneo {
    public func goleadores() -> Ranking<Jugador> { partidos.goleadores() }
}
