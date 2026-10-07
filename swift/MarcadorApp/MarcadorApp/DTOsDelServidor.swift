import Foundation

// DTO (Data Transfer Object): la forma EXACTA del JSON que manda el servidor, con sus nombres
// en inglés. Solo lo usa la capa de red; el resto de la app usa sus propios modelos.
//
// `Decodable`: `JSONDecoder` sabe convertir el JSON en este tipo. Las claves del JSON que no
// aparecen aquí (`username`, `phone`, `company`…) se IGNORAN sin dar error.

// Un usuario de `GET /users`, que en Marcador hace de árbitro de la liga.
struct ArbitroDTO: Decodable {
    let id: Int
    let name: String
    let email: String
    let address: Direccion

    // El JSON trae la dirección como otro objeto dentro: `"address": { "city": "Gwenborough", … }`.
    struct Direccion: Decodable {
        let city: String
    }
}
