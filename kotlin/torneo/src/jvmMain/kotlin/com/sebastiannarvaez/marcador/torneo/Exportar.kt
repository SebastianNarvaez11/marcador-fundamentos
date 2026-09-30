package com.sebastiannarvaez.marcador.torneo

import java.io.File

// Escribe la tabla en un fichero de texto.
// `use` cierra el escritor SIEMPRE, también si `write` lanza una excepción
// (es el try/finally de siempre, en una línea). Todo va dentro de `runCatching`
// para que el fallo (carpeta inexistente, disco lleno…) llegue como Result.
fun Torneo.exportarTabla(destino: File): Result<Unit> = runCatching {
    destino.bufferedWriter().use { escritor ->
        escritor.write(tablaComoTexto())
        escritor.newLine()
    }
}
