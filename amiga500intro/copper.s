;-----------------------------------------------------------
; copper.s - Copperlist principale
;
; La copper synchronise les changements de registres avec le
; faisceau raster :
;   - Configure BPLCONx, DIWSTRT/STOP, DDFSTRT/STOP, modulos
;   - Fournit l'adresse du bitplane scroll (BPL1PT)
;   - Genere une serie de COPPER MOVE WAIT pour les barres
;     de couleur (rainbow / copper bars)
;-----------------------------------------------------------

;--- Constantes copper ---
COP_END         equ     $fffffffe
NB_BARS         equ     128             ; nombre de lignes barres

;-----------------------------------------------------------
; init_copper - patch des pointeurs bitplane dans la liste
;-----------------------------------------------------------
init_copper:
        lea     bitplane,a0
        move.l  a0,d0
        lea     copper_bpl1ptl,a1
        move.w  d0,(a1)                 ; low word
        swap    d0
        lea     copper_bpl1pth,a1
        move.w  d0,(a1)                 ; high word
        rts

;-----------------------------------------------------------
; update_copper_bars - decale la palette des barres
;
; Chaque frame on rotate la table couleurs, ce qui produit
; un effet de barres qui defilent verticalement.
;-----------------------------------------------------------
update_copper_bars:
        ; Patch les NB_BARS lignes COLOR00 dans la copperlist
        lea     copper_bars,a0
        lea     bar_palette,a1
        move.b  bar_offset,d1
        moveq   #0,d2
        move.b  d1,d2

        moveq   #NB_BARS-1,d7
.loop:
        ; chaque entree de la table fait 8 octets :
        ;   dc.w line,$fffe  ; WAIT raster line
        ;   dc.w COLOR00,$xxx ; MOVE couleur
        addq.l  #4,a0                   ; saute le WAIT
        move.b  d2,d3
        and.w   #$3f,d3
        move.w  (a1,d3.w),d3            ; couleur RGB de la palette (mot)
        addq.l  #2,a0                   ; saute le registre COLOR00 ($180)
        move.w  d3,(a0)+                ; ecrit la nouvelle couleur
        addq.b  #1,d2
        dbf     d7,.loop

        addq.b  #1,bar_offset
        rts

;-----------------------------------------------------------
; Liste copper en RAM CHIP (alignee mot)
;-----------------------------------------------------------
        section copper,data_c
        cnop    0,4
copperlist:
        ; Configuration des bitplanes
        dc.w    BPLCON0,$1200           ; 1 bitplane, color, no hires
        dc.w    BPLCON1,$0000           ; pas de scroll hardware
        dc.w    BPLCON2,$0000

        dc.w    BPL1MOD,$0000
        dc.w    BPL2MOD,$0000

        dc.w    DIWSTRT,$2c81
        dc.w    DIWSTOP,$2cc1
        dc.w    DDFSTRT,$0038
        dc.w    DDFSTOP,$00d0

        ; Adresse du bitplane (patchee par init_copper)
        dc.w    BPL1PTH
copper_bpl1pth:
        dc.w    $0000
        dc.w    BPL1PTL
copper_bpl1ptl:
        dc.w    $0000

        ; Palette de base
        dc.w    COLOR00,$0000           ; fond noir
        dc.w    COLOR01,$0fff           ; texte blanc

        ; Attente debut zone visible
        dc.w    $2c01,$ff00
        dc.w    $fffe

;--- Barres de couleurs (NB_BARS lignes a partir de $44) ---
copper_bars:
BAR_LINE        set     $44
        rept    NB_BARS
        dc.w    (BAR_LINE<<8)|$01,$fffe
        dc.w    COLOR00,$0000           ; valeur patchee chaque frame
BAR_LINE        set     BAR_LINE+1
        endr

        ; Repasse en noir apres les barres
        dc.w    $c401,$fffe
        dc.w    COLOR00,$0000

        ; Fin de la copper list
        dc.w    $ffff,$fffe
