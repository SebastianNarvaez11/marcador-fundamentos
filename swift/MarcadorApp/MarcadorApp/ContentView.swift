import SwiftUI
// El paquete local `../Torneo`. Sin este `import`, `Ejemplo` y `Marcador` no existen aquí.
import Torneo

// Pantalla mínima que demuestra que la app ve el paquete: el marcador del partido de
// ejemplo sale de `Torneo`, no de esta app. (La pantalla de verdad llega en f69.)
struct ContentView: View {
    var body: some View {
        // `Ejemplo.rayoContraToros.marcador()` es código del paquete: `Partido.marcador()`.
        let partido = Ejemplo.rayoContraToros
        VStack(spacing: 12) {
            Text("Marcador")
                .font(.largeTitle.bold())
            Text("\(partido.local.nombre) \(partido.marcador().description) \(partido.visitante.nombre)")
                .font(.title2)
        }
        .padding()
    }
}

#Preview {
    ContentView()
}
