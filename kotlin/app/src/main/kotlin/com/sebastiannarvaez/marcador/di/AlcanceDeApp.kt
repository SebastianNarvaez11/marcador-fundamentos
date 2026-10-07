package com.sebastiannarvaez.marcador.di

import javax.inject.Qualifier

// CUALIFICADOR: una etiqueta para distinguir dos piezas del MISMO tipo. Hilt busca por
// tipo; si un dia hubiera dos CoroutineScope, no sabria cual dar. Con la etiqueta, el que
// lo pide dice cual: `@AlcanceDeApp alcance: CoroutineScope`.
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AlcanceDeApp
