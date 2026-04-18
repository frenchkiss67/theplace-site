# BRAINWAVE — Intro Commodore 64 en Assembleur 6510

## Description du projet

Intro-demo style Commodore 64 en assembleur 6510, inspirée de la scène demo (pouet.net, Transmission 64), comportant six effets :

1. **Logo bitmap "BRAINWAVE"** — Logo en mode hi-res bitmap avec police bloc 3×5, affiché dans la partie supérieure de l'écran
2. **Color wash** — Dégradé arc-en-ciel (16 couleurs) défilant en diagonale sur les lettres du logo, par modification du screen RAM bitmap ($3C00)
3. **Raster bars** — 4 barres de dégradé (bleu, rouge, vert, jaune) animées via boucle occupée dans l'IRQ, synchronisées au raster beam
4. **PETSCII plasma** — Effet plasma plein écran (8 lignes) utilisant des caractères block PETSCII (espaces, demi-blocs, checker, blocs pleins) avec double onde sinusoïdale et color cycling arc-en-ciel
5. **Sprites Lissajous** — 8 sprites hardware (4 balles + 4 étoiles) traçant une courbe de Lissajous 2:3, avec expansion sur les sprites impairs
6. **Sinus scroll** — Texte défilant horizontalement avec ondulation sinusoïdale verticale par colonne, utilisant le hardware smooth scroll ($D016)

## Architecture technique

### Carte mémoire

| Adresse | Taille | Contenu |
|---------|--------|---------|
| `$0340–$03BF` | 128 o | Données sprites (balle + étoile, 64 octets chacun) |
| `$0400–$07FF` | 1 Ko | Screen RAM (mode texte) |
| `$07F8–$07FF` | 8 o | Pointeurs sprites (mode texte) |
| `$0800–$0FFF` | 2 Ko | Charset copié depuis la ROM Character Generator |
| `$1000–$1040` | ~64 o | Variables et scroll buffer |
| `$2000–$3F3F` | 8 Ko | Bitmap du logo BRAINWAVE |
| `$3C00–$3FE7` | 1000 o | Screen RAM bitmap (couleurs logo / color wash) |
| `$3FF8–$3FFF` | 8 o | Pointeurs sprites (mode bitmap) |
| `$C000+` | variable | Code principal + données (tables, couleurs, texte) |
| `$D000–$D02E` | | Registres VIC-II (vidéo) et sprites |
| `$D800–$DBFF` | 1 Ko | Color RAM |

### Registres VIC-II essentiels

| Registre | Adresse | Rôle |
|----------|---------|------|
| `$D011` | SCROLY | Contrôle vertical scroll (bits 0-2), mode bitmap (bit 5), écran on/off (bit 4) |
| `$D012` | RASTER | Ligne raster courante / ligne de déclenchement IRQ |
| `$D016` | SCROLX | Contrôle horizontal scroll (bits 0-2), mode multicolor (bit 4), 38/40 colonnes (bit 3) |
| `$D018` | VMCSB | Pointeur mémoire vidéo et jeu de caractères / bitmap |
| `$D020` | EXTCOL | Couleur du border |
| `$D021` | BGCOL0 | Couleur du fond |
| `$D019` | IRQFLAG | Flag d'interruption raster |
| `$D01A` | IRQMASK | Masque d'interruption raster |
| `$D000–$D010` | SPx | Positions X/Y et MSB X des 8 sprites |
| `$D015` | SPENA | Activation des sprites |
| `$D017/$D01D` | | Expansion Y/X des sprites |
| `$D027–$D02E` | SPxCOL | Couleurs individuelles des sprites |

### Chaîne d'interruptions raster (IRQ chain)

