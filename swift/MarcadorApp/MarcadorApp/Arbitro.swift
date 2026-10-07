// EL MODELO: lo que la app entiende por «árbitro», con nombres en español y solo lo que usa.
// La pantalla pinta `Arbitro`, nunca el DTO: si mañana el servidor cambia un nombre, solo cambia
// la traducción de abajo.
struct Arbitro: Identifiable, Equatable {
    let id: Int
    let nombre: String
    let ciudad: String
}

extension Arbitro {
    // De DTO a modelo: el ÚNICO sitio que conoce los dos.
    init(dto: ArbitroDTO) {
        self.init(id: dto.id, nombre: dto.name, ciudad: dto.address.city)
    }
}

// EL REPOSITORIO de los árbitros: la puerta por la que el ViewModel los pide, sin saber de dónde salen.
protocol ArbitrosRepositorio: AnyObject {
    func arbitros() async throws -> [Arbitro]
}
