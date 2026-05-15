# theplace-site — Monorepo

Ce dépôt regroupe deux projets indépendants :

1. **`receipt-scanner/`** — Application **Kotlin Multiplatform / Compose
   Multiplatform** pour scanner les tickets de caisse et les archiver en
   PDF. Une seule cible active aujourd'hui (`androidTarget`) ; les cibles
   iOS sont prêtes à être activées sans restructurer le code.
2. **`c64intro/`** — Intro/demo style Commodore 64 en assembleur 6502/6510
   (KickAssembler).

---

## 1. `receipt-scanner/` — Application Android de scan de tickets

### Objectif

Permettre à l'utilisateur de scanner ses tickets de caisse avec la caméra du
téléphone et de les archiver localement sous forme de PDFs consultables,
partageables et exportables.

### Stack technique

| Couche                  | Choix                                                                                  |
|-------------------------|----------------------------------------------------------------------------------------|
| Langage                 | Kotlin **2.0.21** (Multiplatform, JVM target 17)                                       |
| UI                      | **Compose Multiplatform 1.7.0** + Material 3 (dynamic color Android 12+ injecté)       |
| Navigation              | `org.jetbrains.androidx.navigation:navigation-compose` 2.8.0-alpha10 (KMP)             |
| Architecture            | MVVM (ViewModel KMP + StateFlow + Repository commun)                                   |
| Scan de documents       | **ML Kit Document Scanner** (Google Play Services, `androidMain`)                      |
| OCR (texte des tickets) | **ML Kit Text Recognition v2**, on-device, modèle Latin embarqué (`androidMain`)       |
| Persistance             | Room 2.6 (Android-only, derrière une interface commune) + filesystem privé             |
| Partage inter-app       | `FileProvider` côté Android, `ACTION_SEND` / `ACTION_SEND_MULTIPLE`                    |
| Export utilisateur      | Storage Access Framework (`OpenDocumentTree`) — copie via `DocumentsContract`          |
| App lock                | `androidx.biometric` 1.2.0-alpha05 (biométrie + fallback PIN/motif)                    |
| Tâches de fond          | `androidx.work` 2.9.1 (backup auto SAF + notifications garantie)                       |
| Build                   | Android Gradle Plugin 8.5, Gradle 8.7 (wrapper livré), KSP (Room)                      |
| `minSdk`                | 24 (Android 7.0)                                                                       |
| `compileSdk`/`target`   | 34                                                                                     |
| Cibles                  | `androidTarget()` active ; `iosX64/iosArm64/iosSimulatorArm64` prêtes                  |
| Tests                   | `commonTest` (kotlin-test + coroutines-test) ; `androidUnitTest` (Robolectric 4.13)   |
| i18n                    | Compose Resources, FR par défaut, EN dans `values-en/`                                 |

### Pourquoi ML Kit Document Scanner

- **Détection des bords, correction de perspective et amélioration
  contraste/luminosité gérées par Google** — pas besoin d'OpenCV ni de
  pipeline custom.
- **Multi-pages natif** et **export PDF direct** (`RESULT_FORMAT_PDF`).
- Activité système (Play Services) qui possède la permission caméra :
  **aucune permission runtime à demander dans l'app**.
- Modèle téléchargé à la demande → APK léger.

### Arborescence