```
; Point d'entrée : $C000
;
; Initialisation :
;   - Désactiver les interruptions Kernal (SEI, $01 = $35)
;   - Copier le Character ROM vers RAM à $0800
;   - Générer le bitmap logo BRAINWAVE à $2000
;   - Initialiser les sprites, le plasma PETSCII, le scroll
;   - Configurer la chaîne de 3 raster IRQ
;
; Chaîne IRQ :
;   IRQ1 (ligne $30)  → Mode bitmap ($D011=$3B, $D018=$F8)
;                        Affiche le logo BRAINWAVE en haut
;   IRQ2 (ligne $82)  → Mode texte ($D011=$1B, $D018=$12)
;                        Configure le smooth scroll ($D016)
;                        Exécute les raster bars (boucle occupée 48 lignes)
;   IRQ3 (ligne $F8)  → Signale frame_flag pour la boucle principale
;                        Reprogramme IRQ1
;
; Boucle principale (entre les frames) :
;   1. update_colorwash  → arc-en-ciel sur le logo
;   2. update_petscii    → plasma PETSCII (rows 10-17)
;   3. update_sprites    → positions Lissajous
;   4. update_scroll     → smooth scroll + placement sinus
;   5. inc bar_offset    → animation des raster bars
;   6. sin_phase += 2    → ondulation du scroll
```

## Conventions de code

### Assembleur
- **Assembleur cible** : KickAssembler (`.asm` files)
- **Syntaxe** : Opcodes 6502/6510 en minuscules (`lda`, `sta`, `jsr`, etc.)
- **Labels** : snake_case pour les labels, MAJUSCULES pour les constantes
- **Commentaires** : En français, `//` style KickAssembler
- **Variables** : Placées à `$1000+` (jamais entre `$0800-$0FFF` → zone charset)

### Structure des fichiers

```
c64intro/
├── main.asm            # Point d'entrée, init, boucle principale, constantes
├── irq.asm             # Configuration et 3 handlers raster IRQ
├── logo.asm            # Bitmap BRAINWAVE (génération procédurale via .fill)
├── colorwash.asm       # Color wash arc-en-ciel sur le logo bitmap
├── rasterbars.asm      # Tables de couleurs (4×16 bytes × 4 répétitions)
├── petscii.asm         # Plasma PETSCII (double sinus + color cycling)
├── sprites.asm         # 8 sprites bouncing (Lissajous 2:3, balle+étoile)
├── sinscroll.asm       # Sinus scroll + texte défilant
├── tables.asm          # Table sinus 256 octets (amplitude 0-6)
├── charset.asm         # Copie du Character ROM ($D000→$0800)
├── setup-c64-dev.sh    # Installation auto: Java + KickAssembler + VICE + VS Code
├── build.sh            # Script de compilation
└── Makefile            # Build avec make
```

## Détails des effets

### 1. Logo bitmap BRAINWAVE + Color wash

```
   ##  ##   #  ### # # # #  #  # # ###
   # # # # # #  #  ### # # # # # # #
   ##  ##  ###  #  ### ### ### # # ##
   # # #   # #  #  # # ### # #  #  #
   ##  #   # # ### # #  #  # #  #  ###
```

- Mode bitmap hi-res ($D011 bit 5 = 1), résolution 320×200
- Logo sur les char rows 2-6, généré par KickAssembler (`bitmapByte()`)
- Screen RAM bitmap à $3C00 : high nibble = couleur foreground
- Color wash : palette 16 couleurs cyclique, index = `(col + row×2 + offset) & $0F`
- Défilement diagonal par incrémentation de `wash_offset` chaque frame

### 2. Raster bars

```asm
; 4 barres de dégradé × 4 répétitions = 256 octets (wrapping naturel)
; Barre bleue:  $00,$00,$06,$06,$0e,$0e,$03,$01,$03,$0e,$0e,$06,$06,$00,$00,$00
; Barre rouge:  $00,$00,$02,$02,$0a,$0a,$07,$01,$07,$0a,$0a,$02,$02,$00,$00,$00
; Barre verte:  $00,$00,$05,$05,$0d,$0d,$03,$01,$03,$0d,$0d,$05,$05,$00,$00,$00
; Barre jaune:  $00,$00,$09,$09,$08,$08,$07,$01,$07,$08,$08,$09,$09,$00,$00,$00
;
; Boucle occupée dans IRQ2: 48 lignes, ~63 cycles/ligne (NOP padding)
; bar_offset s'incrémente chaque frame → défilement vertical
```

