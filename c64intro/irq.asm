// ============================================================
// IRQ.ASM - Chaîne d'interruptions raster (4 IRQ)
//
// IRQ1 irq_top    ($30) → Mode bitmap + logo wobble
// IRQ2 irq_mid    ($82) → Mode texte + tech-tech bars
// IRQ3 irq_mux    ($B8) → Sprite multiplexer (set 2)
// IRQ4 irq_bottom ($F8) → Border opening + SID + frame sync
// ============================================================

.const IRQ_MUX_LINE = $b8      // Après les raster bars, avant le scroll

// --- Configuration de la chaîne IRQ ---
setup_irq:
        // Désactiver les interruptions CIA (timer)
        lda #$7f
        sta $dc0d
        sta $dd0d

        // Acquitter les interruptions CIA pendantes
        lda $dc0d
        lda $dd0d

        // Activer l'interruption raster du VIC-II
        lda #$01
        sta VIC_D01A

        // Configurer la première IRQ
        lda #IRQ1_LINE
        sta VIC_D012

        // Bit 8 de la ligne raster = 0
        lda VIC_D011
        and #$7f
        sta VIC_D011

        // Vecteur d'interruption hardware
        lda #<irq_top
        sta $fffe
        lda #>irq_top
        sta $ffff

        lda #$ff
        sta VIC_D019

        rts

// ============================================================
// IRQ 1: Haut de l'écran - Mode bitmap + logo wobble
// ============================================================
irq_top:
        pha
        txa
        pha
        tya
        pha

        lda #$ff
        sta VIC_D019

        // Activer le mode bitmap hi-res + 25 lignes (restaurer après border opening)
        lda #$3b
        sta VIC_D011
        lda #$f8
        sta VIC_D018

        // Logo wobble: ondulation horizontale via $D016
        ldx logo_wobble
        lda sin_table,x
        and #$07
        ora #$08                // 40 colonnes
        sta VIC_D016
        inc logo_wobble

        lda #BORDER_COLOR
        sta VIC_BORDER

        // Configurer les sprites du set 1 (zone haute)
        jsr set_sprites_top

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
// IRQ 2: Milieu - Mode texte + Tech-tech raster bars
// ============================================================
irq_mid:
        pha
        txa
        pha
        tya
        pha

        lda #$ff
        sta VIC_D019

        // Basculer en mode texte
        lda #$1b
        sta VIC_D011
        lda #$12
        sta VIC_D018

        // Smooth scroll horizontal (masqué pour éviter glitch multicolor)
        lda scroll_x
        and #$07
        sta VIC_D016

        // --- Tech-tech raster bars ---
        // Lecture depuis merged_bars (pré-calculé dans la boucle principale)
        ldx #0
        ldy #RASTER_BAR_LINES

rbar_loop:
        lda merged_bars,x
        sta VIC_BG
        sta VIC_BORDER

        .for (var i = 0; i < 8; i++) nop    // 16 cycles
        bit $ea                              // 3 cycles
        .for (var i = 0; i < 8; i++) nop    // 16 cycles
        bit $ea                              // 3 cycles
        nop
        nop
        nop

        inx
        dey
        bne rbar_loop

        // Remettre le fond noir
        lda #BG_COLOR
        sta VIC_BG
        sta VIC_BORDER

        // Programmer le prochain IRQ → irq_mux (sprite multiplexer)
        lda #IRQ_MUX_LINE
        sta VIC_D012
        lda #<irq_mux
        sta $fffe
        lda #>irq_mux
        sta $ffff

        pla
        tay
        pla
        tax
        pla
        rti

// ============================================================
// IRQ 3: Sprite multiplexer - Reprogrammer les sprites (set 2)
// ============================================================
irq_mux:
        pha
        txa
        pha
        tya
        pha

        lda #$ff
        sta VIC_D019

        // Reprogrammer les 8 sprites avec les positions du set 2
        jsr set_sprites_bottom

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
// IRQ 4: Bas de l'écran - Border opening + SID + frame sync
// ============================================================
irq_bottom:
        pha
        txa
        pha

        lda #$ff
        sta VIC_D019

        // --- Ouverture du border bas ---
        // Attendre la ligne $f9 (entre les points de fermeture $F7 et $FB)
!wait_border:
        lda VIC_D012
        cmp #$f9
        bne !wait_border-

        // Passer en 24 lignes → le VIC rate le point de fermeture $FB
        // (le point $F7 en 24-row est déjà passé)
        lda VIC_D011
        and #$f7                // Bit 3 = 0 → 24 rows
        sta VIC_D011

        // Signaler la fin de frame
        lda #1
        sta frame_flag

        // Jouer la musique SID (une fois par frame)
        jsr play_sid

        // Programmer le prochain IRQ → retour au sommet
        lda #IRQ1_LINE
        sta VIC_D012
        lda #<irq_top
        sta $fffe
        lda #>irq_top
        sta $ffff

        pla
        tax
        pla
        rti