```
receipt-scanner/
├── build.gradle.kts                       # Plugins racine (KMP, CMP, AGP, KSP)
├── settings.gradle.kts                    # includes :composeApp
├── gradle.properties
├── gradle/wrapper/                        # gradle-wrapper.{properties,jar} (8.7)
├── gradlew / gradlew.bat                  # Wrapper livré (le clone build dès l'arrivée)
├── README.md
├── design.md                              # Design doc + ADRs (1..13)
└── composeApp/
    ├── build.gradle.kts                   # KMP : androidTarget() + iOS commenté
    ├── proguard-rules.pro
    └── src/
        ├── commonMain/
        │   ├── kotlin/com/theplace/receiptscanner/
        │   │   ├── App.kt                          # NavHost + SnackbarHost + BiometricGate
        │   │   ├── data/                           # Receipt + ReceiptCategory + ReceiptRepository
        │   │   ├── platform/                       # expect : scanner, PdfActions, PdfPreview,
        │   │   │                                   #          PdfThumbnail, ExportTarget,
        │   │   │                                   #          BackHandler, AppLockSettings,
        │   │   │                                   #          BackupSettings, TextRecognizer
        │   │   ├── ui/                             # ListScreen + DetailScreen + SortOption +
        │   │   │                                   #   CategoryLabels + theme/
        │   │   ├── util/                           # Formatting (date/amount), Clock,
        │   │   │                                   #   ReceiptInfoExtractor (OCR heuristics),
        │   │   │                                   #   Warranty (end + days left)
        │   │   └── viewmodel/                      # ReceiptViewModel
        │   ├── composeResources/values/strings.xml # Strings FR (par défaut)
        │   └── composeResources/values-en/strings.xml # Strings EN
        ├── commonTest/                             # Tests JVM partagés (Formatting, ViewModel,
        │                                           #   SortOption, ReceiptInfoExtractor, Warranty)
        ├── androidUnitTest/                        # Robolectric : Room DAO + PdfStorage
        └── androidMain/
            ├── AndroidManifest.xml                 # Activity + FileProvider + POST_NOTIFICATIONS
            ├── kotlin/com/theplace/receiptscanner/
            │   ├── MainActivity.kt                 # FragmentActivity (requis pour BiometricPrompt)
            │   ├── ReceiptScannerApp.kt            # Application + ServiceLocator init
            │   ├── ServiceLocator.kt
            │   ├── data/                           # ReceiptEntity + DAO + DB Room (migrations
            │   │                                   #   1→2 catégories, 2→3 OCR, 3→4 garanties)
            │   ├── platform/                       # actual ML Kit (scan + OCR), PdfActions,
            │   │                                   #   PdfStorage, ThumbnailCache, BackupSettings,
            │   │                                   #   AppLock (BiometricPrompt + session)
            │   └── work/                           # BackupWorker, WarrantyWorker, Schedulers,
            │                                       #   WarrantyNotifier (channel + notif)
            └── res/                                # Manifest strings, themes, file_paths, backup, icône
```

### Conventions de code

- **Kotlin idiomatique** : `data class` pour les modèles, `sealed interface`
  pour les états (`ScanOutcome`), `StateFlow` pour exposer la liste à l'UI.
- **Compose stateless** : `ReceiptListScreen` reçoit l'état et les
  callbacks ; le ViewModel détient la source de vérité.
- **commonMain pur** : aucune annotation/import Android dans
  `commonMain/`. Tout ce qui dépend de l'OS passe par `expect/actual`
  (scanner, PDF) ou par une interface plateforme (Repository, PdfActions).
- **Commentaires en français**, courts, uniquement quand le « pourquoi »
  n'est pas évident depuis le code.
- **Permissions runtime minimales** : ML Kit Document Scanner gère la
  caméra dans son propre process (aucune permission caméra demandée).
  Seule `POST_NOTIFICATIONS` est déclarée (Android 13+) pour les rappels
  de garantie ; la demande runtime à l'utilisateur est différée à la
  première activation effective.

### Flux de données

```
[Utilisateur] → FAB "Scanner un ticket"                            (commonMain UI)
        ↓
[DocumentScannerLauncher.launch()]                                 (expect)
        ↓
Android actual : GmsDocumentScanning → IntentSender → ActivityResult
        ↓
[ScanOutcome.Success(PlatformScanResult)]                          (commonMain)
        ↓
[ReceiptViewModel.saveScan]                                        (commonMain)
        ↓
[ReceiptRepository.addFromScan]                                    (interface)
   Android impl :
     - PdfStorage.importFrom(scan.uri, fileName)  → filesDir/receipts/
     - dao.insert(ReceiptEntity)                  → Room
        ↓
[Flow<List<Receipt>>] observé par l'UI → recomposition de la liste.

Et en arrière-plan, sans bloquer l'utilisateur :

[ReceiptViewModel.runOcrInBackground]                              (commonMain)
        ↓
[TextRecognizer.extractText(receipt)]                              (expect)
        ↓
Android actual : PdfRenderer → Bitmap → ML Kit Text Recognition
        ↓
[ReceiptInfoExtractor.extractTotalCents(text)]  → pré-remplit le montant
        ↓
[ReceiptRepository.update(receipt.copy(extractedText, totalCents))]
        ↓
La recherche en commonMain (`filteredByQuery`) couvre aussi `extractedText`.
```

