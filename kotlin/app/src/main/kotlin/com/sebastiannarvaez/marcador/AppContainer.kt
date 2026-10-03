package com.sebastiannarvaez.marcador

import android.content.Context
import com.sebastiannarvaez.marcador.data.PreferenciasDataStore
import com.sebastiannarvaez.marcador.data.room.PartidosRepositoryRoom
import com.sebastiannarvaez.marcador.data.room.crearBaseDeDatos
import com.sebastiannarvaez.marcador.domain.PartidosRepository
import com.sebastiannarvaez.marcador.domain.PreferenciasRepository
import com.sebastiannarvaez.marcador.domain.RegistrarGol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

// INYECCION DE DEPENDENCIAS, A MANO
//
// «Dependencia» = algo que una clase necesita para funcionar (un repositorio). Hay dos
// maneras de conseguirla:
//   - ir a buscarla uno mismo (`Repositorios.partidos`, o `PartidosRepositoryEnMemoria()`
//     dentro del ViewModel): la clase queda atada a UNA implementacion concreta y no se
//     puede probar con otra;
//   - que se la DEN de fuera, por el constructor: eso es inyectarla. La clase pide
//     `PartidosRepository` (la interfaz) y no sabe ni le importa cual le llega.
//
// Alguien tiene que construir las piezas y entregarlas: el CONTENEDOR. Este es el mas
// simple posible: una clase con las dependencias como propiedades `lazy` (se crean la
// primera vez que se piden y se reutilizan: singletons). Vive en la Application, que dura
// lo que el proceso, asi que el repositorio es el mismo en todas las pantallas.
//
// Koin o Hilt hacen lo mismo con menos codigo escrito (Hilt lo genera con KSP; Koin lo
// declara con un DSL). Hacerlo a mano una vez ensena que NO hay magia: es un `new` en un
// solo sitio. En proyectos pequenos, a mano basta.
class AppContainer(private val contexto: Context) {
    // Un alcance que dura lo que el proceso: para trabajo que no es de ninguna pantalla
    // (sembrar la base). Se le da un SupervisorJob para que un fallo no cancele el resto.
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // La base de datos se abre la primera vez que alguien la pide. UNA sola instancia:
    // abrir dos bases sobre el mismo fichero es un error clasico.
    private val baseDeDatos by lazy { crearBaseDeDatos(contexto) }

    // Antes `PartidosRepositoryEnMemoria()`. Cambia UNA linea y nada mas.
    val partidosRepository: PartidosRepository by lazy { PartidosRepositoryRoom(baseDeDatos.partidoDao(), alcance) }

    // Los ajustes, en DataStore.
    val preferenciasRepository: PreferenciasRepository by lazy { PreferenciasDataStore(contexto) }

    val registrarGol: RegistrarGol by lazy { RegistrarGol(partidosRepository) }
}
