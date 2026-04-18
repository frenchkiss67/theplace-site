// ============================================================
// BRAINWAVE - Intro Commodore 64
// Assembleur: KickAssembler
// 10 effets: Logo bitmap + Color wash + Tech-tech bars
//            + PETSCII plasma + Sprites Lissajous (×16 mux)
//            + Sinus scroll + Scroll color wash + SID music
//            + Logo wobble + Border opening + Fade-in entrance
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
.const BITMAP_SCR      = $3c00
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
.const IRQ3_LINE       = $f8    // Fin d'écran → border opening + SID

// --- Constantes scroll ---
.const SCROLL_BASE_ROW = 18
.const SCROLL_NUM_ROWS = 7
.const RASTER_BAR_LINES = 48
.const SIN_SPACING     = 4

// ============================================================
// BASIC Upstart ($0801)
// ============================================================
.pc = $0801 "BASIC Upstart"
BasicUpstart2(start)

// ============================================================
// Variables ($1000)
// ============================================================
.pc = $1000 "Variables"

frame_flag:     .byte 0
scroll_x:      .byte 7
text_ptr:       .word scroll_text
sin_phase:      .byte 0
bar_offset:     .byte 0
temp_x:         .byte 0

// PETSCII plasma
petscii_phase1: .byte 0
petscii_phase2: .byte 0
color_cycle:    .byte 0
petscii_row:    .byte 0
temp_row_val1:  .byte 0
temp_row_val2:  .byte 0
temp_sin1:      .byte 0

// Color wash logo
wash_offset:    .byte 0
wash_row:       .byte 0

// Sprites
sprite_phase:   .byte 0
mux_phase:      .byte 0

// SID music
sid_counter:    .byte 1
sid_pos:        .byte 0

// Logo wobble
logo_wobble:    .byte 0

// FLD entrance
fld_step:       .byte 0

// Scroll buffer
scroll_buffer:  .fill 41, $20

// Tech-tech bars: buffer fusionné (lu par l'IRQ raster)
merged_bars:    .fill RASTER_BAR_LINES, 0

// ============================================================
// Code principal ($C000)
// ============================================================
.pc = $c000 "Main Code"

start:
        sei

        lda #$35
        sta $01

        lda #BORDER_COLOR
        sta VIC_BORDER
        lda #BG_COLOR
        sta VIC_BG

        jsr copy_charset
        jsr clear_screen
        jsr setup_logo
        jsr init_petscii
        jsr init_sprites
        jsr init_scroll
        jsr init_sid

        // Activer le mode bitmap pour le fade-in
        lda #$3b
        sta VIC_D011
        lda #$f8
        sta VIC_D018
        lda #$08
        sta VIC_D016

        // Animation d'entrée: fade-in du logo
        jsr fld_entrance

        // Lancer la chaîne IRQ
        jsr setup_irq
        cli

// --- Boucle principale ---
mainloop:
        lda frame_flag
        beq mainloop

        lda #0
        sta frame_flag

        // Color wash arc-en-ciel sur le logo
        jsr update_colorwash

        // Plasma PETSCII
        jsr update_petscii

        // Positions des 16 sprites (2 sets via multiplexeur)
        jsr update_sprites

        // Sinus scroll + color wash texte
        jsr update_scroll

        // Fusionner les barres tech-tech (2 offsets croisés)
        jsr merge_techtech

        // Animer les phases
        inc bar_offset

        lda sin_phase
        clc
        adc #2
        sta sin_phase

        jmp mainloop

// ============================================================
// Fusionner 2 jeux de raster bars (tech-tech croisé)
// Set 1 monte (bar_offset), Set 2 descend (255-bar_offset)
// ============================================================
merge_techtech:
        lda bar_offset
        eor #$ff
        sta tt_off2

        ldx #0
        ldy bar_offset

!merge:
        // Couleur du set 1 (montant)
        lda bar_colors,y
        bne !use1-

        // Si set 1 est noir, prendre le set 2 (descendant)
        sty tt_save_y
        ldy tt_off2
        lda bar_colors,y
        ldy tt_save_y

!use1:
        sta merged_bars,x

        iny
        inc tt_off2
        inx
        cpx #RASTER_BAR_LINES
        bne !merge-

        rts

tt_off2:    .byte 0
tt_save_y:  .byte 0

// --- Effacer l'écran ---
clear_screen:
        ldx #0
        lda #$20
!loop:
        sta SCREEN_RAM,x
        sta SCREEN_RAM + $100,x
        sta SCREEN_RAM + $200,x
        sta SCREEN_RAM + $2e8,x
        inx
        bne !loop-

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
#import "shaderpetscii.asm"
#import "sinscroll.asm"
#import "tables.asm"
#import "sid.asm"
#import "fld.asm"
