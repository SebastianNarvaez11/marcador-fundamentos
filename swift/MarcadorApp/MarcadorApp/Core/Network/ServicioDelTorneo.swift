import Foundation

// EL SERVICIO: lo que la app puede pedir al servidor (gemelo de la interfaz `LigaApi` de Retrofit).
//
// Es un PROTOCOLO, como el repositorio: quien lo usa dice QUÉ quiere («dame los árbitros») y no
// sabe cómo viaja la petición. En las pruebas se cambia por un falso que no toca la red.
protocol ServicioDelTorneo {
    func arbitros() async throws -> [ArbitroDTO]
    func publicarCronica(_ cronica: CronicaNuevaDTO) async throws -> CronicaPublicadaDTO
}

// La implementación de verdad: `URLSession` contra JSONPlaceholder, un servidor de pruebas gratuito.
//
// En iOS no hay que pedir ningún permiso para usar internet. Lo que sí hay es ATS (App Transport
// Security): las peticiones tienen que ir por `https://`. Esta dirección lo es, así que no se toca nada.
struct ServicioJSONPlaceholder: ServicioDelTorneo {
    let base: URL
    let sesion: URLSession

    init(base: URL = URL(string: "https://jsonplaceholder.typicode.com")!, sesion: URLSession = .shared) {
        self.base = base
        self.sesion = sesion
    }

    // GET https://jsonplaceholder.typicode.com/users
    func arbitros() async throws -> [ArbitroDTO] {
        try await enviar(URLRequest(url: base.appending(path: "users")))
    }

    // POST https://jsonplaceholder.typicode.com/posts, con la crónica en JSON en el cuerpo.
    // Responde 201 («creado») con lo mismo que se envió más un `id`. Es un servidor de pruebas:
    // no guarda nada, y el id es siempre 101.
    func publicarCronica(_ cronica: CronicaNuevaDTO) async throws -> CronicaPublicadaDTO {
        var peticion = URLRequest(url: base.appending(path: "posts"))
        peticion.httpMethod = "POST"
        // Sin esta cabecera el servidor responde 201 igual, pero no entiende el cuerpo como JSON.
        peticion.setValue("application/json; charset=UTF-8", forHTTPHeaderField: "Content-Type")
        peticion.httpBody = try JSONEncoder().encode(cronica)
        return try await enviar(peticion)
    }

    // Lo que tienen en común todas las peticiones: enviar, comprobar el código y decodificar.
    // `T` es el tipo que se espera en la respuesta (`[ArbitroDTO]`, `CronicaPublicadaDTO`…).
    // Aquí se traducen los fallos de `URLSession` al error de dominio, `ErrorDeRed`.
    private func enviar<T: Decodable>(_ peticion: URLRequest) async throws -> T {
        do {
            let (datos, respuesta) = try await sesion.data(for: peticion)
            // Un 404 o un 500 NO lanzan ningún error en `URLSession`: hay que mirar el código a mano.
            let codigo = (respuesta as? HTTPURLResponse)?.statusCode ?? 0
            guard (200...299).contains(codigo) else { throw ErrorDeRed.servidor(codigo: codigo) }
            return try JSONDecoder().decode(T.self, from: datos)
        } catch is URLError {
            // No hubo respuesta: sin internet, el servidor no existe, se cortó…
            throw ErrorDeRed.sinConexion
        }
    }
}
