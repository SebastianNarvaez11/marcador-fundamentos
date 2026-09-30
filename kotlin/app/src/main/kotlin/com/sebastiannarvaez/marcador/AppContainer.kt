package com.sebastiannarvaez.marcador

import com.sebastiannarvaez.marcador.data.PartidosRepositoryEnMemoria
import com.sebastiannarvaez.marcador.domain.PartidosRepository
import com.sebastiannarvaez.marcador.domain.RegistrarGol

// f44 · INYECCION DE DEPENDENCIAS, A MANO
//
// «Dependencia» = algo que una clase necesita para funcionar (un repositorio). Hay dos
// maneras de conseguirla:
//   - ir a buscarla uno mismo (`Repositorios.partidos`, f43, o `PartidosRepositoryEnMemoria()`
//     dentro del ViewModel): la clase queda atada a UNA implementacion concreta y no se
//     puede probar con otra;
//   - que se la DEN de fuera, por el constructor: eso es inyectarla. La clase pide
//     `PartidosRepository` (la interfaz) y no sabe ni le importa cual le llega.
//
// Alguien tiene que construir las piezas y entregarlas: el CONTENEDOR. Este es el mas
// simple posible: una clase con las dependencias como propiedades `lazy` (se crean la
// primera vez que se piden y se reutilizan: singletons). Vive en la Application, que dura
// lo que el proceso (f27), asi que el repositorio es el mismo en todas las pantallas.
//
// Koin o Hilt hacen lo mismo con menos codigo escrito (Hilt lo genera con KSP; Koin lo
// declara con un DSL). Hacerlo a mano una vez ensena que NO hay magia: es un `new` en un
// solo sitio. En proyectos pequenos, a mano basta.
class AppContainer {
    val partidosRepository: PartidosRepository by lazy { PartidosRepositoryEnMemoria() }

    val registrarGol: RegistrarGol by lazy { RegistrarGol(partidosRepository) }
}
