// ============================================================
//  text.asm — Message du scrolltext (codes écran C64)
//
//  Mapping des codes écran :
//    $01..$1A = A..Z        $20 = space
//    $2C = ','              $2D = '-'
//    $2E = '.'              $2F = '/'
//    $30..$39 = 0..9        $3A = ':'
//    $3F = '?'
//
//  Le message se termine par $FF (sentinelle).
//  Le scroller reboucle automatiquement en fin de texte.
// ============================================================

scroll_text:
    // "    HELLO WORLD    THIS IS A ZOOM SCROLLTEXT ON C64    "
    .byte $20,$20,$20,$20
    .byte $08,$05,$0c,$0c,$0f                  // HELLO
    .byte $20
    .byte $17,$0f,$12,$0c,$04                  // WORLD
    .byte $20,$20,$20,$20
    .byte $14,$08,$09,$13                      // THIS
    .byte $20
    .byte $09,$13                              // IS
    .byte $20
    .byte $01                                  // A
    .byte $20
    .byte $1a,$0f,$0f,$0d                      // ZOOM
    .byte $20
    .byte $13,$03,$12,$0f,$0c,$0c              // SCROLL
    .byte $14,$05,$18,$14                      // TEXT
    .byte $20
    .byte $0f,$0e                              // ON
    .byte $20
    .byte $03,$36,$34                          // C64
    .byte $20,$20,$20,$20

    // "    GREETINGS TO THE SCENE    "
    .byte $07,$12,$05,$05,$14,$09,$0e,$07,$13  // GREETINGS
    .byte $20
    .byte $14,$0f                              // TO
    .byte $20
    .byte $14,$08,$05                          // THE
    .byte $20
    .byte $13,$03,$05,$0e,$05                  // SCENE
    .byte $20,$20,$20,$20

    // "    CODED IN 6510 ASSEMBLY    "
    .byte $03,$0f,$04,$05,$04                  // CODED
    .byte $20
    .byte $09,$0e                              // IN
    .byte $20
    .byte $36,$35,$31,$30                      // 6510
    .byte $20
    .byte $01,$13,$13,$05,$0d,$02,$0c,$19      // ASSEMBLY
    .byte $20,$20,$20,$20

    // "    ENJOY THE RIDE    "
    .byte $05,$0e,$0a,$0f,$19                  // ENJOY
    .byte $20
    .byte $14,$08,$05                          // THE
    .byte $20
    .byte $12,$09,$04,$05                      // RIDE
    .byte $20,$20,$20,$20,$20,$20,$20,$20

    // Sentinelle fin de texte
    .byte $ff
scroll_text_end:
