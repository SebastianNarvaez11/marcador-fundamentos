package com.sebastiannarvaez.marcador.torneo

// Escribe la tabla en un fichero de texto. Antes usaba `bufferedWriter().use { ... }`
// de java.io; ahora pasa por `escribirTexto`, que cierra el fichero por dentro.
// Todo va dentro de `runCatching` para que el fallo (carpeta inexistente, disco
// lleno...) llegue como Result.
fun Torneo.exportarTabla(ruta: String): Result<Unit> = runCatching {
    escribirTexto(ruta, tablaComoTexto() + "\n")
}
