import SwiftUI
import Torneo

// LISTAS E IDENTIDAD (gemelo de LazyColumn con `key`)
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
// NAVEGACIÓN POR VALOR (gemelo de Navigation 3)
//
// `NavigationStack` es la pila de pantallas de iOS. Cada fila es un `NavigationLink(value:)`: al
// tocarla se EMPUJA un valor (`Destino`) a la pila, y `.navigationDestination(for:)` traduce cada
// valor en una pantalla. La barra de arriba con el botón «atrás» la pone SwiftUI. Sustituye a las
// hojas modales de antes, que no eran navegación (tapaban la lista, no la «apilaban»).
struct ListaDePartidosView: View {
    // La lista LEE los partidos del repositorio (`@Observable`): al registrar un gol, la fila cambia sola.
    @Environment(RepositorioDePartidos.self) private var repositorio
    // El contenedor fabrica los ViewModels de las pantallas que se apilan.
    @Environment(ContenedorDeLaApp.self) private var contenedor

    // La pila entera es un array de destinos, en un `@State`: se puede inspeccionar y manipular.
    @State private var ruta: [Destino] = []

    var body: some View {
        NavigationStack(path: $ruta) {
            List(repositorio.partidos) { item in
                NavigationLink(value: Destino.partido(item.id)) {
                    FilaDePartido(
                        local: item.partido.local.nombre,
                        visitante: item.partido.visitante.nombre,
                        resultado: item.partido.marcador().description
                    )
                }
                .accessibilityIdentifier("partido-\(item.id)")
            }
            .listStyle(.plain)
            .navigationTitle("Partidos")
            // Un botón en la barra de arriba que empuja la pantalla de árbitros.
            .toolbar {
                NavigationLink("Árbitros", value: Destino.arbitros)
            }
            // UN solo sitio decide qué pantalla corresponde a cada destino.
            .navigationDestination(for: Destino.self) { destino in
                switch destino {
                case let .partido(id):
                    PartidoContenedor(id: id)
                case .goleadores:
                    GoleadoresView()
                case .arbitros:
                    ArbitrosView(viewModel: contenedor.arbitrosViewModel())
                case let .cronica(id):
                    CronicaView(viewModel: contenedor.cronicaViewModel(id: id))
                }
            }
        }
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
            Text(resultado).bold().monospacedDigit().accessibilityIdentifier("resultado")
            Text(visitante).frame(maxWidth: .infinity, alignment: .trailing)
        }
        // Que toda la fila (no solo el texto) sea tocable. Sin esto, tocar el hueco entre textos no cuenta.
        .contentShape(Rectangle())
    }
}

#Preview {
    // Un contenedor sin almacenes: la vista previa no lee ni escribe en el disco.
    let contenedor = ContenedorDeLaApp(servicio: ServicioJSONPlaceholder(), almacenDelTorneo: nil, almacenDeArbitros: nil)
    ListaDePartidosView()
        .environment(contenedor)
        .environment(contenedor.partidos)
        .environment(contenedor.notificador)   // lo lee el partido al navegar
}
