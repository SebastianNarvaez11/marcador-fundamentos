import SwiftUI

// La pantalla de la crónica: el título, los goles y el botón «Publicar».
struct CronicaView: View {
    @State private var viewModel: CronicaViewModel

    init(viewModel: CronicaViewModel) {
        _viewModel = State(wrappedValue: viewModel)
    }

    var body: some View {
        let estado = viewModel.uiState
        List {
            if let cronica = estado.cronica {
                Section("Título") {
                    Text(cronica.titulo).bold()
                }
                Section("Goles") {
                    Text(cronica.texto)
                }
                Section {
                    Button("Publicar") {
                        Task { await viewModel.publicar() }
                    }
                    .disabled(estado.publicando)
                    if estado.publicando {
                        ProgressView()
                    }
                }
            }
            if let mensaje = estado.mensaje {
                Text(mensaje).accessibilityIdentifier("mensaje-cronica")
            }
        }
        .navigationTitle("Crónica del partido")
        .navigationBarTitleDisplayMode(.inline)
    }
}
