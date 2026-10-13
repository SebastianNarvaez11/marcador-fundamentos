import Foundation
import Observation

// ESTADO DE TODA LA APP: el partido que se está jugando (gemelo de `PartidoEnJuego` en Android).
//
// Lo leen varias pantallas a la vez: la del propio partido, la barra de abajo (en todas las pestañas)
// y la de otro partido (para decir «Ya hay un partido en juego»). Por eso no vive en ningún ViewModel:
// hay UNO solo, lo crea el contenedor de la app y se reparte por el entorno (`.environment`).
//
// LA TAREA ES SUYA, no de `.task`. Un `.task` de una vista se cancela al cerrar esa vista; esta `Task`
// la guarda este objeto, que vive lo que la app. Por eso el partido sigue aunque salgas de su pantalla.
//
// Regla: UNO A LA VEZ. Mientras un partido no ha terminado, no se puede empezar otro. Al terminar, se
// queda aquí («Final») hasta que alguien lo cierra.
@MainActor
@Observable
final class PartidoEnJuego {
    // El partido en juego (o el que acaba de terminar). Solo este objeto lo cambia.
    private(set) var directo: Directo?

    // La tarea que lo juega. No se pinta: `@ObservationIgnored`. Las pruebas la esperan con `await tarea?.value`.
    @ObservationIgnored private(set) var tarea: Task<Void, Never>?

    // En la app no se libera nunca (vive lo que el contenedor); en las pruebas, sí.
    deinit {
        Registro.anotar("PartidoEnJuego LIBERADO")
    }

    // El directo de ESE partido, si es el que está en juego. La pantalla lo reutiliza en vez de crear otro.
    func directo(id: Int) -> Directo? {
        directo?.partidoId == id ? directo : nil
    }

    // Empieza `nuevo`. Devuelve `false`, sin hacer nada, si hay otro partido sin terminar.
    func empezar(_ nuevo: Directo) -> Bool {
        if let directo, !directo.terminado { return false }
        directo = nuevo
        tarea = Task { await nuevo.jugar() }
        return true
    }

    // Quitar el partido (la X de la barra). Solo cuando ya terminó: en juego no se cierra.
    func cerrar() {
        guard let directo, directo.terminado else { return }
        tarea = nil
        self.directo = nil
    }
}
