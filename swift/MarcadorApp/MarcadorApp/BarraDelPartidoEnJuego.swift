import SwiftUI
import Torneo

// TU PROPIO VALOR DEL ENTORNO: `abrirPartido` (como `\.dismiss`, pero tuyo).
//
// `@Entry` declara una clave del entorno con su valor por defecto en una línea. La raíz pone el valor
// de verdad (`.environment(\.abrirPartido) { … }`) y cualquier vista de debajo lo lee con
// `@Environment(\.abrirPartido)`. Si nadie lo pone (una vista previa), vale el de por defecto: no
// hace nada. Compáralo con `@Environment(Tipo.self)`: sin el objeto en el entorno, la app se detiene.
extension EnvironmentValues {
    @Entry var abrirPartido: (Int) -> Void = { _ in }
}

// El texto de la barra (gemelo de `textoDeLaBarra` en Android):
//   en juego -> «Rayo FC 2–1 Toros · 34'»
//   al final -> «Final · Rayo FC 3–2 Toros»
func textoDeLaBarra(local: String, visitante: String, marcador: Marcador, minuto: Int, terminado: Bool) -> String {
    let resultado = "\(local) \(marcador.local)–\(marcador.visitante) \(visitante)"
    return terminado ? "Final · \(resultado)" : "\(resultado) · \(minuto)'"
}

// LA BARRA DEL PARTIDO EN JUEGO: abajo, encima de las pestañas, en todas las pantallas menos la del
// propio partido. Al tocarla se abre el partido; al terminar, una X la cierra.
//
// `.safeAreaInset(edge: .bottom)` coloca la barra abajo y ENCOGE la zona segura del contenido: una
// lista se desplaza por encima de la barra, sin quedar tapada. (En iOS 26 existe
// `tabViewBottomAccessory`, hecho para esto; la app es para iOS 18.)
struct BarraDelPartidoEnJuego: ViewModifier {
    @Environment(PartidoEnJuego.self) private var enJuego
    @Environment(\.abrirPartido) private var abrirPartido
    // La pantalla que se ve en esta pestaña: si es el propio partido, la barra no sale.
    let pantallaActual: Destino?

    func body(content: Content) -> some View {
        content.safeAreaInset(edge: .bottom, spacing: 0) {
            if let directo = enJuego.directo, pantallaActual != .partido(directo.partidoId) {
                HStack(spacing: 4) {
                    Button {
                        abrirPartido(directo.partidoId)
                    } label: {
                        Text(textoDeLaBarra(
                            local: directo.partido.local.nombre, visitante: directo.partido.visitante.nombre,
                            marcador: directo.marcador, minuto: directo.minuto, terminado: directo.terminado
                        ))
                        .font(.subheadline.weight(.semibold))
                        .monospacedDigit()
                        .frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
                        .contentShape(.rect)
                    }
                    .accessibilityHint("Abre el partido")
                    .accessibilityIdentifier("barraDelPartido")
                    // Mientras se juega no se puede cerrar: la X solo sale al final.
                    if directo.terminado {
                        Button {
                            enJuego.cerrar()
                        } label: {
                            Image(systemName: "xmark")
                                .frame(width: 44, height: 44)
                                .contentShape(.rect)
                        }
                        .accessibilityLabel("Cerrar")
                    }
                }
                .buttonStyle(.plain)
                .padding(.horizontal, 16)
                .background(.bar)
                .overlay(alignment: .top) { Divider() }
            }
        }
    }
}

extension View {
    func barraDelPartidoEnJuego(pantallaActual: Destino? = nil) -> some View {
        modifier(BarraDelPartidoEnJuego(pantallaActual: pantallaActual))
    }
}

#Preview("En juego") {
    let enJuego = PartidoEnJuego()
    _ = enJuego.empezar(Directo(partidoId: 1, partido: Ejemplo.rayoContraToros, duracion: 90, msPorMinuto: 250))
    return List(1...20, id: \.self) { Text("Fila \($0)") }
        .barraDelPartidoEnJuego()
        .environment(enJuego)
}
