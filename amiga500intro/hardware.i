;-----------------------------------------------------------
; hardware.i - Equates des registres custom Amiga (OCS)
; Sous-ensemble minimal pour cette intro
;-----------------------------------------------------------

CUSTOM          equ     $dff000

; --- DMA / IRQ ---
DMACONR         equ     $002
VPOSR           equ     $004
VHPOSR          equ     $006
INTENAR         equ     $01c
INTREQR         equ     $01e

; --- Copper ---
COP1LCH         equ     $080
COP1LCL         equ     $082
COP2LCH         equ     $084
COPJMP1         equ     $088
COPJMP2         equ     $08a

; --- Display Window / DataFetch ---
DIWSTRT         equ     $08e
DIWSTOP         equ     $090
DDFSTRT         equ     $092
DDFSTOP         equ     $094

; --- Control ---
DMACON          equ     $096
INTENA          equ     $09a
INTREQ          equ     $09c

; --- Bitplanes ---
BPL1PTH         equ     $0e0
BPL1PTL         equ     $0e2
BPLCON0         equ     $100
BPLCON1         equ     $102
BPLCON2         equ     $104
BPL1MOD         equ     $108
BPL2MOD         equ     $10a

; --- Couleurs ---
COLOR00         equ     $180
COLOR01         equ     $182
