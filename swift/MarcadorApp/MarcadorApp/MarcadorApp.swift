import SwiftUI

// `@main` marca el punto de entrada: es el `Application` de Android (`MarcadorApplication`)
// y la `MainActivity` juntos. Un `App` no dibuja nada por sí mismo: declara ESCENAS.
//
// El asistente de Xcode lo llama `MarcadorAppApp` (nombre del proyecto + «App»).
// Aquí se renombra a `MarcadorApp` porque el módulo ya se llama así.
@main
struct MarcadorApp: App {
    // `@Environment(\.scenePhase)` lee en qué fase está la ESCENA: `.active` (delante y
    // recibiendo toques), `.inactive` (visible pero sin interacción: llamada, selector de
    // apps) y `.background` (fuera de la vista). Es el `onStart/onStop` de Android, pero de
    // la escena y no de una pantalla.
    @Environment(\.scenePhase) private var fase

    // EL CONTENEDOR, creado UNA vez al arrancar: crea el servicio, los repositorios, el caso de uso y
    // el notificador, y se reparte a las pantallas por el entorno.
    // Con el torneo guardado en `Documents/torneo.json` y los árbitros en `Documents/arbitros.json`
    // (`-reiniciarDatos YES` borra los dos al arrancar).
    @State private var contenedor: ContenedorDeLaApp = {
        if Configuracion.reiniciarDatos {
            AlmacenDelTorneo.predeterminado.borrar()
            AlmacenDeArbitros.predeterminado.borrar()
            UserDefaults.standard.removeObject(forKey: Ajustes.claveDuracion)
        }
        return ContenedorDeLaApp(
            servicio: ServicioJSONPlaceholder(),
            almacenDelTorneo: .predeterminado,
            almacenDeArbitros: .predeterminado
        )
    }()

    var body: some Scene {
        // `WindowGroup` es la escena normal de una app de iOS: una ventana con esta vista raíz.
        WindowGroup {
            ContentView()
                .environment(contenedor)
                // Las vistas que ya leían el repositorio de partidos o el notificador los siguen
                // encontrando en el entorno: son las mismas piezas, sacadas del contenedor.
                .environment(contenedor.partidos)
                .environment(contenedor.notificador)
        }
        // `onChange(of:)` con dos parámetros (antiguo, nuevo) es la forma de iOS 17+.
        .onChange(of: fase) { antigua, nueva in
            Registro.anotar("scenePhase: \(antigua) -> \(nueva)")
        }
    }
}
