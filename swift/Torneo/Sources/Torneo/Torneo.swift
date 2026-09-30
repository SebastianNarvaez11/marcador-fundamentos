// El torneo es lo único mutable del dominio: va acumulando partidos. Es una
// `class` porque tiene IDENTIDAD y estado compartido: si la pantalla de la
// tabla y la de resultados reciben «el torneo», deben ver EL MISMO.
public final class Torneo {
    public let nombre: String
    public let equipos: [Equipo]

    // `any Reglamento`: «algún tipo que cumple el protocolo». El torneo TIENE un
    // reglamento (se lo dan de fuera) en vez de SER un tipo de reglamento:
    // composición, como en Kotlin. Cambiar de reglas no obliga a cambiar de clase.
    public let reglamento: any Reglamento

    // `private(set) var` sobre un array: de fuera se lee, y lo que se lee es una
    // COPIA (los arrays son de valor). Kotlin necesitaba una lista privada, un
    // getter con `toList()` y explicar el `as MutableList`: aquí el lenguaje ya
    // lo evita.
    //
    // `didSet` corre DESPUÉS de cada cambio de la propiedad: aquí invalida la
    // tabla guardada, que ya no vale. (`willSet` correría antes.)
    public private(set) var partidos: [Partido] = [] {
        didSet { tablaEnCache = nil }
    }

    private var tablaEnCache: [FilaDePosicion]?

    public init(nombre: String, equipos: [Equipo], reglamento: any Reglamento = ReglamentoLiga()) {
        self.nombre = nombre
        self.equipos = equipos
        self.reglamento = reglamento
    }

    // Propiedad calculada de solo lectura, sin campo propio.
    public var cantidadDePartidos: Int { partidos.count }

    // La tabla se calcula la primera vez y se reutiliza hasta el siguiente partido.
    public func tablaDePosiciones() -> [FilaDePosicion] {
        if let guardada = tablaEnCache { return guardada }
        let tabla = partidos.tablaDePosiciones(equipos: equipos, reglamento: reglamento)
        tablaEnCache = tabla
        return tabla
    }

    // Registrar puede fallar (equipo no inscrito, gol de alguien ajeno…), y eso no
    // es una situación excepcional sino esperable. `throws(ErrorDeTorneo)` es un
    // `throws` TIPADO (Swift 6): el compilador sabe que solo puede lanzar ese
    // error, así que quien lo capture no necesita un `catch` genérico. Con
    // `throws` a secas el error sería un `any Error` cualquiera.
    // Devuelve el partido registrado; `@discardableResult` permite ignorarlo.
    @discardableResult
    public func registrar(_ partido: Partido) throws(ErrorDeTorneo) -> Partido {
        guard partido.local != partido.visitante else { throw .equipoContraSiMismo }
        guard equipos.contains(partido.local), equipos.contains(partido.visitante) else {
            throw .equiposNoInscritos(torneo: nombre)
        }
        for evento in partido.eventos {
            try validar(evento, en: partido)
        }
        partidos.append(partido)
        return partido
    }

    @discardableResult
    public func registrar(
        local: Equipo,
        visitante: Equipo,
        eventos: [EventoDePartido] = []
    ) throws(ErrorDeTorneo) -> Partido {
        try registrar(Partido(local: local, visitante: visitante, eventos: eventos))
    }

    // El equivalente del `Result<Partido>` de Kotlin: el error pasa a ser un valor.
    // Con `throws` tipado, `Result { … }` conserva el tipo del error.
    public func intentarRegistrar(_ partido: Partido) -> Result<Partido, ErrorDeTorneo> {
        Result { () throws(ErrorDeTorneo) in try registrar(partido) }
    }

    private func validar(_ evento: EventoDePartido, en partido: Partido) throws(ErrorDeTorneo) {
        guard (0...120).contains(evento.minuto) else { throw .minutoFueraDelPartido(evento.minuto) }
        let jugadoresDelPartido = partido.local.plantilla + partido.visitante.plantilla
        // `switch` exhaustivo sobre el enum: si aparece un evento nuevo, no compila.
        switch evento {
        case let .gol(_, jugador, equipo):
            guard equipo == partido.local || equipo == partido.visitante else {
                throw .golDeEquipoAjeno(equipo: equipo.nombre)
            }
            guard equipo.plantilla.contains(jugador) else {
                throw .jugadorNoJuegaEnElEquipo(jugador: jugador.nombre, equipo: equipo.nombre)
            }
        case let .tarjeta(_, jugador, _):
            guard jugadoresDelPartido.contains(jugador) else {
                throw .jugadorNoJuegaElPartido(jugador: jugador.nombre)
            }
        case let .cambio(_, sale, entra):
            guard jugadoresDelPartido.contains(sale), jugadoresDelPartido.contains(entra) else {
                throw .cambioConAjenos
            }
        }
    }
}

extension Torneo {
    // Los partidos son valores, así que la foto no se ve afectada por lo que se
    // registre después.
    public func instantanea() -> InstantaneaDelTorneo {
        InstantaneaDelTorneo(partidos: partidos, equipos: equipos, reglamento: reglamento)
    }
}
