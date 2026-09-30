import SwiftUI

// Pantallas de aprendizaje que no son parte de la app: se agrupan en una pestaña.
struct LaboratorioView: View {
    @State private var mostrandoControlador = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                Button("Ciclo de vida (UIKit)") { mostrandoControlador = true }
                    .buttonStyle(.borderedProminent)
                OrdenDeModifiers()
            }
            .padding()
        }
        .sheet(isPresented: $mostrandoControlador) {
            CicloDeVidaView()
        }
    }
}

#Preview {
    LaboratorioView()
}
