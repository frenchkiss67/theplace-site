# Design — Application Scan de tickets de caisse

Document de design pour l'app Android `receipt-scanner`. Couvre le produit,
les flux utilisateur, les écrans, l'architecture logicielle, le modèle de
données, le stockage et les décisions techniques.

---

## 1. Vision produit

### Problème

L'utilisateur conserve ses tickets de caisse papier pour : garantie produit,
notes de frais, comptabilité personnelle, retour magasin. Les tickets
thermiques s'effacent en quelques mois ; les photos brutes dans la galerie
sont éparpillées et illisibles (perspective, ombres).

### Solution

Une app simple à un écran qui transforme un ticket papier en **PDF propre
archivé localement**, classé par date, partageable en deux taps.

### Non-objectifs (v1)

- Pas d'**OCR du texte** des tickets (pas d'extraction montant/TVA).
- Pas de **synchronisation cloud** (Drive, iCloud, etc.).
- Pas de **catégorisation automatique** (commerce, type de dépense).
- Pas de **multi-utilisateurs** / compte / connexion.
- Pas de **statistiques** ni de tableau de bord financier.

Ces sujets pourront faire l'objet d'évolutions ultérieures avec un design
explicite (permissions, vie privée, sécurité).

### Public

Usage personnel mono-utilisateur sur son propre téléphone.

---

## 2. Principes de design

| Principe                                | Conséquence                                                  |
|-----------------------------------------|--------------------------------------------------------------|
| **Zero-friction**                       | Un seul écran ; FAB toujours accessible.                     |
| **Local-first, privacy-first**          | Aucune donnée ne quitte l'appareil sans action explicite.    |
| **Réutiliser plutôt que réinventer**    | ML Kit fournit le scan ; FileProvider fournit le partage.    |
| **Material 3 par défaut**               | Pas de design system custom.                                 |
| **Dégradation gracieuse**               | Si Play Services manque : message clair, pas de crash.       |

---

## 3. Flux utilisateur

### 3.1 Flux principal — Scanner et archiver un ticket

