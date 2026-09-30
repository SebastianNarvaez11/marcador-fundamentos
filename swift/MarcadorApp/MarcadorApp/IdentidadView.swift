import SwiftUI

// f71 · LA MISMA LISTA, DOS IDENTIDADES
//
// Cada fila tiene su propio `@State` (un «visto»). Se marca la primera fila de cada lista y se
// pulsa «Insertar arriba», que mete un nombre nuevo al principio.
//
//   A) `ForEach(indices)`: la identidad es la POSICIÓN (0, 1, 2…). Tras insertar, la posición 0
//      sigue siendo «la misma fila»: conserva la marca, pero ahora enseña otro nombre.
//   B) `ForEach(nombres, id: \.self)`: la identidad es el DATO (el nombre). Tras insertar, la
//      marca sigue con el nombre al que se puso.
struct IdentidadView: View {
    @State private var nombres = ["Ana", "Luis", "Marta"]
    @State private var contador = 0

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button("Insertar arriba") {
                contador += 1
                nombres.insert("Nuevo\(contador)", at: 0)
            }
            .buttonStyle(.bordered)

            Text("A · identidad por posición").font(.subheadline.bold())
            // OJO: `id: \.self` sobre los ÍNDICES: el índice 0 es siempre «la fila 0».
            ForEach(nombres.indices, id: \.self) { indice in
                FilaConMarca(etiqueta: "A \(nombres[indice])")
            }

            Text("B · identidad por dato").font(.subheadline.bold())
            // `String` es `Hashable`: `\.self` usa el propio texto como id (vale si no se repiten).
            ForEach(nombres, id: \.self) { nombre in
                FilaConMarca(etiqueta: "B \(nombre)")
            }
        }
    }
}

// Una fila con estado propio. La etiqueta llega de fuera; la marca vive aquí.
private struct FilaConMarca: View {
    let etiqueta: String
    @State private var visto = false

    var body: some View {
        Toggle(etiqueta, isOn: $visto)
    }
}

#Preview {
    IdentidadView().padding()
}
