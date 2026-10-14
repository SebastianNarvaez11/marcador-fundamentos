import Foundation
import Torneo

// EL TORNEO GUARDADO COMO JSON EN `Documents`
//
// Cada app de iOS vive en su SANDBOX: una carpeta propia que ninguna otra app puede ver, con
//   Documents/   lo del usuario; se incluye en las copias de seguridad (aquí, `torneo.json`)
//   Library/     cosas de la app (`Preferences/` es donde `UserDefaults` guarda su .plist; `Caches/`)
//   tmp/         se puede borrar en cualquier momento
// (El equivalente de `filesDir` / `cacheDir` de Android.) La ruta cambia en CADA instalación:
// nunca se guarda una ruta absoluta, se pregunta al sistema cada vez (`URL.documentsDirectory`).
//
// El formato es el de `Guardado.swift` del paquete `Torneo` (`aJson()` / `torneoDesdeJson`): el mismo
// que escribe la versión Kotlin. Aquí no se escribe ningún `Codable` nuevo: la app solo decide DÓNDE.
struct AlmacenDelTorneo {
    let url: URL

    static let predeterminado = AlmacenDelTorneo(url: .documentsDirectory.appending(path: "torneo.json"))

    var existe: Bool { FileManager.default.fileExists(atPath: url.path) }

    func guardar(_ torneo: Torneo) throws {
        // `.atomic`: escribe a un fichero temporal y lo renombra. Si la app muere a mitad,
        // queda el fichero viejo entero en vez de uno a medias.
        try torneo.aJson().write(to: url, options: .atomic)
    }

    func cargar() throws -> Torneo {
        try torneoDesdeJson(Data(contentsOf: url))
    }

    func borrar() {
        try? FileManager.default.removeItem(at: url)
    }
}
