# Zoom Scrolltext C64 — Projet Assembleur 6502/6510

## Description du projet

Effet classique de la scène demo Commodore 64 : un **texte défilant horizontalement** (scrolltext) dont les caractères sont **agrandis verticalement** (zoom / stretch raster) via manipulation cycle-exact du VIC-II. Le résultat est un scroll "géant" qui occupe une large bande de l'écran, chaque pixel vertical du caractère étant répété sur plusieurs lignes raster.

Deux variantes couramment implémentées :

1. **Zoom fixe (stretch)** — Chaque ligne raster d'un caractère 8×8 est répétée N fois (p. ex. N=4 → hauteur 32 px). On utilise le registre `$D011` (bits 0-2 YSCROLL) pour forcer le VIC-II à relire la même ligne de char matrix.
2. **Zoom dynamique (sinus zoom)** — Le facteur de répétition varie dans le temps selon une table sinus, produisant un effet "respirant" où le texte grossit/rétrécit à chaque frame.

## Principe technique du zoom raster

Sur C64, le VIC-II n'a pas de mode "scale 2x" natif. On exploite une astuce bien connue :

- Le registre `$D011` bits 0-2 (YSCROLL) décale verticalement la zone de caractères de 0 à 7 pixels.
- Modifier YSCROLL au milieu d'une ligne raster force le VIC-II à **relire la même ligne de la character matrix** depuis le début.
- En synchronisant ces changements avec le faisceau raster (Bad Lines maîtrisées, NOP-slide, double IRQ), on répète chaque ligne pixel autant de fois qu'on veut.

```
Ligne pixel char :  0 1 2 3 4 5 6 7
Zoom x1 (normal) :  L L L L L L L L      → 8 lignes raster
Zoom x4          :  LLLL LLLL LLLL ...   → 32 lignes raster par ligne pixel
```

## Architecture mémoire

| Plage | Usage |
|-------|-------|
| `$0400–$07FF` | Screen RAM (matrice de caractères du scroll) |
| `$0800–$0FFF` | Buffer double (optionnel) pour éviter flicker |
| `$1000–$17FF` | Charset custom (police 8×8 chargée ici) |
| `$2000–$20FF` | Table sinus (zoom factor par frame) |
| `$2100–$21FF` | Table de décalage YSCROLL pré-calculée |
| `$3000–$3FFF` | Texte à scroller (messages, null-terminés) |
| `$C000–$CFFF` | Code principal (init + main loop) |
| `$C100–$C2FF` | Handlers IRQ raster |

### Registres VIC-II clés pour le zoom scrolltext

| Registre | Adresse | Rôle |
|----------|---------|------|
| `$D011` | SCROLY | **YSCROLL (bits 0-2)** — moteur du zoom vertical |
| `$D012` | RASTER | Ligne raster courante / trigger IRQ |
| `$D016` | SCROLX | **XSCROLL (bits 0-2)** — smooth scroll horizontal |
| `$D018` | VMCSB | Pointeur screen RAM + charset |
| `$D019` | IRQFLAG | ACK interruption raster (écrire `#$ff`) |
| `$D01A` | IRQMASK | Masque IRQ raster (bit 0 = 1 pour activer) |
| `$D020` | EXTCOL | Couleur border (optionnel pour les barres) |
| `$D021` | BGCOL0 | Couleur fond |

## Structure des fichiers

```
c64zoomscroll/
├── main.asm          # Init VIC-II, install IRQ, boucle main
├── irq.asm           # Chaîne d'IRQ raster (top + zoom-band + bottom)
├── zoom.asm          # Moteur de zoom : écriture YSCROLL cycle-exact
├── scroll.asm        # Scroll horizontal + avance du texte
├── tables.asm        # Tables sinus, YSCROLL, couleurs
├── charset.asm       # Installation charset custom en $1000
├── text.asm          # Messages scrollés
├── data/
│   ├── font.bin      # Police 8×8 (2 Ko)
│   └── sin.bin       # Table sinus précalculée (256 octets)
├── build.sh
└── Makefile
```

## Boucle IRQ — chaîne typique

```asm
; IRQ_TOP   ($30)   : préparer zone du zoom, couleur fond, stabilisation
; IRQ_ZOOM  ($60)   : entrée de la bande zoom — démarre la répétition YSCROLL
; IRQ_LINE  (chaque ligne de la bande) : écrit YSCROLL selon la table
; IRQ_END   ($C8)   : restaure $D011 normal, ré-arme IRQ_TOP
```

