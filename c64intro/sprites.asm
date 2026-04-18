// ============================================================
// SPRITES.ASM - 16 sprites via multiplexeur (2 × 8 hardware)
// Set 1 (zone haute): Lissajous 2:3, Y = 65-135
// Set 2 (zone basse): Lissajous 3:2, Y = 190-230
// ============================================================

// --- Données sprite: balle ronde ---
.pc = $1040 "Sprite Ball Data"

sprite_ball:
    .byte %00000000, %00000000, %00000000
    .byte %00000000, %01111000, %00000000
    .byte %00000001, %11111110, %00000000
    .byte %00000011, %11111111, %00000000
    .byte %00000111, %11111111, %10000000
    .byte %00000111, %11111111, %10000000
    .byte %00001111, %11111111, %11000000
    .byte %00001111, %11111111, %11000000
    .byte %00001111, %11111111, %11000000
    .byte %00001111, %11111111, %11000000
    .byte %00001111, %11111111, %11000000
    .byte %00000111, %11111111, %10000000
    .byte %00000111, %11111111, %10000000
    .byte %00000011, %11111111, %00000000
    .byte %00000001, %11111110, %00000000
    .byte %00000000, %01111000, %00000000
    .byte %00000000, %00000000, %00000000
    .byte %00000000, %00000000, %00000000
    .byte %00000000, %00000000, %00000000
    .byte %00000000, %00000000, %00000000
    .byte %00000000, %00000000, %00000000
    .byte $00

// --- Données sprite: étoile/diamant ---
.pc = $1080 "Sprite Star Data"

sprite_star:
    .byte %00000000, %00000000, %00000000
    .byte %00000000, %01000000, %00000000
    .byte %00000000, %11100000, %00000000
    .byte %00000001, %11110000, %00000000
    .byte %00000011, %11111000, %00000000
    .byte %00000001, %11110000, %00000000
    .byte %00000000, %11100000, %00000000
    .byte %00000000, %01000000, %00000000
    .byte %00000000, %00000000, %00000000
    .byte %00000000, %00000000, %00000000
    .byte %00000000, %00000000, %00000000
    .byte %00000000, %00000000, %00000000
    .byte %00000000, %00000000, %00000000
    .byte %00000000, %00000000, %00000000
    .byte %00000000, %00000000, %00000000
    .byte %00000000, %00000000, %00000000
    .byte %00000000, %00000000, %00000000
    .byte %00000000, %00000000, %00000000
    .byte %00000000, %00000000, %00000000
    .byte %00000000, %00000000, %00000000
    .byte %00000000, %00000000, %00000000
    .byte $00

// --- Tables ---
.pc = * "Sprite Tables"

sprite_colors_top:
    .byte $01, $03, $0d, $05, $07, $08, $02, $0a

sprite_colors_bottom:
    .byte $0a, $02, $08, $07, $05, $0d, $03, $01

sprite_ptrs:
    .byte $41, $42, $41, $42, $41, $42, $41, $42

// Tables sinus partagées pour X (les 2 sets utilisent le même X)
sprite_sin_x:
    .fill 256, round(172 + 70 * sin(toRadians(i * 360 * 2 / 256)))

// Set 1 (zone haute): Y centré à 100, amplitude 35
sprite_sin_y1:
    .fill 256, round(100 + 35 * sin(toRadians(i * 360 * 3 / 256)))

// Set 2 (zone basse): Y centré à 210, amplitude 20
sprite_sin_y2:
    .fill 256, round(210 + 20 * sin(toRadians(i * 360 * 2 / 256)))

// Buffer positions set 2 (rempli par update_sprites, lu par irq_mux)
sprite2_x: .fill 8, 0
sprite2_y: .fill 8, 0

// ============================================================
// Initialisation des sprites
// ============================================================
.pc = * "Sprite Code"

init_sprites:
        lda #$ff
        sta $d015               // Activer les 8 sprites

        lda #$00
        sta $d01b               // Sprites devant
        sta $d010               // Pas de MSB X
        sta $d01c               // Hi-res

        // Expansion sur les sprites impairs (étoiles)
        lda #$aa
        sta $d017               // Y expand
        sta $d01d               // X expand

        // Pointeurs sprite dans les deux screen RAM
        ldx #7
!ptrs:
        lda sprite_ptrs,x
        sta $07f8,x
        sta $3ff8,x
        dex
        bpl !ptrs-

        // Couleurs initiales (set 1)
        ldx #7
!colors:
        lda sprite_colors_top,x
        sta $d027,x
        dex
        bpl !colors-

        rts

// ============================================================
// Mise à jour des positions (16 sprites virtuels)
// Set 1 → registres VIC directs
// Set 2 → buffer sprite2_x/y (appliqué par irq_mux)
// ============================================================
update_sprites:
        // --- Set 1 (zone haute) ---
        ldy sprite_phase
        ldx #0
        lda #0
        sta $d010

!loop1:
        lda sprite_sin_x,y
        sta $d000,x
        lda sprite_sin_y1,y
        sta $d001,x

        tya
        clc
        adc #32
        tay
        inx
        inx
        cpx #16
        bne !loop1-

        // --- Set 2 (zone basse) ---
        lda mux_phase
        tay
        ldx #0

!loop2:
        lda sprite_sin_x,y
        sta sprite2_x,x
        lda sprite_sin_y2,y
        sta sprite2_y,x

        tya
        clc
        adc #32
        tay
        inx
        cpx #8
        bne !loop2-

        // Avancer les phases
        inc sprite_phase
        lda mux_phase
        clc
        adc #2              // Set 2 avance plus vite → mouvement différent
        sta mux_phase

        rts

// ============================================================
// Appliquer le set 1 (appelé par irq_top)
// ============================================================
set_sprites_top:
        ldx #7
!colors:
        lda sprite_colors_top,x
        sta $d027,x
        dex
        bpl !colors-
        rts

// ============================================================
// Appliquer le set 2 (appelé par irq_mux)
// ============================================================
set_sprites_bottom:
        // Repositionner les 8 sprites avec les données du set 2
        ldx #0
        ldy #0

!repos:
        lda sprite2_x,x
        sta $d000,y
        lda sprite2_y,x
        sta $d001,y

        iny
        iny
        inx
        cpx #8
        bne !repos-

        // Changer les couleurs pour le set 2
        ldx #7
!colors:
        lda sprite_colors_bottom,x
        sta $d027,x
        dex
        bpl !colors-

        rts
