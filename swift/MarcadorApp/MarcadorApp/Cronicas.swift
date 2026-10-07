// LA CRÓNICA DE UN PARTIDO: un título («Rayo FC 2-1 Toros») y un texto con una línea por gol.
struct Cronica: Equatable {
    let titulo: String
    let texto: String
}

// EL REPOSITORIO de las crónicas: publicar una y recibir el número que le dio el servidor.
protocol CronicasRepositorio: AnyObject {
    func publicar(titulo: String, texto: String) async throws -> Int
}

// La implementación con el servicio: arma el DTO, hace el POST y se queda con el id.
// Los fallos ya llegan traducidos a `ErrorDeRed` desde el servicio.
final class RepositorioDeCronicasEnRed: CronicasRepositorio {
    private let servicio: any ServicioDelTorneo

    init(servicio: any ServicioDelTorneo) {
        self.servicio = servicio
    }

    func publicar(titulo: String, texto: String) async throws -> Int {
        // `userId` es obligatorio en JSONPlaceholder: cualquier usuario del 1 al 10 vale.
        let nueva = CronicaNuevaDTO(userId: 1, title: titulo, body: texto)
        return try await servicio.publicarCronica(nueva).id
    }
}
