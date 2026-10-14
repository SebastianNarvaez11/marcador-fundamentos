import Foundation
import Observation

// DOS FUENTES, UNA PUERTA: el disco manda y la red refresca.
//
// La pantalla lee `arbitros`, que sale SIEMPRE de lo guardado: al abrir la app se ve al instante,
// con red o sin ella. `refrescar()` sigue el camino servidor → modelo → disco → `arbitros`. Si la red
// falla, lanza el error y lo guardado sigue ahí: la lista no se vacía.
//
// `@Observable`: cuando `refrescar()` cambia `arbitros`, la pantalla se redibuja sola.
@Observable
final class RepositorioDeArbitros: ArbitrosRepositorio {
    private(set) var arbitros: [Arbitro]

    @ObservationIgnored private let servicio: any ServicioDelTorneo
    @ObservationIgnored private let almacen: AlmacenDeArbitros?

    // Con `almacen: nil` (las vistas previas) no se guarda nada: vive solo en memoria.
    init(servicio: any ServicioDelTorneo, almacen: AlmacenDeArbitros? = nil) {
        self.servicio = servicio
        self.almacen = almacen
        self.arbitros = almacen?.cargar() ?? []   // lo último guardado, sin esperar a la red
    }

    func refrescar() async throws {
        let nuevos = try await servicio.arbitros().map(Arbitro.init(dto:))   // servidor → modelo
        do {
            try almacen?.guardar(nuevos)                                     // → disco
        } catch {
            Registro.anotar("no se pudo guardar arbitros.json: \(error)")
        }
        arbitros = nuevos                                                   // → la pantalla
        Registro.anotar("árbitros REFRESCADOS: \(nuevos.count)")
    }
}
