// `<T>` es el tipo de lo que se clasifica (Jugador, String…): se decide al usarlo.
public struct Ranking<T> {
    // Cómo se puntúa un elemento. Se GUARDA en el struct, por eso `@escaping` en el `init`.
    private let puntos: (T) -> Int

    // Los elementos, de más a menos puntos.
    public let ordenados: [T]

    public init(
        elementos: [T],
        // `@escaping`: se guarda en `puntos` y se usa después de que el `init` termine.
        puntos: @escaping (T) -> Int,
        // «¿va `a` antes que `b`?» para los empates. Por defecto, nadie va antes.
        desempate: (T, T) -> Bool = { _, _ in false }
    ) {
        self.puntos = puntos
        // `sorted` ordena con una closure que responde «¿va `a` antes que `b`?».
        self.ordenados = elementos.sorted { a, b in
            let puntosDeA = puntos(a)
            let puntosDeB = puntos(b)
            // Si los puntos son distintos, va antes el que más tiene; si no, decide el desempate.
            return puntosDeA != puntosDeB ? puntosDeA > puntosDeB : desempate(a, b)
        }
    }

    // Los puntos de un elemento: llama a la closure guardada.
    public func puntosDe(_ elemento: T) -> Int { puntos(elemento) }

    // Los primeros `cantidad`: `prefix` da menos si no hay tantos, y `Array(…)` lo vuelve array.
    public func top(_ cantidad: Int) -> [T] { Array(ordenados.prefix(cantidad)) }
}

// Con `Sequence`, un Ranking se recorre con `for … in` y gana `map`, `filter`…
extension Ranking: Sequence {
    // Devuelve el iterador de `ordenados`: el que entrega los elementos uno a uno.
    public func makeIterator() -> IndexingIterator<[T]> { ordenados.makeIterator() }
}

// Extensión CONDICIONAL: este método solo existe cuando `T` es `Equatable`.
// Para saber si un elemento «está» hay que poder compararlo; con un `T`
// cualquiera no se puede (en Kotlin, `!in` usa `equals` de cualquier objeto).
extension Ranking where T: Equatable {
    public func puesto(de elemento: T) -> Int? {
        // Si el elemento no está en el ranking, no tiene puesto.
        guard ordenados.contains(elemento) else { return nil }
        let suyos = puntos(elemento)
        // La posición del primero que tiene los mismos puntos…
        guard let primero = ordenados.firstIndex(where: { puntos($0) == suyos }) else { return nil }
        // …más 1, porque las posiciones empiezan en 0 y los puestos en 1.
        return primero + 1
    }
}

// Un protocolo con un hueco: `Elemento` lo rellena cada tipo que lo adopta.
public protocol Clasificacion {
    associatedtype Elemento
    var ordenados: [Elemento] { get }
}

// Ranking adopta el protocolo: aquí el hueco `Elemento` se rellena con `T`.
extension Ranking: Clasificacion {
    public typealias Elemento = T
}

// Función genérica sobre el protocolo: `C.Elemento` es el tipo que ese `C` haya elegido.
public func lider<C: Clasificacion>(_ clasificacion: C) -> C.Elemento? {
    clasificacion.ordenados.first
}

// `some Sequence<Jugador>`: «algún tipo concreto que es una secuencia de Jugador».
public func nombres(de jugadores: some Sequence<Jugador>) -> [String] {
    jugadores.map(\.nombre)   // `\.nombre` es un key path: atajo de `{ $0.nombre }`
}

// `any Clasificacion`: cualquier clasificación; permite una lista de tipos distintos,
// a cambio de perder el tipo del `Elemento` y de un pequeño coste al usarla.
public func cuantosPuestosHay(_ clasificaciones: [any Clasificacion]) -> Int {
    clasificaciones.reduce(0) { $0 + $1.ordenados.count }
}

// Solo para arrays de partidos: `[Partido]` también es `Array<Partido>`.
extension Array where Element == Partido {
    public func goleadores() -> Ranking<Jugador> {
        // `flatMap` junta en una sola lista los goleadores de todos los partidos.
        // `reduce(into:)` va rellenando un diccionario «jugador → goles».
        let goles = flatMap { $0.goleadores() }.reduce(into: [Jugador: Int]()) { acumulado, jugador in
            // Si el jugador aún no está, parte de 0; cada gol suma 1.
            acumulado[jugador, default: 0] += 1
        }
        return Ranking(
            elementos: [Jugador](goles.keys),      // los jugadores que marcaron (un array de Jugador)
            puntos: { goles[$0] ?? 0 },            // sus goles
            desempate: { $0.nombre < $1.nombre }   // a igualdad de goles, por nombre
        )
    }
}

extension Torneo {
    // El mismo ranking, sobre los partidos del torneo.
    public func goleadores() -> Ranking<Jugador> { partidos.goleadores() }
}