### Stockage

- **PDFs** : `filesDir/receipts/ticket_<yyyyMMdd_HHmmss>.pdf` — privés à
  l'app, supprimés à la désinstallation, inclus dans Android Backup.
- **Vignettes PDF** : `cacheDir/thumbs/<fileName>.png` — rendues à la
  demande (~400 px de large) via `PdfRenderer`, supprimées avec le
  ticket. Le système peut purger le cache sous pression de stockage —
  les vignettes sont re-rendues à la prochaine ouverture.
- **Base** : `receipts.db` (Room v4) — table `receipts` (`id`, `name`,
  `fileName`, `pageCount`, `sizeBytes`, `createdAt`, `category`,
  `totalCents`, `extractedText`, `purchasedAt`, `warrantyMonths`).
- **Préférences** : 3 fichiers SharedPreferences distincts :
  - `app_lock` : flag biométrie activée.
  - `backup_settings` : URI du dossier de backup auto + set des
    fileNames déjà sauvegardés.
  - `warranty_notif` : set des IDs déjà notifiés J-30 (anti-spam).
- **Partage externe** : authority `${applicationId}.fileprovider` mappée
  sur `files-path name="receipts"` dans `res/xml/file_paths.xml`.
- **Export utilisateur** : copies ponctuelles ou périodiques (worker)
  vers un dossier SAF choisi par l'utilisateur, jamais vers du stockage
  cloud propriétaire.

### Commandes utiles

```bash
cd receipt-scanner

# Build debug Android (le wrapper Gradle est livré, ./gradlew marche au clone)
./gradlew :composeApp:assembleDebug

# Installation sur appareil/émulateur connecté
./gradlew :composeApp:installDebug

# Tests JVM (commonTest + androidUnitTest via Robolectric)
./gradlew :composeApp:testDebugUnitTest
```

CI : `.github/workflows/receipt-scanner-ci.yml` lance les tests puis
l'`assembleDebug` sur chaque push/PR touchant le dossier.

### Points d'attention

- **CMP 1.7.0 + Kotlin 2.0.21** : le plugin Compose compiler est
  `org.jetbrains.kotlin.plugin.compose` (séparé depuis Kotlin 2.0).
- ML Kit Document Scanner est en **`16.0.0-beta1`** : vérifier la dispo
  d'une release stable lors d'une mise à jour des dépendances.
- ML Kit Text Recognition v2 (`16.0.1`) : modèle Latin embarqué dans
  l'APK, aucun appel réseau. Pour les langues non-latines (arabe,
  chinois, etc.) il faudrait basculer sur un modèle séparé.
- Compose Resources expose les strings via `Res.string.*` ; le package
  est configuré dans `composeApp/build.gradle.kts`
  (`packageOfResClass = "com.theplace.receiptscanner.resources"`).
- `BiometricPrompt` exige une `FragmentActivity` (et non
  `ComponentActivity`) — c'est la raison pour laquelle `MainActivity`
  hérite de `FragmentActivity`, pas de `ComponentActivity` malgré
  l'usage de Compose.
- `androidx.navigation:navigation-compose` est en **alpha** côté KMP
  (`2.8.0-alpha10`). Surveiller les évolutions API jusqu'à la stable.
- **iOS** non activé. Pour le faire : décommenter les 3 cibles dans
  `composeApp/build.gradle.kts`, créer `iosMain/` avec les `actual`
  (VisionKit pour le scan, `UIActivityViewController` pour le partage,
  SQLDelight pour la persistance, `CGPDFDocument` pour `PdfPreview` et
  `PdfThumbnail`, `UIDocumentPickerViewController` pour
  `PlatformExportTarget`, Vision Framework pour l'OCR), compiler sur
  macOS. Tout le code commun est conçu pour cette extension.
- **Périmètre actuel** : usage personnel, tout local. L'OCR (ML Kit
  on-device, ADR-12) et le backup auto (SAF utilisateur, pas de cloud
  propriétaire, ADR-11) restent dans cet esprit — pas de télémétrie,
  pas de service tiers. Toute évolution introduisant un service distant
  (cloud sync, partage multi-utilisateurs, etc.) doit passer par un
  ADR dédié couvrant permissions, vie privée et sécurité.

