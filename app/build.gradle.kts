plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.esnaflokantalari.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.esnaflokantalari.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"

        resourceConfigurations += listOf("tr", "en")
    }

    signingConfigs {
        // Test amaçlı imza. Google Play'e yükleyeceğin sürüm için Android
        // Studio'dan kendi anahtarını üretip burayı onunla değiştir —
        // ve o anahtarı kaybetme, uygulamayı bir daha güncelleyemezsin.
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
            signingConfig = signingConfigs.getByName("testRelease")
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

    // Elle girilen foto_url alanları için (varsayılan olarak kullanılmaz)
    implementation("io.coil-kt:coil-compose:2.6.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