Chaque IRQ :
1. Sauve A/X/Y (`pha`/`txa`/`pha`/`tya`/`pha`)
2. Ack `$D019` ← `#$ff`
3. Programme la prochaine ligne dans `$D012`
4. Pointe le vecteur IRQ `$FFFE` (ou `$0314` si Kernal actif) vers le handler suivant
5. Restaure registres, `rti`

## Moteur de zoom — exemple simplifié

```asm
; Bande de zoom : 8 lignes pixel × facteur de zoom
; Pour zoom x4 on écrit YSCROLL : 0,0,0,0, 1,1,1,1, 2,2,2,2, ... 7,7,7,7

zoom_line:
    ldy #$00                ; index dans la table pré-calculée
.loop:
    ; attendre ligne raster exacte (busy-wait + NOP-slide)
    lda yscroll_table,y
    ora #%00011000          ; conserver bit bitmap/ECM + écran ON
    sta $d011
    ; waste cycles pour atteindre la ligne suivante pile poil
    nop : nop : nop : nop
    iny
    cpy #zoom_band_height
    bne .loop
    rts
```

### Table YSCROLL pour zoom variable

```asm
; Table de 200 octets (hauteur bande max) contenant la valeur YSCROLL
; à écrire à chaque ligne raster. Régénérée chaque frame selon le zoom courant.
;
; Exemple pour zoom x3 :
;   ligne 0..2 → YSCROLL=0   (pixel 0 du char répété 3x)
;   ligne 3..5 → YSCROLL=1
;   ligne 6..8 → YSCROLL=2
;   ...
```

## Scroll horizontal

```asm
; Même principe que le sinscroll classique :
;   1. Décrémenter XSCROLL ($D016 bits 0-2)
;   2. Quand XSCROLL = 0 : XSCROLL ← 7, décaler screen RAM d'une colonne
;   3. Injecter le prochain caractère à la colonne 39 depuis text_ptr
;   4. Avancer text_ptr ; si $00 → revenir au début du message

scroll_step:
    lda $d016
    sec
    sbc #$01
    and #$07
    sta scroll_x
    ora #%11000000          ; préserver bits multicolor/38col
    sta $d016
    lda scroll_x
    cmp #$07
    bne .done               ; pas de wrap cette frame
    jsr shift_screen_left
    jsr fetch_next_char
.done:
    rts
```

## Tables précalculées

```asm
; Sinus zoom factor : amplitude 1..6, 256 valeurs
sin_zoom:
    .fill 256, round(3.5 + 2.5 * sin(toRadians(i * 360 / 256)))

; Couleurs optionnelles pour colorier la bande de zoom
zoom_colors:
    .byte $06,$0e,$03,$01,$03,$0e,$06,$00
```

## Conventions de code

- **Assembleur** : KickAssembler (`.asm`) préféré — macros et `.fill` natifs
- **Opcodes** : minuscules (`lda`, `sta`, `jmp`, `rti`)
- **Labels** : `snake_case`, constantes en MAJUSCULES
- **Commentaires** : français, chaque routine documentée
- **Cycle-exact** : toujours noter en commentaire le nombre de cycles restants avant la prochaine ligne raster quand on fait du timing serré

## Contraintes techniques C64 PAL

- **312 lignes raster, 63 cycles/ligne** (PAL) — toutes les synchros sont sur cette base
- **Bad Lines** : toutes les 8 lignes quand YSCROLL = raster_line & 7, le VIC-II vole 40-43 cycles → **gérer ou éviter** pendant la bande zoom
- **Stabilisation raster** : double IRQ (second IRQ 1 ligne plus bas) + NOP-slide obligatoire pour garantir un timing cycle-exact
- **Désactiver les IRQ Kernal** via `$01` = `#$35` (ou `#$34`) + `sei` ; vecteur IRQ en `$FFFE/$FFFF`
- **Pas de flickering** : toute modification de `$D011`/`$D018` en dehors des bornes de la bande zoom

## Compilation

```bash
# KickAssembler (recommandé)
java -jar KickAss.jar main.asm -o zoomscroll.prg

# ACME
acme -f cbm -o zoomscroll.prg main.asm

# Test avec VICE
x64sc -warp zoomscroll.prg
```

## Étapes de développement conseillées

1. **Squelette IRQ** — afficher une couleur différente sur une bande raster pour valider le timing
2. **Charset + screen statique** — charger la police, afficher un texte fixe
3. **Scroll horizontal simple** — sans zoom, valider le défilement
4. **Zoom fixe x2** — écrire YSCROLL sur chaque ligne de la bande
5. **Zoom variable** — régénérer la table YSCROLL à chaque frame depuis la table sinus
6. **Polish** — couleurs par ligne, border-FX, musique SID (optionnel)

