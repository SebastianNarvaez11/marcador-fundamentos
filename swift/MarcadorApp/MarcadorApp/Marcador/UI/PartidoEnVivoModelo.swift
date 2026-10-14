import Foundation
import Observation
import Torneo

// EL MODELO DEL PARTIDO EN DIRECTO: nació como `ObservableObject`, pasó a `@Observable` y se
// juega con `async` desde `.task`, sin `Task` propia.
//
// Es el `Directo` de Kotlin: cuenta los minutos, aplica los goles del guion y suma los goles
// «a mano» de los botones. La pantalla no cuenta nada: lee esto y lo pinta.
//
// Antes: `final class X: ObservableObject` con `var minuto`. Cada cambio de CUALQUIER
// propiedad publicada avisaba a todas las vistas que observaban el objeto, lean lo que lean.
//
// Ahora: `@Observable`. SwiftUI apunta qué propiedades LEE cada `body` y solo lo recalcula si
// cambia una de ésas: una vista que solo enseña `minuto` no se redibuja cuando cambia `ultimoAviso`.
// Sin `@Published`, sin `import Combine`, y los `private(set)` siguen valiendo.
//
// UN OBJETO ASÍ TIENE VIDA PROPIA: es una clase, alguien lo crea y alguien lo mantiene vivo.
// Quién lo mantiene vivo es EXACTAMENTE la diferencia entre `@StateObject` y `@ObservedObject`
// (mira `PartidoEnDirectoView`).
@Observable
final class PartidoEnVivoModelo {
    let partido: Partido
    private let msPorMinuto: Int

    private(set) var minuto = 0
    private(set) var corriendo = false
    private(set) var ultimoAviso = "ninguno"
    // Los goles que va marcando el guion y los que se ponen «a mano» con los botones.
    private(set) var golesEnVivo = Marcador(local: 0, visitante: 0)
    private(set) var golesAMano = Marcador(local: 0, visitante: 0)

    init(partido: Partido, msPorMinuto: Int = Configuracion.msPorMinuto) {
        self.partido = partido
        self.msPorMinuto = msPorMinuto
        Registro.anotar("modelo del partido CREADO (\(partido.local.nombre)-\(partido.visitante.nombre)) \(identificador)")
    }

    deinit {
        Registro.anotar("modelo del partido LIBERADO")
    }

    // Un identificador corto para distinguir instancias en el registro.
    var identificador: String { String(UInt(bitPattern: ObjectIdentifier(self).hashValue) & 0xFFFFF, radix: 16) }

    // El marcador que se ve = el del partido en vivo + los goles «a mano».
    var marcador: Marcador {
        Marcador(local: golesEnVivo.local + golesAMano.local, visitante: golesEnVivo.visitante + golesAMano.visitante)
    }

    func golAMano(_ lado: Lado) {
        switch lado {
        case .local: golesAMano = Marcador(local: golesAMano.local + 1, visitante: golesAMano.visitante)
        case .visitante: golesAMano = Marcador(local: golesAMano.local, visitante: golesAMano.visitante + 1)
        }
    }

    // Juega el partido ENTERO. Es `async` y la vista la llama desde `.task`, así que:
    //   - no crea ninguna `Task` propia (antes tenía `tarea = Task { … }`, que nadie cancelaba);
    //   - si la pantalla se va, SwiftUI cancela la tarea del `.task`, `Task.sleep` lanza
    //     `CancellationError` y el bucle termina (lo anota en el registro);
    //   - vuelve a empezar de cero cada vez que se llama (`.task(id: duración)` la relanza).
    //
    // UN SOLO BUCLE: en cada minuto aplica los goles del guion y, si toca, calcula el aviso con
    // el marcador que acaba de quedar (dos relojes distintos harían que el aviso fuese un gol por detrás).
    func jugar(duracion: Int) async {
        minuto = 0
        golesEnVivo = Marcador(local: 0, visitante: 0)
        golesAMano = Marcador(local: 0, visitante: 0)
        ultimoAviso = "ninguno"
        corriendo = true
        let eventos = partido.eventos.filter { $0.minuto <= duracion }
        for nuevo in 1...duracion {
            do {
                try await Task.sleep(for: .milliseconds(msPorMinuto))
            } catch {
                // `Task.sleep` lanza CancellationError si cancelan la tarea. NO se traga en
                // silencio: se deja constancia y se sale.
                Registro.anotar("partido CANCELADO en el minuto \(minuto) (\(identificador))")
                corriendo = false
                return
            }
            for evento in eventos where evento.minuto == nuevo {
                golesEnVivo = golesEnVivo.despuesDe(evento, en: partido)
            }
            minuto = nuevo
            if nuevo % 15 == 0 { ultimoAviso = "minuto \(nuevo) con \(marcador)" }
        }
        corriendo = false
        Registro.anotar("partido TERMINADO en el minuto \(minuto) (\(identificador))")
    }
}
