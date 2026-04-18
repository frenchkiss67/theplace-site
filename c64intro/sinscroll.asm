// ============================================================
// SINSCROLL.ASM - Sinus scroll horizontal avec vague verticale
// ============================================================

// --- Tables d'adresses écran par ligne ---
// Adresse basse et haute de chaque ligne écran (25 lignes)
.pc = * "Row Address Tables"

row_addr_lo:
    .fill 25, <(SCREEN_RAM + i * 40)
row_addr_hi:
    .fill 25, >(SCREEN_RAM + i * 40)

color_addr_lo:
    .fill 25, <(COLOR_RAM + i * 40)
color_addr_hi:
    .fill 25, >(COLOR_RAM + i * 40)

// ============================================================
// Initialisation du scroll
// ============================================================
.pc = * "Scroll Code"

init_scroll:
        // Remplir le buffer avec des espaces
        ldx #40
        lda #$20
!loop:
        dex
        sta scroll_buffer,x
        bne !loop-

        // Initialiser le pointeur de texte
        lda #<scroll_text
        sta text_ptr
        lda #>scroll_text
        sta text_ptr + 1

        // Mettre la couleur blanche dans la zone de scroll
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
// Mise à jour du scroll (appelée chaque frame)
// ============================================================
update_scroll:
        // Décrémenter le compteur de smooth scroll
        dec scroll_x
        bpl no_char_advance

        // Le compteur est passé sous 0 → remettre à 7
        lda #7
        sta scroll_x

        // Décaler tout le buffer d'un caractère vers la gauche
        ldx #0
!shift:
        lda scroll_buffer + 1,x
        sta scroll_buffer,x
        inx
        cpx #39
        bne !shift-

        // Lire le prochain caractère du texte
        ldy #0
        lda (text_ptr),y
        bne !not_end-

        // Fin du texte → revenir au début
        lda #<scroll_text
        sta text_ptr
        lda #>scroll_text
        sta text_ptr + 1
        lda (text_ptr),y

!not_end:
        // Stocker le nouveau caractère à droite du buffer
        sta scroll_buffer + 39

        // Avancer le pointeur de texte
        inc text_ptr
        bne !skip-
        inc text_ptr + 1
!skip:

no_char_advance:
        // Placer les caractères sur l'écran avec l'effet sinus
        jsr place_scroll_chars
        rts

// ============================================================
// Placement des caractères avec ondulation sinusoïdale
// ============================================================
place_scroll_chars:
        // Effacer la zone de scroll (lignes SCROLL_BASE_ROW à +SCROLL_NUM_ROWS)
        ldx #39
        lda #$20        // Espace
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

        // Pour chaque colonne (0-39): placer le caractère à la bonne ligne
        ldx #0          // X = colonne courante
!place_loop:
        stx temp_x

        // Calculer l'index dans la table sinus
        // index = (colonne × SIN_SPACING + sin_phase) & $FF
        txa
        .for (var s = 0; s < 2; s++) {  // × 4 (SIN_SPACING=4)
            asl
        }
        clc
        adc sin_phase
        tay             // Y = index dans sin_table

        // Lire le déplacement vertical (0 à SCROLL_NUM_ROWS-1)
        lda sin_table,y

        // Calculer la ligne écran = SCROLL_BASE_ROW + déplacement
        clc
        adc #SCROLL_BASE_ROW

        // Charger l'adresse de cette ligne écran via la table de lookup
        tax             // X = numéro de ligne
        lda row_addr_lo,x
        sta $fb
        lda row_addr_hi,x
        sta $fc

        // Lire le caractère du buffer et le placer
        ldx temp_x      // Restaurer le numéro de colonne
        ldy temp_x      // Y = colonne pour l'adressage (zp),Y
        lda scroll_buffer,x
        sta ($fb),y

        // Colonne suivante
        inx
        cpx #40
        bne !place_loop-

        rts

// ============================================================
// Texte du scroll (encodé en screen codes C64 uppercase)
// ============================================================
.encoding "screencode_upper"

.pc = * "Scroll Text"

scroll_text:
        .text "     BIENVENUE DANS BRAINWAVE !!!"
        .text "     UNE INTRO COMMODORE 64 EN ASSEMBLEUR 6510..."
        .text "     GREETINGS A TOUS LES SCENERS DE POUET.NET ET TRANSMISSION 64 !!!"
        .text "     EFFETS: LOGO BITMAP + COLOR WASH + RASTER BARS + PETSCII PLASMA"
        .text " + SPRITES LISSAJOUS + SINUS SCROLL..."
        .text "     DESIGN ET CODE PAR CLAUDE --- 2026 ---"
        .text "     SALUTATIONS A LA SCENE DEMO FRANCAISE... VIVE LE C64 !!!          "
        .byte 0

.encoding "petscii_mixed"