## Ressources

- VIC-II reference : Christian Bauer's *The MOS 6567/6569 Video Controller*
- Codebase 64 (codebase64.org) : articles "YSCROLL trick", "Stable raster", "Big scroller"
- Tables sinus : `sin(i * 2π / 256) * amplitude + offset`, précalculer en Python/KickAssembler

## Fichiers annexes du projet

Le dossier `c64zoomscroll/` contient maintenant :

| Fichier | Contenu |
|---------|---------|
| `CLAUDE.md` | Ce document (architecture, conventions) |
| `references.md` | Liens vérifiés vers code sources & tutoriels C64 |
| `tables.asm` | Constantes VIC-II + tables sinus + tables YSCROLL (zoom x2/x4/x8) + palettes raster bars |
| `irq_stable.asm` | Squelette de la chaîne double-IRQ stable (cycle-exact) |
| `font_cyber.asm` | Police 8×8 futuriste "Cyber Outline" (courbes + angles biseautés) |
| `font_neon.asm` | Police 8×8 futuriste "Neon Digital" (style LED 7-segments) |
| `fonts_preview.txt` | Aperçu ASCII-art des glyphes + mode d'emploi |

## Polices de caractères futuristes

Deux charsets 8×8 livrés avec le projet, tous deux au format standard
C64 (256 glyphes × 8 octets = 2 Ko) prêts à charger en mémoire VIC :

### `font_cyber` — style outline / cyberpunk
Lettres creuses avec contour angulaire, coins biseautés, épaisseur
de trait double pixel pour rester lisible sous stretch vertical.
Adapté aux scrolls lents et aux zoom modérés (x1..x4).

### `font_neon` — style LED digital / 7-segments
Glyphes rectangulaires inspirés des afficheurs LED, segments droits,
idéal pour les zoom forts (x4..x8) et les scrolls rapides où la
lisibilité prime.

### Chargement

```asm
*= $2000
.import source "font_cyber.asm"   // ou font_neon.asm

// dans l'init :
lda #$18        // screen RAM=$0400, charset=$2000
sta $d018
```

### Codes écran utilisés
- `$01..$1A` → A..Z
- `$20` → espace
- `$21` → `!`, `$2C` → `,`, `$2D` → `-`, `$2E` → `.`, `$2F` → `/`
- `$30..$39` → 0..9, `$3A` → `:`, `$3F` → `?`

Exemple de message : `.byte $08,$05,$0c,$0c,$0f,$20,$17,$0f,$12,$0c,$04` = "HELLO WORLD".

Les tables (`sin_zoom`, `sin_factor`, `yscroll_x2/x4/x8`) et le squelette double-IRQ
sont directement issus des techniques documentées sur Codebase64
(`the_double_irq_method`, `fpd`) et sur les papiers Pasi Ojala / Raistlin.

## Sources code C64 référencées

Liste complète dans `references.md`. Principales :

- **[Codebase64](https://codebase.c64.org/)** — wiki de référence scène C64
- **[Codebase64 — Double IRQ method](https://codebase.c64.org/doku.php?id=base:the_double_irq_method)** — base de `irq_stable.asm`
- **[Codebase64 — FPD (Flexible Pixel Distance)](https://codebase.c64.org/doku.php?id=base:fpd)** — base du zoom YSCROLL
- **[0xc64 — flexlinedistance.asm](https://github.com/0xc64/c64/blob/master/raster/flexlinedistance.asm)** — implémentation FLD complète
- **[Raistlin Papers — DXYCP Scrollers](https://c64demo.com/dxycp-scrollers/)** — scrollers à caractères déformés
- **[Slarti64 — 1x2 Font Scroller](https://github.com/Slarti64/C64-Code-Hacking/wiki/1x2-Font-Scroller-with-Music)** — exemple minimal de scroller stretched
- **[Antimon — DYCP (Pasi Ojala)](http://www.antimon.org/dl/c64/code/dycp.txt)** — le classique DYCP
- **[Bumbershoot — Stabilizing the VIC-II Raster](https://bumbershootsoft.wordpress.com/2015/12/29/stabilizing-the-vic-ii-raster/)** — explications cycles
- **[nurpax — BINTRIS part 5 : Bad lines & FLD](https://nurpax.github.io/posts/2018-06-19-bintris-on-c64-part-5.html)** — mise en pratique moderne