### 3. PETSCII plasma

```asm
; Double onde sinusoïdale avec pas premiers pour interférence organique :
;   Onde 1: col×7 + row×35 + phase1 (pas horizontaux premiers)
;   Onde 2: col×11 + row×23 + phase2 (contre-diagonale)
;   Valeur = (sin_table[idx1] + sin_table2[idx2] + color_cycle) & $0F
;
; 16 niveaux de gradient PETSCII :
;   $20(espace) $2E(.) $51(●) $66(▒) $5F(▶) $62(▄) $61(▀) $A0(█) ...retour
;
; Palette arc-en-ciel: bleu→cyan→vert→jaune→orange→blanc→retour
; Phases: +2 (diagonal) et -3 (contre-diagonal) par frame
```

### 4. Sprites Lissajous

```asm
; 8 sprites hardware formant une chaîne :
;   X = 172 + 80 × sin(2θ)    ; fréquence 2
;   Y = 150 + 70 × sin(3θ)    ; fréquence 3 → figure de Lissajous 2:3
;   Décalage inter-sprite: 32 (= 256/8, répartition uniforme)
;
; Sprites 0,2,4,6: balle pleine (pointer $0D → $0340)
; Sprites 1,3,5,7: étoile/diamant expansée (pointer $0E → $0380)
; Couleurs: blanc, cyan, vert clair, vert, jaune, orange, rouge, rose
```

### 5. Sinus scroll

```asm
; Smooth scroll horizontal via $D016 bits 0-2 (7→0, 38 colonnes)
; À chaque wrap (scroll_x < 0) :
;   - Décaler scroll_buffer[0..38] ← scroll_buffer[1..39]
;   - Lire le prochain caractère du texte (screen codes)
;   - Insérer à scroll_buffer[39]
;
; Placement vertical sinusoïdal :
;   Pour chaque colonne: row = 18 + sin_table[(col×4 + sin_phase) & $FF]
;   Caractère placé via lookup table row_addr_lo/hi[] → adressage ($fb),Y
;
; sin_table: 256 valeurs, round(3 + 3 × sin(i × 2π / 256))
```

## Compilation et exécution

### Installation automatique (Linux/macOS)

```bash
cd c64intro
./setup-c64-dev.sh       # Installe Java, KickAssembler, VICE, extensions VS Code
```

### Compilation manuelle

```bash
# Avec KickAssembler
java -jar KickAss.jar main.asm -o intro.prg

# Exécuter avec VICE (émulateur C64 PAL)
x64sc intro.prg
```

### Dans VS Code

| Action | Raccourci |
|--------|-----------|
| Compiler | `Ctrl+Shift+B` |
| Compiler + Lancer | `Ctrl+Shift+P` → Run Task → "Lancer dans VICE" |
| Build/Run (extension KickAss) | `F6` |

## Contraintes techniques

- **Timing raster** : C64 PAL = 312 lignes, 63 cycles/ligne. Les changements de mode et de couleur doivent respecter le timing raster
- **Variables hors zone charset** : Ne jamais placer de données entre `$0800-$0FFF` (écrasées par la copie du Character ROM)
- **Sprites dans les 2 screen RAM** : Les pointeurs sprites doivent être définis à `$07F8` (mode texte) ET `$3FF8` (mode bitmap)
- **Pas de flickering** : Tous les changements visuels dans les bons handlers IRQ
- **Taille mémoire** : ~38 Ko disponibles (Kernal/BASIC désactivés, $01 = $35)
- **Compatibilité PAL** : Timing européen, standard scène demo C64

## Ressources

- [Codebase64](https://codebase64.org/) — Référence techniques demo C64
- [pouet.net](https://www.pouet.net/) — Productions de la scène demo
- [Transmission 64](https://transmission64.com/) — Demo party C64 en ligne
- Registres VIC-II : `$D000-$D03F`
- SID pour la musique : `$D400-$D41C` (optionnel)
- Registre `$01` (bank switching 6510) : contrôle la visibilité du Kernal/BASIC/I-O
