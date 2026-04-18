// ============================================================
// SID.ASM - Musique SID 3 voix (arpège + basse + percussion)
// Style chiptune C64 scène demo
// ============================================================

// --- Registres SID ---
.const SID_BASE = $d400

// Voice 1 (Lead - Pulse)
.const SID_V1_FL = SID_BASE + $00
.const SID_V1_FH = SID_BASE + $01
.const SID_V1_PL = SID_BASE + $02
.const SID_V1_PH = SID_BASE + $03
.const SID_V1_CR = SID_BASE + $04
.const SID_V1_AD = SID_BASE + $05
.const SID_V1_SR = SID_BASE + $06

// Voice 2 (Bass - Sawtooth)
.const SID_V2_FL = SID_BASE + $07
.const SID_V2_FH = SID_BASE + $08
.const SID_V2_PL = SID_BASE + $09
.const SID_V2_PH = SID_BASE + $0a
.const SID_V2_CR = SID_BASE + $0b
.const SID_V2_AD = SID_BASE + $0c
.const SID_V2_SR = SID_BASE + $0d

// Voice 3 (HiHat - Noise)
.const SID_V3_FL = SID_BASE + $0e
.const SID_V3_FH = SID_BASE + $0f
.const SID_V3_CR = SID_BASE + $12
.const SID_V3_AD = SID_BASE + $13
.const SID_V3_SR = SID_BASE + $14

// Global
.const SID_FILT_LO = SID_BASE + $15
.const SID_FILT_HI = SID_BASE + $16
.const SID_FILT_CR = SID_BASE + $17
.const SID_VOLUME  = SID_BASE + $18

.const SID_SPEED = 3            // Frames par step (vitesse du séquenceur)
.const PATTERN_LEN = 32         // Longueur du pattern

// ============================================================
// Table de fréquences des notes (PAL: Fclk = 985248 Hz)
// Freg = Fnote × 16777216 / 985248
// ============================================================
.pc = * "SID Note Table"

note_freq_lo:
    .byte $00                   //  0 = silence
    .byte <$045a               //  1 = C2
    .byte <$0530               //  2 = D2
    .byte <$053e               //  3 = Eb2
    .byte <$0685               //  4 = G2
    .byte <$079d               //  5 = Bb2
    .byte <$08b4               //  6 = C3
    .byte <$0a7c               //  7 = Eb3
    .byte <$0d0a               //  8 = G3
    .byte <$0f3a               //  9 = Bb3
    .byte <$1168               // 10 = C4
    .byte <$14f8               // 11 = Eb4
    .byte <$1a14               // 12 = G4
    .byte <$1e74               // 13 = Bb4
    .byte <$22d0               // 14 = C5

note_freq_hi:
    .byte $00
    .byte >$045a
    .byte >$0530
    .byte >$053e
    .byte >$0685
    .byte >$079d
    .byte >$08b4
    .byte >$0a7c
    .byte >$0d0a
    .byte >$0f3a
    .byte >$1168
    .byte >$14f8
    .byte >$1a14
    .byte >$1e74
    .byte >$22d0

// ============================================================
// Patterns (32 steps chacun)
// Index dans note_freq. 0 = gate off (silence)
// ============================================================
.pc = * "SID Patterns"

// Voice 1: arpège Cm (C-Eb-G) montant/descendant
lead_pattern:
    .byte 10, 11, 12, 14, 12, 11, 10, 8
    .byte  6,  7,  8, 10,  8,  7,  6,  8
    .byte 10, 12, 10,  8, 10, 11, 12, 14
    .byte 12, 10,  8,  6,  8, 10, 12, 10

// Voice 2: basse (notes racines, chaque 4 steps)
bass_pattern:
    .byte  1, 0, 0, 0,  1, 0, 0, 0
    .byte  4, 0, 0, 0,  4, 0, 0, 0
    .byte  1, 0, 0, 0,  3, 0, 0, 0
    .byte  4, 0, 0, 0,  1, 0, 0, 0

// Voice 3: hihat (1 = trigger, 0 = off)
hihat_pattern:
    .byte  1, 0, 1, 0,  1, 0, 1, 1
    .byte  1, 0, 1, 0,  1, 0, 1, 1
    .byte  1, 0, 1, 0,  1, 0, 1, 1
    .byte  1, 0, 1, 0,  1, 1, 1, 1

// ============================================================
// Initialisation du SID
// ============================================================
.pc = * "SID Code"

init_sid:
        // Volume max, pas de filtre
        lda #$0f
        sta SID_VOLUME

        // Voice 1: Pulse, attack rapide, decay moyen, sustain moyen
        lda #$00
        sta SID_V1_PL
        lda #$08                // Pulse width 50%
        sta SID_V1_PH
        lda #$22                // Attack=2, Decay=2
        sta SID_V1_AD
        lda #$a8                // Sustain=10, Release=8
        sta SID_V1_SR

        // Voice 2: Sawtooth, attack instant, decay long, sustain fort
        lda #$09                // Attack=0, Decay=9
        sta SID_V2_AD
        lda #$a0                // Sustain=10, Release=0
        sta SID_V2_SR

        // Voice 3: Noise, très court (percussion)
        lda #$00                // Attack=0, Decay=0
        sta SID_V3_AD
        lda #$00                // Sustain=0, Release=0
        sta SID_V3_SR
        lda #$80                // Fréquence haute pour le bruit
        sta SID_V3_FH
        lda #$00
        sta SID_V3_FL

        // Init séquenceur
        lda #1
        sta sid_counter
        lda #0
        sta sid_pos

        rts

// ============================================================
// Player SID (appelé chaque frame)
// ============================================================
play_sid:
        // Décrémenter le compteur de speed
        dec sid_counter
        bne !done-

        // Recharger le compteur
        lda #SID_SPEED
        sta sid_counter

        // Avancer la position dans le pattern
        ldx sid_pos

        // --- Voice 1: Lead (Pulse) ---
        lda #$40                // Gate off (release avant retrigger)
        sta SID_V1_CR

        lda lead_pattern,x
        beq !v1_off-
        tay
        lda note_freq_lo,y
        sta SID_V1_FL
        lda note_freq_hi,y
        sta SID_V1_FH
        lda #$41                // Pulse + Gate on
        sta SID_V1_CR
        jmp !v1_done-
!v1_off:
        lda #$40                // Pulse, gate off
        sta SID_V1_CR
!v1_done:

        // --- Voice 2: Bass (Sawtooth) ---
        lda bass_pattern,x
        beq !v2_off-
        lda #$20                // Gate off d'abord
        sta SID_V2_CR
        lda bass_pattern,x
        tay
        lda note_freq_lo,y
        sta SID_V2_FL
        lda note_freq_hi,y
        sta SID_V2_FH
        lda #$21                // Sawtooth + Gate on
        sta SID_V2_CR
        jmp !v2_done-
!v2_off:
        lda #$20                // Sawtooth, gate off
        sta SID_V2_CR
!v2_done:

        // --- Voice 3: HiHat (Noise) ---
        lda hihat_pattern,x
        beq !v3_off-
        lda #$80                // Gate off d'abord
        sta SID_V3_CR
        lda #$81                // Noise + Gate on
        sta SID_V3_CR
        jmp !v3_done-
!v3_off:
        lda #$80                // Noise, gate off
        sta SID_V3_CR
!v3_done:

        // Avancer la position
        inx
        cpx #PATTERN_LEN
        bcc !ok-
        ldx #0
!ok:
        stx sid_pos

!done:
        rts
