import Combine   // `ObservableObject` y `@Published` son de Combine, no de Foundation
import Foundation
import Torneo

// f72 · EL MODELO DEL PARTIDO EN DIRECTO, como `ObservableObject`
//
// Es el `Directo` de Kotlin: cuenta los minutos, aplica los goles del guion y suma los goles
// «a mano» de los botones. La pantalla no cuenta nada: lee esto y lo pinta.
//
// `ObservableObject` + `@Published`: cada vez que cambia una propiedad publicada, SwiftUI
// recalcula el `body` de las vistas que observan el objeto. (En f73 esto se sustituye por
// `@Observable`, que es más fino y más corto.)
//
// UN OBJETO ASÍ TIENE VIDA PROPIA: es una clase, alguien lo crea y alguien lo mantiene vivo.
// Quién lo mantiene vivo es EXACTAMENTE la diferencia entre `@StateObject` y `@ObservedObject`
// (mira `PartidoEnDirectoView`).
final class PartidoEnVivoModelo: ObservableObject {
    let partido: Partido
    private let msPorMinuto: Int

    @Published private(set) var minuto = 0
    @Published private(set) var corriendo = false
    @Published private(set) var ultimoAviso = "ninguno"
    // Los goles que va marcando el guion y los que se ponen «a mano» con los botones.
    @Published private(set) var golesEnVivo = Marcador(local: 0, visitante: 0)
    @Published private(set) var golesAMano = Marcador(local: 0, visitante: 0)

    private var tarea: Task<Void, Never>?

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

    // Arranca el partido. Solo se puede iniciar una vez.
    func empezar(duracion: Int) {
        guard !corriendo, minuto == 0 else { return }
        corriendo = true
        // ASÍ NO VALE del todo (se arregla en f74 con `.task`): esta `Task` la lanzamos a mano y
        // nadie la cancela si la pantalla se va; además retiene `self` (captura fuerte) hasta que termine.
        tarea = Task {
            await jugar(duracion: duracion)
        }
    }

    // UN SOLO BUCLE: en cada minuto aplica los goles del guion y, si toca, calcula el aviso con
    // el marcador que acaba de quedar (dos relojes distintos harían que el aviso fuese un gol por detrás).
    private func jugar(duracion: Int) async {
        let eventos = partido.eventos.filter { $0.minuto <= duracion }
        for nuevo in 1...duracion {
            try? await Task.sleep(for: .milliseconds(msPorMinuto))
            for evento in eventos where evento.minuto == nuevo {
                golesEnVivo = golesEnVivo.despuesDe(evento, en: partido)
            }
            minuto = nuevo
            if nuevo % 15 == 0 { ultimoAviso = "minuto \(nuevo) con \(marcador)" }
        }
        corriendo = false
    }
}
