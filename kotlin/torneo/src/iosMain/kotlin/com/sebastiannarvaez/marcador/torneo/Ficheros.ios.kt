package com.sebastiannarvaez.marcador.torneo

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

// En iOS no hay java.io: se llama a Foundation (el mismo NSString que usaria Swift).
// Los metodos de Objective-C no lanzan: devuelven null/false y rellenan un NSError.
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun leerTexto(ruta: String): String = memScoped {
    val error = alloc<ObjCObjectVar<NSError?>>()
    NSString.stringWithContentsOfFile(ruta, NSUTF8StringEncoding, error.ptr)
        ?: throw IllegalStateException("No se pudo leer $ruta: ${error.value?.localizedDescription}")
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun escribirTexto(ruta: String, texto: String) {
    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        @Suppress("CAST_NEVER_SUCCEEDS")
        val escrito = (texto as NSString).writeToFile(ruta, atomically = true, encoding = NSUTF8StringEncoding, error = error.ptr)
        if (!escrito) throw IllegalStateException("No se pudo escribir $ruta: ${error.value?.localizedDescription}")
    }
}
