# Tickets de caisse — Scanner Android (Compose Multiplatform)

Application pour scanner les tickets de caisse au format PDF et les
archiver localement. Conçue comme un module **Kotlin / Compose Multiplatform**
avec une cible Android active ; les cibles iOS sont prêtes à être activées
sans restructurer le code.

## Stack technique

- **Kotlin Multiplatform** (Kotlin 2.0.21)
- **Compose Multiplatform 1.7.0** — UI partagée (`commonMain`)
- **Material 3** (dynamic color Android 12+ via injection plateforme)
- **AndroidX Lifecycle multiplatforme** (ViewModel + Compose)
- **kotlinx-datetime / kotlinx-coroutines** — partagés
- **Room 2.6.1** — persistance Android uniquement (KSP)
- **ML Kit Document Scanner** (`play-services-mlkit-document-scanner:16.0.0-beta1`) —
  Android uniquement ; détection bords, perspective, contraste, multi-pages,
  export PDF natif.
- **FileProvider** — partage des PDFs vers d'autres apps.

## Structure

```
receipt-scanner/
├── build.gradle.kts                  # Plugins racine (KMP, CMP, AGP, KSP)
├── settings.gradle.kts               # includes :composeApp
├── gradle.properties
├── gradle/wrapper/
└── composeApp/
    ├── build.gradle.kts              # Module KMP : androidTarget()
    ├── proguard-rules.pro
    └── src/
        ├── commonMain/
        │   ├── kotlin/com/theplace/receiptscanner/
        │   │   ├── App.kt            # Composable entry partagé
        │   │   ├── data/             # Receipt (modèle), ReceiptRepository (interface)
        │   │   ├── platform/         # expect : scanner, PdfActions, PlatformScanResult
        │   │   ├── ui/               # ReceiptListScreen + theme/
        │   │   ├── util/             # Formatting, Clock (kotlinx-datetime)
        │   │   └── viewmodel/        # ReceiptViewModel
        │   └── composeResources/values/strings.xml
        └── androidMain/
            ├── AndroidManifest.xml
            ├── kotlin/com/theplace/receiptscanner/
            │   ├── MainActivity.kt
            │   ├── ReceiptScannerApp.kt    # Application + ServiceLocator
            │   ├── ServiceLocator.kt
            │   ├── data/                   # ReceiptEntity + DAO + DB Room + AndroidReceiptRepository
            │   └── platform/               # actual scanner ML Kit + AndroidPdfActions + PdfStorage
            └── res/                        # Manifest, themes, file_paths, backup, icône
```

## Frontière `expect` / `actual`

| `expect`                                       | `actual` Android                     | `actual` iOS (futur) |
|------------------------------------------------|--------------------------------------|----------------------|
| `class PlatformScanResult`                     | `(Uri, pageCount)`                   | `(NSURL, pageCount)` |
| `rememberDocumentScannerLauncher`              | ML Kit Document Scanner              | `VNDocumentCameraViewController` |
| `interface PdfActions`                         | `AndroidPdfActions` (Intent+FileProvider) | `UIActivityViewController` |
| `interface ReceiptRepository`                  | Room + `PdfStorage` (filesDir)       | SQLDelight + NSFileManager |

Le code `commonMain` (UI Compose, ViewModel, modèle, formatage) compilera
tel quel pour iOS dès que les cibles seront activées et les `actual`
fournis.

## Activer les cibles iOS

Dans `composeApp/build.gradle.kts`, décommenter :

```kotlin
iosX64()
iosArm64()
iosSimulatorArm64()
```

Puis créer `composeApp/src/iosMain/kotlin/.../platform/` avec les `actual`
correspondants. La compilation des cibles iOS nécessite macOS + Xcode.

## Compilation

Le wrapper Gradle n'est pas livré dans le dépôt — initialiser avant la
première build :

```bash
cd receipt-scanner
gradle wrapper --gradle-version 8.7
./gradlew :composeApp:assembleDebug
```

Installation sur un appareil/émulateur Android :

```bash
./gradlew :composeApp:installDebug
```

## Configuration minimale

- `minSdk` 24 (Android 7.0)
- `targetSdk`/`compileSdk` 34
- Google Play Services à jour sur l'appareil (Document Scanner téléchargé
  à la demande).

## Permissions

Aucune permission runtime. ML Kit Document Scanner utilise sa propre
activité système et gère la caméra hors-process. `<uses-feature
android:name="android.hardware.camera">` est annoncé au Play Store.

## Stockage et confidentialité

- PDFs dans `context.filesDir/receipts/` — privés, supprimés à la
  désinstallation, inclus dans Android Backup et device-transfer.
- Base Room (`receipts.db`) — métadonnées uniquement.
- Partage via `FileProvider` (URI temporaire, sans copie).
