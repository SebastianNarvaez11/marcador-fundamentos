plugins {
    // Plugin de Kotlin para la JVM: libreria o programa de consola, sin Android.
    alias(libs.plugins.kotlinJvm)
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
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
