import SwiftUI

// La pantalla de los árbitros: pinta lo GUARDADO y, encima, el aviso si la red falló.
struct ArbitrosView: View {
    @State private var viewModel: ArbitrosViewModel

    init(viewModel: ArbitrosViewModel) {
        _viewModel = State(wrappedValue: viewModel)
    }

    var body: some View {
        let estado = viewModel.uiState
        List {
            if let error = estado.error, !estado.arbitros.isEmpty {
                // Hay datos guardados, pero no se pudieron renovar: se avisa sin vaciar la lista.
                Label(error, systemImage: "exclamationmark.triangle")
                    .font(.footnote)
                    .foregroundStyle(.orange)
            }
            ForEach(estado.arbitros) { arbitro in
                // «Leanne Graham · Gwenborough»
                Text("\(arbitro.nombre) · \(arbitro.ciudad)")
            }
        }
        .listStyle(.plain)
        // Si no hay nada guardado, en lugar de una lista vacía: la ruedecita o el error.
        .overlay {
            if estado.arbitros.isEmpty {
                if let error = estado.error {
                    // `ContentUnavailableView`: la pantalla vacía de iOS, con icono, texto y acciones.
                    ContentUnavailableView {
                        Label(error, systemImage: "wifi.slash")
                    } description: {
                        Text("Comprueba la conexión y vuelve a intentarlo.")
                    } actions: {
                        Button("Reintentar") {
                            Task { await viewModel.refrescar() }
                        }
                        .buttonStyle(.borderedProminent)
                    }
                } else {
                    ProgressView("Buscando árbitros…")
                }
            }
        }
        .navigationTitle("Árbitros")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            Button("Actualizar") {
                Task { await viewModel.refrescar() }
            }
            .disabled(estado.refrescando)
        }
        // Tirar de la lista hacia abajo también refresca.
        .refreshable { await viewModel.refrescar() }
        // Al aparecer: se ve lo guardado al instante y la red refresca por detrás.
        .task { await viewModel.refrescar() }
    }
}
