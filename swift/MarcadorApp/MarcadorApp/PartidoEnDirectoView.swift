import SwiftUI
import Torneo

// f72 · @StateObject FRENTE A @ObservedObject FRENTE A @EnvironmentObject
//
// Los tres sirven para que una vista use un `ObservableObject`. Cambia QUIÉN ES EL DUEÑO:
//
//   @StateObject       la vista CREA el objeto y es su dueña. SwiftUI lo guarda fuera del struct
//                      y lo conserva aunque la vista se vuelva a crear. Se crea UNA sola vez.
//   @ObservedObject    la vista SOLO OBSERVA un objeto que le pasan (que otro mantiene vivo).
//                      Si lo crea ella misma, cada vez que su struct se recrea, nace otro objeto.
//   @EnvironmentObject el objeto lo puso un ANCESTRO con `.environmentObject(…)`; la vista lo
//                      recoge sin que se lo pasen por el constructor. Si nadie lo puso, la app se detiene.
//
// EL BUG QUE ESTE FICHERO CORRIGE (mira el diario, f72):
//
//     struct PartidoEnDirectoView: View {
//         @ObservedObject private var modelo: PartidoEnVivoModelo      // ← ASÍ NO VALE
//         init(partido: Partido) { modelo = PartidoEnVivoModelo(partido: partido) }
//
// Una vista es un struct barato y SwiftUI lo recrea cada vez que el padre recalcula su `body`.
// Con `@ObservedObject`, cada recreación ejecuta `init` y fabrica un modelo NUEVO: el minuto
// vuelve a 0, el marcador a 0-0 y el partido se «reinicia» sin que nadie lo pida. (El modelo
// viejo se queda huérfano; su bucle sigue corriendo y nadie lo ve.)
struct PartidoEnDirectoView: View {
    // ARREGLO: `@StateObject`. Y como el valor inicial depende de un parámetro, se crea en el
    // `init` con `StateObject(wrappedValue:)`, que recibe el objeto en un autoclosure: SwiftUI solo
    // lo evalúa la PRIMERA vez que se monta la vista; en las recreaciones lo ignora.
    @StateObject private var modelo: PartidoEnVivoModelo

    // Este NO lo crea esta vista: lo puso la raíz de la app. Por eso es @EnvironmentObject.
    @EnvironmentObject private var ajustes: AjustesModelo

    // Estado propio de la pantalla: cuántas veces pasó a segundo plano (`pausas` en Kotlin).
    @State private var pausas = 0
    @Environment(\.scenePhase) private var fase

    init(partido: Partido) {
        _modelo = StateObject(wrappedValue: PartidoEnVivoModelo(partido: partido))
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                TarjetaDePartido(partido: modelo.partido, marcador: modelo.marcador)

                BotonesDeGolConAccion(partido: modelo.partido) { lado in
                    modelo.golAMano(lado)
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("Minuto \(modelo.minuto)'")
                        .font(.title)
                        .monospacedDigit()
                        .accessibilityIdentifier("minuto")
                    Button(textoDelBoton) { modelo.empezar(duracion: ajustes.duracion) }
                        .buttonStyle(.borderedProminent)
                        .disabled(modelo.corriendo || modelo.minuto > 0)
                    Text("Último aviso (cada 15'): \(modelo.ultimoAviso)")
                    Text("Veces que la pantalla pasó a segundo plano: \(pausas)")
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                // El @EnvironmentObject en acción: la duración viene de los ajustes de la raíz.
                VStack(alignment: .leading, spacing: 8) {
                    Text("Duración del partido")
                    // `$ajustes.duracion`: el `$` de un @EnvironmentObject da un Binding a su propiedad.
                    Picker("Duración", selection: $ajustes.duracion) {
                        ForEach(AjustesModelo.duraciones, id: \.self) { minutos in
                            Text("\(minutos) min").tag(minutos)
                        }
                    }
                    .pickerStyle(.segmented)
                    // Cambiarla con el partido en marcha no afecta: se lee al pulsar «Empezar».
                    .disabled(modelo.corriendo || modelo.minuto > 0)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(16)
        }
        .onChange(of: fase) { _, nueva in
            if nueva == .background { pausas += 1 }
        }
    }

    private var textoDelBoton: String {
        if modelo.minuto >= ajustes.duracion { "Partido terminado" }
        else if modelo.corriendo { "En juego" }
        else { "Empezar partido" }
    }
}

// Los botones de gol, ahora con el HOISTING de Compose: reciben una closure y avisan de QUÉ ha
// pasado (`alGol(lado)`), en vez de escribir en un @Binding (f70). Quien los usa decide qué hacer.
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

// El PADRE que provoca el bug: tiene su propio estado y, al cambiarlo, recalcula su `body`, con
// lo que RECREA el struct de `PartidoEnDirectoView` (no la pantalla: el struct, que es barato).
// El botón «Redibujar el padre» hace exactamente eso a propósito, para reproducirlo sin esperar.
struct PartidoContenedor: View {
    let item: PartidoDeLista
    @State private var redibujados = 0

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("Partido n.º \(item.id)").font(.headline)
                Spacer()
                Button("Redibujar el padre (\(redibujados))") { redibujados += 1 }
                    .buttonStyle(.bordered)
            }
            .padding([.horizontal, .top], 16)

            PartidoEnDirectoView(partido: item.partido)
        }
    }
}
