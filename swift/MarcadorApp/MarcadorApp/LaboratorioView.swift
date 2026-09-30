import SwiftUI

// Pantallas de aprendizaje que no son parte de la app: se agrupan en una pestaña.
struct LaboratorioView: View {
    @State private var mostrandoControlador = false
    @State private var mostrandoTask = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                Button("Ciclo de vida (UIKit)") { mostrandoControlador = true }
                    .buttonStyle(.borderedProminent)
                Button(".task frente a .onAppear") { mostrandoTask = true }
                    .buttonStyle(.bordered)
                OrdenDeModifiers()
                IdentidadView()
            }
            .padding()
        }
        .sheet(isPresented: $mostrandoControlador) {
            CicloDeVidaView()
        }
        .sheet(isPresented: $mostrandoTask) {
            TaskFrenteAOnAppearView()
        }
    }
}

#Preview {
    LaboratorioView()
}
