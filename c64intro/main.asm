// ============================================================
// BRAINWAVE - Intro Commodore 64
// Assembleur: KickAssembler
// Effets: Logo bitmap + Color wash + Raster bars + PETSCII plasma
//         + Sprites Lissajous + Sinus scroll
// ============================================================

// --- Configuration ---
.const BORDER_COLOR    = $00    // Noir
.const BG_COLOR        = $00    // Noir
.const LOGO_FG_COLOR   = $0e   // Bleu clair
.const LOGO_BG_COLOR   = $06   // Bleu foncé
.const SCROLL_COLOR    = $01   // Blanc

// --- Adresses mémoire ---
.const SCREEN_RAM      = $0400
.const CHARSET_ADDR    = $0800
.const BITMAP_ADDR     = $2000
.const BITMAP_SCR      = $3c00  // Screen RAM pour mode bitmap
.const COLOR_RAM       = $d800

// --- Registres VIC-II ---
.const VIC_D011        = $d011
.const VIC_D012        = $d012
.const VIC_D016        = $d016
.const VIC_D018        = $d018
.const VIC_D019        = $d019
.const VIC_D01A        = $d01a
.const VIC_BORDER      = $d020
.const VIC_BG          = $d021

// --- Lignes raster pour les IRQ ---
.const IRQ1_LINE       = $30    // Avant zone visible → mode bitmap
.const IRQ2_LINE       = $82    // Après logo → mode texte + rasters
.const IRQ3_LINE       = $f8    // Fin d'écran → frame sync

// --- Constantes scroll ---
.const SCROLL_BASE_ROW = 18     // Première ligne du sinus scroll
.const SCROLL_NUM_ROWS = 7      // Amplitude max du sinus (en lignes texte)
.const RASTER_BAR_LINES = 48    // Nombre de lignes de raster bars
.const SIN_SPACING     = 4      // Espacement dans la table sinus entre colonnes

// ============================================================
// BASIC Upstart ($0801)
// ============================================================
.pc = $0801 "BASIC Upstart"
BasicUpstart2(start)

// ============================================================
// Variables ($1000)
// IMPORTANT: ne pas placer entre $0800-$0FFF (zone charset)
// ============================================================
.pc = $1000 "Variables"

frame_flag:     .byte 0
scroll_x:      .byte 7         // Smooth scroll horizontal (7→0)
text_ptr:       .word scroll_text
sin_phase:      .byte 0        // Phase courante du sinus scroll
bar_offset:     .byte 0        // Offset animation raster bars
temp_x:         .byte 0        // Variable temporaire

// Variables PETSCII plasma
petscii_phase1: .byte 0        // Phase onde 1 du plasma
petscii_phase2: .byte 0        // Phase onde 2 du plasma
color_cycle:    .byte 0        // Offset de color cycling
petscii_row:    .byte 0        // Compteur ligne courante (temp)
temp_row_val1:  .byte 0        // Offset ligne onde 1 (temp)
temp_row_val2:  .byte 0        // Offset ligne onde 2 (temp)
temp_sin1:      .byte 0        // Valeur sinus temporaire

// Variables color wash
wash_offset:    .byte 0        // Phase du color wash logo
wash_row:       .byte 0        // Compteur rangée wash (temp)

// Variables sprites
sprite_phase:   .byte 0        // Phase animation Lissajous

// Buffer des 40 caractères affichés à l'écran
scroll_buffer:  .fill 41, $20  // 40 + 1 extra, initialisé avec espaces

// ============================================================
// Code principal ($C000)
// ============================================================
.pc = $c000 "Main Code"

start:
        sei

        // Désactiver le BASIC et le Kernal ROM
        lda #$35
        sta $01

        // Couleurs de base
        lda #BORDER_COLOR
        sta VIC_BORDER
        lda #BG_COLOR
        sta VIC_BG

        // Copier le charset ROM → RAM à $0800
        jsr copy_charset

        // Effacer l'écran texte
        jsr clear_screen

        // Initialiser le logo bitmap
        jsr setup_logo

        // Initialiser l'animation PETSCII plasma
        jsr init_petscii

        // Initialiser les sprites bouncing
        jsr init_sprites

        // Initialiser le sinus scroll
        jsr init_scroll

        // Configurer la chaîne d'interruptions raster
        jsr setup_irq

        cli

// --- Boucle principale (synchronisée par IRQ) ---
mainloop:
        lda frame_flag
        beq mainloop

        lda #0
        sta frame_flag

        // Color wash arc-en-ciel sur le logo
        jsr update_colorwash

        // Mettre à jour le plasma PETSCII
        jsr update_petscii

        // Mettre à jour les sprites bouncing (Lissajous)
        jsr update_sprites

        // Mettre à jour le sinus scroll
        jsr update_scroll

        // Animer les raster bars (décalage de la table de couleurs)
        inc bar_offset

        // Animer la phase du sinus scroll (vitesse = 2 par frame)
        lda sin_phase
        clc
        adc #2
        sta sin_phase

        jmp mainloop

// --- Effacer l'écran texte ($0400) et la color RAM ---
clear_screen:
        ldx #0
        lda #$20        // Espace (screen code)
!loop:
        sta SCREEN_RAM,x
        sta SCREEN_RAM + $100,x
        sta SCREEN_RAM + $200,x
        sta SCREEN_RAM + $2e8,x
        inx
        bne !loop-

        // Color RAM à noir
        lda #$00
!color:
        sta COLOR_RAM,x
        sta COLOR_RAM + $100,x
        sta COLOR_RAM + $200,x
        sta COLOR_RAM + $2e8,x
        inx
        bne !color-
        rts

// ============================================================
// Inclure les modules
// ============================================================
#import "charset.asm"
#import "irq.asm"
#import "logo.asm"
#import "colorwash.asm"
#import "sprites.asm"
#import "rasterbars.asm"
#import "petscii.asm"
#import "sinscroll.asm"
#import "tables.asm"
