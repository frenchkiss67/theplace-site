// ============================================================
// CHARSET.ASM - Copie du Character ROM vers RAM
// ============================================================

// Copier le jeu de caractères depuis la ROM ($D000)
// vers la RAM à $0800 (2048 octets = 256 caractères × 8 octets)
//
// Le Character ROM est visible à $D000 quand le bit 2
// du registre $01 est mis à 0 (I/O désactivé)

.pc = * "Charset Code"

copy_charset:
        // Sauvegarder la configuration mémoire
        lda $01
        pha

        // Désactiver les I/O pour rendre le Character ROM visible à $D000
        // $01 = %00110011 → BASIC off, Kernal off, I/O off, charrom visible
        lda #$33
        sta $01

        // Copier 2048 octets: $D000-$D7FF → $0800-$0FFF
        // 8 pages de 256 octets
        ldx #0
!loop:
        lda $d000,x
        sta CHARSET_ADDR,x
        lda $d100,x
        sta CHARSET_ADDR + $100,x
        lda $d200,x
        sta CHARSET_ADDR + $200,x
        lda $d300,x
        sta CHARSET_ADDR + $300,x
        lda $d400,x
        sta CHARSET_ADDR + $400,x
        lda $d500,x
        sta CHARSET_ADDR + $500,x
        lda $d600,x
        sta CHARSET_ADDR + $600,x
        lda $d700,x
        sta CHARSET_ADDR + $700,x
        inx
        bne !loop-

        // Restaurer la configuration mémoire
        pla
        sta $01

        rts
