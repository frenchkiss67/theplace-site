;-----------------------------------------------------------
; THE PLACE - Intro Amiga 500
; CPU       : Motorola 68000
; Assembleur: vasm (syntaxe Motorola)
; Effets    : Copper bars + Sinus scroll 1 bitplane
;-----------------------------------------------------------

        include "hardware.i"

;-----------------------------------------------------------
; Constantes ecran
;-----------------------------------------------------------
SCREEN_W        equ     320
SCREEN_H        equ     256
BPL_DEPTH       equ     1               ; 1 bitplane = 2 couleurs
BPL_SIZE        equ     SCREEN_W/8*SCREEN_H

DIWSTRT_VAL     equ     $2c81           ; coin haut/gauche zone affichee
DIWSTOP_VAL     equ     $2cc1           ; coin bas/droit
DDFSTRT_VAL     equ     $0038           ; debut data fetch (lores)
DDFSTOP_VAL     equ     $00d0           ; fin data fetch

;-----------------------------------------------------------
; Section CODE
;-----------------------------------------------------------
        section code,code

start:
        ;--- Sauvegarde et prise du systeme ---
        move.l  4.w,a6                  ; ExecBase
        jsr     -132(a6)                ; Forbid()

        ; Sauvegarde de l'etat des registres custom
        move.w  $dff002,old_dmaconr
        move.w  $dff01c,old_intenar
        move.l  $dff080,old_cop1lc

        ; Coupe DMA et IRQ pour avoir la machine pour nous
        move.w  #$7fff,$dff09a          ; INTENA  : coupe toutes IRQ
        move.w  #$7fff,$dff09c          ; INTREQ  : clear pending
        move.w  #$7fff,$dff096          ; DMACON  : coupe tout DMA

        ;--- Preparation des bitplanes ---
        bsr     clear_bitplane
        bsr     draw_scroll_text        ; pre-rendu du texte (bitmap)

        ;--- Initialisation de la copperlist ---
        bsr     init_copper

        ; Pointeur copper list 1
        lea     copperlist,a0
        move.l  a0,$dff080              ; COP1LCH
        move.w  #0,$dff088              ; STRPCOPJMP1 -> demarre la copper

        ; Active : MASTER + RASTER + COPPER + BLITTER + SPRITES off
        move.w  #$83a0,$dff096          ; DMACON SET|MASTER|BPL|COPPER|BLIT

;-----------------------------------------------------------
; Boucle principale - synchro VBlank
;-----------------------------------------------------------
mainloop:
        bsr     wait_vbl
        bsr     update_scroll
        bsr     update_copper_bars

        ; Sortie sur clic gauche souris (CIA-A bit 6 = 0 si appuye)
        btst    #6,$bfe001
        bne.s   mainloop

;-----------------------------------------------------------
; Restauration du systeme
;-----------------------------------------------------------
exit:
        move.w  #$7fff,$dff09a
        move.w  #$7fff,$dff09c
        move.w  #$7fff,$dff096

        ; Restore copper original
        move.l  old_cop1lc,$dff080
        move.w  #0,$dff088

        ; Restore DMA et IRQ tels qu'au boot
        move.w  old_dmaconr,d0
        or.w    #$8000,d0
        move.w  d0,$dff096
        move.w  old_intenar,d0
        or.w    #$8000,d0
        move.w  d0,$dff09a

        move.l  4.w,a6
        jsr     -138(a6)                ; Permit()

        moveq   #0,d0
        rts

;-----------------------------------------------------------
; wait_vbl - attend ligne raster 0 (debut de frame)
;-----------------------------------------------------------
wait_vbl:
.w1:    move.l  $dff004,d0              ; VPOSR/VHPOSR
        and.l   #$1ff00,d0
        cmp.l   #303<<8,d0
        bne.s   .w1
.w2:    move.l  $dff004,d0
        and.l   #$1ff00,d0
        cmp.l   #303<<8,d0
        beq.s   .w2
        rts

;-----------------------------------------------------------
; clear_bitplane - efface le bitplane (BPL_SIZE octets)
;-----------------------------------------------------------
clear_bitplane:
        lea     bitplane,a0
        move.w  #(BPL_SIZE/4)-1,d0
.cl:    clr.l   (a0)+
        dbf     d0,.cl
        rts

;-----------------------------------------------------------
; Sauvegarde etat systeme
;-----------------------------------------------------------
        section data,data
old_dmaconr:    dc.w    0
old_intenar:    dc.w    0
old_cop1lc:     dc.l    0

;-----------------------------------------------------------
; Inclusion des modules
;-----------------------------------------------------------
        include "copper.s"
        include "scroll.s"
        include "tables.s"

;-----------------------------------------------------------
; Bitplane (BSS - non initialise) - CHIP RAM obligatoire
; (Agnus ne peut adresser que la chip RAM pour les DMA)
;-----------------------------------------------------------
        section bss_chip,bss_c
bitplane:       ds.b    BPL_SIZE
