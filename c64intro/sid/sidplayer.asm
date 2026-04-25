// ============================================================
// SIDPLAYER.ASM - Player digi-sample 4-bit pour PSID
// Assembleur: KickAssembler
//
// Technique oldskool: chaque appel de play() ecrit un nibble
// (4 bits) dans le registre de volume du SID ($D418). Le DAC
// volume produit alors un signal audio numerique, comme dans
// les classiques de la scene C64 (Ghostbusters, Arkanoid,
// Mahoney's "Musik Run/Stop", Cinemaware...).
//
// Le SID host (VICE/sidplayfp) appelle:
//   - init() une fois au demarrage
//   - play() a la frequence du timer A CIA1 programmee par init
//
// Disposition memoire:
//   $1000  init  (entree)
//   $1080  play  (entree)
//   $1100  donnees nibble-packees (low nibble = pair, high = impair)
//
// Variables zero page:
//   $02   nibble_flag (0 = lo nibble, 1 = hi nibble)
//   $FB   sample_ptr_lo
//   $FC   sample_ptr_hi
//
// Notes:
//   - Doit correspondre EXACTEMENT a la disposition emise par
//     tools/audio2sid.py (memes adresses, meme convention nibble).
//   - SAMPLE_RATE_RELOAD et SAMPLE_END sont a patcher selon
//     l'audio convertit.
// ============================================================

.const SID_VOLUME           = $d418
.const CIA1_TIMER_A_LO      = $dc04
.const CIA1_TIMER_A_HI      = $dc05

.const ZP_NIBBLE_FLAG       = $02
.const ZP_PTR_LO            = $fb
.const ZP_PTR_HI            = $fc

// Valeurs par defaut: 8000 Hz @ PAL (985248 / 8000 ~= 123)
.const SAMPLE_RATE_RELOAD   = 123
.const SAMPLE_DATA          = $1100
.const SAMPLE_END           = $a000  // a patcher (= SAMPLE_DATA + taille)

// ------------------------------------------------------------
// INIT @ $1000
// ------------------------------------------------------------
.pc = $1000 "Init"
init:
        sei
        lda #<SAMPLE_RATE_RELOAD
        sta CIA1_TIMER_A_LO
        lda #>SAMPLE_RATE_RELOAD
        sta CIA1_TIMER_A_HI
        lda #<SAMPLE_DATA
        sta ZP_PTR_LO
        lda #>SAMPLE_DATA
        sta ZP_PTR_HI
        lda #0
        sta ZP_NIBBLE_FLAG
        cli
        rts

// ------------------------------------------------------------
// PLAY @ $1080  (alignement fixe pour matcher l'entete PSID)
// ------------------------------------------------------------
.pc = $1080 "Play"
play:
        ldy #0
        lda (ZP_PTR_LO),y       // octet courant (2 nibbles)
        ldx ZP_NIBBLE_FLAG
        bne high_nib

low_nib:
        and #$0f
        sta SID_VOLUME
        inc ZP_NIBBLE_FLAG      // passe au nibble haut au prochain tick
        jmp done

high_nib:
        lsr
        lsr
        lsr
        lsr
        sta SID_VOLUME
        lda #0
        sta ZP_NIBBLE_FLAG
        // avance le pointeur
        inc ZP_PTR_LO
        bne check_end
        inc ZP_PTR_HI

check_end:
        lda ZP_PTR_HI
        cmp #>SAMPLE_END
        bcc done                // ptr_hi < end_hi: pas fini
        bne wrap                // ptr_hi > end_hi: deborde, on boucle
        lda ZP_PTR_LO
        cmp #<SAMPLE_END
        bcc done                // ptr_lo < end_lo: pas fini

wrap:
        lda #<SAMPLE_DATA
        sta ZP_PTR_LO
        lda #>SAMPLE_DATA
        sta ZP_PTR_HI

done:
        rts

// ------------------------------------------------------------
// Donnees echantillon (a remplir par l'outil de conversion)
// ------------------------------------------------------------
.pc = $1100 "Sample Data"
sample_data:
        // .import binary "sample.bin"  // genere par audio2sid.py
