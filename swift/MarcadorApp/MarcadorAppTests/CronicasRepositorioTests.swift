import Testing
@testable import MarcadorApp

// El repositorio de crónicas con un servicio FALSO: ni red ni servidor, y los fallos a la carta.
@MainActor
struct CronicasRepositorioTests {

    @Test func publicaYDevuelveElIdDelServidor() async throws {
        let servicio = ServicioFalso()
        let repositorio = RepositorioDeCronicasEnRed(servicio: servicio)
        let id = try await repositorio.publicar(titulo: "Rayo FC 2-1 Toros", texto: "12' Ana (Rayo FC)")
        #expect(id == 101)
        // Lo que viajó al servidor: el DTO con el título y el texto.
        #expect(servicio.publicadas.first?.title == "Rayo FC 2-1 Toros")
        #expect(servicio.publicadas.first?.body == "12' Ana (Rayo FC)")
    }

    @Test func sinConexionLanzaSinConexion() async {
        let servicio = ServicioFalso()
        servicio.falloAlPublicar = .sinConexion
        let repositorio = RepositorioDeCronicasEnRed(servicio: servicio)
        await #expect(throws: ErrorDeRed.sinConexion) {
            try await repositorio.publicar(titulo: "Rayo FC 2-1 Toros", texto: "Sin goles")
        }
    }

    @Test func unQuinientosLanzaElErrorDelServidorConSuCodigo() async {
        let servicio = ServicioFalso()
        servicio.falloAlPublicar = .servidor(codigo: 500)
        let repositorio = RepositorioDeCronicasEnRed(servicio: servicio)
        await #expect(throws: ErrorDeRed.servidor(codigo: 500)) {
            try await repositorio.publicar(titulo: "Rayo FC 2-1 Toros", texto: "Sin goles")
        }
    }
}
