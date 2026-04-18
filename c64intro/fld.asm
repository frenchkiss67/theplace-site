// ============================================================
// FLD.ASM - Effet d'entrée du logo (fade-in + slide)
// ============================================================

// Palette de fade: noir → gris → bleu → bleu clair → blanc
.pc = * "FLD Tables"

fade_palette:
    .byte $00, $00, $0b, $0b, $0c, $0c, $0f, $0f
    .byte $06, $06, $0e, $0e, $03, $03, $01, $01

.const FADE_STEPS = 16

// ============================================================
// Animation d'entrée du logo
// Appelée une seule fois au démarrage, avant la boucle principale
// ============================================================
.pc = * "FLD Code"

fld_entrance:
        lda #0
        sta fld_step

!next_frame:
        // Attendre le VBlank (ligne $F8)
!wait_vbl:
        lda VIC_D012
        cmp #$f8
        bne !wait_vbl-

        // Couleur courante du fade
        ldx fld_step
        lda fade_palette,x
        asl
        asl
        asl
        asl                     // Décaler en high nibble (foreground)

        // Appliquer à toute la zone du logo (200 cellules)
        ldy #0
!set_color:
        sta BITMAP_SCR + 80,y
        iny
        cpy #200
        bne !set_color-

        // Petite pause (2 frames entre chaque step)
!wait2:
        lda VIC_D012
        cmp #$30
        bne !wait2-
!wait3:
        lda VIC_D012
        cmp #$f8
        bne !wait3-

        // Step suivant
        inc fld_step
        lda fld_step
        cmp #FADE_STEPS
        bne !next_frame-

        rts