```
┌─────────────────────────────────────────────────────────────────┐
│  1. App lancée → liste des tickets (ou empty state)             │
│  2. Tap sur FAB "Scanner un ticket"                             │
│  3. Activité système ML Kit Document Scanner s'ouvre :          │
│       - Caméra live preview                                     │
│       - Détection automatique des bords (overlay)               │
│       - Capture (auto ou manuelle)                              │
│       - Édition : recadrage, rotation, filtre, ajouter pages    │
│       - Tap "Enregistrer"                                       │
│  4. Retour à l'app via ActivityResult :                         │
│       - GmsDocumentScanningResult.pdf.uri                       │
│  5. ReceiptViewModel.saveScan() :                               │
│       - Copie le PDF dans filesDir/receipts/ticket_<ts>.pdf     │
│       - INSERT métadonnées en Room                              │
│  6. Toast « Ticket archivé » + liste mise à jour                │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 Flux annulation

L'utilisateur quitte le scanner (back, croix) → `ScanOutcome.Cancelled` →
toast discret « Scan annulé ». Aucun fichier créé, aucun INSERT.

### 3.3 Flux erreur

`ScanOutcome.Failure(message)` (Play Services indispo, modèle non
téléchargé, etc.) → toast d'erreur lisible en français incluant le motif.

### 3.4 Actions sur un ticket archivé

- **Ouvrir** : intent `ACTION_VIEW` `application/pdf` via FileProvider.
- **Partager** : `ACTION_SEND` `application/pdf` (mail, drive, etc.).
- **Renommer** : dialogue avec champ texte ; vide ou inchangé = no-op.
- **Supprimer** : dialogue de confirmation, puis suppression du PDF et
  de la ligne Room.

---

## 4. Maquettes d'écrans

### 4.1 Écran unique : `ReceiptListScreen`

#### État vide (premier lancement)

```
┌──────────────────────────────────────────┐
│  Tickets de caisse                       │   ← TopAppBar Material 3
├──────────────────────────────────────────┤
│                                          │
│                                          │
│              📄                          │
│         (icône PDF)                      │
│                                          │
│      Aucun ticket archivé                │
│                                          │
│   Appuyez sur le bouton + pour           │
│   scanner votre premier ticket.          │
│                                          │
│                                          │
│                                          │
│                           ┌────────────┐ │
│                           │ + Scanner  │ │   ← ExtendedFAB
│                           └────────────┘ │
└──────────────────────────────────────────┘
```

#### État rempli

```
┌──────────────────────────────────────────┐
│  Tickets de caisse                       │
├──────────────────────────────────────────┤
│  3 ticket(s) — 142 Ko au total           │   ← Summary
│                                          │
│  ┌────────────────────────────────────┐  │
│  │ 📄  Ticket du 14/05/2026 10:32     │  │
│  │     14 mai 2026, 10:32             │  │
│  │     1 page · 38 Ko                 │  │
│  │              ↗   ⇪   ✎   🗑       │  │   ← Open/Share/Rename/Delete
│  └────────────────────────────────────┘  │
│  ┌────────────────────────────────────┐  │
│  │ 📄  Carrefour 02 mai               │  │
│  │     2 mai 2026, 18:14              │  │
│  │     2 pages · 76 Ko                │  │
│  │              ↗   ⇪   ✎   🗑       │  │
│  └────────────────────────────────────┘  │
│  ...                                     │
│                                          │
│                           ┌────────────┐ │
│                           │ + Scanner  │ │
│                           └────────────┘ │
└──────────────────────────────────────────┘
```

### 4.2 Dialogues

**Renommer** :

```
┌──────────────────────────────────────┐
│  Renommer le ticket                  │
│                                      │
│  ┌────────────────────────────────┐ │
│  │ Nom du ticket                   │ │
│  │ Carrefour 02 mai               │ │
│  └────────────────────────────────┘ │
│                                      │
│           Annuler   Confirmer        │
└──────────────────────────────────────┘
```

**Supprimer** :

```
┌──────────────────────────────────────┐
│  Supprimer ce ticket ?               │
│                                      │
│  Le PDF sera définitivement          │
│  supprimé.                           │
│                                      │
│           Annuler   Confirmer        │
└──────────────────────────────────────┘
```

### 4.3 Thème et palette

- **Material 3** + **dynamic color** sur Android 12+ (suit le wallpaper).
- Fallback hors dynamic color :
  - Primary `#0E7C66` (vert sapin, palette `Teal`)
  - Secondary `#466A60` (vert grisé, palette `TealGrey`)
  - Tertiary `#7A5C2E` (ocre, palette `Sand`)
- Dark theme géré automatiquement (`isSystemInDarkTheme()`).
- Icône adaptive : foreground vectoriel ticket + background `#0E7C66`.

---

## 5. Architecture logicielle

### 5.0 Vue Kotlin Multiplatform

Le projet est un module Kotlin Multiplatform (`composeApp`) avec une seule
cible active (`androidTarget()`) en v1, prête à accueillir `iosX64`,
`iosArm64` et `iosSimulatorArm64`.

```
composeApp/
├── src/commonMain/   ← UI Compose, ViewModel, modèle, util, expect
└── src/androidMain/  ← Room, ML Kit, FileProvider, actual, MainActivity
```

La frontière `expect/actual` couvre les trois points qui dépendent du
système d'exploitation :

| `expect`                                 | `actual` Android                     | `actual` iOS (futur)                |
|------------------------------------------|--------------------------------------|-------------------------------------|
| `class PlatformScanResult`               | `(Uri, pageCount)`                   | `(NSURL, pageCount)`                |
| `@Composable rememberDocumentScannerLauncher` | ML Kit Document Scanner          | `VNDocumentCameraViewController`    |
| `interface PdfActions`                   | Intent VIEW/SEND + FileProvider      | `UIActivityViewController`          |
| `interface ReceiptRepository`            | Room + `PdfStorage` (filesDir)       | SQLDelight + `NSFileManager`        |

### 5.1 Vue d'ensemble (couches)

