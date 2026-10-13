package com.sebastiannarvaez.marcador.core

import com.sebastiannarvaez.marcador.core.database.ArbitroDao
import com.sebastiannarvaez.marcador.core.database.ArbitroEntity
import com.sebastiannarvaez.marcador.core.network.ArbitroDto
import com.sebastiannarvaez.marcador.core.network.CronicaDto
import com.sebastiannarvaez.marcador.core.network.CronicaPublicadaDto
import com.sebastiannarvaez.marcador.core.network.DireccionDto
import com.sebastiannarvaez.marcador.core.network.LigaApi
import com.sebastiannarvaez.marcador.feature.arbitros.domain.Arbitro
import com.sebastiannarvaez.marcador.feature.arbitros.domain.ArbitrosRepository
import com.sebastiannarvaez.marcador.feature.cronica.domain.CronicasRepository
import com.sebastiannarvaez.marcador.feature.marcador.domain.PreferenciasRepository
import com.sebastiannarvaez.marcador.feature.partidos.domain.Goleador
import com.sebastiannarvaez.marcador.feature.partidos.domain.PartidoDeLista
import com.sebastiannarvaez.marcador.feature.partidos.domain.PartidosRepository
import com.sebastiannarvaez.marcador.torneo.Ejemplo
import com.sebastiannarvaez.marcador.torneo.Gol
import com.sebastiannarvaez.marcador.torneo.Jugador
import com.sebastiannarvaez.marcador.torneo.Partido
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

// FAKES frente a MOCKS
//
// Un MOCK (Mockito, MockK) es un objeto que finge ser la interfaz y al que se le PROGRAMA
// cada respuesta (`every { repo.partido(1) } returns ...`) y se le PREGUNTA despues
// (`verify { repo.registrarGol(...) }`). El test queda atado a COMO el codigo llama al colaborador:
// si se refactoriza sin cambiar el comportamiento, el test se rompe.
//
// Un FAKE es una implementacion REAL pero simple (una lista en memoria) de la misma interfaz.
// El test comprueba el RESULTADO (que el gol quedo guardado), no las llamadas. Aguanta refactors,
// se reutiliza en todos los tests y es exactamente lo que ya escribimos para la app. Por eso
// la interfaz del repositorio y la inyeccion son lo que hace esto posible.
class FakePartidosRepository(
    partidos: List<PartidoDeLista> = listOf(PartidoDeLista(1, Partido(Ejemplo.rayo, Ejemplo.toros))),
    // Lo que tarda en guardar un gol (Room tambien tarda): para probar que no se pierde a medias.
    private val retrasoAlGuardar: Long = 0,
) : PartidosRepository {
    private val lista = MutableStateFlow(partidos)

    // Las plantillas, por nombre de equipo: hacen de tabla `jugador`.
    private val plantillas = MutableStateFlow(listOf(Ejemplo.rayo, Ejemplo.toros).associate { it.nombre to it.plantilla })

    override fun observarPartidos(): Flow<List<PartidoDeLista>> = lista

    override suspend fun partido(id: Int): Partido? = lista.value.firstOrNull { it.id == id }?.partido

    override suspend fun registrarGol(partidoId: Int, gol: Gol) {
        delay(retrasoAlGuardar)
        lista.value = lista.value.map { if (it.id == partidoId) it.copy(partido = it.partido.registrar(gol)) else it }
    }

    override fun observarGoleadores(): Flow<List<Goleador>> = lista.map { filas ->
        filas.flatMap { it.partido.eventos.filterIsInstance<Gol>() }
            .groupingBy { it.jugador.nombre }.eachCount().map { Goleador(it.key, it.value) }
    }

    override fun observarPlantilla(equipo: String): Flow<List<Jugador>> = plantillas.map { it[equipo].orEmpty() }

    override suspend fun anadirJugador(equipo: String, jugador: Jugador) {
        plantillas.value = plantillas.value + (equipo to plantillas.value[equipo].orEmpty() + jugador)
    }
}

// Un guion CON goles: local al 15, visitante al 30 y local al 60. Los avisos caen cada 15
// minutos, asi que el del 15 y el del 30 coinciden con un gol: justo el caso que rompia un
// segundo reloj para los goles (el aviso salia con el marcador de antes). Un guion sin goles
// no puede ver ese fallo.
fun partidoConGoles() = Partido(
    Ejemplo.rayo, Ejemplo.toros,
    eventos = listOf(
        Gol(15, Ejemplo.rayo.plantilla[0], Ejemplo.rayo),
        Gol(30, Ejemplo.toros.plantilla[0], Ejemplo.toros),
        Gol(60, Ejemplo.rayo.plantilla[0], Ejemplo.rayo),
    ),
)

class FakePreferencias(inicial: Int = 90) : PreferenciasRepository {
    val duracion = MutableStateFlow(inicial)
    override val duracionDelPartido: Flow<Int> = duracion
    override suspend fun cambiarDuracion(minutos: Int) {
        duracion.value = minutos
    }
}

// Las dos fuentes, de mentira: `guardados` hace de base y `deLaRed` de servidor.
// `refrescar` copia la red en la base, o falla con lo que el test le ponga.
class FakeArbitrosRepository(
    guardados: List<Arbitro> = emptyList(),
    var deLaRed: List<Arbitro> = listOf(Arbitro(1, "Leanne Graham", "Gwenborough")),
    var fallo: Exception? = null,
) : ArbitrosRepository {
    private val base = MutableStateFlow(guardados)

    override fun observarArbitros(): Flow<List<Arbitro>> = base

    override suspend fun refrescar(): Result<Unit> {
        fallo?.let { return Result.failure(it) }
        base.value = deLaRed
        return Result.success(Unit)
    }
}

// La tabla `arbitro`, en memoria: mismo orden y mismo upsert que la de verdad.
class FakeArbitroDao(guardados: List<ArbitroEntity> = emptyList()) : ArbitroDao {
    private val filas = MutableStateFlow(guardados)

    override fun observarArbitros(): Flow<List<ArbitroEntity>> = filas.map { it.sortedBy(ArbitroEntity::nombre) }

    override suspend fun guardar(arbitros: List<ArbitroEntity>) {
        val nuevos = arbitros.associateBy { it.id }
        filas.value = filas.value.filterNot { it.id in nuevos } + arbitros
    }
}

// Una API de Retrofit cumplida A MANO: ni servidor, ni OkHttp (construir un OkHttpClient
// en la JVM escribe un error de `android.util.Log` en la salida). Guarda lo que le envian
// y lanza el fallo que el test le ponga.
class FakeLigaApi(
    var arbitros: List<ArbitroDto> = listOf(ArbitroDto(1, "Leanne Graham", DireccionDto("Gwenborough"))),
    var fallo: Exception? = null,
) : LigaApi {
    val enviadas = mutableListOf<CronicaDto>()

    override suspend fun arbitros(): List<ArbitroDto> {
        fallo?.let { throw it }
        return arbitros
    }

    override suspend fun publicarCronica(cronica: CronicaDto): CronicaPublicadaDto {
        fallo?.let { throw it }
        enviadas += cronica
        return CronicaPublicadaDto(id = 101)
    }
}

// El repositorio de cronicas, de mentira: apunta lo publicado y responde con el 101.
class FakeCronicasRepository(var fallo: Exception? = null) : CronicasRepository {
    val publicadas = mutableListOf<Pair<String, String>>()

    override suspend fun publicar(titulo: String, texto: String): Result<Int> {
        fallo?.let { return Result.failure(it) }
        publicadas += titulo to texto
        return Result.success(101)
    }
}
