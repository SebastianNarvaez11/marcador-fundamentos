import Foundation

// LA FUENTE LOCAL de los árbitros: la última lista que llegó del servidor, en `Documents/arbitros.json`.
// Es la misma idea que `AlmacenDelTorneo`, con un fichero propio.
struct AlmacenDeArbitros {
    let url: URL

    static let predeterminado = AlmacenDeArbitros(url: .documentsDirectory.appending(path: "arbitros.json"))

    func guardar(_ arbitros: [Arbitro]) throws {
        // `.atomic`: si la app muere a mitad, queda el fichero viejo entero.
        try JSONEncoder().encode(arbitros).write(to: url, options: .atomic)
    }

    // Si el fichero no existe (aún no se bajó nada) o está roto, una lista vacía.
    func cargar() -> [Arbitro] {
        guard let datos = try? Data(contentsOf: url) else { return [] }
        return (try? JSONDecoder().decode([Arbitro].self, from: datos)) ?? []
    }

    func borrar() {
        try? FileManager.default.removeItem(at: url)
    }
}
