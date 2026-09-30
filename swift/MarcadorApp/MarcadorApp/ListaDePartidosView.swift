import SwiftUI
import Torneo

// f71 · LISTAS E IDENTIDAD (gemelo de f35, LazyColumn con `key`)
//
// `List` es la lista de iOS: filas con separadores, se desplaza y RECICLA las filas que no se
// ven (como LazyColumn: solo se crean las visibles). `ForEach` es la pieza que convierte una
// colección en vistas; se puede usar dentro de `List`, de un `VStack`, de lo que sea.
//
// IDENTIDAD. SwiftUI necesita saber «qué fila es cuál» entre una pasada y la siguiente, para
// animar los cambios y para no perder el estado de cada fila. De dónde sale esa identidad:
//   - EXPLÍCITA: la das tú (`id: \.id`, o un tipo `Identifiable`). Sigue al DATO: si la fila
//     se mueve, su estado se va con ella. Es la `key` de LazyColumn.
//   - ESTRUCTURAL: si no hay id, SwiftUI usa la POSICIÓN en el código (la vista que va
//     primero, la segunda…). Sirve para vistas fijas (`VStack { A; B }`) y falla en listas que
//     cambian: el estado se queda pegado a la posición. Es lo que pasa en Compose sin `key`.
struct ListaDePartidosView: View {
    let partidos: [PartidoDeLista]

    var body: some View {
        // `PartidoDeLista` es `Identifiable`, así que basta pasarle la colección.
        // Equivale a `List(partidos, id: \.id)`.
        List(partidos) { item in
            FilaDePartido(
                local: item.partido.local.nombre,
                visitante: item.partido.visitante.nombre,
                resultado: item.partido.marcador().description
            )
            .accessibilityIdentifier("partido-\(item.id)")
        }
        .listStyle(.plain)
    }
}

// La fila: local a la izquierda, resultado en el centro, visitante a la derecha.
struct FilaDePartido: View {
    let local: String
    let visitante: String
    let resultado: String

    var body: some View {
        HStack {
            Text(local).frame(maxWidth: .infinity, alignment: .leading)
            Text(resultado).bold().monospacedDigit()
            Text(visitante).frame(maxWidth: .infinity, alignment: .trailing)
        }
        // Que toda la fila (no solo el texto) sea tocable. Sin esto, tocar el hueco entre textos no cuenta.
        .contentShape(Rectangle())
    }
}

#Preview {
    ListaDePartidosView(partidos: DatosDeEjemplo.partidos)
}
