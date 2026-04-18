// ============================================================
// SPRITES.ASM - Sprites bouncing en courbe de Lissajous
// Style Transmission 64 / scene demo C64
//
// 8 hardware sprites formant une chaîne de balles colorées
// qui tracent une figure de Lissajous (fréquence 2:3).
// Chaque sprite est décalé de 32 positions dans la phase,
// créant un effet de "serpent" fluide.
// ============================================================

// --- Données sprite: balle ronde 16×16 pixels ---
// Placées à $0340 (= bloc 13, pointer value = $0D)
// La zone $0340-$037F est le cassette buffer, libre en demo
.pc = $0340 "Sprite Ball Data"

sprite_ball:
    .byte %00000000, %00000000, %00000000   // ligne 0
    .byte %00000000, %01111000, %00000000   // ligne 1
    .byte %00000001, %11111110, %00000000   // ligne 2
    .byte %00000011, %11111111, %00000000   // ligne 3
    .byte %00000111, %11111111, %10000000   // ligne 4
    .byte %00000111, %11111111, %10000000   // ligne 5
    .byte %00001111, %11111111, %11000000   // ligne 6
    .byte %00001111, %11111111, %11000000   // ligne 7
    .byte %00001111, %11111111, %11000000   // ligne 8
    .byte %00001111, %11111111, %11000000   // ligne 9
    .byte %00001111, %11111111, %11000000   // ligne 10
    .byte %00000111, %11111111, %10000000   // ligne 11
    .byte %00000111, %11111111, %10000000   // ligne 12
    .byte %00000011, %11111111, %00000000   // ligne 13
    .byte %00000001, %11111110, %00000000   // ligne 14
    .byte %00000000, %01111000, %00000000   // ligne 15
    .byte %00000000, %00000000, %00000000   // ligne 16
    .byte %00000000, %00000000, %00000000   // ligne 17
    .byte %00000000, %00000000, %00000000   // ligne 18
    .byte %00000000, %00000000, %00000000   // ligne 19
    .byte %00000000, %00000000, %00000000   // ligne 20
    .byte $00                               // padding (64e octet)

// Deuxième sprite: petite étoile/diamant
.pc = $0380 "Sprite Star Data"

sprite_star:
    .byte %00000000, %00000000, %00000000   // ligne 0
    .byte %00000000, %01000000, %00000000   // ligne 1
    .byte %00000000, %11100000, %00000000   // ligne 2
    .byte %00000001, %11110000, %00000000   // ligne 3
    .byte %00000011, %11111000, %00000000   // ligne 4
    .byte %00000001, %11110000, %00000000   // ligne 5
    .byte %00000000, %11100000, %00000000   // ligne 6
    .byte %00000000, %01000000, %00000000   // ligne 7
    .byte %00000000, %00000000, %00000000   // ligne 8
    .byte %00000000, %00000000, %00000000   // ligne 9
    .byte %00000000, %00000000, %00000000   // ligne 10
    .byte %00000000, %00000000, %00000000   // ligne 11
    .byte %00000000, %00000000, %00000000   // ligne 12
    .byte %00000000, %00000000, %00000000   // ligne 13
    .byte %00000000, %00000000, %00000000   // ligne 14
    .byte %00000000, %00000000, %00000000   // ligne 15
    .byte %00000000, %00000000, %00000000   // ligne 16
    .byte %00000000, %00000000, %00000000   // ligne 17
    .byte %00000000, %00000000, %00000000   // ligne 18
    .byte %00000000, %00000000, %00000000   // ligne 19
    .byte %00000000, %00000000, %00000000   // ligne 20
    .byte $00                               // padding

// Couleurs des 8 sprites (palette arc-en-ciel)
.pc = * "Sprite Tables"

sprite_colors:
    .byte $01, $03, $0d, $05, $07, $08, $02, $0a
    //    wht  cyan ltgr  grn  yel  org  red  pink

// Sprite pointer values: alternance balle/étoile
sprite_ptrs:
    .byte $0d, $0e, $0d, $0e, $0d, $0e, $0d, $0e
    //    ball star ball star ball star ball star

// Tables sinus pour la trajectoire Lissajous
// X: fréquence 2, centre=172, amplitude=80 (range 92-252)
// Y: fréquence 3, centre=150, amplitude=70 (range 80-220)
sprite_sin_x:
    .fill 256, round(172 + 80 * sin(toRadians(i * 360 * 2 / 256)))
sprite_sin_y:
    .fill 256, round(150 + 70 * sin(toRadians(i * 360 * 3 / 256)))

// ============================================================
// Initialisation des sprites
// ============================================================
.pc = * "Sprite Code"

init_sprites:
        // Activer les 8 sprites
        lda #$ff
        sta $d015

        // Sprites devant les caractères/bitmap
        lda #$00
        sta $d01b           // Priority: in front
        sta $d010           // Pas de MSB X (positions < 256)
        sta $d01c           // Hi-res (pas multicolor)

        // Expansion: sprites pairs (0,2,4,6) = taille normale
        //            sprites impairs (1,3,5,7) = expansés (étoiles plus grandes)
        lda #$aa            // Bits 1,3,5,7 = sprites impairs
        sta $d017           // Y expand
        sta $d01d           // X expand

        // Configurer les pointeurs sprite dans les deux screen RAM
        // (text mode à $07F8, bitmap mode à $3FF8)
        ldx #7
!ptrs:
        lda sprite_ptrs,x
        sta $07f8,x
        sta $3ff8,x
        dex
        bpl !ptrs-

        // Couleurs individuelles
        ldx #7
!colors:
        lda sprite_colors,x
        sta $d027,x
        dex
        bpl !colors-

        rts

// ============================================================
// Mise à jour des positions des sprites (chaque frame)
// 8 sprites tracent une courbe de Lissajous
// ============================================================
update_sprites:
        ldy sprite_phase
        ldx #0              // Registre sprite (0,2,4,...14)
        lda #0
        sta $d010           // Reset X MSB

!loop:
        // Position X du sprite
        lda sprite_sin_x,y
        sta $d000,x

        // Position Y du sprite
        lda sprite_sin_y,y
        sta $d001,x

        // Décalage phase pour le prochain sprite
        // 256/8 = 32 → répartition uniforme sur la courbe
        tya
        clc
        adc #32
        tay

        // Registre suivant (+2 car X/Y entrelacés)
        inx
        inx
        cpx #16
        bne !loop-

        // Avancer la phase d'animation
        inc sprite_phase

        rts
