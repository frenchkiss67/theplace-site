# android-builder

Outil Python en ligne de commande pour **scaffolder rapidement des applications Android**.
Aucune dépendance externe : seul Python 3.10+ est requis.

Trois flavors de projet sont pris en charge :

| Flavor           | Langage | UI                 |
|------------------|---------|--------------------|
| `kotlin-compose` | Kotlin  | Jetpack Compose    |
| `kotlin-views`   | Kotlin  | XML + ViewBinding  |
| `java-views`     | Java    | XML + ViewBinding  |

## Installation

Depuis la racine du dépôt :

```bash
pip install ./android_builder
```

Ou en mode "sans installation" :

```bash
python -m android_builder --help
```

## Commandes

### Créer un projet (mode flags)

```bash
python -m android_builder create \
    --name "Mon App" \
    --package com.exemple.monapp \
    --flavor kotlin-compose \
    --output ./out
```

### Créer un projet (mode interactif)

```bash
python -m android_builder create --interactive
```

Le mode interactif demande tour à tour :
- le nom de l'application,
- le package,
- l'Activity principale,
- le flavor (Compose / Views / Java),
- les SDK min / target / compile.

### Lister les flavors disponibles

```bash
python -m android_builder list-flavors
```

### Ajouter une Activity à un projet existant

```bash
python -m android_builder add-activity SettingsActivity --project ./mon_app
```

La commande détecte automatiquement le flavor du projet (Compose, Views Kotlin
ou Java) et génère :
- la classe Activity (Kotlin ou Java),
- le layout XML correspondant pour les flavors Views,
- l'enregistrement de l'Activity dans `AndroidManifest.xml`.

### Inspecter un projet

```bash
python -m android_builder info --project ./mon_app
```

## Ce qui est généré

Pour chaque nouveau projet :

```
mon_app/
├── .gitignore
├── build.gradle.kts                # plugins root + version catalog
├── gradle.properties
├── settings.gradle.kts
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts            # plugin Android, deps Compose/Material
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/<package>/
        │   │   ├── MainActivity.(kt|java)
        │   │   └── ui/theme/Theme.kt   # uniquement Compose
        │   └── res/
        │       ├── layout/activity_main.xml   # uniquement Views
        │       └── values/{strings,colors,themes}.xml
        └── test/java/<package>/ExampleUnitTest.(kt|java)
```

> ℹ️ Le wrapper Gradle (`gradlew`, `gradle-wrapper.jar`) n'est volontairement
> pas généré (binaire). Ouvrez le projet dans Android Studio ou exécutez
> `gradle wrapper --gradle-version 8.7` une fois Gradle installé.

## Architecture du paquet

```
android_builder/
├── __init__.py
├── __main__.py          # python -m android_builder
├── cli.py               # argparse + sous-commandes
├── generator.py         # ProjectConfig, generate_project, add_activity, detect_project
├── interactive.py       # mode interactif
├── utils.py             # validation, rendu de templates, helpers I/O
└── templates/
    ├── common.py        # Gradle racine, manifest, ressources
    ├── kotlin_compose.py
    ├── kotlin_views.py
    └── java_views.py
```

Les templates sont **embarqués** sous forme de chaînes Python (jetons
`{{var}}`) — pas de fichiers `.tmpl` à packager, aucune dépendance à un moteur
de templating externe.

## Exemple complet

```bash
# 1. Génère un projet Compose
python -m android_builder create \
    --name "ChatBot" --package com.exemple.chatbot \
    --flavor kotlin-compose --output ./out

# 2. Ajoute un écran supplémentaire
python -m android_builder add-activity SettingsActivity --project ./out/chatbot

# 3. Inspecte la configuration détectée
python -m android_builder info --project ./out/chatbot
```

## Limitations connues

- Aucune gestion d'icônes / mipmaps : utiliser Android Studio (Image Asset Studio) après génération.
- Pas de Compose Multiplatform / KMP : ce générateur cible Android natif uniquement.
- Versions des libs figées au moment de la rédaction (Gradle 8.7, AGP 8.5, Kotlin 1.9.24, Compose BOM 2024.06).
