import Torneo

// CASO DE USO: una acción del usuario con sentido para el negocio (gemelo de `RegistrarGol.kt`).
//
// Regla: el gol a mano no dice quién lo marcó, así que lo marca el jugador de la plantilla que toca
// por turno (los goles que ya lleva el equipo, módulo el tamaño). Los errores esperables (no existe el
// partido, el equipo no tiene jugadores) salen en un `Result` tipado, no como excepción.
struct RegistrarGol {
    let repositorio: any PartidosRepositorio

    enum Fallo: Error, Equatable {
        case noExisteElPartido(Int)
        case equipoSinJugadores(String)
    }

    func callAsFunction(partidoId: Int, lado: Lado, minuto: Int) -> Result<EventoDePartido, Fallo> {
        guard let partido = repositorio.partido(id: partidoId) else {
            return .failure(.noExisteElPartido(partidoId))
        }
        let equipo = lado == .local ? partido.local : partido.visitante
        guard !equipo.plantilla.isEmpty else { return .failure(.equipoSinJugadores(equipo.nombre)) }
        let jugador = equipo.plantilla[partido.golesDe(equipo) % equipo.plantilla.count]
        let gol = EventoDePartido.gol(minuto: minuto, jugador: jugador, equipo: equipo)
        repositorio.registrarGol(partidoId: partidoId, gol: gol)
        return .success(gol)
    }
}
