import Foundation
import Observation
import Torneo

// El formulario vive en el ViewModel (`@Observable`): la vista escribe en `formulario` con un
// `Binding` y lee los errores, que se calculan cada vez con la plantilla GUARDADA.
@MainActor
@Observable
final class AnadirJugadorViewModel {
    let equipo: String
    var formulario = FormularioJugador()

    @ObservationIgnored private let repositorio: any PartidosRepositorio

    init(equipo: String, repositorio: any PartidosRepositorio) {
        self.equipo = equipo
        self.repositorio = repositorio
    }

    // Se lee del repositorio en cada consulta: si cambia, los errores cambian con él.
    private var plantilla: [Jugador] {
        repositorio.equipos.first { $0.nombre == equipo }?.plantilla ?? []
    }

    // Lo que ve la vista: el error SOLO si el campo está tocado.
    var errorNombre: ErrorNombre? { formulario.nombreTocado ? formulario.errorNombre(en: plantilla) : nil }
    var errorDorsal: ErrorDorsal? { formulario.dorsalTocado ? formulario.errorDorsal(en: plantilla) : nil }
    var sePuedeGuardar: Bool { formulario.esValido(en: plantilla) }

    // Devuelve si se guardó: la vista cierra la pantalla solo entonces.
    func guardar() -> Bool {
        guard sePuedeGuardar else { return false }
        return repositorio.anadirJugador(formulario.aJugador(), a: equipo)
    }
}