---

## 2. `c64intro/` — Intro Commodore 64

Intro-demo style Commodore 64 en assembleur 6510, comportant trois effets
classiques de la scène demo :

1. **Logo en haut de l'écran** — bitmap hi-res ou multicolor.
2. **Raster bars** — barres de couleurs synchronisées avec le raster beam
   via IRQ.
3. **Sinus scroll** — texte défilant avec mouvement sinusoïdal vertical.

### Mémoire C64

- **$0400–$07FF** : Écran texte (screen RAM)
- **$D000–$D3FF** : Registres VIC-II
- **$D400–$D7FF** : Registres SID (son, optionnel)
- **$D800–$DBFF** : Color RAM
- **$C000–$CFFF** : Code principal
- **$2000–$3FFF** : Bitmap du logo (8 Ko)
- **$E000–$FFFF** : Zone alternative si le Kernal est désactivé

### Registres VIC-II essentiels

| Registre | Adresse | Rôle                                                                |
|----------|---------|---------------------------------------------------------------------|
| `$D011`  | SCROLY  | Vertical scroll (0-2), bitmap (bit 5), écran on/off (bit 4)         |
| `$D012`  | RASTER  | Ligne raster / ligne d'IRQ                                          |
| `$D016`  | SCROLX  | Horizontal scroll (0-2), multicolor (bit 4), 38/40 col. (bit 3)     |
| `$D018`  | VMCSB   | Pointeur mémoire vidéo / jeu de caractères                          |
| `$D020`  | EXTCOL  | Couleur du border                                                   |
| `$D021`  | BGCOL0  | Couleur du fond                                                     |
| `$D019`  | IRQFLAG | Flag d'interruption raster                                          |
| `$D01A`  | IRQMASK | Masque d'interruption raster                                        |

### Structure du programme

```
; Point d'entrée : $C000
;
; Initialisation :
;   - Désactiver les interruptions Kernal (SEI)
;   - Configurer le VIC-II pour le mode bitmap (logo)
;   - Charger la table sinus précalculée
;   - Configurer la chaîne de raster IRQ
;
; Boucle principale (IRQ chain) :
;   IRQ1 (ligne ~$00)  → Logo bitmap en haut
;   IRQ2 (ligne ~$60)  → Raster bars
;   IRQ3 (ligne ~$F8)  → Zone du sinus scroll en mode texte
;   Chaque IRQ programme le suivant avant RTI
```

### Conventions de code

- **Assembleur cible** : KickAssembler (`.asm`) ou ACME (`.a`).
- **Syntaxe** : opcodes 6502/6510 en minuscules.
- **Labels** : `snake_case` ; constantes en `MAJUSCULES`.
- **Commentaires** : en français, chaque routine documentée.

### Fichiers

```
c64intro/
├── main.asm          # Init et boucle principale
├── irq.asm           # Chaîne d'interruptions raster
├── logo.asm          # Affichage du logo bitmap
├── rasterbars.asm    # Effet raster bars
├── sinscroll.asm     # Sinus scroll
├── tables.asm        # Tables précalculées (sinus, couleurs)
├── charset.asm       # Jeu de caractères custom
├── build.sh
└── Makefile
```

### Compilation et exécution

```bash
# Avec KickAssembler
java -jar KickAss.jar main.asm -o intro.prg

# Avec ACME
acme -f cbm -o intro.prg main.asm

# Exécuter avec VICE
x64sc intro.prg
```

### Contraintes techniques

- **Timing raster** : 312 lignes PAL × 63 cycles — changements synchronisés
  au cycle près.
- **Pas de flickering** : tous les changements pendant la bonne ligne raster.
- **Taille mémoire** : ~38 Ko utilisables si le Kernal est désactivé.
- **Compatibilité PAL** : standard pour la scène demo C64.

### Ressources

- Registres VIC-II : mapping complet `$D000-$D03F`.
- SID (musique optionnelle) : `$D400-$D41C`.
- Tables sinus : précalculer pour éviter tout calcul en temps réel.
- `$01` : registre de bank switching 6510 (visibilité Kernal/BASIC/I-O).
