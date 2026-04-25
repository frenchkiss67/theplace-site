#!/usr/bin/env python3
"""
audio2sid.py - Convertisseur Audio (WAV) -> PSID (Commodore 64)

Technique oldskool: digi-sample 4-bit via le registre de volume SID ($D418),
piloté par une interruption CIA Timer A. C'est la methode utilisee par les
jeux et demos C64 historiques (Ghostbusters, Arkanoid, Mahoney's "Musik
Run/Stop", etc.) pour la voix numerique sur le SID.

Pipeline:
  WAV -> mono -> reechantillonnage -> normalisation -> quantification 4-bit
       -> packing nibbles -> player 6510 hand-assemble -> entete PSID v2

Usage:
  python3 audio2sid.py input.wav output.sid [--rate 8000] [--max-bytes 32768]
                                            [--name "..."] [--author "..."]
                                            [--no-loop] [--ntsc]

Aucune dependance externe: stdlib uniquement.
"""

from __future__ import annotations

import argparse
import os
import struct
import sys
import wave


# --------------------------------------------------------------------------
# 1. Lecture WAV + conversion mono float
# --------------------------------------------------------------------------

def read_wav_mono(path: str) -> tuple[list[float], int]:
    """Lit un fichier WAV PCM, retourne (samples float [-1, 1], framerate)."""
    with wave.open(path, "rb") as w:
        n_channels = w.getnchannels()
        sampwidth = w.getsampwidth()
        framerate = w.getframerate()
        n_frames = w.getnframes()
        raw = w.readframes(n_frames)

    if sampwidth == 1:
        # 8-bit unsigned
        fmt = f"{n_frames * n_channels}B"
        ints = struct.unpack(fmt, raw)
        floats = [(v - 128) / 128.0 for v in ints]
    elif sampwidth == 2:
        fmt = f"<{n_frames * n_channels}h"
        ints = struct.unpack(fmt, raw)
        floats = [v / 32768.0 for v in ints]
    elif sampwidth == 3:
        # 24-bit signed little-endian
        floats = []
        for i in range(0, len(raw), 3):
            b0, b1, b2 = raw[i], raw[i + 1], raw[i + 2]
            v = b0 | (b1 << 8) | (b2 << 16)
            if v & 0x800000:
                v -= 0x1000000
            floats.append(v / 8388608.0)
    elif sampwidth == 4:
        fmt = f"<{n_frames * n_channels}i"
        ints = struct.unpack(fmt, raw)
        floats = [v / 2147483648.0 for v in ints]
    else:
        raise ValueError(f"Largeur d'echantillon non supportee: {sampwidth} bytes")

    if n_channels == 1:
        mono = floats
    else:
        mono = []
        inv = 1.0 / n_channels
        for i in range(0, len(floats), n_channels):
            s = 0.0
            for c in range(n_channels):
                s += floats[i + c]
            mono.append(s * inv)

    return mono, framerate


# --------------------------------------------------------------------------
# 2. Reechantillonnage (interpolation lineaire, suffisant pour 4-bit)
# --------------------------------------------------------------------------

def resample_linear(samples: list[float], src_rate: int, dst_rate: int) -> list[float]:
    if src_rate == dst_rate or not samples:
        return list(samples)
    n_src = len(samples)
    n_dst = max(1, int(round(n_src * dst_rate / src_rate)))
    out = [0.0] * n_dst
    ratio = (n_src - 1) / max(1, n_dst - 1) if n_dst > 1 else 0
    for i in range(n_dst):
        x = i * ratio
        i0 = int(x)
        i1 = min(i0 + 1, n_src - 1)
        frac = x - i0
        out[i] = samples[i0] * (1.0 - frac) + samples[i1] * frac
    return out


# --------------------------------------------------------------------------
# 3. Normalisation + pre-emphasis simple (compense l'attenuation HF du DAC SID)
# --------------------------------------------------------------------------

def normalize(samples: list[float]) -> list[float]:
    if not samples:
        return samples
    peak = max(abs(s) for s in samples)
    if peak < 1e-9:
        return samples
    g = 0.98 / peak
    return [s * g for s in samples]


def pre_emphasis(samples: list[float], alpha: float = 0.20) -> list[float]:
    """Filtre HF leger pour compenser le filtre passe-bas naturel du DAC volume."""
    if not samples:
        return samples
    out = [samples[0]]
    prev = samples[0]
    for s in samples[1:]:
        out.append(s + alpha * (s - prev))
        prev = s
    return out


# --------------------------------------------------------------------------
# 4. Quantification 4-bit + packing nibbles
# --------------------------------------------------------------------------

