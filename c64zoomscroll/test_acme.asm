; ============================================================
;  test_acme.asm — Port ACME du zoom scrolltext pour validation
;  Assembleur : ACME (installé dans l'env de test)
;  But : valider algorithme, labels, arithmétique, timing.
;
;  NOTE : la version canonique reste main.asm (KickAssembler).
;  Ce fichier sert uniquement à vérifier que le code 6510
;  s'assemble sans erreur.
; ============================================================

!cpu 6510
; Build : acme -f cbm -o test.prg test_acme.asm

; ---------- Constantes VIC-II ----------
VIC_SCROLY      = $d011
VIC_RASTER      = $d012
VIC_SCROLX      = $d016
VIC_VMCSB       = $d018
VIC_IRQFLAG     = $d019
VIC_IRQMASK     = $d01a
VIC_BORDER      = $d020
VIC_BGCOL       = $d021
CIA1_ICR        = $dc0d
CIA2_ICR        = $dd0d
IRQ_VECTOR_LO   = $fffe
IRQ_VECTOR_HI   = $ffff

SCROLLER_ROW            = 12
SCROLLER_SCREEN_ADDR    = $0400 + 40 * SCROLLER_ROW     ; $05e0
SCROLLER_COLOR_ADDR     = $d800 + 40 * SCROLLER_ROW     ; $d9e0
RASTER_SCROLLER_TOP     = $33 + 8 * SCROLLER_ROW        ; $93
RASTER_FRAME_START      = $f8
SCROLL_SPEED            = 2

; ZP
ZP_XSCROLL      = $02
ZP_TEXT_PTR     = $03       ; + $04
ZP_SIN_IDX      = $05
ZP_FRAME        = $06

; ---------- Stub BASIC : 10 SYS 2064 ----------
* = $0801
    !byte $0c,$08,$0a,$00,$9e,$32,$30,$36,$34,$00,$00,$00

; ---------- Main ----------
* = $0810
main:
    sei
    lda #$00
    sta VIC_BORDER
    sta VIC_BGCOL

    ; clear screen
    lda #$20
    ldx #$00
.clr
    sta $0400,x
    sta $0500,x
    sta $0600,x
    sta $06e8,x
    inx
    bne .clr

    ; color RAM
    lda #$06
    ldx #$00
.col
    sta $d800,x
    sta $d900,x
    sta $da00,x
    sta $dae8,x
    inx
    bne .col

    jsr scroller_init
    cli
.hang
    jmp .hang

; ---------- scroller_init ----------
scroller_init:
    sei
    lda #$18
    sta VIC_VMCSB

    ldx #39
    lda #$0e
.cl1
    sta SCROLLER_COLOR_ADDR,x
    dex
    bpl .cl1

    ldx #39
    lda #$20
.cl2
    sta SCROLLER_SCREEN_ADDR,x
    dex
    bpl .cl2

    lda #$07
    sta ZP_XSCROLL
    lda #<scroll_text
    sta ZP_TEXT_PTR
    lda #>scroll_text
    sta ZP_TEXT_PTR+1
    lda #$00
    sta ZP_SIN_IDX
    sta ZP_FRAME

    lda #$7f
    sta CIA1_ICR
    sta CIA2_ICR
    lda CIA1_ICR
    lda CIA2_ICR

    lda #$35
    sta $01

    lda #<scroller_irq
    sta IRQ_VECTOR_LO
    lda #>scroller_irq
    sta IRQ_VECTOR_HI

    lda #RASTER_SCROLLER_TOP
    sta VIC_RASTER
    lda #$1b
    sta VIC_SCROLY

    lda #$01
    sta VIC_IRQMASK
    lda #$ff
    sta VIC_IRQFLAG

    cli
    rts

; ---------- scroller_irq (bande zoom) ----------
scroller_irq:
    pha
    txa
    pha
    tya
    pha

    ldx #$00
.zl
    lda yscroll_x4,x
    ldy VIC_RASTER
.w
    cpy VIC_RASTER
    beq .w
    sta VIC_SCROLY
    inx
    cpx #32
    bne .zl

    lda #$1b
    sta VIC_SCROLY

    lda #RASTER_FRAME_START
    sta VIC_RASTER
    lda #<scroller_irq_frame
    sta IRQ_VECTOR_LO
    lda #>scroller_irq_frame
    sta IRQ_VECTOR_HI

    lda #$ff
    sta VIC_IRQFLAG

    pla
    tay
    pla
    tax
    pla
    rti

; ---------- scroller_irq_frame (update) ----------
scroller_irq_frame:
    pha
    txa
    pha
    tya
    pha

    inc VIC_BORDER
    jsr scroller_update
    dec VIC_BORDER

    lda #RASTER_SCROLLER_TOP
    sta VIC_RASTER
    lda #<scroller_irq
    sta IRQ_VECTOR_LO
    lda #>scroller_irq
    sta IRQ_VECTOR_HI

    lda #$ff
    sta VIC_IRQFLAG

    pla
    tay
    pla
    tax
    pla
    rti

; ---------- scroller_update ----------
scroller_update:
    inc ZP_FRAME
    inc ZP_SIN_IDX

    lda ZP_XSCROLL
    sec
    sbc #SCROLL_SPEED
    bcs .nw

    and #$07
    sta ZP_XSCROLL
    jsr shift_screen_left
    jsr fetch_next_char
    jmp .write

.nw
    sta ZP_XSCROLL

.write
    lda ZP_XSCROLL
    ora #$c8
    sta VIC_SCROLX
    rts

; ---------- shift_screen_left ----------
shift_screen_left:
    ldx #$00
.sl
    lda SCROLLER_SCREEN_ADDR+1,x
    sta SCROLLER_SCREEN_ADDR,x
    inx
    cpx #39
    bne .sl
    rts

; ---------- fetch_next_char ----------
fetch_next_char:
    ldy #$00
    lda (ZP_TEXT_PTR),y
    cmp #$ff
    bne .ok

    lda #<scroll_text
    sta ZP_TEXT_PTR
    lda #>scroll_text
    sta ZP_TEXT_PTR+1
    lda (ZP_TEXT_PTR),y

.ok
    sta SCROLLER_SCREEN_ADDR+39

    inc ZP_TEXT_PTR
    bne .nc
    inc ZP_TEXT_PTR+1
.nc
    rts

; ---------- Tables ----------
!align 255,0    ; align à page
yscroll_x4:
    !for i, 0, 7 {
        !byte i|$18, i|$18, i|$18, i|$18
    }

; Table sinus amplitude 0..15, 256 valeurs (ACME : !for + sin builtin)
sin_zoom:
    !for i, 0, 255 {
        !byte int(7.5 + 7.5 * sin(i * 6.2831853 / 256) + 0.5)
    }

; ---------- Message scrolltext ----------
scroll_text:
    !byte $20,$20,$20,$20
    !byte $08,$05,$0c,$0c,$0f           ; HELLO
    !byte $20
    !byte $17,$0f,$12,$0c,$04           ; WORLD
    !byte $20,$20,$20,$20
    !byte $1a,$0f,$0f,$0d               ; ZOOM
    !byte $20
    !byte $13,$03,$12,$0f,$0c,$0c       ; SCROLL
    !byte $20
    !byte $03,$36,$34                   ; C64
    !byte $20,$20,$20,$20
    !byte $ff
