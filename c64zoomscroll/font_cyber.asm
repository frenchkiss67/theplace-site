// ============================================================
//  font_cyber.asm — Police 8x8 "Cyber Outline"
//  Style : futuriste, lettres creuses avec contour angulaire,
//          coins biseautés, inspiré des demos C64 classiques.
//
//  Mapping : codes écran C64 (screen codes)
//    $00 = @      $01-$1A = A-Z      $1B = [
//    $20 = space  $21 = !            $2C = ,
//    $2D = -      $2E = .            $2F = /
//    $30-$39 = 0-9                   $3A = :
//    $3F = ?
//
//  Chaque glyphe = 8 octets (1 ligne = 1 octet, bit 7 = pixel gauche).
//  Charset complet = 256 glyphes × 8 = 2048 octets (tenir sur 2 Ko).
//
//  À charger en $2000 (ou tout multiple de $0800 dans la bank VIC).
//  Activer via $D018 : ((charset_addr / $0400) << 1) | (screen_ram_bits).
// ============================================================

.align $800
font_cyber:

// -----------------------------
// $00 = @ (placeholder — non utilisé en scrolltext)
// -----------------------------
    .byte %00111100
    .byte %01100110
    .byte %11011011
    .byte %11010011
    .byte %11011011
    .byte %01100000
    .byte %00111110
    .byte %00000000

// -----------------------------
// $01 = A
// -----------------------------
    .byte %00111100
    .byte %01100110
    .byte %11000011
    .byte %11000011
    .byte %11111111
    .byte %11000011
    .byte %11000011
    .byte %00000000

// $02 = B
    .byte %11111110
    .byte %11000011
    .byte %11000011
    .byte %11111110
    .byte %11000011
    .byte %11000011
    .byte %11111110
    .byte %00000000

// $03 = C
    .byte %00111110
    .byte %01100000
    .byte %11000000
    .byte %11000000
    .byte %11000000
    .byte %01100011
    .byte %00111110
    .byte %00000000

// $04 = D
    .byte %11111100
    .byte %11000110
    .byte %11000011
    .byte %11000011
    .byte %11000011
    .byte %11000110
    .byte %11111100
    .byte %00000000

// $05 = E
    .byte %11111111
    .byte %11000000
    .byte %11000000
    .byte %11111100
    .byte %11000000
    .byte %11000000
    .byte %11111111
    .byte %00000000

// $06 = F
    .byte %11111111
    .byte %11000000
    .byte %11000000
    .byte %11111100
    .byte %11000000
    .byte %11000000
    .byte %11000000
    .byte %00000000

// $07 = G
    .byte %00111110
    .byte %01100000
    .byte %11000000
    .byte %11001111
    .byte %11000011
    .byte %01100011
    .byte %00111110
    .byte %00000000

// $08 = H
    .byte %11000011
    .byte %11000011
    .byte %11000011
    .byte %11111111
    .byte %11000011
    .byte %11000011
    .byte %11000011
    .byte %00000000

// $09 = I
    .byte %01111110
    .byte %00011000
    .byte %00011000
    .byte %00011000
    .byte %00011000
    .byte %00011000
    .byte %01111110
    .byte %00000000

// $0A = J
    .byte %00011111
    .byte %00000110
    .byte %00000110
    .byte %00000110
    .byte %00000110
    .byte %11000110
    .byte %01111100
    .byte %00000000

// $0B = K
    .byte %11000011
    .byte %11000110
    .byte %11001100
    .byte %11111000
    .byte %11001100
    .byte %11000110
    .byte %11000011
    .byte %00000000

// $0C = L
    .byte %11000000
    .byte %11000000
    .byte %11000000
    .byte %11000000
    .byte %11000000
    .byte %11000000
    .byte %11111111
    .byte %00000000

// $0D = M
    .byte %11000011
    .byte %11100111
    .byte %11111111
    .byte %11011011
    .byte %11000011
    .byte %11000011
    .byte %11000011
    .byte %00000000

// $0E = N
    .byte %11000011
    .byte %11100011
    .byte %11110011
    .byte %11011011
    .byte %11001111
    .byte %11000111
    .byte %11000011
    .byte %00000000

// $0F = O
    .byte %00111100
    .byte %01100110
    .byte %11000011
    .byte %11000011
    .byte %11000011
    .byte %01100110
    .byte %00111100
    .byte %00000000

// $10 = P
    .byte %11111110
    .byte %11000011
    .byte %11000011
    .byte %11111110
    .byte %11000000
    .byte %11000000
    .byte %11000000
    .byte %00000000

// $11 = Q
    .byte %00111100
    .byte %01100110
    .byte %11000011
    .byte %11000011
    .byte %11011011
    .byte %01100110
    .byte %00111101
    .byte %00000000

// $12 = R
    .byte %11111110
    .byte %11000011
    .byte %11000011
    .byte %11111110
    .byte %11001100
    .byte %11000110
    .byte %11000011
    .byte %00000000

// $13 = S
    .byte %00111111
    .byte %11000000
    .byte %11000000
    .byte %01111110
    .byte %00000011
    .byte %00000011
    .byte %11111110
    .byte %00000000

