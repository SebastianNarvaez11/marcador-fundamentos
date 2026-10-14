import Foundation
import Observation
import UserNotifications

// NOTIFICACIÓN «¡GOL!» (gemelo de `Notificaciones.kt`)
//
// El ViewModel no conoce `UserNotifications`: habla con este protocolo, y en las pruebas
// se sustituye por un espía que anota lo que le piden. Es la misma idea que el repositorio.
protocol Notificador: AnyObject {
    var permitido: Bool { get }
    func notificarGol(minuto: Int, jugador: String, equipo: String)
}

// La implementación de verdad, sobre `UNUserNotificationCenter`.
//
// PERMISOS. En iOS TODA notificación (local o remota) necesita permiso del usuario, y el sistema
// pregunta UNA sola vez: si dice que no, `requestAuthorization` ya no vuelve a mostrar el diálogo y
// solo se puede llevar al usuario a Ajustes. (En Android 13+ el permiso es POST_NOTIFICATIONS, que
// además se declara en el manifiesto; en iOS NO hay que declarar nada en Info.plist ni en
// capabilities para notificaciones LOCALES: solo la petición en tiempo de ejecución.)
//
// Las remotas (push) sí piden capability «Push Notifications» (un `entitlements` con `aps-environment`)
// y una cuenta de desarrollador de pago: fuera de lo que hace esta app.
//
// `@Observable`: la pantalla lee `permitido` y se redibuja cuando cambia. `NSObject` porque el
// delegado de UserNotifications es un protocolo de Objective-C.
@Observable
final class NotificadorDeSistema: NSObject, Notificador, UNUserNotificationCenterDelegate {
    private(set) var permitido = false

    @ObservationIgnored private let centro = UNUserNotificationCenter.current()

    override init() {
        super.init()
        // El delegado decide qué hacer con una notificación que llega CON LA APP DELANTE.
        centro.delegate = self
    }

    // Pregunta al sistema cómo está el permiso (sin mostrar nada al usuario).
    func actualizarPermiso() async {
        let ajustes = await centro.notificationSettings()
        permitido = ajustes.authorizationStatus == .authorized || ajustes.authorizationStatus == .provisional
    }

    // Muestra el diálogo del sistema (solo la primera vez). Es `async`: la respuesta llega después.
    func pedirPermiso() async {
        do {
            permitido = try await centro.requestAuthorization(options: [.alert, .sound])
        } catch {
            Registro.anotar("no se pudo pedir el permiso de notificaciones: \(error)")
        }
        Registro.anotar("permiso de notificaciones: \(permitido ? "concedido" : "denegado")")
    }

    func notificarGol(minuto: Int, jugador: String, equipo: String) {
        guard permitido else { return }
        let contenido = UNMutableNotificationContent()
        contenido.title = "¡Gol!"
        contenido.body = "\(jugador) (\(equipo)), minuto \(minuto)"
        contenido.sound = .default
        // El identificador distingue notificaciones: con el mismo id, la nueva sustituye a la anterior.
        // Lleva el minuto y el jugador para que dos goles del mismo minuto no se pisen.
        // `trigger: nil` = entregarla ya (también existen los disparadores por hora o por lugar).
        let peticion = UNNotificationRequest(identifier: "gol-\(minuto)-\(jugador)", content: contenido, trigger: nil)
        centro.add(peticion) { error in
            if let error { Registro.anotar("no se pudo publicar la notificación: \(error)") }
        }
        Registro.anotar("notificación «¡Gol!» enviada: \(jugador), minuto \(minuto)")
    }

    // Por defecto, iOS NO enseña una notificación si la app está delante. Con este método sí: banner y sonido.
    // `nonisolated`: lo llama el sistema desde cualquier hilo, y no toca estado de la clase.
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        // `-sinAvisosEnPrimerPlano YES` (pruebas de UI): ni banner ni sonido.
        Configuracion.sinAvisosEnPrimerPlano ? [] : [.banner, .sound]
    }
}
