import Foundation
import Testing
@testable import MarcadorApp

// El repositorio de dos fuentes con un servicio FALSO y un fichero de verdad en una carpeta temporal.
@MainActor
struct RepositorioDeArbitrosTests {
    private let leanne = ArbitroDTO(id: 1, name: "Leanne Graham", email: "Sincere@april.biz", address: .init(city: "Gwenborough"))

    // Un fichero distinto en cada prueba, en la carpeta temporal (no toca el `arbitros.json` de la app).
    private func almacenTemporal() -> AlmacenDeArbitros {
        AlmacenDeArbitros(url: FileManager.default.temporaryDirectory.appending(path: "\(UUID().uuidString).json"))
    }

    @Test func alRefrescarGuardaEnElDiscoYOtroRepositorioLoLeeSinRed() async throws {
        let almacen = almacenTemporal()
        defer { almacen.borrar() }
        let servicio = ServicioFalso()
        servicio.arbitrosQueDevuelve = .success([leanne])
        let repositorio = RepositorioDeArbitros(servicio: servicio, almacen: almacen)
        #expect(repositorio.arbitros.isEmpty)   // aún no se bajó nada
        try await repositorio.refrescar()
        #expect(repositorio.arbitros == [Arbitro(id: 1, nombre: "Leanne Graham", ciudad: "Gwenborough")])

        // Otro repositorio (como al volver a abrir la app), sin red: lee lo guardado al crearse.
        let sinRed = ServicioFalso()
        sinRed.arbitrosQueDevuelve = .failure(.sinConexion)
        let otro = RepositorioDeArbitros(servicio: sinRed, almacen: almacen)
        #expect(otro.arbitros.map(\.nombre) == ["Leanne Graham"])
    }

    @Test func sinRedConservaLoGuardadoYElErrorEsDeDominio() async throws {
        let almacen = almacenTemporal()
        defer { almacen.borrar() }
        try almacen.guardar([Arbitro(id: 1, nombre: "Leanne Graham", ciudad: "Gwenborough")])
        let servicio = ServicioFalso()
        servicio.arbitrosQueDevuelve = .failure(.sinConexion)
        let repositorio = RepositorioDeArbitros(servicio: servicio, almacen: almacen)
        await #expect(throws: ErrorDeRed.sinConexion) {
            try await repositorio.refrescar()
        }
        #expect(repositorio.arbitros.map(\.nombre) == ["Leanne Graham"])   // la lista no se vació
    }
}
