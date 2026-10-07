package com.sebastiannarvaez.marcador

import android.content.Context
import com.sebastiannarvaez.marcador.data.ArbitrosRepositoryDosFuentes
import com.sebastiannarvaez.marcador.data.PreferenciasDataStore
import com.sebastiannarvaez.marcador.data.red.CronicasRepositoryRed
import com.sebastiannarvaez.marcador.data.red.LigaApi
import com.sebastiannarvaez.marcador.data.red.URL_LIGA
import com.sebastiannarvaez.marcador.data.red.crearLigaApi
import com.sebastiannarvaez.marcador.data.red.crearOkHttp
import com.sebastiannarvaez.marcador.data.room.ArbitroDao
import com.sebastiannarvaez.marcador.data.room.PartidoDao
import com.sebastiannarvaez.marcador.data.room.PartidosRepositoryRoom
import com.sebastiannarvaez.marcador.data.room.crearBaseDeDatos
import com.sebastiannarvaez.marcador.domain.ArbitrosRepository
import com.sebastiannarvaez.marcador.domain.CronicasRepository
import com.sebastiannarvaez.marcador.domain.PartidosRepository
import com.sebastiannarvaez.marcador.domain.PreferenciasRepository
import com.sebastiannarvaez.marcador.domain.PublicarCronica
import com.sebastiannarvaez.marcador.domain.RegistrarGol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

// INYECCION DE DEPENDENCIAS, A MANO, POR CAPAS
//
// «Dependencia» = algo que una clase necesita para funcionar (un repositorio). La clase
// no la construye: se la DAN por el constructor (eso es inyectarla). Alguien tiene que
// construir las piezas y entregarlas: el CONTENEDOR. Cada pieza es una propiedad `lazy`
// (se crea la primera vez que se pide y se reutiliza). El contenedor vive en la
// Application, que dura lo que el proceso: cada pieza existe UNA vez en toda la app.
//
// Con red, base de datos, repositorios y casos de uso, el contenedor se ordena POR CAPAS,
// de abajo arriba; cada capa solo usa la de debajo:
//   red          -> OkHttpClient y LigaApi                 (ContenedorDeRed)
//   datos        -> la base, sus DAO y DataStore           (ContenedorDeDatos)
//   repositorios -> juntan red y datos, y traducen a dominio
//   casos de uso -> juntan repositorios con una regla
//   ViewModel    -> NO vive aqui: uno por pantalla, lo crea la fabrica (Fabricas.kt)
//
// ALCANCES: lo que esta en el contenedor dura lo que el PROCESO; un ViewModel dura lo que
// su entrada de la pila (la PANTALLA); una peticion, lo que la LLAMADA.
//
// `private` = lo que no debe salir de su capa (el cliente HTTP, la base, los DAO). Fuera
// solo se ven repositorios y casos de uso, con los nombres de siempre: Fabricas.kt no cambia.
//
// Koin o Hilt hacen lo mismo con menos codigo escrito (Hilt lo genera con KSP; Koin lo
// declara con un DSL). Hacerlo a mano una vez ensena que NO hay magia: es un `new` en un
// solo sitio. En proyectos pequenos, a mano basta.

// CAPA DE RED. UN solo OkHttpClient para toda la app: guarda las conexiones abiertas y sus
// hilos, y los reutiliza en cada llamada. Uno por peticion los desperdiciaria.
class ContenedorDeRed(url: String = URL_LIGA) {
    private val okHttp: OkHttpClient by lazy { crearOkHttp() }
    val ligaApi: LigaApi by lazy { crearLigaApi(okHttp, url) }
}

// CAPA DE DATOS LOCALES. UNA sola base: abrir dos sobre el mismo fichero es un error clasico.
class ContenedorDeDatos(contexto: Context) {
    private val baseDeDatos by lazy { crearBaseDeDatos(contexto) }
    val partidoDao: PartidoDao by lazy { baseDeDatos.partidoDao() }
    val arbitroDao: ArbitroDao by lazy { baseDeDatos.arbitroDao() }
    val preferencias: PreferenciasRepository by lazy { PreferenciasDataStore(contexto) }
}

class AppContainer(contexto: Context) {
    // Un alcance que dura lo que el proceso: para trabajo que no es de ninguna pantalla
    // (sembrar la base). Se le da un SupervisorJob para que un fallo no cancele el resto.
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val red = ContenedorDeRed()
    private val datos = ContenedorDeDatos(contexto)

    // REPOSITORIOS
    val partidosRepository: PartidosRepository by lazy { PartidosRepositoryRoom(datos.partidoDao, alcance) }
    val preferenciasRepository: PreferenciasRepository get() = datos.preferencias
    val arbitrosRepository: ArbitrosRepository by lazy { ArbitrosRepositoryDosFuentes(red.ligaApi, datos.arbitroDao) }
    val cronicasRepository: CronicasRepository by lazy { CronicasRepositoryRed(red.ligaApi) }

    // CASOS DE USO
    val registrarGol: RegistrarGol by lazy { RegistrarGol(partidosRepository) }
    val publicarCronica: PublicarCronica by lazy { PublicarCronica(partidosRepository, cronicasRepository) }
}
