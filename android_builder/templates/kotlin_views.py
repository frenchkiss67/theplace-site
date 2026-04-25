"""Templates pour un projet Kotlin + XML Views (architecture classique)."""

from __future__ import annotations

APP_BUILD_GRADLE_KTS = """\
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "{{package}}"
    compileSdk = {{compile_sdk}}

    defaultConfig {
        applicationId = "{{package}}"
        minSdk = {{min_sdk}}
        targetSdk = {{target_sdk}}
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.9.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
"""

MAIN_ACTIVITY_KT = """\
package {{package}}

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import {{package}}.databinding.ActivityMainBinding

class {{main_activity}} : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
"""

EXAMPLE_UNIT_TEST_KT = """\
package {{package}}

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}
"""

ACTIVITY_MAIN_XML = """\
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".{{main_activity}}">

    <TextView
        android:id="@+id/hello_text"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="20sp"
        android:text="@string/hello_world"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
"""

KOTLIN_VIEWS_FILES: list[tuple[str, str]] = [
    ("app/build.gradle.kts", APP_BUILD_GRADLE_KTS),
    ("app/src/main/java/{{package_path}}/{{main_activity}}.kt", MAIN_ACTIVITY_KT),
    ("app/src/main/res/layout/activity_main.xml", ACTIVITY_MAIN_XML),
    ("app/src/test/java/{{package_path}}/ExampleUnitTest.kt", EXAMPLE_UNIT_TEST_KT),
]
