"""Fichiers communs à tous les flavors (Gradle racine, settings, ressources de base)."""

from __future__ import annotations

SETTINGS_GRADLE_KTS = """\
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "{{app_name}}"
include(":app")
"""

PROJECT_BUILD_GRADLE_KTS = """\
// Plugins centralisés via le catalogue de versions ; appliqués dans :app.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("com.android.library") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
"""

GRADLE_PROPERTIES = """\
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
"""

GITIGNORE = """\
# Build / IDE
*.iml
.gradle/
/local.properties
.idea/
.DS_Store
build/
captures/
.externalNativeBuild
.cxx/
local.properties

# Keystore (à committer prudemment)
*.jks
*.keystore
"""

STRINGS_XML = """\
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">{{app_name}}</string>
    <string name="hello_world">Bienvenue dans {{app_name}} !</string>
</resources>
"""

COLORS_XML = """\
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="purple_200">#FFBB86FC</color>
    <color name="purple_500">#FF6200EE</color>
    <color name="purple_700">#FF3700B3</color>
    <color name="teal_200">#FF03DAC5</color>
    <color name="teal_700">#FF018786</color>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
</resources>
"""

THEMES_XML = """\
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.{{theme_name}}" parent="Theme.MaterialComponents.DayNight.DarkActionBar">
        <item name="colorPrimary">@color/purple_500</item>
        <item name="colorPrimaryVariant">@color/purple_700</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="colorSecondary">@color/teal_200</item>
        <item name="colorSecondaryVariant">@color/teal_700</item>
        <item name="colorOnSecondary">@color/black</item>
        <item name="android:statusBarColor" tools:targetApi="l">?attr/colorPrimaryVariant</item>
    </style>
</resources>
"""

PROGUARD_RULES = """\
# Règles ProGuard / R8 personnalisées.
# Voir https://developer.android.com/build/shrink-code
"""

ANDROID_MANIFEST = """\
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.{{theme_name}}">
        <activity
            android:name=".{{main_activity}}"
            android:exported="true"
            android:theme="@style/Theme.{{theme_name}}">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
"""

GRADLE_WRAPPER_PROPS = """\
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\\://services.gradle.org/distributions/gradle-8.7-bin.zip
networkTimeout=10000
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
validateDistributionUrl=true
"""

GRADLEW_HINT = """\
# Fichier généré ultérieurement
#
# Pour finaliser le wrapper Gradle, exécuter une fois Gradle installé :
#   gradle wrapper --gradle-version 8.7
# (ou ouvrir le projet dans Android Studio qui le génèrera automatiquement)
"""

# Layout de base (utilisé par les flavors XML).
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

# Liste de fichiers communs (chemin relatif au projet, contenu).
COMMON_FILES: list[tuple[str, str]] = [
    ("settings.gradle.kts", SETTINGS_GRADLE_KTS),
    ("build.gradle.kts", PROJECT_BUILD_GRADLE_KTS),
    ("gradle.properties", GRADLE_PROPERTIES),
    (".gitignore", GITIGNORE),
    ("app/proguard-rules.pro", PROGUARD_RULES),
    ("app/src/main/AndroidManifest.xml", ANDROID_MANIFEST),
    ("app/src/main/res/values/strings.xml", STRINGS_XML),
    ("app/src/main/res/values/colors.xml", COLORS_XML),
    ("app/src/main/res/values/themes.xml", THEMES_XML),
]

GRADLE_WRAPPER_FILES: list[tuple[str, str]] = [
    ("gradle/wrapper/gradle-wrapper.properties", GRADLE_WRAPPER_PROPS),
    ("gradlew.README.txt", GRADLEW_HINT),
]
