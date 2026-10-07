// Un repositorio cuya ÚNICA fuente es la red: pide los DTO al servicio y los convierte en modelos.
// Sin red no hay árbitros (más adelante se guardarán en el disco).
final class RepositorioDeArbitrosEnRed: ArbitrosRepositorio {
    private let servicio: any ServicioDelTorneo

    init(servicio: any ServicioDelTorneo) {
        self.servicio = servicio
    }

    func arbitros() async throws -> [Arbitro] {
        try await servicio.arbitros().map(Arbitro.init(dto:))
    }
}
