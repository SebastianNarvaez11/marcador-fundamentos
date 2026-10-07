plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    // KSP (Kotlin Symbol Processing) es el motor que ejecuta procesadores: al compilar lee
    // las anotaciones (@Inject, @Module...) y ESCRIBE codigo nuevo. El de Hilt lo necesita.
    alias(libs.plugins.ksp)
    // Hilt (Dagger Hilt): genera el contenedor de dependencias de la app al compilar.
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.sebastiannarvaez.marcador"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.sebastiannarvaez.marcador"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    // FLAVORS: versiones distintas de la MISMA app (gratis y de pago, por ejemplo).
    // Cada flavor pertenece a una «dimension»; con una sola, las variantes salen de
    // multiplicar flavor x build type: gratisDebug, gratisRelease, proDebug, proRelease.
    flavorDimensions += "plan"
    productFlavors {
        create("gratis") {
            dimension = "plan"
            // Un campo constante que el codigo lee como BuildConfig.PLAN.
            // Ojo a las comillas: el tercer argumento se pega TAL CUAL en Java.
            buildConfigField("String", "PLAN", "\"gratis\"")
        }
        create("pro") {
            dimension = "plan"
            // Se suma al applicationId: com.sebastiannarvaez.marcador.pro. Sin sufijo
            // distinto, gratis y pro serian «la misma app» para Android y una pisaria
            // a la otra al instalarla. Ademas hay app/src/pro/res con otro app_name.
            applicationIdSuffix = ".pro"
            buildConfigField("String", "PLAN", "\"pro\"")
        }
    }

    // BUILD TYPES: como se construye, no que se construye. `debug` y `release`
    // existen siempre; aqui se ajustan.
    buildTypes {
        debug {
            // Se suma al applicationId: com.sebastiannarvaez.marcador.debug. Asi la
            // version debug y la de produccion conviven instaladas en el mismo movil.
            applicationIdSuffix = ".debug"
            // Sale en «Informacion de la app»: 1.0-debug.
            versionNameSuffix = "-debug"
            buildConfigField("String", "MODO", "\"DEBUG\"")
        }
        release {
            // Sin minificar de momento: R8 (recortar y ofuscar el codigo) queda para
            // cuando toque publicar, porque hay que probar la app ya recortada.
            isMinifyEnabled = false
            buildConfigField("String", "MODO", "\"PRODUCCION\"")
        }
    }

    buildFeatures {
        compose = true
        // Desde AGP 8 BuildConfig NO se genera salvo que lo pidas. Sin esta linea, AGP 9
        // falla al CONFIGURAR el proyecto: «Build Type 'debug' contains custom BuildConfig
        // fields, but the feature is disabled.»
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Hilt: la libreria viaja en la app; el procesador (`ksp`) solo corre al compilar y
    // escribe las clases Hilt_... y el componente.
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // El modulo de dominio: Partido, Torneo, PartidoEnVivo...
    implementation(project(":torneo"))
    implementation(libs.androidx.activity.compose)
    // lifecycleScope y repeatOnLifecycle: corrutinas atadas al ciclo de vida.
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // LocalLifecycleOwner y collectAsStateWithLifecycle (Compose + ciclo de vida).
    implementation(libs.androidx.lifecycle.runtime.compose)
    // viewModel() para Compose; trae tambien lifecycle-viewmodel (ViewModel, viewModelScope).
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // WorkManager: trabajo diferible que el sistema garantiza ejecutar aunque la app se cierre.
    implementation(libs.androidx.work.runtime.ktx)
    // La Activity usa runBlocking, Dispatchers y Flow: las corrutinas que :torneo
    // solo expone como `implementation` no llegan a :app por transitividad.
    implementation(libs.kotlinx.coroutines.core)
    implementation(platform(libs.androidx.compose.bom))
    // Un bundle del catalogo (gradle/libs.versions.toml): varias librerias con un nombre.
    implementation(libs.bundles.compose)
    // LeakCanary: vigila las Activity destruidas y avisa si alguna no se libera.
    // `debugImplementation` = solo en la variante debug: no viaja en la app de
    // produccion. No hace falta escribir codigo: se instala solo al arrancar.
    debugImplementation(libs.leakcanary.android)
    // Las @Preview y el Layout Inspector necesitan ui-tooling, solo en debug.
    debugImplementation(libs.androidx.compose.ui.tooling)
}
