import Torneo
@testable import MarcadorApp

// DOBLES DE PRUEBA. Un ViewModel que recibe sus dependencias por el constructor se prueba
// dándole imitaciones que controlas tú. Nombres habituales (y no todo el mundo los usa igual):
//   FALSO (fake)  una implementación simple que funciona de verdad pero sin el mundo real (aquí, sin disco).
//   ESPÍA (spy)   anota qué le pidieron, para preguntarlo después.
//   MOCK          un espía que además lleva las expectativas dentro y falla solo si no se cumplen.
// Aquí NO se usa ninguna librería de mocks: en Swift basta un protocolo y una clase de diez líneas.

// El repositorio falso: sirve los partidos que le des y anota los goles que le piden guardar.
// A diferencia del real, NO modifica sus partidos ni escribe ningún fichero.
final class RepositorioFalso: PartidosRepositorio {
    var equipos: [Equipo]
    var partidos: [PartidoDeLista]
    private(set) var golesRegistrados: [(partidoId: Int, gol: EventoDePartido)] = []
    private(set) var consultas: [Int] = []

    init(partidos: [PartidoDeLista] = [PartidoDeLista(id: 1, partido: Ejemplo.rayoContraToros)]) {
        self.partidos = partidos
        self.equipos = [Ejemplo.rayo, Ejemplo.toros]
    }

    func partido(id: Int) -> Partido? {
        consultas.append(id)
        return partidos.first { $0.id == id }?.partido
    }

    func registrarGol(partidoId: Int, gol: EventoDePartido) {
        golesRegistrados.append((partidoId, gol))
    }
}

// El espía de notificaciones (ahora en su sitio).
final class NotificadorEspia: Notificador {
    var permitido = true
    private(set) var avisos: [(minuto: Int, jugador: String, equipo: String)] = []

    func notificarGol(minuto: Int, jugador: String, equipo: String) {
        avisos.append((minuto, jugador, equipo))
    }
}
