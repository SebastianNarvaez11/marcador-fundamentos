import SwiftUI
import Torneo

// f69 · PRIMEROS PASOS CON SWIFTUI (gemelo de f32, Compose)
//
// UNA VISTA ES UN STRUCT que cumple `View`. Solo hay que dar una propiedad: `body`, que
// describe la pantalla PARA LOS DATOS ACTUALES. Cuando los datos cambian, SwiftUI vuelve
// a pedir `body` y compara: nadie hace `label.text = "2-1"`. Es declarativo, como Compose.
//
//   Compose                         SwiftUI
//   @Composable fun Pantalla()      struct Pantalla: View { var body: some View }
//   Modifier                        modificadores (`.padding()`, `.font()`…)
//   Column / Row / Box              VStack / HStack / ZStack
//   Scaffold + slots                NavigationStack, toolbar… (llegan en f76)
//
// `some View`: «un tipo concreto que es una vista, que el compilador conoce y yo no
// quiero escribir». El tipo real de un body con stacks y modificadores es enorme
// (`VStack<TupleView<(Text, HStack<...>)>>`): `some` lo esconde.
//
// Las vistas son BARATAS: son solo la descripción. SwiftUI las crea y las tira todo el rato.
// Guardar cosas dentro de un struct de vista (una variable normal) no vale: se pierde en cuanto
// se recrea. Para eso está `@State` (f70).
struct PantallaMarcadorEstatica: View {
    // Datos fijos: el partido de ejemplo. Una `let` en una vista es solo un dato de entrada.
    let partido: Partido = Ejemplo.rayoContraToros

    var body: some View {
        // VStack apila en vertical (Column), HStack en horizontal (Row), ZStack superpone (Box).
        // `spacing` es el hueco entre hijos, como `Arrangement.spacedBy`.
        VStack(spacing: 24) {
            Text("Marcador")
                .font(.headline)

            TarjetaDePartido(partido: partido)

            HStack(spacing: 8) {
                // Todavía sin acción: el botón necesita estado para hacer algo (f70).
                Button("Gol \(partido.local.nombre)") {}
                Button("Gol \(partido.visitante.nombre)") {}
            }
            .buttonStyle(.borderedProminent)

            // `Spacer` ocupa TODO el espacio libre y empuja a los demás hacia arriba.
            Spacer()
        }
        // El padding va DESPUÉS de los hijos: rodea a todo el VStack. Cada modificador
        // envuelve a lo que tiene delante (ver `OrdenDeModifiers`).
        .padding(16)
    }
}

// La tarjeta del partido: local — marcador — visitante. Es el `TarjetaDePartido` de Compose.
struct TarjetaDePartido: View {
    let partido: Partido

    var body: some View {
        HStack {
            Text(partido.local.nombre)
                .font(.title3)
                // El nombre ocupa el espacio que sobra; el marcador queda centrado.
                .frame(maxWidth: .infinity, alignment: .leading)
            Text("\(partido.golesLocal) - \(partido.golesVisitante)")
                .font(.system(size: 40, weight: .bold, design: .rounded))
                .monospacedDigit()
            Text(partido.visitante.nombre)
                .font(.title3)
                .frame(maxWidth: .infinity, alignment: .trailing)
        }
        .padding(20)
        // El fondo va DESPUÉS del padding: pinta también el margen. Con el orden contrario
        // (fondo y luego padding) el color no cubriría el «aire» (ver `OrdenDeModifiers`).
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
    }
}

#Preview {
    PantallaMarcadorEstatica()
}
