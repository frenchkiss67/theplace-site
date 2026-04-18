// ============================================================
// SHADERPETSCII.ASM - Shader PETSCII: tunnel + plasma radial
// Style demoscene pouet.net / Transmission 64
//
// Technique: tables distance/angle précalculées (320 octets
// chacune) combinées avec 3 couches sinusoïdales pour créer
// un effet tunnel/plasma organique en PETSCII.
//
// Formule par cellule:
//   v1 = sin_table[ distance(row,col) + phase_zoom ]
//   v2 = sin_table2[ angle(row,col) + phase_rotate ]
//   v3 = sin_table[ col×5 + phase_wave ]
//   value = (v1 + v2 + v3 + color_cycle) & $0F
//   char  = shader_gradient[value]
//   color = shader_palette[value]
//
// Zone: lignes écran 10-17 (8 lignes × 40 colonnes = 320 cellules)
// ============================================================

// --- Constantes du shader ---
.const SHADER_CX = 20          // Centre X du tunnel
.const SHADER_CY = 4           // Centre Y du tunnel (relatif à la zone)
.const SHADER_ROWS = 8         // Nombre de lignes
.const SHADER_COLS = 40        // Nombre de colonnes
.const DIST_SCALE = 10         // Facteur d'échelle pour la distance
.const ANGLE_SCALE = 40.743    // 256 / (2 × PI)

// ============================================================
// Tables précalculées par KickAssembler (compile-time)
// ============================================================
.pc = * "Shader Tables"

// Table de distance: sqrt((col-CX)² + (row-CY)²×4) × DIST_SCALE
// 320 octets (8 lignes × 40 colonnes)
distance_table:
    .fill SHADER_ROWS * SHADER_COLS, {
        .var col = mod(i, SHADER_COLS)
        .var row = floor(i / SHADER_COLS)
        .var dx = col - SHADER_CX
        .var dy = (row - SHADER_CY) * 2.5
        .var dist = sqrt(dx * dx + dy * dy)
        round(dist * DIST_SCALE) & $ff
    }

// Table d'angle: atan2(dy, dx) converti en 0-255
// 320 octets (8 lignes × 40 colonnes)
angle_table:
    .fill SHADER_ROWS * SHADER_COLS, {
        .var col = mod(i, SHADER_COLS)
        .var row = floor(i / SHADER_COLS)
        .var dx = col - SHADER_CX
        .var dy = (row - SHADER_CY) * 2.5
        .if (dx == 0 && dy == 0)
            0
        .else
            round((atan2(dy, dx) + PI) * ANGLE_SCALE) & $ff
    }

// Pointeurs base des tables par ligne (pour adressage indirect)
dist_row_lo:
    .fill SHADER_ROWS, <(distance_table + i * SHADER_COLS)
dist_row_hi:
    .fill SHADER_ROWS, >(distance_table + i * SHADER_COLS)

angle_row_lo:
    .fill SHADER_ROWS, <(angle_table + i * SHADER_COLS)
angle_row_hi:
    .fill SHADER_ROWS, >(angle_table + i * SHADER_COLS)

// Offset colonne pour la 3e onde (linéaire horizontale, pas=5)
shader_col_offset:
    .fill SHADER_COLS, (i * 5) & $ff

// Gradient de caractères PETSCII (16 niveaux, symétrique)
// Densité croissante puis décroissante pour un motif pulsant
shader_gradient:
    .byte $20               //  0 - espace
    .byte $2e               //  1 - .
    .byte $3a               //  2 - :
    .byte $2b               //  3 - +
    .byte $51               //  4 - ● cercle
    .byte $66               //  5 - ▒ checker
    .byte $62               //  6 - ▄ demi bas
    .byte $a0               //  7 - █ bloc plein
    .byte $a0               //  8 - █ bloc plein
    .byte $61               //  9 - ▀ demi haut
    .byte $66               // 10 - ▒ checker
    .byte $51               // 11 - ● cercle
    .byte $2b               // 12 - +
    .byte $3a               // 13 - :
    .byte $2e               // 14 - .
    .byte $20               // 15 - espace

// Palette de couleurs (16 niveaux, dégradé spectral)
shader_palette:
    .byte $00               //  0 - noir
    .byte $0b               //  1 - gris foncé
    .byte $06               //  2 - bleu foncé
    .byte $0e               //  3 - bleu clair
    .byte $03               //  4 - cyan
    .byte $0d               //  5 - vert clair
    .byte $07               //  6 - jaune
    .byte $01               //  7 - blanc
    .byte $01               //  8 - blanc
    .byte $07               //  9 - jaune
    .byte $0d               // 10 - vert clair
    .byte $03               // 11 - cyan
    .byte $0e               // 12 - bleu clair
    .byte $06               // 13 - bleu foncé
    .byte $0b               // 14 - gris foncé
    .byte $00               // 15 - noir

