import Foundation
import Torneo

// CASO DE USO QUE SÍ COMPENSA (gemelo de `PublicarCronica.kt`).
//
// Junta DOS repositorios (los partidos y las crónicas) y guarda una REGLA de negocio: cómo se
// escribe la crónica de un partido. El ViewModel solo dice «publica la del partido 3».
//
// Dirección de las dependencias: ViewModel → caso de uso → repositorios. Nunca al revés.
// (Con los árbitros NO hay caso de uso: un `ObtenerArbitros` solo reenviaría la llamada al repositorio.)
struct PublicarCronica {
    let partidos: any PartidosRepositorio
    let cronicas: any CronicasRepositorio

    // Los fallos esperables, en un `Result` tipado, como en `RegistrarGol`.
    enum Fallo: Error, Equatable, LocalizedError {
        case noExisteElPartido(Int)
        case red(ErrorDeRed)
        case otro(String)        // cualquier otro fallo (por ejemplo, una respuesta con otra forma)

        var errorDescription: String? {
            switch self {
            case let .noExisteElPartido(id): "No existe el partido \(id)"
            case let .red(error): error.errorDescription
            case let .otro(texto): texto
            }
        }
    }

    // LA REGLA: el título es el resultado («Rayo FC 2-1 Toros») y el texto, una línea por gol
    // («12' Ana (Rayo FC)»), o «Sin goles». La pantalla la usa para enseñarla antes de publicar.
    func redactar(partidoId: Int) -> Cronica? {
        guard let partido = partidos.partido(id: partidoId) else { return nil }
        var lineas: [String] = []
        for evento in partido.eventos {
            if case let .gol(minuto, jugador, equipo) = evento {
                lineas.append("\(minuto)' \(jugador.nombre) (\(equipo.nombre))")
            }
        }
        return Cronica(
            titulo: "\(partido.local.nombre) \(partido.marcador()) \(partido.visitante.nombre)",
            texto: lineas.isEmpty ? "Sin goles" : lineas.joined(separator: "\n")
        )
    }

    // `callAsFunction`: el caso de uso se llama como una función, `publicarCronica(partidoId: 1)`.
    func callAsFunction(partidoId: Int) async -> Result<Int, Fallo> {
        guard let cronica = redactar(partidoId: partidoId) else {
            return .failure(.noExisteElPartido(partidoId))
        }
        do {
            let id = try await cronicas.publicar(titulo: cronica.titulo, texto: cronica.texto)
            return .success(id)
        } catch let error as ErrorDeRed {
            return .failure(.red(error))
        } catch {
            return .failure(.otro(error.localizedDescription))
        }
    }
}
