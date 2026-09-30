import SwiftUI
import Torneo

// f72 · @StateObject FRENTE A @ObservedObject FRENTE A @EnvironmentObject (el bug de `@ObservedObject`)
// f73 · @State, @Bindable Y @Environment (con `@Observable`)
// f74 · `.task`: el partido arranca solo y se cancela al salir
// f75 · MVVM: la vista solo PINTA el `uiState` del ViewModel y le pasa los toques
//
// (La historia del bug de f72 está en el diario: con `@ObservedObject var modelo = PartidoEnVivoModelo(…)`
// creado en el `init`, cada redibujado del padre fabricaba un modelo nuevo y el partido volvía al minuto 0.)
//
// MATIZ de `@State` con un objeto: `State(wrappedValue:)` NO es un autoclosure. Cada recreación de la
// vista ejecuta `PartidoViewModel(…)` y SwiftUI tira el resultado; solo se queda el de la primera vez.
// Como el ViewModel no hace nada en el `init` salvo leer el partido, no importa.
struct PartidoEnDirectoView: View {
    @State private var viewModel: PartidoViewModel

    // Lo puso la raíz de la app con `.environment(ajustes)`; se recoge por TIPO.
    @Environment(AjustesModelo.self) private var ajustes
    @Environment(\.dismiss) private var cerrar

    // Estado propio de la pantalla: cuántas veces pasó a segundo plano (`pausas` en Kotlin).
    @State private var pausas = 0
    @Environment(\.scenePhase) private var fase

    init(partidoId: Int, repositorio: any PartidosRepositorio) {
        _viewModel = State(wrappedValue: PartidoViewModel(partidoId: partidoId, repositorio: repositorio))
    }

    var body: some View {
        // `switch` exhaustivo sobre el enum: si mañana hay una variante más, no compila.
        Group {
            switch viewModel.uiState {
            case .cargando:
                ProgressView()
            case let .error(mensaje):
                VStack(spacing: 16) {
                    Text(mensaje).foregroundStyle(.red)
                    Button("Volver") { cerrar() }
                }
            case let .exito(datos):
                contenido(datos)
            }
        }
        // `.task(id:)`: arranca al aparecer, se CANCELA al irse la vista y se relanza si cambia la duración.
        .task(id: ajustes.duracion) {
            await viewModel.jugar(duracion: ajustes.duracion)
        }
        .onChange(of: fase) { _, nueva in
            if nueva == .background { pausas += 1 }
        }
    }

    private func contenido(_ datos: MarcadorUiState.Exito) -> some View {
        // `@Bindable` da `$ajustes.duracion` (un Binding) a un objeto `@Observable`.
        @Bindable var ajustes = ajustes
        return ScrollView {
            VStack(spacing: 24) {
                TarjetaDePartido(partido: datos.partido, marcador: datos.marcador)

                // HOISTING de Compose: la vista avisa de QUÉ pasó (`alGol`), el ViewModel decide.
                BotonesDeGolConAccion(partido: datos.partido, alGol: viewModel.alGol)

                VStack(alignment: .leading, spacing: 8) {
                    Text("Minuto \(datos.minuto)'")
                        .font(.title)
                        .monospacedDigit()
                        .accessibilityIdentifier("minuto")
                    Text(estado(datos))
                        .foregroundStyle(.secondary)
                        .accessibilityIdentifier("estado")
                    Text("Último aviso (cada 15'): \(datos.ultimoAviso)")
                    Text("Veces que la pantalla pasó a segundo plano: \(pausas)")
                    // f76: empuja OTRO valor en la pila (gemelo del botón que empuja `Goleadores` en Kotlin).
                    NavigationLink("Ver goleadores", value: Destino.goleadores)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                VStack(alignment: .leading, spacing: 8) {
                    Text("Duración del partido")
                    Picker("Duración", selection: $ajustes.duracion) {
                        ForEach(AjustesModelo.duraciones, id: \.self) { minutos in
                            Text("\(minutos) min").tag(minutos)
                        }
                    }
                    .pickerStyle(.segmented)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(16)
        }
    }

    private func estado(_ datos: MarcadorUiState.Exito) -> String {
        if datos.minuto >= datos.duracion { "Partido terminado" }
        else if datos.corriendo { "En juego" }
        else { "Sin empezar" }
    }
}

// Los botones de gol con el HOISTING de Compose: reciben una closure y avisan de QUÉ ha pasado
// (`alGol(lado)`), en vez de escribir en un @Binding (f70).
struct BotonesDeGolConAccion: View {
    let partido: Partido
    let alGol: (Lado) -> Void

    var body: some View {
        HStack(spacing: 8) {
            Button("Gol \(partido.local.nombre)") { alGol(.local) }
            Button("Gol \(partido.visitante.nombre)") { alGol(.visitante) }
        }
        .buttonStyle(.borderedProminent)
    }
}

// El PADRE que provocaba el bug de f72: tiene su propio estado y, al cambiarlo, recalcula su `body`,
// con lo que RECREA el struct de `PartidoEnDirectoView`. El botón «Redibujar el padre» lo hace a propósito.
struct PartidoContenedor: View {
    let id: Int
    @Environment(RepositorioEnMemoria.self) private var repositorio
    @State private var redibujados = 0

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Spacer()
                Button("Redibujar el padre (\(redibujados))") { redibujados += 1 }
                    .buttonStyle(.bordered)
            }
            .padding([.horizontal, .top], 16)

            PartidoEnDirectoView(partidoId: id, repositorio: repositorio)
        }
        .navigationTitle("Partido n.º \(id)")
        .navigationBarTitleDisplayMode(.inline)
    }
}
