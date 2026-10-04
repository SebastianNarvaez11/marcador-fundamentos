// LOS DESTINOS DE LA NAVEGACIÓN, como DATOS (gemelo de las `NavKey` de Navigation 3).
//
// En vez de decir «abre esta vista», se dice «ve a este VALOR» y una sola función
// (`navigationDestination(for:)`) sabe qué pantalla toca. `Hashable` es obligatorio: la pila guarda
// los valores y SwiftUI los compara para saber qué cambió.
//
// Se puede llevar la pila entera en un `[Destino]` (o un `NavigationPath`) y manipularla como un array:
// volver a la raíz es vaciarlo. Con `NavigationLink(destination: Vista())` (la forma antigua) la vista
// destino se construye al pintar la fila, aunque nadie la abra, y no hay pila que manejar.
enum Destino: Hashable {
    case partido(Int)   // el id del partido (viaja en la clave, en un solo sitio)
    case goleadores
}
