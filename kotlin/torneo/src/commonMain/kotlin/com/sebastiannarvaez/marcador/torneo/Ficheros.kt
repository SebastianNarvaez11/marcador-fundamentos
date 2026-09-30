package com.sebastiannarvaez.marcador.torneo

// Lo unico que :torneo le pide al disco: leer y escribir un texto entero.
// `expect` dice QUE hace falta; cada plataforma escribe su `actual` con SU manera de
// hacerlo (java.io.File en la JVM y en Android, NSString en iOS).
// Es la unica pieza del modulo que no se puede compartir tal cual, porque
// java.io no existe en iOS.
expect fun leerTexto(ruta: String): String

expect fun escribirTexto(ruta: String, texto: String)
