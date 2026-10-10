import SwiftUI
import Torneo

// La plantilla de un equipo: una fila por jugador, con su dorsal o un guion. El equipo llega por
// su NOMBRE (el del `Destino`) y se busca en el repositorio en cada pasada del body: cuando se
// añada un jugador, el repositorio cambia y esta lista se redibuja sola.
struct PlantillaView: View {
    let equipo: String
    @Environment(RepositorioDePartidos.self) private var repositorio

    private var jugadores: [Jugador] {
        repositorio.equipos.first { $0.nombre == equipo }?.plantilla ?? []
    }

    var body: some View {
        List(jugadores, id: \.nombre) { jugador in
            LabeledContent(jugador.nombre, value: jugador.dorsalOGuion())
        }
        .navigationTitle(equipo)
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview {
    NavigationStack { PlantillaView(equipo: "Rayo FC") }
        .environment(RepositorioDePartidos())
}
