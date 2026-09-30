package com.sebastiannarvaez.marcador.torneo

// Una interfaz dice QUÉ se puede hacer, sin decir cómo. Quien la implemente
// rellena lo que falta.
interface Reglamento {
    val puntosPorVictoria: Int
    val puntosPorEmpate: Int

    // Propiedad con implementación por defecto: no hace falta sobrescribirla.
    val puntosPorDerrota: Int
        get() = 0

    // Método con implementación por defecto, escrito sobre las propiedades.
    fun puntosPor(golesAFavor: Int, golesEnContra: Int): Int = when {
        golesAFavor > golesEnContra -> puntosPorVictoria
        golesAFavor == golesEnContra -> puntosPorEmpate
        else -> puntosPorDerrota
    }
}

// Clase abstracta: no se puede instanciar, y sí lleva estado (los puntos) y un
// miembro por rellenar (`nombre`). Una clase solo hereda de UNA clase, pero
// puede implementar muchas interfaces.
abstract class ReglamentoPorPuntos(
    override val puntosPorVictoria: Int,
    override val puntosPorEmpate: Int,
) : Reglamento {
    abstract val nombre: String

    override fun toString(): String = "Reglamento $nombre ($puntosPorVictoria/$puntosPorEmpate/$puntosPorDerrota)"
}

// `object`: una clase con UNA sola instancia, creada al primer uso (singleton).
// Se usa por su nombre, sin paréntesis: ReglamentoLiga.puntosPor(2, 1)
object ReglamentoLiga : ReglamentoPorPuntos(puntosPorVictoria = 3, puntosPorEmpate = 1) {
    override val nombre: String = "Liga"
}
