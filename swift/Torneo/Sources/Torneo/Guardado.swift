import Foundation

// El JSON habla de árboles de datos, no de objetos con referencias (un Gol
// apunta a un Equipo). Por eso el fichero usa unos tipos «guardados» aparte,
// donde cada equipo o jugador se nombra por su nombre, y el dominio se
// reconstruye al cargar. Es EXACTAMENTE la misma idea (y el mismo formato) que
// `Guardado.kt` de la versión Kotlin: el fichero se puede leer desde las dos.
//
// `Codable` = `Encodable` + `Decodable`. Con solo escribirlo, el compilador
// genera el código que convierte el struct a JSON y de vuelta (en Kotlin lo
// hace el plugin de kotlinx.serialization al ver `@Serializable`). Los
// opcionales que valen `nil` NO se escriben, igual que en Kotlin.
//
// CONTROL DE ACCESO: estos tipos no llevan `public`, así que son `internal`:
// solo se ven dentro del módulo Torneo. Lo público es el par de funciones de abajo.

struct JugadorGuardado: Codable, Equatable {
    var nombre: String
    var dorsal: Int?
}

struct EquipoGuardado: Codable, Equatable {
    var nombre: String
    var plantilla: [JugadorGuardado]
    var capitan: String?
}

// Un enum con valores asociados NO se puede codificar solo con `Codable` de la
// manera que espera Kotlin (un campo «tipo» y los datos al lado). Hay que
// escribir `init(from:)` y `encode(to:)` a mano: leer primero `tipo` y, según
// su valor, los demás campos.
enum EventoGuardado: Codable, Equatable {
    case gol(minuto: Int, jugador: String, equipo: String)
    case tarjeta(minuto: Int, jugador: String, color: ColorDeTarjeta)
    case cambio(minuto: Int, sale: String, entra: String)

    // `CodingKey`: los nombres de los campos del JSON.
    private enum Claves: String, CodingKey {
        case tipo, minuto, jugador, equipo, color, sale, entra
    }

    init(from decoder: any Decoder) throws {
        let contenedor = try decoder.container(keyedBy: Claves.self)
        let minuto = try contenedor.decode(Int.self, forKey: .minuto)
        let tipo = try contenedor.decode(String.self, forKey: .tipo)
        switch tipo {
        case "gol":
            self = .gol(
                minuto: minuto,
                jugador: try contenedor.decode(String.self, forKey: .jugador),
                equipo: try contenedor.decode(String.self, forKey: .equipo)
            )
        case "tarjeta":
            self = .tarjeta(
                minuto: minuto,
                jugador: try contenedor.decode(String.self, forKey: .jugador),
                color: try contenedor.decode(ColorDeTarjeta.self, forKey: .color)
            )
        case "cambio":
            self = .cambio(
                minuto: minuto,
                sale: try contenedor.decode(String.self, forKey: .sale),
                entra: try contenedor.decode(String.self, forKey: .entra)
            )
        default:
            throw DecodingError.dataCorruptedError(
                forKey: .tipo, in: contenedor, debugDescription: "Tipo de evento desconocido: \(tipo)"
            )
        }
    }

    func encode(to encoder: any Encoder) throws {
        var contenedor = encoder.container(keyedBy: Claves.self)
        switch self {
        case let .gol(minuto, jugador, equipo):
            try contenedor.encode("gol", forKey: .tipo)
            try contenedor.encode(minuto, forKey: .minuto)
            try contenedor.encode(jugador, forKey: .jugador)
            try contenedor.encode(equipo, forKey: .equipo)
        case let .tarjeta(minuto, jugador, color):
            try contenedor.encode("tarjeta", forKey: .tipo)
            try contenedor.encode(minuto, forKey: .minuto)
            try contenedor.encode(jugador, forKey: .jugador)
            try contenedor.encode(color, forKey: .color)
        case let .cambio(minuto, sale, entra):
            try contenedor.encode("cambio", forKey: .tipo)
            try contenedor.encode(minuto, forKey: .minuto)
            try contenedor.encode(sale, forKey: .sale)
            try contenedor.encode(entra, forKey: .entra)
        }
    }
}

struct PartidoGuardado: Codable, Equatable {
    var local: String
    var visitante: String
    var eventos: [EventoGuardado]
}

struct TorneoGuardado: Codable, Equatable {
    var nombre: String
    var equipos: [EquipoGuardado]
    var partidos: [PartidoGuardado]
}

// Lo que puede salir mal al reconstruir el dominio a partir de un JSON que, aun
// siendo válido, no tiene sentido (un gol de un equipo que no existe…).
public enum ErrorDeCarga: Error, Equatable {
    case equipoInvalido(String)
    case equipoDesconocido(String)
    case jugadorDesconocido(String)
}

// ---- Dominio -> guardado ----

