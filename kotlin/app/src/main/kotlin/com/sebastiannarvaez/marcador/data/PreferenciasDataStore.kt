package com.sebastiannarvaez.marcador.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sebastiannarvaez.marcador.domain.PreferenciasRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DATASTORE PREFERENCES
//
// SharedPreferences (la de siempre) es sincrona: `getString` lee de un mapa en memoria que
// se carga en el HILO PRINCIPAL la primera vez (puede provocar un ANR), `apply()`
// escribe sin avisar de fallos y `commit()` bloquea. DataStore es su sustituto:
//   - asincrono: leer es un Flow, escribir es `suspend` (`edit { }`); nunca bloquea el hilo;
//   - transaccional: cada `edit` es atomico, y los errores llegan como excepciones;
//   - reactivo: quien observa el Flow se entera del cambio, en cualquier pantalla.
// Sirve para POCOS datos sueltos (ajustes). Para datos con estructura y consultas, Room.
//
// La propiedad se declara UNA vez a nivel de fichero: DataStore exige una sola instancia
// por fichero y el delegado la garantiza. (Segun la documentacion de DataStore, tener dos
// activas para el mismo fichero falla al usarlas; aqui no se ha probado a provocarlo.)
private val Context.ajustes: DataStore<Preferences> by preferencesDataStore(name = "ajustes")

class PreferenciasDataStore(private val almacen: DataStore<Preferences>) : PreferenciasRepository {

    constructor(contexto: Context) : this(contexto.applicationContext.ajustes)

    // Cada clave lleva su TIPO: intPreferencesKey, stringPreferencesKey...
    private val claveDuracion = intPreferencesKey("duracion_del_partido")

    // `data` es un Flow<Preferences>; se elige la clave y, si aun no se ha guardado nada,
    // el valor por defecto.
    override val duracionDelPartido: Flow<Int> = almacen.data.map { preferencias ->
        preferencias[claveDuracion] ?: PreferenciasRepository.DURACION_POR_DEFECTO
    }

    override suspend fun cambiarDuracion(minutos: Int) {
        require(minutos in PreferenciasRepository.DURACIONES) { "Duración no permitida: $minutos" }
        almacen.edit { it[claveDuracion] = minutos }
    }
}
