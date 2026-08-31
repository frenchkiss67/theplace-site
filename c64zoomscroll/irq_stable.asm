// ============================================================
//  irq_stable.asm — Squelette double-IRQ stable pour zoom
//  Assembleur cible : KickAssembler
//
//  Source de référence (technique) :
//    https://codebase.c64.org/doku.php?id=base:the_double_irq_method
//    https://www.antimon.org/dl/c64/code/stable.txt
//
//  Principe :
//    IRQ1 (ligne N)   → "impur", jitter de 7 cycles typique.
//                       Arme IRQ2 à la ligne N+1 et remplit
//                       l'intervalle de NOPs.
//    IRQ2 (ligne N+1) → stable à 1 cycle. Compare $d012 avec lui-même
//                       pour gommer le jitter résiduel, puis exécute
//                       le code raster cycle-exact (écritures $d011).
// ============================================================

#import "tables.asm"

// -----------------------------
// Point d'entrée — installer les vecteurs IRQ
// -----------------------------
install_irq:
    sei
    lda #$7f            // désactiver interrupts CIA (timer A/B)
    sta CIA1_ICR
    sta CIA2_ICR
    lda CIA1_ICR        // ack timers
    lda CIA2_ICR

    lda #$35            // bank switch : Kernal/BASIC hors, I/O visible
    sta $01             // ($FFFE/$FFFF pointe directement l'IRQ)

    lda #<irq1
    sta IRQ_VECTOR_LO
    lda #>irq1
    sta IRQ_VECTOR_HI

    lda #ZOOM_TOP       // première IRQ au sommet de la bande
    sta VIC_RASTER
    lda VIC_SCROLY
    and #$7f            // bit 7 raster line = 0 (< ligne 256)
    sta VIC_SCROLY

    lda #$01            // activer raster IRQ uniquement
    sta VIC_IRQMASK
    lda #$ff
    sta VIC_IRQFLAG     // ack toute IRQ pendante

    cli
    rts

// -----------------------------
// IRQ1 — "impure" (jitter 0..7 cycles)
// Rôle : armer IRQ2 à la ligne suivante, remplir de NOPs.
// -----------------------------
.align $100             // éviter qu'un saut de page ajoute 1 cycle
irq1:
    pha
    txa : pha
    tya : pha

    lda #<irq2
    sta IRQ_VECTOR_LO
    lda #>irq2
    sta IRQ_VECTOR_HI

    inc VIC_RASTER      // déclencher IRQ2 à la ligne suivante
    lda #$ff
    sta VIC_IRQFLAG     // ack IRQ1

    tsx                 // forcer une entrée IRQ "propre"
    cli
    // chaîne de NOPs : dépasse la fin de ligne pour atteindre IRQ2
    nop : nop : nop : nop : nop : nop : nop : nop
    nop : nop : nop : nop : nop : nop : nop : nop
    nop : nop : nop : nop : nop : nop : nop : nop
    nop : nop : nop : nop : nop : nop : nop : nop
    // si IRQ2 n'a pas encore frappé on retombe ici : sortie de secours
    pla : tay
    pla : tax
    pla
    rti

// -----------------------------
// IRQ2 — stable (jitter ≤ 1 cycle)
// Le pattern cmp $d012 / beq absorbe le dernier cycle de jitter.
// -----------------------------
irq2:
    // À cet instant on est 1 ligne en-dessous de IRQ1, timing fin.
    // Pattern double-IRQ classique :
    lda VIC_RASTER      // 4 cycles
    cmp VIC_RASTER      // 4 cycles : si raster change entre les 2 → Z=1
    beq stable          // 3 cycles si taken (→ égalisation)
stable:
    // À partir d'ici : stable à ±0 cycle.
    // ---- début du code raster cycle-exact ----
    ldx #$00
!zoom_loop:
    // attente ligne pile : chaque itération doit durer 63 cycles
    lda yscroll_x4,x    // 4
    sta VIC_SCROLY      // 4
    // padding pour compléter une ligne PAL (63 cycles)
    // ici il reste ~55 cycles à "consommer" utilement ou en NOPs
    .for (var i = 0; i < 25; i++) { nop }   // 2 cycles × 25 = 50
    bit $ea             // 3
    inx                 // 2
    cpx #ZOOM_HEIGHT    // 2
    bne !zoom_loop-     // 3
    // ---- fin bande zoom ----

    lda #$18            // restaurer YSCROLL normal (écran texte)
    sta VIC_SCROLY

    // ré-armer IRQ1 pour la frame suivante
    lda #<irq1
    sta IRQ_VECTOR_LO
    lda #>irq1
    sta IRQ_VECTOR_HI
    lda #ZOOM_TOP
    sta VIC_RASTER
    lda #$ff
    sta VIC_IRQFLAG

    pla : tay
    pla : tax
    pla
    rti
