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

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
        }

        // Tests JVM Android (Robolectric) : Room en mémoire, PdfStorage, etc.
        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("junit:junit:4.13.2")
                implementation("org.robolectric:robolectric:4.13")
                implementation("androidx.test:core:1.6.1")
                implementation("androidx.test.ext:junit:1.2.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
            }
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

            // App lock biométrique
            implementation("androidx.biometric:biometric:1.2.0-alpha05")
            implementation("androidx.fragment:fragment-ktx:1.8.5")

            // Sauvegarde périodique en tâche de fond
            implementation("androidx.work:work-runtime-ktx:2.9.1")
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

    testOptions {
        unitTests {
            // Robolectric a besoin de charger les ressources Android.
            isIncludeAndroidResources = true
        }
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
