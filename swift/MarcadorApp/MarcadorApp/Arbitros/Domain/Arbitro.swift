// EL MODELO: lo que la app entiende por «árbitro», con nombres en español y solo lo que usa.
// La pantalla pinta `Arbitro`, nunca el DTO: si mañana el servidor cambia un nombre, solo cambia
// la traducción de abajo.
// `Codable`: además se guarda en el disco (`arbitros.json`) y se vuelve a leer.
struct Arbitro: Identifiable, Codable, Equatable {
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

// EL REPOSITORIO de los árbitros, con DOS FUENTES: el disco y el servidor.
//   - `arbitros`: lo guardado. Se lee al instante, también sin red.
//   - `refrescar()`: pide la lista al servidor, la guarda y entonces cambia `arbitros`.
protocol ArbitrosRepositorio: AnyObject {
    var arbitros: [Arbitro] { get }
    func refrescar() async throws
}
