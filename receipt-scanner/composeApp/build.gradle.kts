import org.jetbrains.compose.resources.ResourcesExtension

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                }
            }
        }
    }

    // Cibles iOS prêtes à être activées le moment venu — décommenter sur macOS.
    // iosX64()
    // iosArm64()
    // iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // Compose Multiplatform — UI partagée
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)

            // Lifecycle ViewModel multiplatforme + intégration Compose
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.8.4")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

            // Navigation Compose multiplatforme
            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha10")

            // Coroutines & date/time
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
        }

        androidMain.dependencies {
            // Activity Compose et runtime Android
            implementation("androidx.activity:activity-compose:1.9.2")
            implementation("androidx.core:core-ktx:1.13.1")

            // Coroutines Android dispatcher
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

            // Room — Android-only en v1, abstrait derrière l'interface commune
            val roomVersion = "2.6.1"
            implementation("androidx.room:room-runtime:$roomVersion")
            implementation("androidx.room:room-ktx:$roomVersion")

            // ML Kit Document Scanner — Android-only (côté iOS : VisionKit ultérieurement)
            implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
        }
    }
}

android {
    namespace = "com.theplace.receiptscanner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.theplace.receiptscanner"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

// Room compiler côté Android uniquement.
dependencies {
    add("kspAndroid", "androidx.room:room-compiler:2.6.1")
}

// Compose Resources : exposer Res sous notre package racine.
extensions.configure<ResourcesExtension> {
    packageOfResClass = "com.theplace.receiptscanner.resources"
    generateResClass = ResourcesExtension.ResourceClassGeneration.Always
}