def quantize_4bit(samples: list[float]) -> list[int]:
    """Mappe [-1, 1] vers [0, 15] (4-bit non signe pour $D418)."""
    out = []
    for s in samples:
        if s < -1.0:
            s = -1.0
        elif s > 1.0:
            s = 1.0
        v = int(round((s + 1.0) * 7.5))
        if v < 0:
            v = 0
        elif v > 15:
            v = 15
        out.append(v)
    return out


def pack_nibbles(nibs: list[int]) -> bytes:
    """Empile 2 nibbles par octet: low = pair, high = impair.
    L'ordre est aligne sur la routine play du C64.
    """
    if len(nibs) % 2:
        nibs = nibs + [nibs[-1]]
    out = bytearray(len(nibs) // 2)
    for i in range(0, len(nibs), 2):
        out[i // 2] = (nibs[i] & 0x0F) | ((nibs[i + 1] & 0x0F) << 4)
    return bytes(out)


# --------------------------------------------------------------------------
# 5. Player 6510 hand-assemble
# --------------------------------------------------------------------------
# Disposition memoire (charge a $1000):
#
#   $1000  init   (entree appelee une fois par le SID host)
#   $1080  play   (entree appelee a chaque tick CIA Timer A)
#   $1100  sample_data (donnees nibble-packees)
#
# Variables zero page:
#   $02    nibble_flag (0 = lo nibble, 1 = hi nibble)
#   $FB    sample_ptr_lo
#   $FC    sample_ptr_hi
#
# Init: SEI; programme la valeur de reload du timer A CIA1 ($DC04/$DC05);
#       initialise le pointeur d'echantillon et le flag nibble; CLI; RTS.
# Play: lit l'octet courant, extrait le bon nibble, l'ecrit dans $D418,
#       avance le pointeur tous les deux appels, gere la fin (boucle ou stop).
# --------------------------------------------------------------------------

PLAYER_BASE = 0x1000
INIT_ADDR = 0x1000
PLAY_ADDR = 0x1080
SAMPLE_ADDR = 0x1100


def assemble_player(rate_reload: int, end_addr: int, loop: bool) -> bytes:
    """Genere le code machine 6510. Retourne 256 octets ($1000-$10FF)."""
    code = bytearray(0x100)
    # Pre-remplissage NOPs ($EA) pour que les zones non utilisees soient
    # neutres en cas de saut errone.
    for i in range(0x100):
        code[i] = 0xEA

    end_lo = end_addr & 0xFF
    end_hi = (end_addr >> 8) & 0xFF
    rate_lo = rate_reload & 0xFF
    rate_hi = (rate_reload >> 8) & 0xFF
    sample_lo = SAMPLE_ADDR & 0xFF
    sample_hi = (SAMPLE_ADDR >> 8) & 0xFF

    # ---------------------------------------------------------- INIT @ $1000
    init = [
        0x78,                          # SEI
        0xA9, rate_lo,                 # LDA #<RATE
        0x8D, 0x04, 0xDC,              # STA $DC04 (CIA1 Timer A lo)
        0xA9, rate_hi,                 # LDA #>RATE
        0x8D, 0x05, 0xDC,              # STA $DC05 (CIA1 Timer A hi)
        0xA9, sample_lo,               # LDA #<sample_data
        0x85, 0xFB,                    # STA $FB
        0xA9, sample_hi,               # LDA #>sample_data
        0x85, 0xFC,                    # STA $FC
        0xA9, 0x00,                    # LDA #$00
        0x85, 0x02,                    # STA $02
        0x58,                          # CLI
        0x60,                          # RTS
    ]
    for i, b in enumerate(init):
        code[i] = b
    # init occupe $1000-$1018 (25 octets), reste NOP jusqu'a $1080.

    # ---------------------------------------------------------- PLAY @ $1080
    # Decalages relatifs a $1080 (= offset 0x80 dans le buffer).
    play_off = 0x80
    play = [
        # $1080  A0 00         LDY #$00
        0xA0, 0x00,
        # $1082  B1 FB         LDA ($FB),Y
        0xB1, 0xFB,
        # $1084  A6 02         LDX $02
        0xA6, 0x02,
        # $1086  D0 0B         BNE high_nib (target $1093)
        0xD0, 0x0B,
        # ----- low_nib ($1088) -----
        # $1088  29 0F         AND #$0F
        0x29, 0x0F,
        # $108A  8D 18 D4      STA $D418
        0x8D, 0x18, 0xD4,
        # $108D  E6 02         INC $02
        0xE6, 0x02,
        # $108F  4C BA 10      JMP done ($10BA)
        0x4C, 0xBA, 0x10,
        # $1092  EA            NOP (pad)
        0xEA,
        # ----- high_nib ($1093) -----
        # $1093  4A 4A 4A 4A   LSR x4
        0x4A, 0x4A, 0x4A, 0x4A,
        # $1097  8D 18 D4      STA $D418
        0x8D, 0x18, 0xD4,
        # $109A  A9 00         LDA #$00
        0xA9, 0x00,
        # $109C  85 02         STA $02
        0x85, 0x02,
        # $109E  E6 FB         INC $FB
        0xE6, 0xFB,
        # $10A0  D0 02         BNE +2
        0xD0, 0x02,
        # $10A2  E6 FC         INC $FC
        0xE6, 0xFC,
        # ----- check_end ($10A4) -----
        # $10A4  A5 FC         LDA $FC
        0xA5, 0xFC,
        # $10A6  C9 EH         CMP #>END
        0xC9, end_hi,
        # $10A8  90 10         BCC done ($10BA)
        0x90, 0x10,
        # $10AA  D0 06         BNE wrap ($10B2)
        0xD0, 0x06,
        # $10AC  A5 FB         LDA $FB
        0xA5, 0xFB,
        # $10AE  C9 EL         CMP #<END
        0xC9, end_lo,
        # $10B0  90 08         BCC done ($10BA)
        0x90, 0x08,
    ]

    # wrap: si loop -> reinitialise pointeur et joue done(RTS);
    #       si !loop -> ecrit $00 dans $D418 (silence) et RTS infini en boucle.
    if loop:
        wrap = [
            # $10B2  A9 LO         LDA #<sample_data
            0xA9, sample_lo,
            # $10B4  85 FB         STA $FB
            0x85, 0xFB,
            # $10B6  A9 HI         LDA #>sample_data
            0xA9, sample_hi,
            # $10B8  85 FC         STA $FC
            0x85, 0xFC,
        ]
    else:
        # Mode "play once": colle le pointeur sur la fin ($FF avant end), met
        # un volume neutre ($08) et reste sur ce nibble. Le host continuera
        # d'appeler play, mais $D418 = 8 = silence centre.
        wrap = [
            # $10B2  A9 08         LDA #$08
            0xA9, 0x08,
            # $10B4  8D 18 D4      STA $D418
            0x8D, 0x18, 0xD4,
            # $10B7  EA EA EA      NOP NOP NOP (pad to $10BA)
            0xEA, 0xEA, 0xEA,
        ]

    # done: RTS
    done = [0x60]  # $10BA

    full_play = play + wrap + done
    for i, b in enumerate(full_play):
        code[play_off + i] = b

    # Sanity check: le RTS doit tomber a $10BA (offset 0xBA dans le buffer).
    assert code[0xBA] == 0x60, f"RTS final mal place: {code[0xBA]:02X} a offset 0xBA"

    return bytes(code)


# --------------------------------------------------------------------------
# 6. Entete PSID v2
# --------------------------------------------------------------------------

def _padfield(s: str, size: int) -> bytes:
    b = s.encode("ascii", errors="replace")[: size - 1]
    return b + b"\x00" * (size - len(b))


def build_psid(
    name: str,
    author: str,
    released: str,
    init_addr: int,
    play_addr: int,
    binary: bytes,
    pal: bool = True,
) -> bytes:
    """Construit un fichier PSID v2 complet."""
    header = bytearray(0x7C)
    header[0:4] = b"PSID"
    struct.pack_into(">H", header, 0x04, 0x0002)  # version 2
    struct.pack_into(">H", header, 0x06, 0x007C)  # data offset
    struct.pack_into(">H", header, 0x08, 0x0000)  # load addr (0 = dans data)
    struct.pack_into(">H", header, 0x0A, init_addr)
    struct.pack_into(">H", header, 0x0C, play_addr)
    struct.pack_into(">H", header, 0x0E, 0x0001)  # 1 song
    struct.pack_into(">H", header, 0x10, 0x0001)  # start song
    struct.pack_into(">I", header, 0x12, 0x00000001)  # speed: bit0 = CIA timer
    header[0x16:0x36] = _padfield(name, 32)
    header[0x36:0x56] = _padfield(author, 32)
    header[0x56:0x76] = _padfield(released, 32)
    # flags: bits 2-3 video (01=PAL, 10=NTSC), bits 4-5 SID model (01=6581)
    video_bits = 0b01 if pal else 0b10
    flags = (video_bits << 2) | (0b01 << 4)  # PAL/NTSC + 6581
    struct.pack_into(">H", header, 0x76, flags)
    # startPage / pageLength / 2nd-3rd SID a 0
    return bytes(header) + binary


# --------------------------------------------------------------------------
# 7. Pipeline principal
# --------------------------------------------------------------------------

PAL_CPU_HZ = 985248
NTSC_CPU_HZ = 1022727


def cia_reload_for_rate(sample_rate: int, pal: bool) -> int:
    cpu = PAL_CPU_HZ if pal else NTSC_CPU_HZ
    reload = int(round(cpu / sample_rate))
    if reload < 16:
        reload = 16
    if reload > 0xFFFF:
        reload = 0xFFFF
    return reload


def main() -> int:
    p = argparse.ArgumentParser(
        description="Convertit un WAV en fichier .sid (PSID v2) jouable par "
        "VICE/SIDPLAY via la technique digi-sample 4-bit ($D418)."
    )
    p.add_argument("input", help="WAV PCM source (8/16/24/32-bit, mono ou stereo)")
    p.add_argument("output", help="Fichier .sid de sortie")
    p.add_argument("--rate", type=int, default=8000,
                   help="Frequence d'echantillonnage cible en Hz (def: 8000)")
    p.add_argument("--max-bytes", type=int, default=32768,
                   help="Taille max des donnees nibble-packees (def: 32768)")
    p.add_argument("--name", default="Audio Sample", help="Titre PSID")
    p.add_argument("--author", default="audio2sid", help="Auteur PSID")
    p.add_argument("--released", default="2026 The Place",
                   help="Champ released PSID")
    p.add_argument("--no-loop", action="store_true",
                   help="Ne pas boucler (silence en fin)")
    p.add_argument("--no-preemphasis", action="store_true",
                   help="Desactive la pre-emphasis HF")
    p.add_argument("--ntsc", action="store_true",
                   help="Cible NTSC (def: PAL)")
    args = p.parse_args()

    if not os.path.isfile(args.input):
        print(f"Erreur: fichier introuvable: {args.input}", file=sys.stderr)
        return 1

    print(f"[1/6] Lecture WAV: {args.input}")
    samples, src_rate = read_wav_mono(args.input)
    duration = len(samples) / src_rate if src_rate else 0
    print(f"      {len(samples)} echantillons @ {src_rate} Hz ({duration:.2f}s)")

    print(f"[2/6] Reechantillonnage -> {args.rate} Hz")
    samples = resample_linear(samples, src_rate, args.rate)

    if not args.no_preemphasis:
        print("[3/6] Pre-emphasis + normalisation")
        samples = pre_emphasis(samples, alpha=0.20)
    else:
        print("[3/6] Normalisation")
    samples = normalize(samples)

    print("[4/6] Quantification 4-bit + packing nibbles")
    nibs = quantize_4bit(samples)
    packed = pack_nibbles(nibs)

    if len(packed) > args.max_bytes:
        print(f"      Tronque: {len(packed)} -> {args.max_bytes} octets")
        packed = packed[: args.max_bytes]
    final_duration = (len(packed) * 2) / args.rate
    print(f"      {len(packed)} octets ({final_duration:.2f}s effectif)")

    end_addr = SAMPLE_ADDR + len(packed)
    if end_addr > 0xD000:
        print(f"Erreur: depasse l'espace I/O ($D000). end={end_addr:04X}",
              file=sys.stderr)
        return 1

    print("[5/6] Assemblage du player 6510")
    pal = not args.ntsc
    reload = cia_reload_for_rate(args.rate, pal)
    actual_rate = (PAL_CPU_HZ if pal else NTSC_CPU_HZ) / reload
    print(f"      CIA reload = ${reload:04X} ({reload}) -> {actual_rate:.1f} Hz reel")
    player = assemble_player(reload, end_addr, loop=not args.no_loop)
    binary = player + packed
    # Prefixer l'adresse de chargement (LE) pour la zone data PSID.
    data_section = struct.pack("<H", PLAYER_BASE) + binary

    print(f"[6/6] Construction PSID -> {args.output}")
    psid = build_psid(
        name=args.name,
        author=args.author,
        released=args.released,
        init_addr=INIT_ADDR,
        play_addr=PLAY_ADDR,
        binary=data_section,
        pal=pal,
    )
    with open(args.output, "wb") as f:
        f.write(psid)
    print(f"      {len(psid)} octets ecrits.")
    print()
    print("OK. Lecture: x64sc -keepmonopen "
          f"{args.output}  ou  sidplayfp {args.output}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