```
┌────────────────────────────────────────────────────────────┐
│  commonMain — UI (Compose Multiplatform, stateless)        │
│  ─ App.kt / ReceiptListScreen                              │
│  ─ Dialogues (rename, delete)                              │
└────────────────────────────────────────────────────────────┘
                    │ state ▲      events ▼
┌────────────────────────────────────────────────────────────┐
│  commonMain — Présentation (ViewModel KMP)                 │
│  ─ ReceiptViewModel : StateFlow<List<Receipt>>             │
│    saveScan / rename / delete / openPdf / sharePdf         │
└────────────────────────────────────────────────────────────┘
                    │
                    ▼
┌────────────────────────────────────────────────────────────┐
│  commonMain — Domain (interfaces)                          │
│  ─ ReceiptRepository, PdfActions, DocumentScannerLauncher  │
└────────────────────────────────────────────────────────────┘
                    │
                    ▼ (actual)
┌────────────────────────┐          ┌─────────────────────────┐
│  androidMain — Données │          │  androidMain — Plateforme│
│  ─ Room (entity, DAO,  │          │  ─ ML Kit Scanner       │
│    DB, repo impl)      │          │  ─ AndroidPdfActions    │
│  ─ PdfStorage          │          │  ─ FileProvider          │
└────────────────────────┘          └─────────────────────────┘
```

### 5.2 Mapping fichiers ↔ responsabilités

#### `commonMain`

| Fichier                                              | Rôle                                                |
|------------------------------------------------------|-----------------------------------------------------|
| `App.kt`                                             | Composable entry partagé, wiring scanner→VM→écran. |
| `ui/ReceiptListScreen.kt`                            | Liste, empty state, cartes, dialogues.              |
| `ui/theme/*`                                         | Material 3 (dynamic color injecté par plateforme).  |
| `viewmodel/ReceiptViewModel.kt`                      | StateFlow, orchestre Repository + PdfActions.       |
| `data/Receipt.kt`                                    | Modèle de domaine (aucune annotation plateforme).   |
| `data/ReceiptRepository.kt`                          | Interface commune.                                  |
| `platform/DocumentScanner.kt`                        | `expect` : scanner + `ScanOutcome`.                 |
| `platform/PdfActions.kt`                             | Interface ouvrir/partager.                          |
| `util/Formatting.kt`, `util/Clock.kt`                | Date/taille KMP (kotlinx-datetime).                 |
| `composeResources/values/strings.xml`                | Strings UI Compose Resources.                       |

#### `androidMain`

| Fichier                                              | Rôle                                                |
|------------------------------------------------------|-----------------------------------------------------|
| `MainActivity.kt`                                    | Hôte Compose, injecte dynamic color + toasts.       |
| `ReceiptScannerApp.kt` + `ServiceLocator.kt`         | DI minimale (un repo, un PdfActions).               |
| `data/ReceiptEntity.kt` + mappers                    | Entité Room séparée du modèle commun.               |
| `data/ReceiptDao.kt`, `ReceiptDatabase.kt`           | Room.                                               |
| `data/AndroidReceiptRepository.kt`                   | Implémente `ReceiptRepository` (Room + PdfStorage). |
| `platform/DocumentScanner.android.kt`                | `actual` ML Kit + `PlatformScanResult(uri, pages)`. |
| `platform/AndroidPdfActions.kt`                      | Intent VIEW/SEND via FileProvider.                  |
| `platform/PdfStorage.kt`                             | I/O `filesDir/receipts/`, génération de nom, URI.   |
| `res/values/strings.xml`                             | `app_name` (manifest) + messages Toast Android.     |
| `res/{xml,values,mipmap,drawable}/...`               | Ressources plateforme (icône, file_paths, backup).  |

### 5.3 Principes

- **UI stateless** : pas d'`androidx.compose.runtime.mutableStateOf` métier
  dans les composables (sauf dialogues éphémères) — la source de vérité
  est le `StateFlow` du ViewModel.
- **DI minimale** : `ServiceLocator` côté Android, branché depuis
  `Application`. Pas de Hilt/Koin en v1.
- **Coroutines** : tout I/O (`saveScan`, `rename`, `delete`) tourne dans
  `viewModelScope`, jamais sur le main thread.
- **Versions Compose** : Compose Multiplatform 1.7.0 pour `compose.*` ;
  les libs Compose Android (`androidx.activity:activity-compose`) restent
  dans `androidMain` uniquement.
