import SwiftUI

// La raíz de la app. `TabView` es la barra de pestañas de iOS.
struct ContentView: View {
    var body: some View {
        TabView {
            Tab("Partidos", systemImage: "list.bullet") {
                ListaDePartidosView()
            }
            Tab("Marcador", systemImage: "sportscourt") {
                PantallaMarcador()
                    .barraDelPartidoEnJuego()
            }
            Tab("Laboratorio", systemImage: "flask") {
                LaboratorioView()
                    .barraDelPartidoEnJuego()
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
