import SwiftUI
import Torneo

// f76 · Tercera pantalla de la pila: lista → partido → goleadores (gemelo de `PantallaGoleadores`).
// Se calcula con el `Ranking` del paquete `Torneo`: la pantalla solo pinta.
struct Goleador: Identifiable, Equatable {
    let jugador: String
    let goles: Int
    var id: String { jugador }   // la identidad es el nombre, como la `key` de Kotlin
}

extension RepositorioDePartidos {
    // Los goleadores no se guardan: se CALCULAN a partir de los partidos (una sola fuente de la verdad).
    func goleadores() -> [Goleador] {
        let ranking = partidos.map(\.partido).goleadores()
        return ranking.ordenados.map { Goleador(jugador: $0.nombre, goles: ranking.puntosDe($0)) }
    }
}

struct GoleadoresView: View {
    @Environment(RepositorioDePartidos.self) private var repositorio
    // `dismiss` quita esta pantalla de la pila: el equivalente de `pila.removeLast()`.
    @Environment(\.dismiss) private var cerrar

    var body: some View {
        // Se lee `goleadores()` dentro del body: si mañana cambia un partido, esto se recalcula solo.
        List(repositorio.goleadores()) { goleador in
            HStack {
                Text(goleador.jugador)
                Spacer()
                Text("\(goleador.goles) goles").monospacedDigit()
            }
        }
        .listStyle(.plain)
        .navigationTitle("Goleadores")
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .bottom) {
            Button("Volver al partido") { cerrar() }
                .buttonStyle(.borderedProminent)
                .padding()
        }
    }
}

#Preview {
    NavigationStack { GoleadoresView() }
        .environment(RepositorioDePartidos())
}
