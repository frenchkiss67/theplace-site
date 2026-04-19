// ============================================================
//  main.asm — Runner standalone pour tester le zoom scrolltext
//  Assembleur : KickAssembler
//
//  Build :
//    java -jar KickAss.jar main.asm -o zoomscroll.prg
//
//  Lancement :
//    x64sc -warp zoomscroll.prg    (émulateur VICE)
//
//  Intégration dans une démo :
//    Supprimer ce fichier et `#import "scroller.asm"` depuis la
//    démo principale, puis appeler `scroller_init` à l'init et
//    `scroller_update` à chaque frame (ou laisser le handler IRQ
//    interne faire le travail).
// ============================================================

// Stub BASIC : 10 SYS 2064 ($0810)
BasicUpstart2(main)

* = $0810 "Main code"

// ------------------------------------------------------------
//  main — point d'entrée
// ------------------------------------------------------------
main:
    sei

    // Border et background noirs
    lda #$00
    sta $d020
    sta $d021

    // Effacer la screen RAM ($20 = espace)
    lda #$20
    ldx #$00
!clear:
    sta $0400, x
    sta $0500, x
    sta $0600, x
    sta $06e8, x
    inx
    bne !clear-

    // Color RAM uniforme (sera écrasée par scroller_init pour sa ligne)
    lda #$06                // bleu foncé
    ldx #$00
!color:
    sta $d800, x
    sta $d900, x
    sta $da00, x
    sta $dae8, x
    inx
    bne !color-

    // Initialiser le module scrolltext
    jsr scroller_init

    cli

    // ------------------------------------------------------------
    //  Boucle principale — ici on ne fait rien, l'IRQ gère tout.
    //  Dans une démo, d'autres effets peuvent tourner en parallèle.
    // ------------------------------------------------------------
main_loop:
    jmp main_loop


// ============================================================
//  Module scrolltext
// ============================================================
#import "scroller.asm"


// ============================================================
//  Charset à $2000 (2 Ko)
//  Utilise font_cyber par défaut — remplacer par font_neon au
//  besoin (ou switcher dynamiquement via $D018).
// ============================================================
* = $2000 "Charset"
#import "font_cyber.asm"

// Pour utiliser la police LED à la place :
// * = $2000 "Charset"
// #import "font_neon.asm"
