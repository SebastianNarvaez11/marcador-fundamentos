package com.sebastiannarvaez.marcador

import com.sebastiannarvaez.marcador.torneo.Ejemplo
import com.sebastiannarvaez.marcador.torneo.Partido
import com.sebastiannarvaez.marcador.torneo.marcador

// Los partidos que la app conoce, por numero. Un enlace `marcador://partido/2`
// llega con ese 2 como texto.
val partidosDeEjemplo: Map<Int, Partido> = mapOf(
    1 to Ejemplo.rayoContraToros,
    2 to Ejemplo.lobosContraAguilas,
)

// «Rayo FC 2-1 Toros»: el texto que se muestra y se comparte.
fun Partido.resumen(): String = "${local.nombre} ${marcador()} ${visitante.nombre}"
