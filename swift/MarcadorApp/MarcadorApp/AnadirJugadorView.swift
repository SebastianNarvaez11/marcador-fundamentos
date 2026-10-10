import SwiftUI

// LA PANTALLA DEL FORMULARIO (gemelo de `PantallaAnadirJugador`). `Form` es la lista agrupada de
// iOS para formularios (la de Ajustes). Lo suyo: el foco, el teclado y el texto de cada error.
struct AnadirJugadorView: View {
    @State private var viewModel: AnadirJugadorViewModel
    @Environment(\.dismiss) private var cerrar

    // Qué campo tiene el foco (el teclado). `nil` = ninguno.
    enum Campo { case nombre, dorsal }
    @FocusState private var campo: Campo?

    init(viewModel: AnadirJugadorViewModel) {
        _viewModel = State(wrappedValue: viewModel)
    }

    var body: some View {
        Form {
            Section {
                // `$viewModel.formulario.nombre`: un Binding que escribe en el struct del ViewModel.
                TextField("Nombre", text: $viewModel.formulario.nombre)
                    .textInputAutocapitalization(.words)
                    .focused($campo, equals: .nombre)
                    .submitLabel(.next)
                    .onSubmit { campo = .dorsal }
            } footer: {
                if let error = viewModel.errorNombre {
                    Text(texto(de: error)).foregroundStyle(.red)
                } else {
                    Text("\(viewModel.formulario.nombre.trimmingCharacters(in: .whitespaces).count)/\(FormularioJugador.maximoNombre)")
                }
            }
            Section {
                TextField("Dorsal (opcional)", text: $viewModel.formulario.dorsal)
                    .keyboardType(.numberPad)
                    .focused($campo, equals: .dorsal)
            } footer: {
                if let error = viewModel.errorDorsal {
                    Text(texto(de: error)).foregroundStyle(.red)
                }
            }
        }
        .navigationTitle("Añadir jugador")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            // `.confirmationAction`: el sitio de iOS para «Guardar» (arriba a la derecha).
            ToolbarItem(placement: .confirmationAction) {
                Button("Guardar") {
                    if viewModel.guardar() { cerrar() }
                }
                .disabled(!viewModel.sePuedeGuardar)
            }
        }
    }

    private func texto(de error: ErrorNombre) -> String {
        switch error {
        case .vacio: "Escribe un nombre"
        case .largo: "Como mucho \(FormularioJugador.maximoNombre) letras"
        case .repetido: "Ya hay un jugador con ese nombre"
        }
    }

    private func texto(de error: ErrorDorsal) -> String {
        switch error {
        case .noEsNumero: "Escribe solo números"
        case .fueraDeRango: "Del 1 al 99"
        case .repetido: "Ese dorsal ya lo lleva otro jugador"
        }
    }
}

#Preview {
    NavigationStack {
        AnadirJugadorView(viewModel: AnadirJugadorViewModel(equipo: "Rayo FC", repositorio: RepositorioDePartidos()))
    }
}
