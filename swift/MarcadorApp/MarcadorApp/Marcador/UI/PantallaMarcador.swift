import SwiftUI
import Torneo

// PRIMEROS PASOS CON SWIFTUI (gemelo de Compose)
// @State Y @Binding (gemelo del estado y el state hoisting de Compose)
//
// UNA VISTA ES UN STRUCT que cumple `View`. Solo hay que dar una propiedad: `body`, que
// describe la pantalla PARA LOS DATOS ACTUALES. Cuando los datos cambian, SwiftUI vuelve
// a pedir `body` y compara: nadie hace `label.text = "2-1"`. Es declarativo, como Compose.
//
//   Compose                         SwiftUI
//   @Composable fun Pantalla()      struct Pantalla: View { var body: some View }
//   Modifier                        modificadores (`.padding()`, `.font()`…)
//   Column / Row / Box              VStack / HStack / ZStack
//   Scaffold + slots                NavigationStack, toolbar… (llegan después)
//
// `some View`: «un tipo concreto que es una vista, que el compilador conoce y yo no
// quiero escribir». El tipo real de un body con stacks y modificadores es enorme
// (`VStack<TupleView<(Text, HStack<...>)>>`): `some` lo esconde.
//
// Las vistas son BARATAS: son solo la descripción. SwiftUI las crea y las tira todo el rato.
// Guardar cosas dentro de un struct de vista (una variable normal) no vale: se pierde en cuanto
// se recrea. Para eso está `@State`.
//
// @STATE: el estado que VIVE en la vista.
//
// Un struct es un valor y `body` no puede modificarlo (`self` es inmutable): `var marcador = …`
// a secas ni siquiera compila al cambiarlo. `@State` saca el valor del struct y lo guarda en un
// almacén que SwiftUI mantiene mientras la vista exista en pantalla; cuando cambia, SwiftUI
// vuelve a pedir `body`. Es el `remember { mutableStateOf(…) }` de Compose.
//
// Va `private`: el dueño del estado es esta vista y nadie más lo toca. Si otra vista tiene que
// LEERLO Y CAMBIARLO, se le pasa un `@Binding` (ver `BotonesDeGol`).
struct PantallaMarcador: View {
    let partido: Partido = Ejemplo.rayoContraToros

    @State private var marcador = Marcador(local: 0, visitante: 0)

    var body: some View {
        // VStack apila en vertical (Column), HStack en horizontal (Row), ZStack superpone (Box).
        // `spacing` es el hueco entre hijos, como `Arrangement.spacedBy`.
        VStack(spacing: 24) {
            Text("Marcador")
                .font(.headline)

            TarjetaDePartido(partido: partido, marcador: marcador)

            // `$marcador` (con dólar) es el BINDING del estado: no el valor, sino una «referencia
            // de lectura y escritura» a él. `marcador` es el valor; `$marcador`, la puerta.
            BotonesDeGol(partido: partido, marcador: $marcador)

            // `Spacer` ocupa TODO el espacio libre y empuja a los demás hacia arriba.
            Spacer()
        }
        // El padding va DESPUÉS de los hijos: rodea a todo el VStack. Cada modificador
        // envuelve a lo que tiene delante (ver `OrdenDeModifiers`).
        .padding(16)
    }
}

// @BINDING: una vista hija que puede CAMBIAR el estado de su padre.
//
// Compose lo resuelve con STATE HOISTING: la hija recibe el valor y una lambda
//     BotonesDeGol(marcador: Marcador, onGol: (Lado) -> Unit)
// y el padre decide qué hacer con cada gol. El estado baja, los eventos suben.
//
// `@Binding` es la versión de SwiftUI de esas dos cosas (valor + setter) en una sola propiedad.
// Es MÁS CÓMODO y MENOS ESTRICTO: la hija escribe directamente en el estado del padre y el
// padre no se entera de «qué pasó», solo de «cómo quedó». Por eso:
//   - para un control genérico (un Toggle, un Stepper, un campo de texto) es lo ideal;
//   - para una acción con reglas de negocio (registrar un gol, avisar, guardar), es mejor
//     pasar una closure `alGol: (Lado) -> Void`, que es el hoisting de Compose. Lo veremos
//     cuando la lógica se mude a un ViewModel.
struct BotonesDeGol: View {
    let partido: Partido
    @Binding var marcador: Marcador

    var body: some View {
        HStack(spacing: 8) {
            // Escribir en un `@Binding` es escribir en el `@State` del padre: aquí `marcador = …`
            // cambia el `marcador` de PantallaMarcador y SwiftUI recalcula su `body`.
            Button("Gol \(partido.local.nombre)") {
                marcador = Marcador(local: marcador.local + 1, visitante: marcador.visitante)
            }
            Button("Gol \(partido.visitante.nombre)") {
                marcador = Marcador(local: marcador.local, visitante: marcador.visitante + 1)
            }
        }
        .buttonStyle(.borderedProminent)
    }
}

// La tarjeta del partido: local — marcador — visitante. Es el `TarjetaDePartido` de Compose.
// No tiene estado: solo dibuja lo que le pasan (es la parte «sin estado» del hoisting).
struct TarjetaDePartido: View {
    let partido: Partido
    let marcador: Marcador

    var body: some View {
        HStack {
            Text(partido.local.nombre)
                .font(.title3)
                // El nombre ocupa el espacio que sobra; el marcador queda centrado.
                .frame(maxWidth: .infinity, alignment: .leading)
            Text("\(marcador.local) - \(marcador.visitante)")
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
    PantallaMarcador()
}