- **`expect/actual` minimal** : on n'expose en `expect` que ce qui ne peut
  pas être Kotlin pur (scanner, partage, persistance plateforme).
- **Aucune annotation Android dans `commonMain`** — le modèle de domaine
  est portable, l'entité Room est en `androidMain` avec mappers.

---

## 6. Modèle de données

### 6.1 Modèle de domaine (commonMain) et entité Room (androidMain)

Le modèle de domaine partagé est une `data class` Kotlin pure :

```kotlin
// commonMain
data class Receipt(
    val id: Long = 0,
    val name: String,        // libellé éditable, par défaut "Ticket du <date>"
    val fileName: String,    // ticket_YYYYMMDD_HHmmss.pdf
    val pageCount: Int,      // pages dans le PDF (1..10)
    val sizeBytes: Long,     // taille du PDF sur disque
    val createdAt: Long,     // ms epoch
)
```

L'entité Room reste cantonnée à `androidMain` avec mappers `toDomain()` /
`toEntity()` :

```kotlin
// androidMain
@Entity(tableName = "receipts")
internal data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val fileName: String,
    val pageCount: Int,
    val sizeBytes: Long,
    val createdAt: Long,
)
```

### 6.2 Invariants

- `fileName` est **unique** par construction (timestamp seconde + dossier privé).
- Un `Receipt` en base ⇔ un fichier sur disque. La suppression supprime
  d'abord le fichier, puis la ligne.
- `id` n'est jamais exposé à l'utilisateur.
- Pas de soft-delete v1 : la suppression est définitive (cf. dialogue de
  confirmation).

### 6.3 Migrations futures

Si on ajoute des colonnes (catégorie, montant OCR, etc.), Room migration v1→v2
en `ALTER TABLE`. Pas de `fallbackToDestructiveMigration()` — les données
utilisateur sont précieuses.

---

## 7. Stockage et permissions

### 7.1 Emplacements

| Donnée                          | Emplacement                           | Backup |
|---------------------------------|----------------------------------------|--------|
| PDFs                            | `context.filesDir/receipts/`           | Oui    |
| Base Room                       | `databases/receipts.db`                | Oui    |
| Cache temporaire (rien en v1)   | `context.cacheDir/`                    | Non    |

- `filesDir` est **privé à l'app** ; aucune autre app n'y accède sans
  `FileProvider`.
- À la désinstallation, tout est supprimé.

### 7.2 Backup

- `android:allowBackup="true"` + `backup_rules.xml` incluent
  `file path="receipts/"` et `database path="receipts.db"`.
- `data_extraction_rules.xml` autorise le device-transfer
  (Android 12+) sur les mêmes ensembles.

### 7.3 Partage externe

- Authority FileProvider : `${applicationId}.fileprovider`.
- `file_paths.xml` expose uniquement `files-path name="receipts"`.
- `FLAG_GRANT_READ_URI_PERMISSION` ajouté aux intents `VIEW`/`SEND`.

### 7.4 Permissions

- **Runtime** : aucune. L'activité ML Kit gère la caméra dans le process
  Play Services.
- **Manifest** : `<uses-feature android:name="android.hardware.camera" required="false" />`
  pour signaler le besoin matériel sans rendre l'app incompatible avec les
  tablettes sans caméra arrière.

---

## 8. Décisions techniques (ADR-lite)

### ADR-1 — Pas de canvas custom pour le scan, on prend ML Kit

**Contexte** : on pourrait construire un pipeline OpenCV (détection de bords
Canny, transformation perspective, binarisation, export PDF via PdfDocument).
**Décision** : utiliser `play-services-mlkit-document-scanner` de Google.
**Conséquences** :
- ✅ Tout le pipeline (bords, perspective, contraste, multi-pages, PDF) est
  fourni clé en main et tourne hors-process.
- ✅ Pas de permission caméra à gérer dans l'app.
- ✅ APK léger (modèle téléchargé à la demande).
- ⚠️ Dépendance à Google Play Services — pas utilisable sur LineageOS sans
  microG ou sur les ROMs sans GMS.
