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
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
}
