// ============================================================
// IRQ.ASM - Chaîne d'interruptions raster
// ============================================================

// --- Configuration de la chaîne IRQ ---
setup_irq:
        // Désactiver les interruptions CIA (timer)
        lda #$7f
        sta $dc0d       // CIA1 - clavier/joystick
        sta $dd0d       // CIA2 - série/NMI

        // Acquitter les interruptions CIA pendantes
        lda $dc0d
        lda $dd0d

        // Activer l'interruption raster du VIC-II
        lda #$01
        sta VIC_D01A

        // Configurer la première IRQ (haut de l'écran)
        lda #IRQ1_LINE
        sta VIC_D012

        // Bit 8 de la ligne raster = 0 (lignes < 256)
        lda VIC_D011
        and #$7f
        sta VIC_D011

        // Vecteur d'interruption hardware
        lda #<irq_top
        sta $fffe
        lda #>irq_top
        sta $ffff

        // Acquitter toute IRQ VIC pendante
        lda #$ff
        sta VIC_D019

        rts

// ============================================================
// IRQ 1: Haut de l'écran - Activer le mode bitmap pour le logo
// ============================================================
irq_top:
        pha
        txa
        pha
        tya
        pha

        // Acquitter l'IRQ raster
        lda #$ff
        sta VIC_D019

        // Activer le mode bitmap hi-res
        lda #$3b                // Bit 5=1 (bitmap), bit 4=1 (écran on), bits 0-2=3 (yscroll)
        sta VIC_D011
        lda #$f8                // Bitmap à $2000, screen RAM à $3C00
        sta VIC_D018
        lda #$08                // 40 colonnes, pas de scroll horizontal
        sta VIC_D016

        // Couleur du border pour la zone logo
        lda #BORDER_COLOR
        sta VIC_BORDER

        // Programmer le prochain IRQ → irq_mid
        lda #IRQ2_LINE
        sta VIC_D012
        lda #<irq_mid
        sta $fffe
        lda #>irq_mid
        sta $ffff

        pla
        tay
        pla
        tax
        pla
        rti

// ============================================================
// IRQ 2: Milieu - Mode texte + Raster bars
// ============================================================
irq_mid:
        pha
        txa
        pha
        tya
        pha

        // Acquitter l'IRQ raster
        lda #$ff
        sta VIC_D019

        // Basculer en mode texte
        lda #$1b                // Bit 5=0 (texte), bit 4=1 (écran on), bits 0-2=3 (yscroll)
        sta VIC_D011
        lda #$12                // Charset à $0800, screen RAM à $0400
        sta VIC_D018

        // Configurer le smooth scroll horizontal pour le texte
        // Masquer les bits 3-7 pour éviter d'activer le multicolor
        // si scroll_x contient temporairement $FF (entre dec et reset)
        lda scroll_x
        and #$07                // Seuls les bits 0-2 (scroll), bit 3=0 (38 colonnes)
        sta VIC_D016

        // -----------------------------------------------
        // Raster bars: boucle occupée changeant les couleurs
        // à chaque ligne raster
        // -----------------------------------------------
        ldx bar_offset
        ldy #RASTER_BAR_LINES

rbar_loop:
        lda bar_colors,x
        sta VIC_BG
        sta VIC_BORDER

        // Attendre environ 1 ligne raster (63 cycles PAL)
        // Cycles consommés par le code de boucle: ~19
        // Délai nécessaire: ~44 cycles
        .for (var i = 0; i < 8; i++) nop    // 16 cycles
        bit $ea                              // 3 cycles
        .for (var i = 0; i < 8; i++) nop    // 16 cycles
        bit $ea                              // 3 cycles
        nop                                  // 2 cycles
        nop                                  // 2 cycles
        nop                                  // 2 cycles

        inx
        dey
        bne rbar_loop

        // Remettre le fond noir après les barres
        lda #BG_COLOR
        sta VIC_BG
        sta VIC_BORDER

        // Programmer le prochain IRQ → irq_bottom
        lda #IRQ3_LINE
        sta VIC_D012
        lda #<irq_bottom
        sta $fffe
        lda #>irq_bottom
        sta $ffff

        pla
        tay
        pla
        tax
        pla
        rti

// ============================================================
// IRQ 3: Bas de l'écran - Synchronisation frame
// ============================================================
irq_bottom:
        pha

        // Acquitter l'IRQ raster
        lda #$ff
        sta VIC_D019

        // Signaler qu'une frame est terminée
        lda #1
        sta frame_flag

        // Programmer le prochain IRQ → retour au sommet
        lda #IRQ1_LINE
        sta VIC_D012
        lda #<irq_top
        sta $fffe
        lda #>irq_top
        sta $ffff

        pla
        rti
