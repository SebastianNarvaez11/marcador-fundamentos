// Datos de ejemplo para la consola y las pruebas de F7: equipos, jugadores y dos
// «guiones» de partido (la lista de cosas que pasan y en qué minuto). Son los
// mismos datos que `Ejemplo.kt`. `enum` sin casos = un espacio de nombres que
// no se puede instanciar (en Kotlin, `object Ejemplo`).
public enum Ejemplo {
    public static let ana = Jugador(nombre: "Ana", dorsal: 9)
    public static let luis = Jugador(nombre: "Luis")
    public static let marta = Jugador(nombre: "Marta", dorsal: 1)
    public static let ivan = Jugador(nombre: "Iván", dorsal: 7)
    public static let sofia = Jugador(nombre: "Sofía", dorsal: 10)
    public static let pedro = Jugador(nombre: "Pedro", dorsal: 4)
    public static let carla = Jugador(nombre: "Carla", dorsal: 5)
    public static let nico = Jugador(nombre: "Nico", dorsal: 11)

    // `!` aquí es razonable: son datos escritos a mano y conocidos, y si estuvieran
    // mal se vería al primer arranque.
    public static let rayo = Equipo(nombre: "Rayo FC", plantilla: [ana, luis, marta])!
    public static let toros = Equipo(nombre: "Toros", plantilla: [ivan, sofia])!
    public static let lobos = Equipo(nombre: "Lobos", plantilla: [pedro, carla])!
    public static let aguilas = Equipo(nombre: "Águilas", plantilla: [nico])!

    public static let rayoContraToros = Partido(
        local: rayo,
        visitante: toros,
        eventos: [
            .gol(minuto: 12, jugador: ana, equipo: rayo),
            .tarjeta(minuto: 30, jugador: ivan, color: .amarilla),
            .gol(minuto: 55, jugador: ivan, equipo: toros),
            .gol(minuto: 80, jugador: ana, equipo: rayo),
            .cambio(minuto: 85, sale: ana, entra: luis),
        ]
    )

    public static let lobosContraAguilas = Partido(
        local: lobos,
        visitante: aguilas,
        eventos: [
            .gol(minuto: 20, jugador: pedro, equipo: lobos),
            .gol(minuto: 33, jugador: nico, equipo: aguilas),
            .tarjeta(minuto: 60, jugador: carla, color: .roja),
            .gol(minuto: 70, jugador: nico, equipo: aguilas),
        ]
    )

    // Un torneo nuevo cada vez (el Torneo es mutable) con los dos partidos ya jugados.
    public static func torneoConPartidos() -> Torneo {
        let torneo = Torneo(nombre: "Copa Barrio", equipos: [rayo, toros, lobos, aguilas])
        try! torneo.registrar(rayoContraToros)
        try! torneo.registrar(lobosContraAguilas)
        return torneo
    }
}
