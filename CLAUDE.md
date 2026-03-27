# Commodore 64 Intro — Projet Assembleur 6502/6510

## Description du projet

Intro-demo style Commodore 64 en assembleur 6510, comportant trois effets classiques de la scène demo :

1. **Logo en haut de l'écran** — Un logo bitmap affiché en mode hi-res ou multicolor dans la partie supérieure de l'écran
2. **Raster bars** — Barres de couleurs horizontales animées dans le border et/ou la zone d'écran, synchronisées avec le raster beam via des interruptions IRQ
3. **Sinus scroll** — Texte défilant horizontalement avec mouvement sinusoïdal vertical (wave effect), utilisant des caractères software-scrollés

## Architecture technique

### Mémoire C64
- **$0400–$07FF** : Écran texte (screen RAM)
- **$D000–$D3FF** : Registres VIC-II (video chip)
- **$D400–$D7FF** : Registres SID (son, optionnel)
- **$D800–$DBFF** : Color RAM
- **$C000–$CFFF** : Zone libre pour le code principal
- **$2000–$3FFF** : Bitmap du logo (8 Ko)
- **$E000–$FFFF** : Zone alternative pour bitmap si le Kernal est désactivé

### Registres VIC-II essentiels
| Registre | Adresse | Rôle |
|----------|---------|------|
| `$D011` | SCROLY | Contrôle vertical scroll (bits 0-2), mode bitmap (bit 5), écran on/off (bit 4) |
| `$D012` | RASTER | Ligne raster courante / ligne de déclenchement IRQ |
| `$D016` | SCROLX | Contrôle horizontal scroll (bits 0-2), mode multicolor (bit 4), 38/40 colonnes (bit 3) |
| `$D018` | VMCSB | Pointeur mémoire vidéo et jeu de caractères |
| `$D020` | EXTCOL | Couleur du border |
| `$D021` | BGCOL0 | Couleur du fond |
| `$D019` | IRQFLAG | Flag d'interruption raster |
| `$D01A` | IRQMASK | Masque d'interruption raster |

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
;   IRQ1 (ligne ~$00)  → Afficher le logo bitmap en haut
;   IRQ2 (ligne ~$60)  → Déclencher les raster bars
;   IRQ3 (ligne ~$F8)  → Zone du sinus scroll en mode texte
;   Chaque IRQ programme le suivant avant de faire RTI
```

## Conventions de code

### Assembleur
- **Assembleur cible** : KickAssembler (`.asm` files) ou ACME (`.a` files)
- **Syntaxe** : Opcodes 6502/6510 en minuscules (`lda`, `sta`, `jsr`, etc.)
- **Labels** : snake_case pour les labels, MAJUSCULES pour les constantes
- **Commentaires** : En français, chaque routine documentée avec son rôle

### Nommage des fichiers
```
c64intro/
├── main.asm          # Point d'entrée, init, boucle principale
├── irq.asm           # Configuration et chaîne d'interruptions raster
├── logo.asm          # Affichage du logo bitmap
├── rasterbars.asm    # Effet raster bars (tables de couleurs)
├── sinscroll.asm     # Sinus scroll (défilement + wave)
├── tables.asm        # Tables précalculées (sinus, couleurs)
├── charset.asm       # Jeu de caractères custom pour le scroll
├── data/
│   ├── logo.kla      # Logo au format Koala Painter (ou .prg)
│   └── font.bin      # Police de caractères 8x8
├── build.sh          # Script de compilation
└── Makefile          # Build avec make
```

## Détails des effets

### 1. Logo bitmap (haut de l'écran)

- Mode bitmap multicolor ($D011 bit 5 = 1, $D016 bit 4 = 1)
- Résolution : 160×200 en multicolor ou 320×200 en hi-res
- Affiché sur les ~100 premières lignes raster
- Basculer en mode texte avant la zone du scroll via IRQ

### 2. Raster bars

```asm
; Principe : modifier $D020/$D021 à chaque ligne raster
; pour créer des barres de couleurs animées
;
; Technique de stabilisation raster :
;   - Double IRQ ou NOP-slide pour synchronisation cycle-exact
;   - Chaque ligne raster = 63 cycles sur PAL
;
; Table de couleurs : séquence de valeurs 0-15 décalée
; à chaque frame pour l'animation
raster_colors:
    .byte $00,$06,$0e,$03,$01,$03,$0e,$06  ; dégradé bleu
    .byte $00,$09,$08,$07,$01,$07,$08,$09  ; dégradé marron/jaune
```

### 3. Sinus scroll

```asm
; Le texte défile de droite à gauche (hardware scroll $D016 bits 0-2)
; Chaque caractère est positionné verticalement selon une table sinus
;
; Étapes par frame :
;   1. Décrémenter le smooth scroll horizontal ($D016)
;   2. Quand scroll = 0, décaler tous les caractères d'une colonne
;   3. Insérer le nouveau caractère à droite
;   4. Appliquer la table sinus pour le placement vertical
;
; Table sinus (256 valeurs, amplitude ~4 lignes de caractères)
sin_table:
    .fill 256, round(3.5 + 3.5 * sin(toRadians(i * 360 / 256)))
```

## Compilation et exécution

```bash
# Avec KickAssembler
java -jar KickAss.jar main.asm -o intro.prg

# Avec ACME
acme -f cbm -o intro.prg main.asm

# Exécuter avec VICE (émulateur C64)
x64sc intro.prg
```

## Contraintes techniques à respecter

- **Timing raster** : Le C64 PAL a 312 lignes raster, 63 cycles par ligne. Les changements de couleur et de mode vidéo doivent être synchronisés au cycle près
- **Pas de flickering** : Tous les changements visuels doivent se faire pendant les bonnes lignes raster
- **Taille mémoire** : Le programme complet (code + données) doit tenir dans la RAM disponible (~38 Ko si Kernal désactivé)
- **Compatibilité PAL** : Cibler le timing PAL (système européen, standard pour la scène demo C64)

## Ressources de référence

- Registres VIC-II : mapping complet dans `$D000-$D03F`
- SID pour la musique : `$D400-$D41C` (optionnel, ajout possible d'un SID tune)
- Tables sinus : précalculer pour éviter tout calcul en temps réel
- La valeur `$01` (registre de bank switching 6510) contrôle la visibilité du Kernal/BASIC/I-O
