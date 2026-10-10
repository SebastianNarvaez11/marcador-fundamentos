package com.sebastiannarvaez.marcador.ui

import com.sebastiannarvaez.marcador.torneo.Jugador
import com.sebastiannarvaez.marcador.torneo.dorsalDesdeTexto

// EL FORMULARIO «AÑADIR JUGADOR», COMO DATOS.
//
// Guarda lo que el usuario ESCRIBIO, tal cual (texto, aunque el dorsal sea un numero: «1»,
// «12», «12a» y «» son todos textos posibles mientras se escribe), y que campos ha TOCADO ya.
// Los errores NO se guardan: se CALCULAN cada vez a partir del texto y de la plantilla. Asi no
// pueden quedarse viejos ni contradecir lo escrito.
data class FormularioJugador(
    val nombre: String = "",
    val dorsal: String = "",
    // Tocado = el usuario ya escribio algo en el campo. Antes de eso, un error no se ensena:
    // nadie quiere ver «Escribe un nombre» en rojo nada mas abrir la pantalla.
    val nombreTocado: Boolean = false,
    val dorsalTocado: Boolean = false,
) {
    // `plantilla` llega de fuera: las reglas «no repetido» consultan lo GUARDADO.
    fun errorNombre(plantilla: List<Jugador>): ErrorNombre? {
        val limpio = nombre.trim()
        return when {
            limpio.isEmpty() -> ErrorNombre.VACIO
            limpio.length > MAXIMO_NOMBRE -> ErrorNombre.LARGO
            plantilla.any { it.nombre.equals(limpio, ignoreCase = true) } -> ErrorNombre.REPETIDO
            else -> null
        }
    }

    fun errorDorsal(plantilla: List<Jugador>): ErrorDorsal? {
        if (dorsal.isBlank()) return null // opcional: vacio es valido
        val numero = dorsalDesdeTexto(dorsal) ?: return ErrorDorsal.NO_ES_NUMERO
        return when {
            numero !in 1..99 -> ErrorDorsal.FUERA_DE_RANGO
            plantilla.any { it.dorsal == numero } -> ErrorDorsal.REPETIDO
            else -> null
        }
    }

    fun esValido(plantilla: List<Jugador>): Boolean = errorNombre(plantilla) == null && errorDorsal(plantilla) == null

    // Solo tiene sentido si es valido: el nombre sin espacios de sobra y el dorsal como numero (o null).
    fun aJugador(): Jugador = Jugador(nombre.trim(), dorsalDesdeTexto(dorsal))

    companion object {
        const val MAXIMO_NOMBRE = 25
    }
}

// Un `enum` por campo: cada caso es UN motivo. La pantalla elige el texto de cada uno (y podria
// traducirlo); las reglas no saben nada de textos.
enum class ErrorNombre { VACIO, LARGO, REPETIDO }

enum class ErrorDorsal { NO_ES_NUMERO, FUERA_DE_RANGO, REPETIDO }