private func guardado(de equipo: Equipo) -> EquipoGuardado {
    EquipoGuardado(
        nombre: equipo.nombre,
        plantilla: equipo.plantilla.map { JugadorGuardado(nombre: $0.nombre, dorsal: $0.dorsal) },
        capitan: equipo.capitan?.nombre
    )
}

// `switch` exhaustivo sobre el evento del dominio: si aparece uno nuevo, esto no compila.
private func guardado(de evento: EventoDePartido) -> EventoGuardado {
    switch evento {
    case let .gol(minuto, jugador, equipo): .gol(minuto: minuto, jugador: jugador.nombre, equipo: equipo.nombre)
    case let .tarjeta(minuto, jugador, color): .tarjeta(minuto: minuto, jugador: jugador.nombre, color: color)
    case let .cambio(minuto, sale, entra): .cambio(minuto: minuto, sale: sale.nombre, entra: entra.nombre)
    }
}

extension Torneo {
    // `prettyPrinted`: sangrías y saltos de línea, para leer el fichero a ojo.
    // `sortedKeys`: el orden de las claves no cambia entre ejecuciones (un
    // diccionario de Swift no garantiza orden; sin esto el fichero variaría).
    public func aJson() throws -> Data {
        let torneoGuardado = TorneoGuardado(
            nombre: nombre,
            equipos: equipos.map { guardado(de: $0) },
            partidos: partidos.map { partido in
                PartidoGuardado(
                    local: partido.local.nombre,
                    visitante: partido.visitante.nombre,
                    eventos: partido.eventos.map { guardado(de: $0) }
                )
            }
        )
        let codificador = JSONEncoder()
        codificador.outputFormatting = [.prettyPrinted, .sortedKeys]
        return try codificador.encode(torneoGuardado)
    }
}

// ---- Guardado -> dominio ----

private func equipo(desde guardado: EquipoGuardado) throws -> Equipo {
    let jugadores = guardado.plantilla.map { Jugador(nombre: $0.nombre, dorsal: $0.dorsal) }
    guard let equipo = Equipo(nombre: guardado.nombre, plantilla: jugadores) else {
        throw ErrorDeCarga.equipoInvalido(guardado.nombre)
    }
    if let nombreDelCapitan = guardado.capitan {
        guard let capitan = jugadores.first(where: { $0.nombre == nombreDelCapitan }) else {
            throw ErrorDeCarga.jugadorDesconocido(nombreDelCapitan)
        }
        equipo.nombrarCapitan(capitan)
    }
    return equipo
}

// Las referencias por nombre se resuelven aquí: si falta un equipo o un jugador,
// se lanza un `ErrorDeCarga`.
private func partido(desde guardado: PartidoGuardado, equipos: [String: Equipo]) throws -> Partido {
    func buscar(_ nombre: String) throws -> Equipo {
        guard let equipo = equipos[nombre] else { throw ErrorDeCarga.equipoDesconocido(nombre) }
        return equipo
    }
    let local = try buscar(guardado.local)
    let visitante = try buscar(guardado.visitante)
    let jugadores = local.plantilla + visitante.plantilla
    func jugador(_ nombre: String) throws -> Jugador {
        guard let encontrado = jugadores.first(where: { $0.nombre == nombre }) else {
            throw ErrorDeCarga.jugadorDesconocido(nombre)
        }
        return encontrado
    }

    let eventos: [EventoDePartido] = try guardado.eventos.map { evento in
        switch evento {
        case let .gol(minuto, nombre, equipo):
            .gol(minuto: minuto, jugador: try jugador(nombre), equipo: try buscar(equipo))
        case let .tarjeta(minuto, nombre, color):
            .tarjeta(minuto: minuto, jugador: try jugador(nombre), color: color)
        case let .cambio(minuto, sale, entra):
            .cambio(minuto: minuto, sale: try jugador(sale), entra: try jugador(entra))
        }
    }
    return Partido(local: local, visitante: visitante, eventos: eventos)
}

// Leer un JSON puede fallar de muchas formas (texto roto, campo que falta,
// equipo que no existe…): la función `throws` y cada causa llega con su error
// (`DecodingError` de Foundation, `ErrorDeCarga` o `ErrorDeTorneo`).
public func torneoDesdeJson(_ datos: Data) throws -> Torneo {
    let guardado = try JSONDecoder().decode(TorneoGuardado.self, from: datos)
    let equipos = try guardado.equipos.map(equipo(desde:))
    let porNombre = Dictionary(uniqueKeysWithValues: equipos.map { ($0.nombre, $0) })
    let torneo = Torneo(nombre: guardado.nombre, equipos: equipos)
    for partidoGuardado in guardado.partidos {
        try torneo.registrar(partido(desde: partidoGuardado, equipos: porNombre))
    }
    return torneo
}