// 2e table sinus (amplitude 0-15, pour la composante distance)
shader_sin2:
    .fill 256, round(7.5 + 7.5 * sin(toRadians(i * 360 / 256)))

// ============================================================
// Pointeurs zero page pour le shader
// ============================================================
.const ZP_DIST  = $f7           // Pointeur table distance
.const ZP_ANGLE = $f9           // Pointeur table angle

// ============================================================
// Initialisation du shader PETSCII
// ============================================================
.pc = * "Shader Code"

init_petscii:
        // Effacer la zone du shader (rows 10-17)
        ldx #39
        lda #$20
!clear:
        .for (var r = 10; r < 18; r++) {
            sta SCREEN_RAM + r * 40,x
        }
        dex
        bpl !clear-

        // Couleurs initiales: noir
        ldx #39
        lda #$00
!color:
        .for (var r = 10; r < 18; r++) {
            sta COLOR_RAM + r * 40,x
        }
        dex
        bpl !color-

        rts

// ============================================================
// Mise à jour du shader PETSCII (appelée chaque frame)
//
// Coût: ~75 cycles/cellule × 320 = ~24000 cycles (~1.2 frames)
// Le shader tourne effectivement à ~25 fps, ce qui est standard
// pour les effets PETSCII complexes sur C64.
// ============================================================
update_petscii:
        lda #0
        sta petscii_row

!next_row:
        // --- Pointeurs écran et Color RAM ---
        ldx petscii_row
        txa
        clc
        adc #10
        tax

        lda row_addr_lo,x
        sta $fb
        lda row_addr_hi,x
        sta $fc
        lda color_addr_lo,x
        sta $fd
        lda color_addr_hi,x
        sta $fe

        // --- Pointeurs distance et angle pour cette ligne ---
        ldx petscii_row
        lda dist_row_lo,x
        sta ZP_DIST
        lda dist_row_hi,x
        sta ZP_DIST + 1
        lda angle_row_lo,x
        sta ZP_ANGLE
        lda angle_row_hi,x
        sta ZP_ANGLE + 1

        // --- Boucle sur les 40 colonnes ---
        ldy #39

!col_loop:
        // Composante 1: distance radiale → effet tunnel zoom
        lda (ZP_DIST),y
        clc
        adc petscii_phase2          // Phase zoom (incrémente → tunnel s'enfonce)
        tax
        lda shader_sin2,x           // Valeur 0-15
        sta temp_sin1

        // Composante 2: angle radial → effet tunnel rotation
        lda (ZP_ANGLE),y
        clc
        adc petscii_phase1          // Phase rotation
        tax
        lda sin_table,x             // Valeur 0-6

        // Composante 3: onde linéaire horizontale → vague latérale
        clc
        adc temp_sin1

        // Ajouter l'onde horizontale
        sty temp_row_val2           // Sauvegarder Y (colonne)
        tay
        lda shader_col_offset,y     // Hmm, Y contient la somme, pas la colonne!

        // Correction: utiliser la colonne sauvegardée
        ldy temp_row_val2           // Restaurer la colonne dans Y
        // Recalculer: ajouter l'offset colonne
        pha                         // Sauvegarder la somme partielle
        lda shader_col_offset,y     // Offset colonne pour onde 3
        clc
        adc petscii_phase1          // Même phase pour effet lié
        tax
        lda sin_table,x             // Valeur 0-6 (onde horizontale)
        sta temp_row_val1           // Stocker temporairement

        pla                         // Récupérer la somme partielle
        clc
        adc temp_row_val1           // Ajouter l'onde horizontale
        clc
        adc color_cycle             // Color cycling global
        and #$0f                    // 16 niveaux
        tax

        // Écrire le caractère PETSCII
        lda shader_gradient,x
        sta ($fb),y

        // Écrire la couleur
        lda shader_palette,x
        sta ($fd),y

        // Colonne suivante
        dey
        bpl !col_loop-

        // --- Ligne suivante ---
        inc petscii_row
        lda petscii_row
        cmp #SHADER_ROWS
        bne !next_row-

        // --- Avancer les phases ---
        // Phase 1 (rotation + onde): mouvement lent
        lda petscii_phase1
        clc
        adc #1
        sta petscii_phase1

        // Phase 2 (zoom tunnel): mouvement plus rapide, sens inverse
        lda petscii_phase2
        sec
        sbc #2
        sta petscii_phase2

        // Color cycling
        inc color_cycle

        rts
