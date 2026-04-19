// ============================================================
//  scroller.asm — Module zoom scrolltext (réutilisable dans démo)
//  Assembleur : KickAssembler
//
//  API exposée :
//    scroller_init    — initialise IRQ, charset, ligne scroller
//    scroller_update  — à appeler 1×/frame (scroll horizontal + anim zoom)
//    scroller_irq     — handler d'interruption raster (chaîné ou direct)
//
//  Dépendances :
//    tables.asm       — constantes VIC-II + sinus + tables YSCROLL
//    text.asm         — message à scroller
//    font_cyber.asm   — charset (ou font_neon.asm)
//
//  Configuration par défaut :
//    - Ligne du scroller : row 12 (raster line ~$96)
//    - Hauteur zoom band : 40 lignes raster (zoom x5)
//    - Charset en $2000, screen en $0400
//    - Scroll 2 pixels/frame (vitesse standard)
// ============================================================

#import "tables.asm"

// -----------------------------
// Configuration
// -----------------------------
.const SCROLLER_ROW        = 12                    // ligne texte du scroller
.const SCROLLER_SCREEN_ADDR = $0400 + 40 * SCROLLER_ROW
// Ligne raster PAL de la 1re ligne pixel du scroller :
//   row 0 commence à la ligne raster 51 ($33), chaque row = 8 lignes.
.const RASTER_SCROLLER_TOP = $33 + 8 * SCROLLER_ROW // = $93 pour row 12
.const RASTER_SCROLLER_END = RASTER_SCROLLER_TOP + 40
.const RASTER_FRAME_START  = $f8                   // fin de visible area
.const SCROLL_SPEED        = 2                      // pixels/frame

// -----------------------------
// Variables de l'état du scroller (page zéro pour rapidité)
// -----------------------------
.const ZP_XSCROLL    = $02   // position fine (0..7)
.const ZP_TEXT_PTR   = $03   // pointeur 16 bits vers scroll_text
.const ZP_SIN_IDX    = $05   // index courant dans sin_factor
.const ZP_FRAME      = $06   // compteur de frames

// ============================================================
//  scroller_init — à appeler une fois depuis l'init de la démo
// ============================================================
scroller_init:
    sei

    // --- Configurer pointeurs VIC ---
    lda #$18                // screen=$0400, charset=$2000
    sta VIC_VMCSB

    // --- Couleurs du scroller (color RAM ligne SCROLLER_ROW) ---
    ldx #39
    lda #$0e                // bleu clair
!loop:
    sta $d800 + 40 * SCROLLER_ROW, x
    dex
    bpl !loop-

    // --- Effacer la ligne du scroller (screen RAM) ---
    ldx #39
    lda #$20
!loop:
    sta SCROLLER_SCREEN_ADDR, x
    dex
    bpl !loop-

    // --- Initialiser état ---
    lda #$07
    sta ZP_XSCROLL
    lda #<scroll_text
    sta ZP_TEXT_PTR
    lda #>scroll_text
    sta ZP_TEXT_PTR + 1
    lda #$00
    sta ZP_SIN_IDX
    sta ZP_FRAME

    // --- Désactiver IRQ CIA ---
    lda #$7f
    sta CIA1_ICR
    sta CIA2_ICR
    lda CIA1_ICR            // ack
    lda CIA2_ICR

    // --- Kernal off, RAM sous ROM, I/O visible ---
    lda #$35
    sta $01

    // --- Installer vecteur IRQ ---
    lda #<scroller_irq
    sta IRQ_VECTOR_LO
    lda #>scroller_irq
    sta IRQ_VECTOR_HI

    // --- Armer raster IRQ à la ligne du top de la bande scroller ---
    lda #RASTER_SCROLLER_TOP
    sta VIC_RASTER
    lda #$1b                // bit 7 raster=0, ECM=0, bitmap=0, screen ON,
    sta VIC_SCROLY          // 25 lignes, YSCROLL=3 (mode texte standard)

    lda #$01                // activer raster IRQ
    sta VIC_IRQMASK
    lda #$ff
    sta VIC_IRQFLAG         // ack pending

    cli
    rts

