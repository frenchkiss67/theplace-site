// ============================================================
//  tables.asm — Tables pré-calculées pour le zoom scrolltext
//  Assembleur cible : KickAssembler
//  Sources consultées :
//    - https://nurpax.github.io/posts/2018-11-08-c64jasm.html
//    - https://codebase.c64.org/doku.php?id=base:fpd
//    - https://codebase.c64.org/doku.php?id=base:the_double_irq_method
// ============================================================

// -----------------------------
// Constantes VIC-II
// -----------------------------
.const VIC_SCROLY   = $d011   // YSCROLL + mode + écran on/off
.const VIC_RASTER   = $d012   // ligne raster / trigger IRQ
.const VIC_SCROLX   = $d016   // XSCROLL + multicolor + 38col
.const VIC_VMCSB    = $d018   // pointeur screen + charset
.const VIC_IRQFLAG  = $d019   // ACK (écrire $ff)
.const VIC_IRQMASK  = $d01a   // masque IRQ raster
.const VIC_BORDER   = $d020
.const VIC_BGCOL    = $d021

.const CIA1_ICR     = $dc0d   // désactiver interruptions timer
.const CIA2_ICR     = $dd0d

.const IRQ_VECTOR_LO = $fffe  // vecteur IRQ matériel (Kernal off)
.const IRQ_VECTOR_HI = $ffff

// Bornes de la bande de zoom à l'écran (lignes raster PAL)
.const ZOOM_TOP     = $60     // ligne 96  → début bande
.const ZOOM_BOTTOM  = $c8     // ligne 200 → fin bande
.const ZOOM_HEIGHT  = ZOOM_BOTTOM - ZOOM_TOP   // 104 lignes

// -----------------------------
// Table sinus — 256 valeurs, amplitude 0..15
// Utilisée pour moduler le facteur de zoom frame par frame.
// Formule : round(7.5 + 7.5 * sin(2π·i/256))
// -----------------------------
.align $100
sin_zoom:
    .fill 256, round(7.5 + 7.5 * sin(toRadians(i * 360 / 256)))

// -----------------------------
// Table sinus étroite — amplitude 1..6 (plus "doux")
// Sert de facteur de répétition ligne pour le zoom dynamique.
// -----------------------------
.align $100
sin_factor:
    .fill 256, max(1, round(3.5 + 2.5 * sin(toRadians(i * 360 / 256))))

// -----------------------------
// Table YSCROLL pour zoom x2 fixe
// Chaque ligne pixel du char 8x8 est répétée 2 fois :
//   0,0, 1,1, 2,2, 3,3, 4,4, 5,5, 6,6, 7,7  (16 lignes raster)
// Le |$18 préserve bits 3 (écran ON) et 4 (mode texte 25 lignes).
// -----------------------------
yscroll_x2:
    .byte $00|$18, $00|$18
    .byte $01|$18, $01|$18
    .byte $02|$18, $02|$18
    .byte $03|$18, $03|$18
    .byte $04|$18, $04|$18
    .byte $05|$18, $05|$18
    .byte $06|$18, $06|$18
    .byte $07|$18, $07|$18

// -----------------------------
// Table YSCROLL pour zoom x4 fixe — 32 lignes raster par char
// -----------------------------
yscroll_x4:
    .for (var i = 0; i < 8; i++) {
        .for (var j = 0; j < 4; j++) {
            .byte i | $18
        }
    }

// -----------------------------
// Table YSCROLL pour zoom x8 fixe — 64 lignes raster par char
// -----------------------------
yscroll_x8:
    .for (var i = 0; i < 8; i++) {
        .for (var j = 0; j < 8; j++) {
            .byte i | $18
        }
    }

// -----------------------------
// Couleurs pour raster bars (border) — dégradé bleu
// Séquence typique documentée sur Codebase64.
// -----------------------------
bar_colors_blue:
    .byte $00,$06,$0e,$03,$01,$03,$0e,$06
    .byte $00,$06,$0e,$03,$01,$03,$0e,$06

bar_colors_red:
    .byte $00,$09,$02,$08,$0a,$07,$01,$07
    .byte $0a,$08,$02,$09,$00,$09,$02,$08

// -----------------------------
// Table de déplacement horizontal (XSCROLL) — vitesse variable
// Chaque entrée = nombre de pixels à scroller cette frame (0..2).
// -----------------------------
scroll_speed_table:
    .byte 1,1,2,1,1,2,1,2,2,1,1,1,2,1,2,1

// -----------------------------
// Fin des tables
// -----------------------------
tables_end:
