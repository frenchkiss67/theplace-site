// ============================================================
// RASTERBARS.ASM - Tables de couleurs pour les raster bars
// ============================================================

// Table de 256 couleurs pour les raster bars
// Composée de 4 barres de dégradé différentes, répétées 4 fois
// Chaque barre: noir → couleur sombre → couleur claire → blanc → retour
//
// Palette C64:
//  0=noir       1=blanc     2=rouge      3=cyan
//  4=violet     5=vert      6=bleu       7=jaune
//  8=orange     9=marron   10=rose      11=gris foncé
// 12=gris moyen 13=vert clair 14=bleu clair 15=gris clair

.pc = * "Raster Bar Colors"

bar_colors:
.for (var rep = 0; rep < 4; rep++) {
    // --- Barre bleue (16 octets) ---
    .byte $00, $00, $06, $06, $0e, $0e, $03, $01
    .byte $03, $0e, $0e, $06, $06, $00, $00, $00

    // --- Barre rouge (16 octets) ---
    .byte $00, $00, $02, $02, $0a, $0a, $07, $01
    .byte $07, $0a, $0a, $02, $02, $00, $00, $00

    // --- Barre verte (16 octets) ---
    .byte $00, $00, $05, $05, $0d, $0d, $03, $01
    .byte $03, $0d, $0d, $05, $05, $00, $00, $00

    // --- Barre jaune/orange (16 octets) ---
    .byte $00, $00, $09, $09, $08, $08, $07, $01
    .byte $07, $08, $08, $09, $09, $00, $00, $00
}
