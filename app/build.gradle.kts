plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// La versione arriva dal tag del rilascio (-Pbetterout.version=0.2.0); il codice numerico ne deriva: 0.2.0 → 200.
val appVersion = providers.gradleProperty("betterout.version").getOrElse("0.2.0")
val appVersionCode = appVersion.split(".").map(String::toInt).let { (major, minor, patch) -> major * 10_000 + minor * 100 + patch }

// La chiave di firma non sta nel repository: il flusso di rilascio la passa con queste variabili.
val keystoreFile = System.getenv("BETTEROUT_KEYSTORE_FILE")
val keystorePassword = System.getenv("BETTEROUT_KEYSTORE_PASSWORD")

android {
    namespace = "app.betterout"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "app.betterout"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersion
    }

    signingConfigs {
        if (keystoreFile != null && keystorePassword != null) create("release") {
            storeFile = file(keystoreFile)
            storePassword = keystorePassword
            keyAlias = "betterout"
            keyPassword = keystorePassword
        }
    }

    // Un APK per architettura: le librerie native della mappa pesano, e ogni telefono ne usa una sola.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // Senza le variabili della chiave (cioè fuori dal flusso di rilascio) l'APK release esce non firmato.
            // R8 toglie il codice non usato e ottimizza: l'app parte e scorre meglio. Le risorse non vengono tolte,
            // perché alcune etichette sono cercate per nome.
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.maplibre)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
