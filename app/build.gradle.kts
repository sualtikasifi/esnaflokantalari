import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

/**
 * Yayın imzası bilgileri repoya girmez. Kök dizindeki `keystore.properties`
 * dosyasından okunur (bkz. keystore.properties.example).
 * Dosya yoksa test anahtarına düşer, böylece derleme her zaman çalışır.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) load(file.inputStream())
}
val hasReleaseKeystore = keystoreProperties.getProperty("storeFile")
    ?.let { rootProject.file(it).exists() } == true

android {
    namespace = "com.esnaflokantalari.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.esnaflokantalari.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 16
        versionName = "1.2.3"

        resourceConfigurations += listOf("tr", "en")
    }

    signingConfigs {
        // Google Play'e yüklenecek gerçek imza.
        create("release") {
            if (hasReleaseKeystore) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
        // Elde gerçek anahtar yokken test derlemesi yapabilmek için.
        create("testRelease") {
            val keystore = rootProject.file("test-release.keystore")
            if (keystore.exists()) {
                storeFile = keystore
                storePassword = "esnaf2026"
                keyAlias = "esnaflokantalari"
                keyPassword = "esnaf2026"
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName(
                if (hasReleaseKeystore) "release" else "testRelease",
            )
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // Favoriler ve kullanıcı önerileri için kalıcı depolama
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // "Yakınımdaki lokantalar" için konum servisi
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Lokanta fotoğraflarını göstermek için
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Galeriden seçilen fotoğrafın yönünü düzeltmek için
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
