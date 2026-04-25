"""Templates pour un projet Java + XML Views."""

from __future__ import annotations

APP_BUILD_GRADLE_KTS = """\
plugins {
    id("com.android.application")
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
"""

MAIN_ACTIVITY_JAVA = """\
package {{package}};

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import {{package}}.databinding.ActivityMainBinding;

public class {{main_activity}} extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}
"""

EXAMPLE_UNIT_TEST_JAVA = """\
package {{package}};

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
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

JAVA_VIEWS_FILES: list[tuple[str, str]] = [
    ("app/build.gradle.kts", APP_BUILD_GRADLE_KTS),
    ("app/src/main/java/{{package_path}}/{{main_activity}}.java", MAIN_ACTIVITY_JAVA),
    ("app/src/main/res/layout/activity_main.xml", ACTIVITY_MAIN_XML),
    ("app/src/test/java/{{package_path}}/ExampleUnitTest.java", EXAMPLE_UNIT_TEST_JAVA),
]
