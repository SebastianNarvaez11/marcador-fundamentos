import SwiftUI

// La raíz de la app. `TabView` es la barra de pestañas de iOS.
struct ContentView: View {
    var body: some View {
        TabView {
            Tab("Marcador", systemImage: "sportscourt") {
                PantallaMarcador()
            }
            Tab("Laboratorio", systemImage: "flask") {
                LaboratorioView()
            }
        }
    }
}

#Preview {
    ContentView()
}
