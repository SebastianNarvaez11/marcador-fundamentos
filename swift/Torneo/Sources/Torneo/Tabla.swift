// Una fila de la tabla de posiciones. Un struct con propiedades guardadas y una
// CALCULADA (`diferencia`): no ocupa espacio, se evalúa cada vez que se lee.
public struct FilaDePosicion: Equatable {
    public let equipo: Equipo
    public let jugados: Int
    public let ganados: Int
    public let empatados: Int
    public let perdidos: Int
    public let golesAFavor: Int
    public let golesEnContra: Int
    public let puntos: Int

    public var diferencia: Int { golesAFavor - golesEnContra }
}

// Extensión de `Array` restringida a `Element == Partido`: añade un método a
// `[Partido]` sin tocar Array (la `List<Partido>.tablaDePosiciones()` de Kotlin).
// `equipos` permite que salgan también los que aún no han jugado.
extension Array where Element == Partido {
    public func tablaDePosiciones(
        equipos: [Equipo] = [],
        reglamento: any Reglamento = ReglamentoLiga()
    ) -> [FilaDePosicion] {
        // Cada partido cuenta dos veces: una desde el lado local y otra desde el
        // visitante. Cada elemento es una tupla con nombres: (equipo, aFavor, enContra).
        let apariciones = flatMap { partido in
            [
                (equipo: partido.local, aFavor: partido.golesLocal, enContra: partido.golesVisitante),
                (equipo: partido.visitante, aFavor: partido.golesVisitante, enContra: partido.golesLocal),
            ]
        }

        // `Dictionary(grouping:by:)`: agrupa por clave. `\.equipo` es un KEY PATH:
        // una referencia a la propiedad, equivalente a `{ $0.equipo }`. Es
        // Dictionary<Equipo, [(equipo:, aFavor:, enContra:)]> (el `groupBy` de Kotlin).
        let porEquipo = Dictionary(grouping: apariciones, by: \.equipo)

        // Un Set no admite repetidos: `insert` devuelve si de verdad se insertó.
        // Así se unen los equipos dados y los que ya jugaron, sin repetir.
        var vistos = Set<Equipo>()
        let todos = (equipos + porEquipo.keys).filter { vistos.insert($0).inserted }

        let filas = todos.map { equipo in
            let marcadores = porEquipo[equipo] ?? []
            return FilaDePosicion(
                equipo: equipo,
                jugados: marcadores.count,
                ganados: marcadores.count { $0.aFavor > $0.enContra },
                empatados: marcadores.count { $0.aFavor == $0.enContra },
                perdidos: marcadores.count { $0.aFavor < $0.enContra },
                golesAFavor: marcadores.map(\.aFavor).reduce(0, +),
                golesEnContra: marcadores.map(\.enContra).reduce(0, +),
                puntos: marcadores.reduce(0) { total, marcador in
                    total + reglamento.puntosPor(golesAFavor: marcador.aFavor, golesEnContra: marcador.enContra)
                }
            )
        }

        // La tabla es un Ranking<FilaDePosicion> puntuado por los puntos de la liga.
        return Ranking(
            elementos: filas,
            puntos: \.puntos,
            desempate: { a, b in
                if a.diferencia != b.diferencia { return a.diferencia > b.diferencia }
                if a.golesAFavor != b.golesAFavor { return a.golesAFavor > b.golesAFavor }
                return a.equipo.nombre < b.equipo.nombre
            }
        ).ordenados
    }
}

// Una «foto» del torneo en un momento dado: sus partidos ya no cambian. Por eso
// aquí SÍ tiene sentido calcular la tabla una sola vez y guardarla.
//
// `lazy var`: una propiedad guardada cuyo valor se calcula la PRIMERA vez que se
// lee (el `by lazy` de Kotlin). Hace falta `var` y una clase (o un struct
// `mutating`), porque leerla por primera vez la modifica.
public final class InstantaneaDelTorneo {
    public let partidos: [Partido]
    public let equipos: [Equipo]
    public let reglamento: any Reglamento

    // Solo para que las pruebas vean cuántas veces se calculó de verdad.
    public private(set) var calculosDeLaTabla = 0

    public lazy var tabla: [FilaDePosicion] = {
        calculosDeLaTabla += 1
        return partidos.tablaDePosiciones(equipos: equipos, reglamento: reglamento)
    }()

    public lazy var goleadores: Ranking<Jugador> = partidos.goleadores()

    init(partidos: [Partido], equipos: [Equipo], reglamento: any Reglamento) {
        self.partidos = partidos
        self.equipos = equipos
        self.reglamento = reglamento
    }
}
