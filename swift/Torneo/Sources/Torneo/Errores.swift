import Foundation

// Un error propio es un tipo cualquiera que adopta el protocolo `Error`; lo
// habitual es un `enum` con un caso por cada cosa que puede salir mal, y con
// datos asociados para saber DÓNDE falló (en Kotlin, una clase que extiende
// `Exception`, o simplemente `require` con un mensaje).
public enum ErrorDeTorneo: Error, Equatable {
    case equipoContraSiMismo
    case equiposNoInscritos(torneo: String)
    case minutoFueraDelPartido(Int)
    case golDeEquipoAjeno(equipo: String)
    case jugadorNoJuegaEnElEquipo(jugador: String, equipo: String)
    case jugadorNoJuegaElPartido(jugador: String)
    case cambioConAjenos
}

// `LocalizedError` da un texto legible en `errorDescription`. Los mensajes son
// los mismos que los de los `require` de la versión Kotlin.
extension ErrorDeTorneo: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case .equipoContraSiMismo:
            "Un equipo no puede jugar contra sí mismo"
        case let .equiposNoInscritos(torneo):
            "Los dos equipos deben estar inscritos en \(torneo)"
        case let .minutoFueraDelPartido(minuto):
            "Minuto fuera del partido: \(minuto)"
        case let .golDeEquipoAjeno(equipo):
            "El gol es de un equipo que no juega: \(equipo)"
        case let .jugadorNoJuegaEnElEquipo(jugador, equipo):
            "\(jugador) no juega en \(equipo)"
        case let .jugadorNoJuegaElPartido(jugador):
            "\(jugador) no juega este partido"
        case .cambioConAjenos:
            "El cambio implica a alguien que no juega este partido"
        }
    }
}

// Un error para leer texto de fuera: una función que puede fallar de varias
// maneras y por eso declara `throws`.
public enum ErrorDeDorsal: Error, Equatable {
    case noEsUnNumero(String)
    case fueraDeRango(Int)
}

// `throws`: esta función puede terminar lanzando un error en vez de devolver un
// valor. Quien la llame TIENE que decidir qué hacer: `try` dentro de `do/catch`,
// `try?` (nil si falla), `try!` (se detiene si falla) o volver a lanzarlo.
// Frente a `Int?` (f50), que solo dice «no se pudo», aquí se sabe POR QUÉ.
public func dorsalValido(_ texto: String) throws -> Int {
    guard let numero = dorsalDesdeTexto(texto) else {
        throw ErrorDeDorsal.noEsUnNumero(texto)
    }
    guard (1...99).contains(numero) else {
        throw ErrorDeDorsal.fueraDeRango(numero)
    }
    return numero
}
