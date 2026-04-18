// ============================================================
// SINSCROLL.ASM - Sinus scroll + color wash sur le texte
// ============================================================

// --- Tables d'adresses écran par ligne ---
.pc = * "Row Address Tables"

row_addr_lo:
    .fill 25, <(SCREEN_RAM + i * 40)
row_addr_hi:
    .fill 25, >(SCREEN_RAM + i * 40)

color_addr_lo:
    .fill 25, <(COLOR_RAM + i * 40)
color_addr_hi:
    .fill 25, >(COLOR_RAM + i * 40)

// Palette arc-en-ciel pour le color wash du scroll text (16 couleurs)
scroll_wash_palette:
    .byte $01, $01, $03, $0d, $05, $07, $08, $02
    .byte $0a, $02, $08, $07, $05, $0d, $03, $01

// ============================================================
// Initialisation du scroll
// ============================================================
.pc = * "Scroll Code"

init_scroll:
        ldx #40
        lda #$20
!loop:
        dex
        sta scroll_buffer,x
        bne !loop-

        lda #<scroll_text
        sta text_ptr
        lda #>scroll_text
        sta text_ptr + 1

        // Couleurs initiales de la zone de scroll
        ldx #0
        lda #SCROLL_COLOR
!color:
        sta COLOR_RAM + SCROLL_BASE_ROW * 40,x
        sta COLOR_RAM + (SCROLL_BASE_ROW + 1) * 40,x
        sta COLOR_RAM + (SCROLL_BASE_ROW + 2) * 40,x
        sta COLOR_RAM + (SCROLL_BASE_ROW + 3) * 40,x
        sta COLOR_RAM + (SCROLL_BASE_ROW + 4) * 40,x
        sta COLOR_RAM + (SCROLL_BASE_ROW + 5) * 40,x
        sta COLOR_RAM + (SCROLL_BASE_ROW + 6) * 40,x
        inx
        cpx #40
        bne !color-

        rts

// ============================================================
// Mise à jour du scroll
// ============================================================
update_scroll:
        dec scroll_x
        bpl no_char_advance

        lda #7
        sta scroll_x

        ldx #0
!shift:
        lda scroll_buffer + 1,x
        sta scroll_buffer,x
        inx
        cpx #39
        bne !shift-

        ldy #0
        lda (text_ptr),y
        bne !not_end-

        lda #<scroll_text
        sta text_ptr
        lda #>scroll_text
        sta text_ptr + 1
        lda (text_ptr),y

!not_end:
        sta scroll_buffer + 39

        inc text_ptr
        bne !skip-
        inc text_ptr + 1
!skip:

no_char_advance:
        jsr place_scroll_chars
        rts

// ============================================================
// Placement des caractères avec sinus + color wash
// ============================================================
place_scroll_chars:
        // Effacer la zone de scroll
        ldx #39
        lda #$20
!clear:
        sta SCREEN_RAM + (SCROLL_BASE_ROW + 0) * 40,x
        sta SCREEN_RAM + (SCROLL_BASE_ROW + 1) * 40,x
        sta SCREEN_RAM + (SCROLL_BASE_ROW + 2) * 40,x
        sta SCREEN_RAM + (SCROLL_BASE_ROW + 3) * 40,x
        sta SCREEN_RAM + (SCROLL_BASE_ROW + 4) * 40,x
        sta SCREEN_RAM + (SCROLL_BASE_ROW + 5) * 40,x
        sta SCREEN_RAM + (SCROLL_BASE_ROW + 6) * 40,x
        dex
        bpl !clear-

        // Placer chaque caractère avec sinus + couleur arc-en-ciel
        ldx #0
!place_loop:
        stx temp_x

        // Index sinus = (colonne × 4 + sin_phase) & $FF
        txa
        asl
        asl
        clc
        adc sin_phase
        tay

        // Déplacement vertical (0-6)
        lda sin_table,y
        clc
        adc #SCROLL_BASE_ROW

        // Adresse de la ligne écran
        tax
        lda row_addr_lo,x
        sta $fb
        lda row_addr_hi,x
        sta $fc

        // Adresse de la ligne Color RAM (pour le color wash)
        lda color_addr_lo,x
        sta $fd
        lda color_addr_hi,x
        sta $fe

        // Placer le caractère
        ldx temp_x
        ldy temp_x
        lda scroll_buffer,x
        sta ($fb),y

        // --- Color wash sur le texte ---
        // Couleur = palette[(colonne + sin_phase) & $0F]
        tya
        clc
        adc sin_phase
        lsr
        and #$0f
        tax
        lda scroll_wash_palette,x
        ldy temp_x
        sta ($fd),y

        // Colonne suivante
        ldx temp_x
        inx
        cpx #40
        bne !place_loop-

        rts

// ============================================================
// Texte du scroll (screen codes uppercase)
// ============================================================
.encoding "screencode_upper"

.pc = * "Scroll Text"

scroll_text:
        .text "     BIENVENUE DANS BRAINWAVE !!!"
        .text "     UNE INTRO COMMODORE 64 EN ASSEMBLEUR 6510..."
        .text "     GREETINGS A TOUS LES SCENERS DE POUET.NET ET TRANSMISSION 64 !!!"
        .text "     EFFETS: LOGO BITMAP + COLOR WASH + RASTER BARS + PETSCII PLASMA"
        .text " + SPRITES LISSAJOUS + SINUS SCROLL + SID MUSIC + BORDER OPENING..."
        .text "     DESIGN ET CODE PAR CLAUDE --- 2026 ---"
        .text "     SALUTATIONS A LA SCENE DEMO FRANCAISE... VIVE LE C64 !!!          "
        .byte 0

.encoding "petscii_mixed"
