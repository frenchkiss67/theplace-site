# Tickets de caisse — Scanner Android

Application Android pour scanner les tickets de caisse au format PDF et les
archiver localement sur l'appareil.

## Stack technique

- **Kotlin** + **Jetpack Compose** (Material 3)
- **ML Kit Document Scanner** (`play-services-mlkit-document-scanner`) :
  détection automatique des bords, correction de perspective, ajustement
  contraste/luminosité, multi-pages, export PDF natif. Le module tourne dans
  Google Play Services — pas de poids ajouté à l'APK et permission caméra
  gérée par le service.
- **Room** : persistance des métadonnées (nom, date, taille, nombre de pages)
- **AndroidX Activity Result API** : `StartIntentSenderForResult` pour lancer
  le scanner.
- **FileProvider** : partage/ouverture sécurisés des PDFs vers d'autres apps.

## Fonctionnalités

- Scan d'un ou plusieurs tickets en une session (jusqu'à 10 pages par PDF).
- Archivage automatique dans `filesDir/receipts/` — données privées à l'app
  et sauvegardées par Android Backup.
- Liste chronologique avec date, taille et nombre de pages.
- Renommer, ouvrir dans un viewer PDF, partager (e-mail, drive, etc.),
  supprimer.

## Structure

```
receipt-scanner/
├── build.gradle.kts                 # Plugins de niveau projet
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/                  # Wrapper Gradle
└── app/
    ├── build.gradle.kts             # Module application
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/theplace/receiptscanner/
        │   ├── MainActivity.kt            # Point d'entrée Compose
        │   ├── ReceiptScannerApp.kt       # Application
        │   ├── data/                      # Entité Room, DAO, DB, Repository
        │   ├── scanner/                   # Intégration ML Kit
        │   ├── util/                      # PDF storage, intents, formatters
        │   ├── ui/                        # Écrans Compose + thème
        │   └── viewmodel/                 # ReceiptViewModel
        └── res/
            ├── values/                    # strings, themes, colors
            ├── xml/                       # file_paths, backup rules
            ├── drawable/                  # icône
            └── mipmap-*/                  # launcher
```

## Compilation

Le wrapper Gradle n'est pas livré dans le dépôt — initialiser avant la première
build :

```bash
cd receipt-scanner
gradle wrapper --gradle-version 8.7
./gradlew :app:assembleDebug
```

Installation sur un appareil/émulateur :

```bash
./gradlew :app:installDebug
```

## Configuration minimale

- `minSdk` 24 (Android 7.0)
- `targetSdk`/`compileSdk` 34
- Google Play Services à jour sur l'appareil (le module Document Scanner est
  téléchargé à la demande la première fois).

## Permissions

Aucune permission runtime n'est déclarée : l'API ML Kit Document Scanner
utilise sa propre activité système et gère la caméra hors-process. Seul
`<uses-feature android:name="android.hardware.camera">` est annoncé pour le
Play Store.

## Stockage et confidentialité

- Les PDFs vivent dans `filesDir/receipts/` — privés à l'app, supprimés à
  la désinstallation.
- La base Room (`receipts.db`) ne contient que les métadonnées.
- Le partage vers d'autres apps passe par `FileProvider` (URI temporaire,
  sans copie).
- Les règles de sauvegarde (`backup_rules.xml`, `data_extraction_rules.xml`)
  incluent les PDFs et la base dans Android Backup et le transfert d'appareil.
