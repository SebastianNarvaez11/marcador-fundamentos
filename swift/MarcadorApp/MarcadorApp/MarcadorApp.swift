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
    // apps) y `.background` (fuera de la vista). Es el `onStart/onStop` de F3, pero de
    // la escena y no de una pantalla.
    @Environment(\.scenePhase) private var fase

    // f75: el repositorio, creado una vez y repartido a las pantallas por el entorno.
    // f77: con el torneo guardado en `Documents/torneo.json` (`-reiniciarDatos YES` lo borra al arrancar).
    @State private var repositorio: RepositorioDePartidos = {
        if Configuracion.reiniciarDatos {
            AlmacenDelTorneo.predeterminado.borrar()
            UserDefaults.standard.removeObject(forKey: Ajustes.claveDuracion)
        }
        return RepositorioDePartidos(almacen: .predeterminado)
    }()

    // f78: el notificador se crea UNA vez al arrancar la app (registra su delegado en el centro de notificaciones)
    // y se reparte por el entorno.
    @State private var notificador = NotificadorDeSistema()

    var body: some Scene {
        // `WindowGroup` es la escena normal de una app de iOS: una ventana con esta vista raíz.
        WindowGroup {
            ContentView()
                .environment(repositorio)
                .environment(notificador)
        }
        // `onChange(of:)` con dos parámetros (antiguo, nuevo) es la forma de iOS 17+.
        .onChange(of: fase) { antigua, nueva in
            Registro.anotar("scenePhase: \(antigua) -> \(nueva)")
        }
    }
}
