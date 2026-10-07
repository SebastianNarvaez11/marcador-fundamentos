import SwiftUI
import Torneo

// @StateObject FRENTE A @ObservedObject FRENTE A @EnvironmentObject (el bug de `@ObservedObject`)
// @State, @Bindable Y @Environment (con `@Observable`)
// `.task`: el partido arranca solo y se cancela al salir
// La duración vive en `@AppStorage`
// MVVM: la vista solo PINTA el `uiState` del ViewModel y le pasa los toques
//
// (La historia del bug de `@ObservedObject`: con `@ObservedObject var modelo = PartidoEnVivoModelo(…)`
// creado en el `init`, cada redibujado del padre fabricaba un modelo nuevo y el partido volvía al minuto 0.)
//
// MATIZ de `@State` con un objeto: `State(wrappedValue:)` NO es un autoclosure. Cada recreación de la
// vista ejecuta `PartidoViewModel(…)` y SwiftUI tira el resultado; solo se queda el de la primera vez.
// Como el ViewModel no hace nada en el `init` salvo leer el partido, no importa.
struct PartidoEnDirectoView: View {
    @State private var viewModel: PartidoViewModel

    // `@AppStorage`: una propiedad que LEE y ESCRIBE en `UserDefaults` (un diccionario clave-valor
    // guardado en `Library/Preferences/<bundle id>.plist` dentro del sandbox). Funciona como un `@State`
    // (cambiar el valor redibuja la vista) y además sobrevive a cerrar la app: es el DataStore de
    // Android en una línea. Solo vale para cosas pequeñas y simples (Bool, Int, String, Double, URL).
    // Sustituye al `AjustesModelo` de antes.
    @AppStorage(Ajustes.claveDuracion) private var duracion = Ajustes.duracionPorDefecto
    @Environment(\.dismiss) private var cerrar

    // Estado propio de la pantalla: cuántas veces pasó a segundo plano (`pausas` en Kotlin).
    @State private var pausas = 0
    @Environment(\.scenePhase) private var fase

    // El notificador (lo puso la raíz en el entorno) y su permiso, que se lee para pintar el botón.
    @Environment(NotificadorDeSistema.self) private var notificador

    // La vista RECIBE su ViewModel ya montado (lo fabrica el contenedor): no sabe con qué repositorio
    // ni con qué notificador se creó.
    init(viewModel: PartidoViewModel) {
        _viewModel = State(wrappedValue: viewModel)
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
        .task(id: duracion) {
            await viewModel.jugar(duracion: duracion)
        }
        .task { await notificador.actualizarPermiso() }
        .onChange(of: fase) { _, nueva in
            if nueva == .background { pausas += 1 }
        }
    }

    private func contenido(_ datos: MarcadorUiState.Exito) -> some View {
        ScrollView {
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
                    // Empuja OTRO valor en la pila (gemelo del botón que empuja `Goleadores` en Kotlin).
                    NavigationLink("Ver goleadores", value: Destino.goleadores)
                    // La crónica de ESTE partido: el id viaja en el destino.
                    NavigationLink("Crónica", value: Destino.cronica(datos.partidoId))
                    // Pedir el permiso de notificaciones. La primera vez sale el diálogo del sistema.
                    Button(notificador.permitido ? "Avisos de gol: activados" : "Activar avisos de gol") {
                        Task { await notificador.pedirPermiso() }
                    }
                    .buttonStyle(.bordered)
                    .disabled(notificador.permitido)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                VStack(alignment: .leading, spacing: 8) {
                    Text("Duración del partido")
                    Picker("Duración", selection: $duracion) {
                        ForEach(Ajustes.duraciones, id: \.self) { minutos in
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
// (`alGol(lado)`), en vez de escribir en un @Binding.
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

// El PADRE que provocaba el bug de `@ObservedObject`: tiene su propio estado y, al cambiarlo, recalcula su `body`,
// con lo que RECREA el struct de `PartidoEnDirectoView`. El botón «Redibujar el padre» lo hace a propósito.
struct PartidoContenedor: View {
    let id: Int
    @Environment(ContenedorDeLaApp.self) private var contenedor
    @State private var redibujados = 0

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Spacer()
                Button("Redibujar el padre (\(redibujados))") { redibujados += 1 }
                    .buttonStyle(.bordered)
            }
            .padding([.horizontal, .top], 16)

            PartidoEnDirectoView(viewModel: contenedor.partidoViewModel(id: id))
        }
        .navigationTitle("Partido n.º \(id)")
        .navigationBarTitleDisplayMode(.inline)
    }
}
