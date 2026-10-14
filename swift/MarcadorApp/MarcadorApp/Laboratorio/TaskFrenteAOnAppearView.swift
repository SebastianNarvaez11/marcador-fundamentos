import SwiftUI

// `.task` FRENTE A `.onAppear { Task { … } }`
//
// Dos contadores que cuentan cada 200 ms hasta 50 (10 segundos). Ábrelo en una hoja y ciérrala
// a los 2 segundos; mira el registro:
//   .task      → «task: CANCELADO en el tick 9»      (SwiftUI cancela al irse la vista)
//   .onAppear  → «onAppear: tick 10, 11, 12…»         (la Task suelta sigue viva sin pantalla)
struct TaskFrenteAOnAppearView: View {
    @State private var conTask = 0
    @State private var conOnAppear = 0

    var body: some View {
        VStack(spacing: 16) {
            Text("Cierra esta hoja y mira el registro").font(.headline)
            Text("con .task: \(conTask)").accessibilityIdentifier("contadorTask")
            Text("con .onAppear: \(conOnAppear)").accessibilityIdentifier("contadorOnAppear")
        }
        .padding()
        .task {
            await contar(nombre: "task") { conTask = $0 }
        }
        .onAppear {
            // Una `Task` NUEVA y sin dueño: nadie la cancela.
            Task { await contar(nombre: "onAppear") { conOnAppear = $0 } }
        }
    }

    private func contar(nombre: String, actualizar: @escaping (Int) -> Void) async {
        for tick in 1...50 {
            do {
                try await Task.sleep(for: .milliseconds(200))
            } catch {
                Registro.anotar("\(nombre): CANCELADO en el tick \(tick)")
                return
            }
            actualizar(tick)
            Registro.anotar("\(nombre): tick \(tick)")
        }
        Registro.anotar("\(nombre): terminó")
    }
}

#Preview {
    TaskFrenteAOnAppearView()
}
