import Foundation

// EL SERVICIO: lo que la app puede pedir al servidor (gemelo de la interfaz `LigaApi` de Retrofit).
//
// Es un PROTOCOLO, como el repositorio: quien lo usa dice QUÉ quiere («dame los árbitros») y no
// sabe cómo viaja la petición. En las pruebas se cambia por un falso que no toca la red.
protocol ServicioDelTorneo {
    func arbitros() async throws -> [ArbitroDTO]
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
        let url = base.appending(path: "users")
        // `await`: la espera de la red no ocupa el hilo principal; la pantalla sigue respondiendo.
        let (datos, respuesta) = try await sesion.data(from: url)
        // Un 404 o un 500 NO lanzan ningún error en `URLSession`: hay que mirar el código a mano.
        let codigo = (respuesta as? HTTPURLResponse)?.statusCode ?? 0
        guard (200...299).contains(codigo) else { throw URLError(.badServerResponse) }
        return try JSONDecoder().decode([ArbitroDTO].self, from: datos)
    }
}
