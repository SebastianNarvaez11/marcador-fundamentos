plugins {
    // Plugin de Kotlin Multiplatform: un modulo, varios «targets» (destinos de compilacion).
    alias(libs.plugins.kotlinMultiplatform)
    // El plugin de AGP para bibliotecas KMP: sustituye a `com.android.library`.
    alias(libs.plugins.androidKmpLibrary)
    // Es un plugin de COMPILADOR: mira cada clase marcada con @Serializable y
    // genera, al compilar, el codigo que la convierte a JSON y de vuelta.
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    // Compila y ejecuta con JDK 17 aunque la maquina tenga otro.
    jvmToolchain(17)

    // Target 1: la JVM. No es un producto: sirve para los tests y para el main() de consola.
    jvm {
        // Sustituye al plugin `application`: crea la tarea ./gradlew :torneo:jvmRun
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            mainClass.set("com.sebastiannarvaez.marcador.torneo.MainKt")
        }
    }

    // Target 2: Android, como biblioteca que consume :app.
    android {
        namespace = "com.sebastiannarvaez.marcador.torneo"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
        // Tests que corren en la maquina (JVM), no en un dispositivo.
        withHostTest {}
    }

    // Targets 3 y 4: iOS (dispositivo y simulador en Mac con Apple Silicon).
    listOf(iosArm64(), iosSimulatorArm64()).forEach { ios ->
        ios.binaries.framework {
            baseName = "Torneo"
            // Estatico: Xcode solo tiene que enlazarlo, sin incrustarlo ni firmarlo.
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // La LIBRERIA: trae Json, encodeToString, decodeFromString y las anotaciones.
            implementation(libs.kotlinx.serialization.json)
            // Corrutinas: launch, async, delay, Flow... Es una LIBRERIA de Kotlin, no del
            // lenguaje: solo `suspend` es del lenguaje; todo lo demas vive aqui.
            // api: Flow y StateFlow asoman en la cara publica de :torneo, y :app los usa.
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            // runTest, TestScope, StandardTestDispatcher y el reloj virtual.
            implementation(libs.kotlinx.coroutines.test)
            // Turbine: probar Flows con `test { awaitItem() ... }`.
            implementation(libs.turbine)
        }
    }
}