- ⚠️ Module encore en `16.0.0-beta1` à la date de design.

### ADR-2 — Compose plutôt que XML/Fragments

**Décision** : 100% Compose. **Supersédé partiellement par ADR-8** :
on est passé à **Compose Multiplatform** ; la décision « pas de XML »
reste valide.

### ADR-3 — Room + filesystem séparés plutôt que BLOB en base

**Décision** : PDFs sur disque, métadonnées en Room, jointure par `fileName`.
**Raisons** :
- Pas de blob lourd en SQLite (corruption, taille, performance).
- Partage natif : `FileProvider` veut un fichier sur disque.
- Lecture d'un PDF = `Uri` → viewer ; pas de step de conversion.

### ADR-4 — Une seule activité, pas de navigation Compose pour v1

**Décision** : `MainActivity` + un écran Compose `ReceiptListScreen`. Les
dialogues sont gérés en local-state Compose. Aucune route Navigation.
**Raisons** : un seul écran, ajouter Navigation serait du sur-design.
**Limite** : si on ajoute un écran « détail » avec preview PDF, on
introduira `androidx.navigation:navigation-compose` (déjà dans les
dépendances, anticipé).

### ADR-5 — Pas de DI (Hilt/Koin) en v1

**Décision** : un `ServiceLocator` côté `androidMain` construit `Repository`
+ `PdfActions` ; l'`Application` les expose, la `MainActivity` les
injecte au ViewModel via une `ViewModelProvider.Factory`.
**Raisons** : surface de dépendances trop petite pour justifier Hilt
(Hilt ne fonctionne d'ailleurs qu'en Android). Si on active iOS, Koin
multiplatforme deviendra le candidat naturel. Réévaluer si on ajoute un
OCR ou un sync cloud.

### ADR-6 — Stockage privé interne plutôt que MediaStore/Documents

**Décision** : `filesDir/receipts/`.
**Raisons** :
- Pas de permission `WRITE_EXTERNAL_STORAGE` (deprecated).
- Pas d'exposition accidentelle dans la galerie photos.
- Sauvegarde Android Backup directe.
**Limite** : à la désinstallation, les PDFs sont perdus si l'utilisateur
n'a pas utilisé « Partager » pour les exporter. Documenté dans le README.

### ADR-7 — Comparaison élargie des frameworks UI (Compose vs Views vs CMP vs Flutter vs RN)

**Contexte** : ADR-2 retient Compose en quelques lignes. Cet ADR documente
la comparaison complète face aux alternatives multiplateformes, afin de
pouvoir relire la décision si un objectif iOS apparaît.

**Synthèse** (état mai 2026) :

| Critère                                       | Compose | XML Views | Compose MP | Flutter | React Native |
|-----------------------------------------------|:-------:|:---------:|:----------:|:-------:|:------------:|
| Recommandé par Google pour nouvelles apps     | ✅      | ⚠️ legacy | ✅         | —       | —            |
| Accès direct ML Kit Document Scanner          | ✅      | ✅        | ⚠️ Android| ⚠️ plugin| ⚠️ plugin   |
| Material 3 + dynamic color first-class        | ✅      | ⚠️        | ⚠️         | ⚠️      | ⚠️           |
| Stack persistance Room first-class            | ✅      | ✅        | ✅         | — drift | — divers     |
| Multiplateforme Android+iOS                   | —       | —         | ✅         | ✅      | ✅           |
| Adapté à `receipt-scanner` (Android-only)     | **✅**  | —         | —          | —       | —            |

**Décision** : conserver **Jetpack Compose** (ADR-2 confirmé).

**Détail par alternative** :

- **XML Views + Fragments** : non déprécié mais positionné comme support
  legacy par Google. Aucun bénéfice pour une nouvelle app sans contrainte SDK.
- **Compose Multiplatform** : **stable iOS depuis la 1.8.0 (mai 2025)**,
  utilisé en prod par Netflix, McDonald's, Cash App. Sweet spot =
  formulaires / listes / écrans détail, soit exactement
  `receipt-scanner`. Coût pour ce projet :
    - Pas de composants iOS natifs (look Material par défaut).
    - ML Kit Document Scanner est **Android-only** : côté iOS il faudrait
      basculer sur `VNDocumentCameraViewController` (VisionKit). Le
      partage de code utile chute hors UI pure.
    - Pertinent uniquement si une **v2 iOS** entre dans le scope.
