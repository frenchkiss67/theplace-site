"""Templates pour un projet Kotlin + Jetpack Compose."""

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
        vectorDrawables.useSupportLibrary = true
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
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
"""

MAIN_ACTIVITY_KT = """\
package {{package}}

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import {{package}}.ui.theme.{{theme_name}}Theme

class {{main_activity}} : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            {{theme_name}}Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Greeting(
                        name = "{{app_name}}",
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Bienvenue dans $name !",
        style = MaterialTheme.typography.headlineMedium,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    {{theme_name}}Theme {
        Greeting("{{app_name}}")
    }
}
"""

THEME_KT = """\
package {{package}}.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6200EE),
    onPrimary = Color.White,
    secondary = Color(0xFF03DAC5),
    onSecondary = Color.Black,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBB86FC),
    onPrimary = Color.Black,
    secondary = Color(0xFF03DAC5),
    onSecondary = Color.Black,
)

@Composable
fun {{theme_name}}Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
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

# Le manifeste de Compose n'a pas besoin de layout XML supplémentaire,
# mais on garde un strings dédié au flavor (déjà fourni par common).

KOTLIN_COMPOSE_FILES: list[tuple[str, str]] = [
    ("app/build.gradle.kts", APP_BUILD_GRADLE_KTS),
    ("app/src/main/java/{{package_path}}/{{main_activity}}.kt", MAIN_ACTIVITY_KT),
    ("app/src/main/java/{{package_path}}/ui/theme/Theme.kt", THEME_KT),
    ("app/src/test/java/{{package_path}}/ExampleUnitTest.kt", EXAMPLE_UNIT_TEST_KT),
]
