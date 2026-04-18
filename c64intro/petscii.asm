// ============================================================
// PETSCII.ASM - Animation plasma PETSCII
// Style pouet.net / scene demo C64
//
// Technique: combinaison de 2 ondes sinusoïdales (X et Y)
// avec dithering par caractères block PETSCII et color cycling
// Zone: lignes écran 10-17 (entre le logo et le sinus scroll)
// ============================================================

// --- Tables pré-calculées pour le plasma ---
.pc = * "PETSCII Tables"

// Offset colonne: facteur non-linéaire pour motif organique
// Utilise un pas de 7 (nombre premier) pour casser la régularité
col_offset_petscii:
    .fill 40, (i * 7) & $ff

// Offset ligne: grand pas (35) pour variation verticale forte
row_offset_petscii:
    .fill 8, (i * 35) & $ff

// Deuxième série d'offsets pour la 2e onde sinusoïdale
// Pas de 11 (premier) pour interférence avec la 1ère onde
col_offset2_petscii:
    .fill 40, (i * 11) & $ff

row_offset2_petscii:
    .fill 8, (i * 23) & $ff

// Table de caractères PETSCII pour le gradient plasma (16 niveaux)
// Utilise les caractères block/graphic du jeu PETSCII:
//   espace → points → demi-blocs → checker → plein → retour
// Screen codes C64 pour caractères graphiques:
//   $20 = espace        $2e = '.'         $51 = cercle plein
//   $66 = checker       $61 = demi haut   $62 = demi bas
//   $a0 = bloc plein    $5f = triangle
petscii_gradient:
    .byte $20           //  0 - espace (vide)
    .byte $2e           //  1 - point
    .byte $51           //  2 - cercle
    .byte $66           //  3 - checker (damier)
    .byte $5f           //  4 - triangle
    .byte $62           //  5 - demi-bloc bas
    .byte $61           //  6 - demi-bloc haut
    .byte $a0           //  7 - bloc plein
    .byte $61           //  8 - demi-bloc haut (retour)
    .byte $62           //  9 - demi-bloc bas
    .byte $5f           // 10 - triangle
    .byte $66           // 11 - checker
    .byte $51           // 12 - cercle
    .byte $2e           // 13 - point
    .byte $20           // 14 - espace
    .byte $20           // 15 - espace

// Palette de couleurs plasma: dégradé arc-en-ciel cyclique (16 niveaux)
// Palette C64: 0=noir 1=blanc 2=rouge 3=cyan 4=violet
//              5=vert 6=bleu 7=jaune 8=orange 9=marron
//             10=rose 11=gris foncé 12=gris 13=vert clair
//             14=bleu clair 15=gris clair
petscii_palette:
    .byte $06           //  0 - bleu foncé
    .byte $0e           //  1 - bleu clair
    .byte $03           //  2 - cyan
    .byte $0d           //  3 - vert clair
    .byte $05           //  4 - vert
    .byte $07           //  5 - jaune
    .byte $08           //  6 - orange
    .byte $01           //  7 - blanc (centre du plasma)
    .byte $08           //  8 - orange
    .byte $07           //  9 - jaune
    .byte $05           // 10 - vert
    .byte $0d           // 11 - vert clair
    .byte $03           // 12 - cyan
    .byte $0e           // 13 - bleu clair
    .byte $06           // 14 - bleu foncé
    .byte $00           // 15 - noir

// 2e table sinus avec un décalage de phase pour le 2e composant
// Amplitude 0-15 pour indexer les tables de gradient (16 niveaux)
sin_table2:
    .fill 256, round(7.5 + 7.5 * sin(toRadians(i * 360 / 256)))

// ============================================================
// Initialisation de l'animation PETSCII
// ============================================================
.pc = * "PETSCII Code"

init_petscii:
        // Remplir la zone PETSCII avec des espaces
        ldx #39
        lda #$20
!clear:
        .for (var r = 10; r < 18; r++) {
            sta SCREEN_RAM + r * 40,x
        }
        dex
        bpl !clear-

        // Couleurs initiales: bleu foncé
        ldx #39
        lda #$06
!color:
        .for (var r = 10; r < 18; r++) {
            sta COLOR_RAM + r * 40,x
        }
        dex
        bpl !color-

        rts

// ============================================================
// Mise à jour du plasma PETSCII (appelée chaque frame)
//
// Algorithme:
//   Pour chaque cellule (row, col):
//     index1 = sin_table[ col_offset[col] + row_offset[row] + phase1 ]
//     index2 = sin_table2[ col_offset2[col] + row_offset2[row] + phase2 ]
//     value  = (index1 + index2) >> 1   (moyenne des 2 ondes)
//     char   = petscii_gradient[value]
//     color  = petscii_palette[value]
//
// Coût: ~50 cycles/cellule × 320 cellules = ~16000 cycles (~1 frame)
// ============================================================
update_petscii:
        lda #0
        sta petscii_row

!next_row:
        // --- Préparer les pointeurs écran et Color RAM ---
        ldx petscii_row
        txa
        clc
        adc #10                 // Ligne absolue = 10 + row_relatif
        tax

        lda row_addr_lo,x
        sta $fb
        lda row_addr_hi,x
        sta $fc
        lda color_addr_lo,x
        sta $fd
        lda color_addr_hi,x
        sta $fe

        // --- Calculer les offsets de ligne pour les 2 ondes ---
        // Onde 1: row_base1 = row_offset[row] + petscii_phase1
        ldx petscii_row
        lda row_offset_petscii,x
        clc
        adc petscii_phase1
        sta temp_row_val1

        // Onde 2: row_base2 = row_offset2[row] + petscii_phase2
        lda row_offset2_petscii,x
        clc
        adc petscii_phase2
        sta temp_row_val2

        // --- Boucle sur les 40 colonnes ---
        ldy #39

!col_loop:
        // Onde 1: index1 = col_offset[y] + row_base1
        lda col_offset_petscii,y
        clc
        adc temp_row_val1
        tax
        lda sin_table,x         // Valeur 0-6 (table sinus originale)
        sta temp_sin1

        // Onde 2: index2 = col_offset2[y] + row_base2
        lda col_offset2_petscii,y
        clc
        adc temp_row_val2
        tax
        lda sin_table2,x        // Valeur 0-15 (2e table sinus)

        // Combiner les 2 ondes: (sin1 + sin2 + color_cycle) & $0F
        clc
        adc temp_sin1
        clc
        adc color_cycle         // Color cycling (rotation de palette)
        and #$0f                // 16 niveaux (0-15)
        tax

        // Écrire le caractère PETSCII
        lda petscii_gradient,x
        sta ($fb),y

        // Écrire la couleur depuis la palette
        lda petscii_palette,x
        sta ($fd),y

        // Colonne suivante
        dey
        bpl !col_loop-

        // --- Ligne suivante ---
        inc petscii_row
        lda petscii_row
        cmp #8                  // 8 lignes (rows 10-17)
        bne !next_row-

        // --- Avancer les phases d'animation ---
        // Phase 1: mouvement lent (diagonal)
        lda petscii_phase1
        clc
        adc #2
        sta petscii_phase1

        // Phase 2: mouvement rapide (contre-diagonal)
        lda petscii_phase2
        sec
        sbc #3
        sta petscii_phase2

        // Color cycling: rotation lente de la palette
        inc color_cycle

        rts
