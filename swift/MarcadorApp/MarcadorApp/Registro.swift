import Foundation
import os

// Un único sitio por donde pasan los mensajes de depuración de la app.
//
// `print` sale por la consola de Xcode (y por `simctl launch --console-pty`), pero solo
// mientras la app está lanzada desde ahí. `Logger` escribe en el registro unificado del
// sistema (el «Logcat» de iOS): se puede leer después, con la app ya cerrada, con
//   xcrun simctl spawn <udid> log show --predicate 'subsystem == "com.sebastiannarvaez.marcador"'
// Es el equivalente de `Log.d("Ciclo", …)` de Android (F3).
// `nonisolated`: con el aislamiento a MainActor por defecto (ajuste del proyecto), sin esto solo
// se podría llamar desde el hilo principal; un `deinit` o una tarea de fondo no podrían.
nonisolated enum Registro {
    private static let logger = Logger(subsystem: "com.sebastiannarvaez.marcador", category: "app")

    // `privacy: .public`: sin él, iOS sustituye los textos dinámicos por `<private>`
    // cuando no hay un depurador delante.
    static func anotar(_ mensaje: String) {
        print(mensaje)
        fflush(stdout)
        logger.notice("\(mensaje, privacy: .public)")
    }
}
