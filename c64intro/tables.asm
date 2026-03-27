// ============================================================
// TABLES.ASM - Tables précalculées
// ============================================================

// Table sinus: 256 valeurs, amplitude 0 à SCROLL_NUM_ROWS-1
// Utilisée pour le déplacement vertical du sinus scroll
//
// Formule: round((SCROLL_NUM_ROWS-1)/2 + (SCROLL_NUM_ROWS-1)/2 * sin(i × 2π / 256))

.pc = * "Sine Table"

sin_table:
    .fill 256, round(3 + 3 * sin(toRadians(i * 360 / 256)))