// ============================================================
//  scroller_irq — handler d'interruption raster
//  Chaîne interne : scroller_irq → irq_zoom_end → scroller_irq
// ============================================================
scroller_irq:
    pha
    txa : pha
    tya : pha

    // --- Entrée bande zoom : forcer YSCROLL séquence ---
    // Version simple non-cycle-exact : on boucle en écrivant
    // YSCROLL tant que $D012 n'a pas dépassé la bande.
    // (Pour timing exact → voir irq_stable.asm)
    ldx #$00
!zoom_loop:
    lda yscroll_x4, x
    ldy VIC_RASTER          // attendre progression raster
!wait:
    cpy VIC_RASTER
    beq !wait-
    sta VIC_SCROLY
    inx
    cpx #32                 // 32 lignes raster = 4 lignes pixel × 8
    bne !zoom_loop-

    // --- Sortie bande : restaurer YSCROLL normal ---
    lda #$1b                // bit3=écran ON + bit4=25 lignes + YSCROLL=3
    sta VIC_SCROLY

    // --- Programmer IRQ suivant en haut de la frame ---
    lda #RASTER_FRAME_START
    sta VIC_RASTER
    lda #<scroller_irq_frame
    sta IRQ_VECTOR_LO
    lda #>scroller_irq_frame
    sta IRQ_VECTOR_HI

    lda #$ff
    sta VIC_IRQFLAG

    pla : tay
    pla : tax
    pla
    rti

// ============================================================
//  scroller_irq_frame — IRQ de fin de frame, fait l'update
// ============================================================
scroller_irq_frame:
    pha
    txa : pha
    tya : pha

    // Flash border pour debug (optionnel, commenter en prod)
    inc VIC_BORDER
    jsr scroller_update
    dec VIC_BORDER

    // --- Ré-armer IRQ au sommet de la bande scroller ---
    lda #RASTER_SCROLLER_TOP
    sta VIC_RASTER
    lda #<scroller_irq
    sta IRQ_VECTOR_LO
    lda #>scroller_irq
    sta IRQ_VECTOR_HI

    lda #$ff
    sta VIC_IRQFLAG

    pla : tay
    pla : tax
    pla
    rti

// ============================================================
//  scroller_update — logique de scroll (1× par frame)
//  Appelée depuis scroller_irq_frame ou depuis la main loop
//  d'une démo externe (dans ce cas ne pas utiliser les IRQs ici).
// ============================================================
scroller_update:
    inc ZP_FRAME
    inc ZP_SIN_IDX

    // --- Smooth scroll horizontal ($D016) ---
    //   Décrémente XSCROLL de SCROLL_SPEED pixels. La carry
    //   après SBC indique un wrap : on shift alors la ligne
    //   d'un caractère à gauche et on injecte le prochain char.
    lda ZP_XSCROLL
    sec
    sbc #SCROLL_SPEED
    bcs !no_wrap+

    // wrap → shift screen + injecter char
    and #$07
    sta ZP_XSCROLL
    jsr shift_screen_left
    jsr fetch_next_char
    jmp !write+

!no_wrap:
    sta ZP_XSCROLL

!write:
    lda ZP_XSCROLL
    ora #$c8                // préserver bits 3-7 par défaut ($D016)
    sta VIC_SCROLX
    rts

// ------------------------------------------------------------
//  shift_screen_left — décale les 40 cols de la ligne scroller
//  d'une position vers la gauche (colonne 0 perdue).
// ------------------------------------------------------------
shift_screen_left:
    ldx #$00
!loop:
    lda SCROLLER_SCREEN_ADDR + 1, x
    sta SCROLLER_SCREEN_ADDR, x
    inx
    cpx #39
    bne !loop-
    rts

// ------------------------------------------------------------
//  fetch_next_char — injecte le prochain char à la colonne 39
// ------------------------------------------------------------
fetch_next_char:
    ldy #$00
    lda (ZP_TEXT_PTR), y
    cmp #$ff                // sentinelle fin ?
    bne !no_wrap+

    // rembobiner
    lda #<scroll_text
    sta ZP_TEXT_PTR
    lda #>scroll_text
    sta ZP_TEXT_PTR + 1
    lda (ZP_TEXT_PTR), y

!no_wrap:
    sta SCROLLER_SCREEN_ADDR + 39

    // avancer ptr
    inc ZP_TEXT_PTR
    bne !skip+
    inc ZP_TEXT_PTR + 1
!skip:
    rts

// ============================================================
//  Imports
// ============================================================
#import "text.asm"
