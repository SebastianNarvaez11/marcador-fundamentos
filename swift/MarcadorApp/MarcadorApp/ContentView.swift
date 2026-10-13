import SwiftUI

// Las pestañas de la app, como DATOS: así la raíz puede cambiar de pestaña por código.
enum Pestana: Hashable {
    case partidos, marcador, laboratorio
}

// La raíz de la app. `TabView` es la barra de pestañas de iOS.
//
// La pestaña elegida y la pila de Partidos SUBEN aquí (antes la pila era un `@State` de la lista): la
// barra del partido en juego, que sale en todas las pestañas, necesita cambiarlas. La lista recibe la
// pila con `@Binding`.
struct ContentView: View {
    @State private var pestana: Pestana = .partidos
    @State private var ruta: [Destino] = []

    var body: some View {
        TabView(selection: $pestana) {
            Tab("Partidos", systemImage: "list.bullet", value: .partidos) {
                ListaDePartidosView(ruta: $ruta)
                    .barraDelPartidoEnJuego(pantallaActual: ruta.last)
            }
            Tab("Marcador", systemImage: "sportscourt", value: .marcador) {
                PantallaMarcador()
                    .barraDelPartidoEnJuego()
            }
            Tab("Laboratorio", systemImage: "flask", value: .laboratorio) {
                LaboratorioView()
                    .barraDelPartidoEnJuego()
            }
        }
        // Abrir un partido desde cualquier sitio: a la pestaña Partidos y, si ya está en la pila, se
        // vuelve hasta él; si no, se apila.
        .environment(\.abrirPartido) { id in
            pestana = .partidos
            if let donde = ruta.lastIndex(of: .partido(id)) {
                ruta.removeSubrange(ruta.index(after: donde)...)
            } else {
                ruta.append(.partido(id))
            }
        }
    }
}

#Preview {
    // La lista y el partido leen el contenedor, el repositorio, el notificador y el partido en juego del
    // entorno: aquí se los da la vista previa, con un contenedor sin almacenes (no toca el disco).
    let contenedor = ContenedorDeLaApp(servicio: ServicioJSONPlaceholder(), almacenDelTorneo: nil, almacenDeArbitros: nil)
    ContentView()
        .environment(contenedor)
        .environment(contenedor.partidos)
        .environment(contenedor.notificador)
        .environment(contenedor.partidoEnJuego)
}