// $14 = T
    .byte %11111111
    .byte %00011000
    .byte %00011000
    .byte %00011000
    .byte %00011000
    .byte %00011000
    .byte %00011000
    .byte %00000000

// $15 = U
    .byte %11000011
    .byte %11000011
    .byte %11000011
    .byte %11000011
    .byte %11000011
    .byte %11000011
    .byte %01111110
    .byte %00000000

// $16 = V
    .byte %11000011
    .byte %11000011
    .byte %11000011
    .byte %11000011
    .byte %01100110
    .byte %00111100
    .byte %00011000
    .byte %00000000

// $17 = W
    .byte %11000011
    .byte %11000011
    .byte %11000011
    .byte %11011011
    .byte %11111111
    .byte %11100111
    .byte %11000011
    .byte %00000000

// $18 = X
    .byte %11000011
    .byte %01100110
    .byte %00111100
    .byte %00011000
    .byte %00111100
    .byte %01100110
    .byte %11000011
    .byte %00000000

// $19 = Y
    .byte %11000011
    .byte %01100110
    .byte %00111100
    .byte %00011000
    .byte %00011000
    .byte %00011000
    .byte %00011000
    .byte %00000000

// $1A = Z
    .byte %11111111
    .byte %00000110
    .byte %00001100
    .byte %00011000
    .byte %00110000
    .byte %01100000
    .byte %11111111
    .byte %00000000

// $1B..$1F — symboles non utilisés : remplir de zéros
    .fill 5 * 8, 0

// -----------------------------
// $20 = space
// -----------------------------
    .byte 0,0,0,0,0,0,0,0

// $21 = !
    .byte %00011000
    .byte %00011000
    .byte %00011000
    .byte %00011000
    .byte %00011000
    .byte %00000000
    .byte %00011000
    .byte %00000000

// $22..$2B — non utilisés
    .fill 10 * 8, 0

// $2C = ,
    .byte %00000000
    .byte %00000000
    .byte %00000000
    .byte %00000000
    .byte %00000000
    .byte %00011000
    .byte %00011000
    .byte %00110000

// $2D = -
    .byte %00000000
    .byte %00000000
    .byte %00000000
    .byte %11111111
    .byte %00000000
    .byte %00000000
    .byte %00000000
    .byte %00000000

// $2E = .
    .byte %00000000
    .byte %00000000
    .byte %00000000
    .byte %00000000
    .byte %00000000
    .byte %00000000
    .byte %00011000
    .byte %00000000

// $2F = /
    .byte %00000011
    .byte %00000110
    .byte %00001100
    .byte %00011000
    .byte %00110000
    .byte %01100000
    .byte %11000000
    .byte %00000000

// -----------------------------
// $30 = 0
// -----------------------------
    .byte %00111100
    .byte %01100110
    .byte %11000111
    .byte %11011011
    .byte %11100011
    .byte %01100110
    .byte %00111100
    .byte %00000000

// $31 = 1
    .byte %00011000
    .byte %00111000
    .byte %01111000
    .byte %00011000
    .byte %00011000
    .byte %00011000
    .byte %01111110
    .byte %00000000

// $32 = 2
    .byte %00111100
    .byte %01100110
    .byte %00000110
    .byte %00001100
    .byte %00110000
    .byte %01100000
    .byte %11111111
    .byte %00000000

// $33 = 3
    .byte %01111110
    .byte %00000011
    .byte %00000011
    .byte %00111110
    .byte %00000011
    .byte %00000011
    .byte %01111110
    .byte %00000000

// $34 = 4
    .byte %00001110
    .byte %00011110
    .byte %00110110
    .byte %01100110
    .byte %11111111
    .byte %00000110
    .byte %00000110
    .byte %00000000

// $35 = 5
    .byte %11111111
    .byte %11000000
    .byte %11111110
    .byte %00000011
    .byte %00000011
    .byte %11000011
    .byte %01111110
    .byte %00000000

// $36 = 6
    .byte %00111110
    .byte %01100000
    .byte %11000000
    .byte %11111110
    .byte %11000011
    .byte %11000011
    .byte %01111110
    .byte %00000000

// $37 = 7
    .byte %11111111
    .byte %00000011
    .byte %00000110
    .byte %00001100
    .byte %00011000
    .byte %00110000
    .byte %01100000
    .byte %00000000

// $38 = 8
    .byte %01111110
    .byte %11000011
    .byte %11000011
    .byte %01111110
    .byte %11000011
    .byte %11000011
    .byte %01111110
    .byte %00000000

// $39 = 9
    .byte %01111110
    .byte %11000011
    .byte %11000011
    .byte %01111111
    .byte %00000011
    .byte %00000011
    .byte %01111110
    .byte %00000000

// $3A = :
    .byte %00000000
    .byte %00011000
    .byte %00011000
    .byte %00000000
    .byte %00000000
    .byte %00011000
    .byte %00011000
    .byte %00000000

// $3B..$3E — non utilisés
    .fill 4 * 8, 0

// $3F = ?
    .byte %00111100
    .byte %01100110
    .byte %00000110
    .byte %00001100
    .byte %00011000
    .byte %00000000
    .byte %00011000
    .byte %00000000

// Compléter jusqu'à 256 glyphes (2048 octets total)
.fill 2048 - (* - font_cyber), 0

font_cyber_end:
