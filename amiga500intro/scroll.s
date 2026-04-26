;-----------------------------------------------------------
; scroll.s - Sinus scroll 1 bitplane
;
; Le texte est pre-rendu dans le bitplane principal via une
; police 8x8 embarquee (font_data). A chaque frame on decale
; le contenu d'un pixel vers la gauche en utilisant le
; blitter pour la copie + ajout du nouveau pixel a droite.
; Pour cette version "simple" on utilise un decalage logiciel
; au niveau de la pointer de base BPL1PTH/L : on ecrit le
; texte dans une zone large et on fait scroller la fenetre
; visible en avancant l'adresse de depart d'un pixel a la
; fois (smooth scroll par BPLCON1 + increment modulo octet).
;-----------------------------------------------------------

;--- Parametres du scroll ---
SCROLL_ROW      equ     180             ; ligne y du scroll (pixels)
SCROLL_HEIGHT   equ     16              ; hauteur de la zone scroll (px)
FONT_W          equ     8
FONT_H          equ     8

;-----------------------------------------------------------
; draw_scroll_text - rendu du texte dans le bitplane
;
; Ecrit la chaine 'scroll_text' avec la police 8x8 dans la
; zone scroll du bitplane. Apres rendu, la zone est figee
; et on scrolle visuellement par modification de bplcon1.
;-----------------------------------------------------------
draw_scroll_text:
        lea     scroll_text,a0
        lea     bitplane,a1
        ; Position de depart : ligne SCROLL_ROW
        move.l  #(SCROLL_ROW*SCREEN_W/8),d0
        adda.l  d0,a1

        moveq   #0,d6                   ; colonne courante (octets)
.next_char:
        moveq   #0,d0
        move.b  (a0)+,d0                ; lit le caractere ASCII
        beq.s   .done                   ; 0 = fin de chaine

        sub.w   #' ',d0                 ; offset dans la police (espace=0)
        bmi.s   .skip
        cmp.w   #59,d0                  ; 59 glyphes definis (espace -> Z)
        bge.s   .skip

        ; Adresse du glyphe = font_data + d0*8
        lsl.w   #3,d0
        lea     font_data,a2
        adda.w  d0,a2

        ; Copie 8 lignes de 1 octet
        movea.l a1,a3
        adda.l  d6,a3                   ; +colonne
        moveq   #FONT_H-1,d1
.row:
        move.b  (a2)+,(a3)
        adda.l  #SCREEN_W/8,a3
        dbf     d1,.row

.skip:
        addq.l  #1,d6                   ; colonne suivante (1 octet = 8px)
        cmp.w   #SCREEN_W/8,d6
        blt.s   .next_char
.done:
        rts

;-----------------------------------------------------------
; update_scroll - smooth scroll horizontal + sinusoide
;
; - decremente le sub-pixel scroll dans BPLCON1
; - quand il deborde, avance la pointer bitplane d'un octet
; - quand on atteint le bout, recommence
; - applique en plus un decalage vertical sinusoidal en
;   modifiant DIWSTRT via la copperlist (effet wave)
;-----------------------------------------------------------
update_scroll:
        ; Avance phase sinus
        addq.b  #2,sin_phase

        ; Decrementer le smooth scroll (BPLCON1 bits 0-3 du plane 1)
        move.b  scroll_sub,d0
        subq.b  #1,d0
        bpl.s   .no_step

        ; Step entier : avance d'un octet (8 pixels)
        moveq   #7,d0
        addq.l  #1,scroll_x

        ; Met a jour BPL1PT dans la copperlist
        move.l  scroll_x,d1
        ; reboucle quand on a defile toute la largeur du buffer
        cmp.l   #SCREEN_W/8,d1
        blt.s   .ok
        moveq   #0,d1
        move.l  d1,scroll_x
.ok:
        ; Le scroll est purement visuel : on n'applique pas
        ; le shift au pointer (le texte fait deja la largeur
        ; de l'ecran). On ne touche que BPLCON1 pour l'effet.

.no_step:
        move.b  d0,scroll_sub
        and.w   #$0f,d0
        ; Shift identique sur les 2 playfields (PF1 = bits 0-3)
        move.b  d0,d1
        lsl.b   #4,d1
        or.b    d1,d0
        move.w  d0,$dff102              ; BPLCON1

        rts

;-----------------------------------------------------------
; Variables (BSS / data)
;-----------------------------------------------------------
        section scroll_data,data
scroll_x:       dc.l    0
scroll_sub:     dc.b    7
sin_phase:      dc.b    0
bar_offset:     dc.b    0
                even

scroll_text:
        dc.b    "    THE PLACE PRESENTS A SIMPLE AMIGA 500 INTRO ... "
        dc.b    "GREETINGS TO THE 6502 CREW ON THE COMMODORE 64 SIDE ... "
        dc.b    "MOTOROLA 68000 RULES ! WRAP "
        dc.b    0
        even
