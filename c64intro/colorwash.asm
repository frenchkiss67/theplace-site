// ============================================================
// COLORWASH.ASM - Color wash arc-en-ciel sur le logo bitmap
// Style Transmission 64 / pouet.net
//
// Technique: modifier les couleurs dans le screen RAM bitmap
// ($3C00) à chaque frame pour créer un dégradé qui défile
// diagonalement à travers les lettres du logo.
// ============================================================

// Palette arc-en-ciel pour le wash (16 couleurs cycliques)
// Ordre: bleu → cyan → vert → jaune → orange → rouge → rose → violet → bleu
.pc = * "Color Wash Tables"

wash_palette:
    .byte $06, $0e, $0e, $03, $0d, $05, $07, $07
    .byte $08, $02, $0a, $04, $04, $06, $06, $0e

// Adresses base du screen RAM bitmap pour les 5 rangées du logo
// Logo occupe les char rows 2-6 → offsets 80, 120, 160, 200, 240
logo_scr_lo:
    .byte <(BITMAP_SCR + 80), <(BITMAP_SCR + 120), <(BITMAP_SCR + 160)
    .byte <(BITMAP_SCR + 200), <(BITMAP_SCR + 240)
logo_scr_hi:
    .byte >(BITMAP_SCR + 80), >(BITMAP_SCR + 120), >(BITMAP_SCR + 160)
    .byte >(BITMAP_SCR + 200), >(BITMAP_SCR + 240)

// ============================================================
// Mise à jour du color wash (appelée chaque frame)
// ============================================================
.pc = * "Color Wash Code"

update_colorwash:
        lda #0
        sta wash_row

!next_row:
        // Pointer vers le screen RAM bitmap de cette rangée
        ldx wash_row
        lda logo_scr_lo,x
        sta $fb
        lda logo_scr_hi,x
        sta $fc

        // Parcourir les 40 colonnes
        ldy #39

!col:
        // Index couleur = (colonne + rangée×2 + wash_offset) & $0F
        // Le ×2 sur la rangée crée un effet diagonal
        tya
        clc
        adc wash_row
        adc wash_row
        clc
        adc wash_offset
        and #$0f
        tax

        // Lire la couleur de la palette
        lda wash_palette,x

        // Décaler en nibble haut (= couleur foreground en mode bitmap)
        // Le nibble bas reste à 0 (fond noir)
        asl
        asl
        asl
        asl

        // Écrire dans le screen RAM bitmap
        sta ($fb),y

        dey
        bpl !col-

        // Rangée suivante
        inc wash_row
        lda wash_row
        cmp #5
        bne !next_row-

        // Avancer la phase du wash
        inc wash_offset

        rts
