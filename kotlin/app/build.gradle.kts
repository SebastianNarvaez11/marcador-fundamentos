plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
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

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // El modulo de dominio: Partido, Torneo, PartidoEnVivo...
    implementation(project(":torneo"))
    implementation(libs.androidx.activity.compose)
    // lifecycleScope y repeatOnLifecycle: corrutinas atadas al ciclo de vida.
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // La Activity usa runBlocking, Dispatchers y Flow: las corrutinas que :torneo
    // solo expone como `implementation` no llegan a :app por transitividad.
    implementation(libs.kotlinx.coroutines.core)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    // LeakCanary: vigila las Activity destruidas y avisa si alguna no se libera.
    // `debugImplementation` = solo en la variante debug: no viaja en la app de
    // produccion. No hace falta escribir codigo: se instala solo al arrancar.
    debugImplementation(libs.leakcanary.android)
}
