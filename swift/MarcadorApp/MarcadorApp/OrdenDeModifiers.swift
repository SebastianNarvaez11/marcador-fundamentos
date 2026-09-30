import SwiftUI

// EL ORDEN DE LOS MODIFICADORES IMPORTA (gemelo del orden de `Modifier` de Compose).
//
// Cada modificador NO cambia la vista: devuelve una vista NUEVA que envuelve a la anterior.
// Se lee de dentro hacia fuera: el texto, luego lo que lo envuelve, luego lo que envuelve eso.
//
//   A) .padding(16).background(.yellow)
//        1º el padding: una vista un poco más grande (texto + 16 de aire).
//        2º el fondo: pinta ESA vista más grande. Resultado: un recuadro amarillo con
//           el texto separado del borde.
//
//   B) .background(.yellow).padding(16)
//        1º el fondo: pinta solo el tamaño del texto.
//        2º el padding: añade 16 de aire FUERA del color. Resultado: el amarillo pegado
//           al texto, y el hueco (invisible) alrededor.
//
// Lo mismo pasa con `.frame` y `.border`: un borde antes de un `frame` rodea al texto; un borde
// después rodea al frame entero.
struct OrdenDeModifiers: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text("El orden de los modificadores")
                .font(.headline)

            Text("A · padding y luego background")
                .padding(16)
                .background(.yellow)
                .accessibilityIdentifier("versionA")

            Text("B · background y luego padding")
                .background(.yellow)
                .padding(16)
                .accessibilityIdentifier("versionB")

            // Para verlo mejor, un borde rojo alrededor de cada versión.
            Text("A con borde")
                .padding(16)
                .background(.yellow)
                .border(.red)
            Text("B con borde")
                .background(.yellow)
                .padding(16)
                .border(.red)

            // frame antes o después del fondo:
            Text("frame y luego fondo")
                .frame(width: 200)
                .background(.green.opacity(0.4))
            Text("fondo y luego frame")
                .background(.green.opacity(0.4))
                .frame(width: 200)
        }
    }
}

#Preview {
    OrdenDeModifiers().padding()
}