- **Flutter** : scan via plugins tiers (`cunning_document_scanner`), PDF
  via libs tierces (`pdf`, `printing`), persistance via `sqflite`/`drift`,
  Material 3 dynamic color moins fin qu'en natif. Justifié uniquement si
  Android + iOS dès la v1 avec une équipe Dart.
- **React Native** : New Architecture (Fabric/TurboModules) par défaut
  désormais, mais même problème — plugins ML Kit/PDF/persistance tiers,
  Material 3 non first-class. Non justifié pour une app perso Android-only.

**Conséquences** :
- **Trigger de réévaluation activé** : ADR-8 prend acte de la décision
  d'aller vers Compose Multiplatform.
- Compose Multiplatform doit être réévalué annuellement (suivre les
  releases JetBrains).

**Sources** :
- Google Android Developers — [Compare Compose and View metrics](https://developer.android.com/develop/ui/compose/migrate/compare-metrics).
- JetBrains — [Compose Multiplatform 1.8.0 : iOS Stable](https://blog.jetbrains.com/kotlin/2025/05/compose-multiplatform-1-8-0-released-compose-multiplatform-for-ios-is-stable-and-production-ready/).
- Volpis — [Is Kotlin Multiplatform production-ready in 2026?](https://volpis.com/blog/is-kotlin-multiplatform-production-ready/).
- DEV — [Android UI: Jetpack Compose vs. Views — The Definitive Shift](https://dev.to/trinadhthatakula/android-ui-jetpack-compose-vs-views-the-definitive-shift-and-what-it-means-for-you-3gi0).

### ADR-8 — Migration vers Compose Multiplatform (cible Android, iOS prête)

**Contexte** : ADR-7 identifie Compose Multiplatform comme l'option à
réétudier dès qu'un objectif iOS est plausible. Nous décidons d'engager
la migration tant que l'app est petite — réorganiser plus tard coûterait
plus cher.

**Décision** :
1. Le module `:app` devient `:composeApp` avec le plugin
   `org.jetbrains.kotlin.multiplatform`.
2. Une seule cible active : `androidTarget()`. Les cibles `iosX64()`,
   `iosArm64()`, `iosSimulatorArm64()` sont **commentées dans le Gradle**
   et activables sans changement structurel.
3. UI Compose (`compose.runtime`, `compose.material3`, etc.), ViewModel
   (`lifecycle-viewmodel` KMP), modèle, formatage et resources passent
   en `commonMain`.
4. ML Kit, Room, FileProvider, `PdfStorage` et `MainActivity` restent
   en `androidMain`.
5. Frontière `expect/actual` minimale :
    - `expect class PlatformScanResult` (uri+pages côté Android).
    - `@Composable expect fun rememberDocumentScannerLauncher`.
    - `interface PdfActions` (Android : Intent + FileProvider).
    - `interface ReceiptRepository` (Android : Room + PdfStorage).

**Conséquences** :
- ✅ Le code partageable (UI, ViewModel, modèle, utils) est désormais
  prêt à compiler pour iOS sans rewrite.
- ✅ L'entité Room reste cantonnée à `androidMain` : `Receipt` (domaine)
  n'a aucune annotation plateforme, ce qui ouvre la porte à SQLDelight
  ou Room KMP plus tard.
- ⚠️ Compose Resources remplace `R.string.*` côté Compose. Quelques
  strings strictement Android (manifest, Toasts) restent dans
  `androidMain/res/values/strings.xml`.
- ⚠️ Le dynamic color Android 12+ est désormais **injecté** depuis
  `MainActivity` (les APIs `dynamicLightColorScheme`/`dynamicDarkColorScheme`
  sont Android-only ; `commonMain` reçoit un `ColorScheme?` optionnel).
- ⚠️ Activer iOS demandera : décommenter les 3 cibles, créer `iosMain`
  avec les `actual` pour scanner (`VNDocumentCameraViewController`),
  PdfActions (`UIActivityViewController`), Repository (SQLDelight +
  `NSFileManager`), et compiler sur macOS.

---

## 9. Gestion des erreurs

| Cas                                                | Stratégie                                         |
|----------------------------------------------------|---------------------------------------------------|
| Play Services absent / trop vieux                  | `ScanOutcome.Failure` → toast en français.        |
| Modèle ML Kit en cours de téléchargement           | Géré par l'activité Play Services elle-même.      |
| Utilisateur annule le scan                         | `ScanOutcome.Cancelled` → toast discret.          |
| `openPdf` mais pas de viewer PDF installé          | `ActivityNotFoundException` → toast.              |
| Échec d'écriture du PDF (disque plein, etc.)       | `IOException` remontée — affichage à prévoir.     |
| DB corrompue                                       | Hors scope v1 (Room gère le re-create normalement).|

À renforcer en v1.1 : centraliser les erreurs dans un canal d'événements
(`SharedFlow<UserMessage>`) plutôt que des toasts dispersés.

---

## 10. Accessibilité

- Toutes les icônes d'action ont une `contentDescription` localisée
  (`@string/action_open`, `action_share`, etc.).
- Tailles tactiles ≥ 48dp (`IconButton` Material 3 par défaut).
- Le FAB Extended affiche le texte « Scanner un ticket » à côté de l'icône
  pour ne pas dépendre d'une icône seule.
- Le ratio de contraste primary/onPrimary suit Material 3 (vérifié pour
  WCAG AA en thème clair et sombre).

À améliorer : tester avec TalkBack en bout-à-bout (scénario : lancer un
scan, ouvrir un PDF).

---

## 11. Test

### 11.1 Couverture cible v1

| Surface                              | Type                              |
|--------------------------------------|-----------------------------------|
| `ReceiptDao` (CRUD)                  | Test d'instrumentation Room (in-memory DB). |
| `PdfStorage` (import / delete)       | Test d'instrumentation (fichier temporaire). |
| `ReceiptViewModel`                   | Test JVM avec `TestDispatcher`.   |
| `ReceiptListScreen`                  | UI test Compose (`ui-test-junit4`). |
| Scan end-to-end                      | Manuel (ML Kit nécessite un device). |

### 11.2 Hors scope automatisé

- L'activité scanner Play Services elle-même.
- Le rendu visuel du PDF généré.

---

## 12. Évolutions envisagées (post-v1)

| Idée                                       | Coût        | Notes                                                    |
|--------------------------------------------|-------------|----------------------------------------------------------|
| OCR du montant et de la date               | Moyen       | ML Kit Text Recognition v2 ; UX d'édition à concevoir.   |
| Catégories (course, restau, transport)     | Faible      | `category: String?` en base, filtre dans la liste.       |
| Recherche plein-texte sur le nom           | Faible      | `LIKE '%...%'` ou Room FTS4 si volume.                   |
| Export ZIP de tous les PDFs                | Faible      | Intent `ACTION_CREATE_DOCUMENT` + zip stream.            |
| Sync cloud (WebDAV, Drive)                 | Élevé       | Auth, conflits, vie privée, modèle de menace.            |
| Preview PDF inline (Renderer Android)      | Moyen       | Écran détail dédié, recyclerView/LazyColumn de bitmaps.  |
| Notifications de rappel d'archivage        | Faible      | WorkManager + heuristiques.                              |

---

## 13. Risques

| Risque                                                | Mitigation                                                  |
|-------------------------------------------------------|-------------------------------------------------------------|
| ML Kit Document Scanner reste en beta longtemps       | Pin sur version connue, suivi releases ; pas de surface API exposée. |
| Désinstallation = perte des PDFs                      | Android Backup actif ; communiquer dans le README et l'app. |
| Saturation disque (utilisateur prolifique)            | Footer avec taille totale (déjà présent) ; à terme, alerte > X Mo. |
| Dépendance Play Services exclut certains appareils    | Documenté ; pas de fallback prévu pour v1.                  |
| Vie privée du contenu des tickets                     | Local-first, pas de réseau ; toute évolution cloud passera par un design séparé. |
