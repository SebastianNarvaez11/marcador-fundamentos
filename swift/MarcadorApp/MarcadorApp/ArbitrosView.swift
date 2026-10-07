import SwiftUI

// La pantalla de los árbitros: pinta el `uiState` del ViewModel, como la del partido.
struct ArbitrosView: View {
    @State private var viewModel: ArbitrosViewModel

    init(viewModel: ArbitrosViewModel) {
        _viewModel = State(wrappedValue: viewModel)
    }

    var body: some View {
        Group {
            switch viewModel.uiState {
            case .cargando:
                ProgressView("Buscando árbitros…")
            case let .error(mensaje):
                // `ContentUnavailableView`: la pantalla vacía de iOS, con icono, texto y acciones.
                ContentUnavailableView {
                    Label(mensaje, systemImage: "wifi.slash")
                } description: {
                    Text("Comprueba la conexión y vuelve a intentarlo.")
                } actions: {
                    Button("Reintentar") {
                        Task { await viewModel.reintentar() }
                    }
                    .buttonStyle(.borderedProminent)
                }
            case let .exito(arbitros):
                List(arbitros) { arbitro in
                    // «Leanne Graham · Gwenborough»
                    Text("\(arbitro.nombre) · \(arbitro.ciudad)")
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle("Árbitros")
        .navigationBarTitleDisplayMode(.inline)
        // Arranca al aparecer la pantalla y se cancela si se va antes de terminar.
        .task { await viewModel.cargar() }
    }
}
