import SwiftUI
import Torneo

// La lista de equipos: cada fila empuja la plantilla de ese equipo. Lee el repositorio
// (`@Observable`): si mañana se añade un equipo, aparece solo.
struct EquiposView: View {
    @Environment(RepositorioDePartidos.self) private var repositorio

    var body: some View {
        List(repositorio.equipos, id: \.nombre) { equipo in
            NavigationLink(value: Destino.plantilla(equipo.nombre)) {
                // «1 jugador», pero «3 jugadores»: el plural depende del número.
                let cantidad = equipo.cantidadDeJugadores
                LabeledContent(equipo.nombre, value: cantidad == 1 ? "1 jugador" : "\(cantidad) jugadores")
            }
        }
        .navigationTitle("Equipos")
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview {
    NavigationStack { EquiposView() }
        .environment(RepositorioDePartidos())
}
