# theplace-site — Monorepo

Ce dépôt regroupe deux projets indépendants :

1. **`receipt-scanner/`** — Application Android (Kotlin / Jetpack Compose)
   pour scanner les tickets de caisse et les archiver en PDF.
2. **`c64intro/`** — Intro/demo style Commodore 64 en assembleur 6502/6510
   (KickAssembler).

---

## 1. `receipt-scanner/` — Application Android de scan de tickets

### Objectif

Permettre à l'utilisateur de scanner ses tickets de caisse avec la caméra du
téléphone et de les archiver localement sous forme de PDFs consultables,
partageables et exportables.

### Stack technique

| Couche                | Choix                                                              |
|-----------------------|--------------------------------------------------------------------|
| Langage               | Kotlin 1.9 (JVM target 17)                                         |
| UI                    | Jetpack Compose + Material 3                                       |
| Architecture          | MVVM (ViewModel + StateFlow + Repository)                          |
| Scan/OCR de documents | **ML Kit Document Scanner** (Google Play Services)                 |
| Persistance           | Room 2.6 (métadonnées) + filesystem privé pour les PDFs            |
| Partage inter-app     | `FileProvider`                                                     |
| Build                 | Android Gradle Plugin 8.5, Gradle 8.7, KSP (Room)                  |
| `minSdk`              | 24 (Android 7.0)                                                   |
| `compileSdk`/`target` | 34                                                                 |

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
├── build.gradle.kts                  # Plugins niveau projet
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/                   # gradle-wrapper.properties (8.7)
├── README.md
└── app/
    ├── build.gradle.kts              # Compose BOM, Room, ML Kit
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml       # Activity + FileProvider
        ├── java/com/theplace/receiptscanner/
        │   ├── MainActivity.kt
        │   ├── ReceiptScannerApp.kt
        │   ├── data/                 # Receipt, Dao, Database, Repository
        │   ├── scanner/              # Intégration ML Kit
        │   ├── util/                 # PdfStorage, PdfIntents, Formatting
        │   ├── ui/                   # ReceiptListScreen + theme/
        │   └── viewmodel/            # ReceiptViewModel
        └── res/
            ├── values/               # strings (FR), themes, colors
            ├── xml/                  # file_paths, backup rules
            ├── drawable/             # ic_launcher_foreground
            └── mipmap-anydpi-v26/    # ic_launcher (adaptive)
```

### Conventions de code

- **Kotlin idiomatique** : `data class` pour les modèles, `sealed interface`
  pour les états (`ScanOutcome`), `StateFlow` pour exposer la liste à l'UI.
- **Compose stateless** : `ReceiptListScreen` reçoit l'état et les
  callbacks ; le ViewModel détient la source de vérité.
- **Commentaires en français**, courts, uniquement quand le « pourquoi »
  n'est pas évident depuis le code.
- **Pas de permissions runtime** déclarées : le scanner ML Kit gère la
  caméra dans son propre process.

### Flux de données

```
[Utilisateur] → FAB "Scanner un ticket"
        ↓
[DocumentScannerLauncher.launch()]
        ↓
GmsDocumentScanning → IntentSender → ActivityResult (Compose)
        ↓
[ScanOutcome.Success(pdf)]
        ↓
[ReceiptViewModel.saveScan]
   - PdfStorage.importFrom(pdf.uri, fileName)  → copie vers filesDir/receipts/
   - ReceiptRepository.add(Receipt(...))       → INSERT Room
        ↓
[Flow<List<Receipt>>] observé par l'UI → recomposition de la liste.
```

### Stockage

- **PDFs** : `filesDir/receipts/ticket_<yyyyMMdd_HHmmss>.pdf` — privés à
  l'app, supprimés à la désinstallation, inclus dans Android Backup.
- **Base** : `receipts.db` (Room) — une seule table `receipts` (`id`,
  `name`, `fileName`, `pageCount`, `sizeBytes`, `createdAt`).
- **Partage externe** : authority `${applicationId}.fileprovider` mappée
  sur `files-path name="receipts"` dans `res/xml/file_paths.xml`.

### Commandes utiles

```bash
# Première initialisation du wrapper Gradle (non livré)
cd receipt-scanner
gradle wrapper --gradle-version 8.7

# Build debug
./gradlew :app:assembleDebug

# Installation sur appareil/émulateur connecté
./gradlew :app:installDebug

# Tests unitaires
./gradlew :app:testDebugUnitTest
```

### Points d'attention

- Les versions Compose suivent le **Compose BOM** (`2024.09.02`) — ne pas
  épingler les versions individuelles des libs Compose.
- ML Kit Document Scanner est en **`16.0.0-beta1`** : vérifier la dispo
  d'une release stable lors d'une mise à jour des dépendances.
- L'app cible un usage personnel : pas d'OCR du texte des tickets, pas de
  cloud sync. Toute évolution dans ces directions doit passer par un
  design explicite (permissions, vie privée, sécurité).

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
