import Foundation

// f77 · Los ajustes del usuario ya no son un objeto en memoria (`AjustesModelo`, f72-f76): la
// «duración del partido» se guarda en `UserDefaults` con `@AppStorage` (mira `PartidoEnDirectoView`)
// y sobrevive a cerrar la app. Aquí solo quedan la clave y los valores posibles.
enum Ajustes {
    // La clave con la que se guarda en `UserDefaults`: escrita UNA vez, no repartida en cadenas sueltas.
    static let claveDuracion = "duracionDelPartido"
    static let duraciones = [90, 60]
    static let duracionPorDefecto = 90
}
