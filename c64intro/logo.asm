// ============================================================
// LOGO.ASM - Logo bitmap "BRAINWAVE"
// ============================================================

// --- Initialisation du logo ---
setup_logo:
        // Remplir le screen RAM bitmap ($3C00) avec noir sur noir
        ldx #0
        lda #$00
!clr:
        sta BITMAP_SCR,x
        sta BITMAP_SCR + $100,x
        inx
        bne !clr-

        ldx #0
!clr2:
        sta BITMAP_SCR + $200,x
        inx
        cpx #$e8
        bne !clr2-

        // Couleurs initiales du logo (lignes char 2-6 = offsets 80-279)
        // Le color wash les remplacera ensuite chaque frame
        ldx #0
        lda #(LOGO_FG_COLOR * 16 + LOGO_BG_COLOR)
!logo_color:
        sta BITMAP_SCR + 80,x
        inx
        cpx #200
        bne !logo_color-

        rts

// ============================================================
// Données bitmap du logo à $2000 (8000 octets)
// ============================================================

// Motif du logo "BRAINWAVE" en cellules 8×8
// Chaque lettre: 3 colonnes de large, 5 lignes de haut
// Espacement: 1 colonne vide entre chaque lettre
// Centré: début colonne 3 (largeur totale = 35 colonnes)
//
// B: ##.  R: ##.  A: .#.  I: ###  N: #.#  W: #.#  A: .#.  V: #.#  E: ###
//    #.#     #.#     #.#     .#.     ###     #.#     #.#     #.#     #..
//    ##.     ##.     ###     .#.     ###     ###     ###     #.#     ##.
//    #.#     #..     #.#     .#.     #.#     ###     #.#     .#.     #..
//    ##.     #..     #.#     ###     #.#     .#.     #.#     .#.     ###
//
//         col: 0         1         2         3
//              0123456789012345678901234567890123456789

.var logo_line0 = "...##..##...#..###.#.#.#.#..#..#.#.###.."
.var logo_line1 = "...#.#.#.#.#.#..#..###.#.#.#.#.#.#.#...."
.var logo_line2 = "...##..##..###..#..###.###.###.#.#.##..."
.var logo_line3 = "...#.#.#...#.#..#..#.#.###.#.#..#..#...."
.var logo_line4 = "...##..#...#.#.###.#.#..#..#.#..#..###.."

// Fonction: obtenir si une cellule du logo est remplie
.function isLogoCell(charRow, charCol) {
    .if (charRow < 0 || charRow > 4 || charCol < 0 || charCol >= 40)
        .return 0

    .var line = logo_line0
    .if (charRow == 1) .eval line = logo_line1
    .if (charRow == 2) .eval line = logo_line2
    .if (charRow == 3) .eval line = logo_line3
    .if (charRow == 4) .eval line = logo_line4

    .return (line.charAt(charCol) == '#') ? 1 : 0
}

// Fonction: générer un octet du bitmap
// Le bitmap C64 est organisé par cellules 8×8:
//   adresse = char_row × 320 + char_col × 8 + pixel_row
.function bitmapByte(idx) {
    .var charRow  = floor(idx / 320)
    .var remainder = mod(idx, 320)
    .var charCol  = floor(remainder / 8)
    .var pixelRow = mod(remainder, 8)

    // Le logo occupe les lignes char 2-6
    .if (charRow >= 2 && charRow <= 6) {
        .if (isLogoCell(charRow - 2, charCol) == 1)
            .return $ff     // Cellule pleine: tous les pixels allumés
    }
    .return $00             // Cellule vide
}

// Générer les 8000 octets du bitmap
.pc = BITMAP_ADDR "Bitmap Data"
.fill 8000, bitmapByte(i)
