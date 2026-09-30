plugins {
    // Plugin de Kotlin para la JVM: libreria o programa de consola, sin Android.
    alias(libs.plugins.kotlinJvm)
    // Es un plugin de COMPILADOR: mira cada clase marcada con @Serializable y
    // genera, al compilar, el codigo que la convierte a JSON y de vuelta.
    // Sin el, la anotacion no hace nada y la ejecucion falla en tiempo de
    // ejecucion, no al compilar.
    alias(libs.plugins.kotlinxSerialization)
    // Anade la tarea `run`: ./gradlew :torneo:run
    application
}

kotlin {
    // Compila y ejecuta con JDK 17 aunque la maquina tenga otro.
    jvmToolchain(17)
}

application {
    mainClass.set("com.sebastiannarvaez.marcador.torneo.MainKt")
}

dependencies {
    // La LIBRERIA: trae Json, encodeToString, decodeFromString y las anotaciones.
    // El plugin de arriba y esta libreria son dos piezas distintas y hacen falta las dos.
    implementation(libs.kotlinx.serialization.json)
    // Corrutinas: launch, async, delay, Flow... Es una LIBRERIA de Kotlin, no del
    // lenguaje: solo `suspend` es del lenguaje; todo lo demas vive aqui.
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(kotlin("test"))
    // runTest, TestScope, StandardTestDispatcher y el reloj virtual.
    testImplementation(libs.kotlinx.coroutines.test)
    // Turbine: probar Flows con `test { awaitItem() ... }`.
    testImplementation(libs.turbine)
}

tasks.test {
    useJUnitPlatform()
}
