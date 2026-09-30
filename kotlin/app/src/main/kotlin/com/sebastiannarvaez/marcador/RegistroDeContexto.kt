package com.sebastiannarvaez.marcador

import android.content.Context

// Un singleton (`object`): existe mientras viva el proceso. Cualquier cosa que
// guarde en sus propiedades sigue viva con el.
object RegistroDeContexto {
    // Guarda el Context «para usarlo luego». Es la trampa clasica.
    var contexto: Context? = null
        private set

    // EL BUG: si `nuevo` es una Activity, el singleton la mantiene en memoria
    // despues de que Android la destruya (al rotar, por ejemplo), con toda su
    // jerarquia de vistas. Cada rotacion deja otra copia. Eso es una fuga.
    fun guardarSinCuidado(nuevo: Context) {
        contexto = nuevo
    }

    // EL ARREGLO: `applicationContext` es la instancia de MarcadorApplication, que
    // dura lo que el proceso, asi que no retiene ninguna pantalla.
    fun guardarConCuidado(nuevo: Context) {
        contexto = nuevo.applicationContext
    }
}
