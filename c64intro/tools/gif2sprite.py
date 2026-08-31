#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
gif2sprite.py — Générateur de sprites depuis un GIF ou un MP4.

Cibles supportées :
  - C64 (Commodore 64, VIC-II)
      * Hi-res     : 24 x 21, 1 bit/pixel, 63+1 octets
      * Multicolor : 12 x 21 logique, 2 bits/pixel, 63+1 octets
  - Amiga 500 (OCS, Denise)
      * Sprite hardware 16 x H (H configurable, défaut 21)
      * 2 bitplanes interleavés (4 couleurs : transparent + 3)
      * En-tête SPRxPOS/SPRxCTL + lignes plane A/plane B + terminator 0,0
      * Mots big-endian m68k, sortie vasm/devpac (dc.w) ou binaire

Sorties : KickAssembler (.asm), ACME (.a), vasm/devpac (.s), binaire brut.

Exemples :
    # C64 multicolor
    python3 gif2sprite.py logo.gif -o data/logo.asm --target c64 \
        --mode multicolor --label logo --address '$3000'

    # Amiga 500, vasm
    python3 gif2sprite.py logo.gif -o data/logo.s --target amiga \
        --label logo --height 21 --vstart 80 --hstart 128
"""

from __future__ import annotations

import argparse
import os
import sys
from dataclasses import dataclass

import numpy as np
from PIL import Image

# Palette C64 standard (Pepto). Index 0..15, valeurs RGB.
C64_PALETTE = np.array([
    (0x00, 0x00, 0x00),   # 0  Noir
    (0xFF, 0xFF, 0xFF),   # 1  Blanc
    (0x88, 0x00, 0x00),   # 2  Rouge
    (0xAA, 0xFF, 0xEE),   # 3  Cyan
    (0xCC, 0x44, 0xCC),   # 4  Violet
    (0x00, 0xCC, 0x55),   # 5  Vert
    (0x00, 0x00, 0xAA),   # 6  Bleu
    (0xEE, 0xEE, 0x77),   # 7  Jaune
    (0xDD, 0x88, 0x55),   # 8  Orange
    (0x66, 0x44, 0x00),   # 9  Marron
    (0xFF, 0x77, 0x77),   # 10 Rouge clair
    (0x33, 0x33, 0x33),   # 11 Gris foncé
    (0x77, 0x77, 0x77),   # 12 Gris moyen
    (0xAA, 0xFF, 0x66),   # 13 Vert clair
    (0x00, 0x88, 0xFF),   # 14 Bleu clair
    (0xBB, 0xBB, 0xBB),   # 15 Gris clair
], dtype=np.int16)

SPRITE_W = 24
SPRITE_H = 21
SPRITE_BYTES = 64  # 63 utiles + 1 de padding

# Amiga OCS : sprite 16 px de large, hauteur variable.
AMIGA_SPRITE_W = 16


def rgb_to_ocs12(rgb: tuple[int, int, int]) -> int:
    """Convertit un RGB 24 bits vers la valeur 12 bits OCS ($XXX)."""
    r, g, b = rgb
    return ((r >> 4) << 8) | ((g >> 4) << 4) | (b >> 4)


def ocs12_to_rgb(v: int) -> tuple[int, int, int]:
    r = (v >> 8) & 0xF
    g = (v >> 4) & 0xF
    b = v & 0xF
    return (r * 0x11, g * 0x11, b * 0x11)


def parse_ocs_color(s: str) -> int:
    """Parse une couleur Amiga OCS : '$f80', '#ff8800' ou 'fa0'."""
    s = s.strip().lower().lstrip("#")
    if s.startswith("$"):
        s = s[1:]
    if len(s) == 3:
        return int(s, 16)
    if len(s) == 6:
        rgb = (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))
        return rgb_to_ocs12(rgb)
    raise argparse.ArgumentTypeError(f"Couleur OCS invalide : {s}")


# ---------------------------------------------------------------------------
# Lecture des frames
# ---------------------------------------------------------------------------

def load_frames(path: str, fps: float | None, max_frames: int | None,
                start_frame: int, frame_step: int) -> list[Image.Image]:
    """Charge les frames depuis un GIF ou un MP4 (ou toute image PIL)."""
    ext = os.path.splitext(path)[1].lower()
    frames: list[Image.Image] = []

    if ext in (".gif", ".webp", ".apng"):
        im = Image.open(path)
        idx = 0
        try:
            while True:
                im.seek(idx)
                frames.append(im.convert("RGBA").copy())
                idx += 1
        except EOFError:
            pass

    elif ext in (".mp4", ".mov", ".avi", ".mkv", ".webm"):
        try:
            import imageio.v3 as iio
        except ImportError:
            sys.exit("imageio est requis pour lire les vidéos : pip install imageio imageio-ffmpeg")

        meta = iio.immeta(path, plugin="pyav") if False else {}
        try:
            meta = iio.immeta(path)
        except Exception:
            meta = {}
        src_fps = float(meta.get("fps", 25.0)) if meta else 25.0
        target_fps = fps if fps else src_fps
        sample_step = max(1, int(round(src_fps / target_fps)))

        for i, frame in enumerate(iio.imiter(path)):
            if i % sample_step != 0:
                continue
            frames.append(Image.fromarray(frame).convert("RGBA"))

    else:
        # Image fixe
        frames.append(Image.open(path).convert("RGBA"))

    # Échantillonnage utilisateur (start / step / max)
    frames = frames[start_frame::max(1, frame_step)]
    if max_frames is not None:
        frames = frames[:max_frames]
    if not frames:
        sys.exit(f"Aucune frame extraite depuis {path}")
    return frames


# ---------------------------------------------------------------------------
# Mise à l'échelle vers la grille du sprite
# ---------------------------------------------------------------------------

def fit_to_sprite(img: Image.Image, mode: str, fit: str,
                  bg_rgb: tuple[int, int, int]) -> Image.Image:
    """Redimensionne une frame vers la grille logique du sprite.

    En multicolor la grille logique est 12x21 (les pixels sont 2x larges).
    On rend toujours le résultat dans une image RGB 24x21 finale en doublant
    les pixels horizontalement quand mode == multicolor.
    """
    target_w = 12 if mode == "multicolor" else SPRITE_W
    target_h = SPRITE_H

    src = img.convert("RGBA")
    sw, sh = src.size

    if fit == "stretch":
        scaled = src.resize((target_w, target_h), Image.LANCZOS)
    else:
        # contain ou cover
        sx = target_w / sw
        sy = target_h / sh
        scale = min(sx, sy) if fit == "contain" else max(sx, sy)
        nw = max(1, int(round(sw * scale)))
        nh = max(1, int(round(sh * scale)))
        resized = src.resize((nw, nh), Image.LANCZOS)
        scaled = Image.new("RGBA", (target_w, target_h), bg_rgb + (255,))
        if fit == "contain":
            scaled.paste(resized, ((target_w - nw) // 2, (target_h - nh) // 2), resized)
        else:  # cover : centre puis crop
            ox = (nw - target_w) // 2
            oy = (nh - target_h) // 2
            scaled.paste(resized.crop((ox, oy, ox + target_w, oy + target_h)),
                         (0, 0), resized.crop((ox, oy, ox + target_w, oy + target_h)))

    # Aplatir alpha sur le fond
    flat = Image.new("RGB", scaled.size, bg_rgb)
    flat.paste(scaled, mask=scaled.split()[3] if scaled.mode == "RGBA" else None)

    if mode == "multicolor":
        # Doubler horizontalement pour produire l'image 24x21 finale
        flat = flat.resize((SPRITE_W, target_h), Image.NEAREST)
    return flat


# ---------------------------------------------------------------------------
# Mapping vers la palette C64
# ---------------------------------------------------------------------------

def nearest_c64(rgb: np.ndarray, allowed: list[int]) -> np.ndarray:
    """Pour chaque pixel RGB d'une image (H, W, 3), retourne l'index C64
    le plus proche parmi `allowed` (distance euclidienne pondérée)."""
    palette = C64_PALETTE[allowed]               # (k, 3)
    flat = rgb.reshape(-1, 3).astype(np.int16)   # (N, 3)
    # Pondération perceptuelle simple
    weights = np.array([0.30, 0.59, 0.11], dtype=np.float32)
    diff = (flat[:, None, :] - palette[None, :, :]).astype(np.float32)
    dist = ((diff ** 2) * weights).sum(axis=2)
    idx_in_allowed = np.argmin(dist, axis=1)
    mapped = np.array(allowed, dtype=np.uint8)[idx_in_allowed]
    return mapped.reshape(rgb.shape[:2])


def quantize_hires(img: Image.Image, fg: int, bg: int,
                   threshold: int, dither: bool) -> np.ndarray:
    """Hi-res : bichromie. Retourne un tableau (21, 24) avec valeurs 0/1."""
    if dither:
        gray = img.convert("L").quantize(colors=2, dither=Image.FLOYDSTEINBERG)
        arr = np.array(gray.convert("L"))
    else:
        arr = np.array(img.convert("L"))
    bits = (arr >= threshold).astype(np.uint8)
    # 1 = couleur sprite (fg), 0 = transparent (bg)
    return bits


def quantize_multicolor(img: Image.Image, bg: int, mc1: int, mc2: int,
                        sprite_color: int) -> np.ndarray:
    """Multicolor : retourne un tableau (21, 24) avec valeurs codées 0/1/2/3.

    Chaque paire de pixels horizontaux partage la même valeur (double-largeur).
    Codage : 00 = bg/transparent, 01 = mc1, 10 = sprite_color, 11 = mc2.
    """
    rgb = np.array(img.convert("RGB"))
    allowed = [bg, mc1, sprite_color, mc2]
    mapped = nearest_c64(rgb, allowed)            # indices C64 réels
    code = np.zeros_like(mapped, dtype=np.uint8)
    code[mapped == mc1] = 1
    code[mapped == sprite_color] = 2
    code[mapped == mc2] = 3
    # Forcer chaque paire horizontale à une seule valeur (vote majoritaire)
    out = code.copy()
    for x in range(0, SPRITE_W, 2):
        pair = code[:, x:x + 2]
        # majorité ; en cas d'égalité on garde la gauche
        left = pair[:, 0]
        right = pair[:, 1]
        chosen = np.where(left == right, left, left)
        out[:, x] = chosen
        out[:, x + 1] = chosen
    return out


# ---------------------------------------------------------------------------
# Encodage en octets de sprite
# ---------------------------------------------------------------------------

def encode_hires(bits: np.ndarray) -> bytes:
    """(21, 24) booléens -> 64 octets (63 utiles + padding)."""
    out = bytearray()
    for y in range(SPRITE_H):
        row = bits[y]
        for bx in range(0, SPRITE_W, 8):
            byte = 0
            for k in range(8):
                if row[bx + k]:
                    byte |= (1 << (7 - k))
            out.append(byte)
    out.append(0)  # padding 64e octet
    return bytes(out)


def encode_multicolor(codes: np.ndarray) -> bytes:
    """(21, 24) avec valeurs 0..3 (paires identiques) -> 64 octets."""
    out = bytearray()
    for y in range(SPRITE_H):
        row = codes[y]
        for bx in range(0, SPRITE_W, 8):
            byte = 0
            # 4 paires de 2 bits par octet
            for p in range(4):
                v = int(row[bx + p * 2]) & 0x03
                byte |= v << (6 - p * 2)
            out.append(byte)
    out.append(0)
    return bytes(out)


# ---------------------------------------------------------------------------
# Pipeline Amiga 500 (OCS) : 16 px de large, 4 couleurs, 2 bitplanes
# ---------------------------------------------------------------------------

def fit_to_amiga(img: Image.Image, width: int, height: int, fit: str,
                 bg_rgb: tuple[int, int, int]) -> Image.Image:
    """Redimensionne vers la grille du sprite Amiga (W x H, RGB aplati)."""
    src = img.convert("RGBA")
    sw, sh = src.size
    if fit == "stretch":
        scaled = src.resize((width, height), Image.LANCZOS)
    else:
        sx = width / sw
        sy = height / sh
        scale = min(sx, sy) if fit == "contain" else max(sx, sy)
        nw = max(1, int(round(sw * scale)))
        nh = max(1, int(round(sh * scale)))
        resized = src.resize((nw, nh), Image.LANCZOS)
        scaled = Image.new("RGBA", (width, height), bg_rgb + (255,))
        if fit == "contain":
            scaled.paste(resized, ((width - nw) // 2, (height - nh) // 2), resized)
        else:
            ox = (nw - width) // 2
            oy = (nh - height) // 2
            crop = resized.crop((ox, oy, ox + width, oy + height))
            scaled.paste(crop, (0, 0), crop)
    flat = Image.new("RGB", scaled.size, bg_rgb)
    flat.paste(scaled, mask=scaled.split()[3])
    return flat


def amiga_palette_from_frames(scaled_frames: list[Image.Image],
                              bg_rgb: tuple[int, int, int],
                              fixed: list[int] | None) -> tuple[list[int], list[np.ndarray]]:
    """Construit une palette OCS de 4 couleurs (12 bits) cohérente entre frames.

    L'index 0 est toujours réservé à la couleur transparente (bg_rgb).
    Si `fixed` est fourni : 3 valeurs OCS 12 bits pour les couleurs 1,2,3.
    Sinon : quantification PIL sur la concaténation des frames.

    Retourne (palette_ocs[4], grids[N] avec valeurs 0..3 par pixel).
    """
    bg_ocs = rgb_to_ocs12(bg_rgb)

    if fixed is not None:
        palette_ocs = [bg_ocs] + list(fixed[:3])
        # compléter à 4
        while len(palette_ocs) < 4:
            palette_ocs.append(0x000)
        palette_rgb = np.array([ocs12_to_rgb(c) for c in palette_ocs], dtype=np.int16)
        grids = []
        for fr in scaled_frames:
            arr = np.array(fr.convert("RGB")).astype(np.int16)
            diff = arr[:, :, None, :] - palette_rgb[None, None, :, :]
            dist = (diff.astype(np.float32) ** 2).sum(axis=3)
            grids.append(np.argmin(dist, axis=2).astype(np.uint8))
        return palette_ocs, grids

    # Quantification automatique : concaténation puis PIL.quantize(4)
    total_w = sum(f.size[0] for f in scaled_frames)
    max_h = max(f.size[1] for f in scaled_frames)
    concat = Image.new("RGB", (total_w, max_h), bg_rgb)
    x = 0
    for f in scaled_frames:
        concat.paste(f, (x, 0))
        x += f.size[0]
    quantized = concat.quantize(colors=4, method=Image.MEDIANCUT, dither=Image.NONE)
    pal_raw = quantized.getpalette()[:12]
    pal_rgb = [(pal_raw[i * 3], pal_raw[i * 3 + 1], pal_raw[i * 3 + 2])
               for i in range(4)]

    # Réordonner : la couleur la plus proche du fond va en index 0.
    def dist2(a, b):
        return sum((x - y) ** 2 for x, y in zip(a, b))
    bg_idx = min(range(len(pal_rgb)), key=lambda i: dist2(pal_rgb[i], bg_rgb))
    pal_rgb[0], pal_rgb[bg_idx] = pal_rgb[bg_idx], pal_rgb[0]

    palette_ocs = [rgb_to_ocs12(c) for c in pal_rgb]
    palette_rgb_arr = np.array(pal_rgb, dtype=np.int16)
    grids = []
    for fr in scaled_frames:
        arr = np.array(fr.convert("RGB")).astype(np.int16)
        diff = arr[:, :, None, :] - palette_rgb_arr[None, None, :, :]
        dist = (diff.astype(np.float32) ** 2).sum(axis=3)
        grids.append(np.argmin(dist, axis=2).astype(np.uint8))
    return palette_ocs, grids


def encode_amiga_sprite(grid: np.ndarray, vstart: int, hstart: int,
                        attached: bool) -> bytes:
    """grid (H, 16) avec valeurs 0..3 -> mots big-endian m68k.

    Format DMA Amiga par sprite :
      [SPRxPOS][SPRxCTL]                            (2 mots = 4 octets)
      [planeA_row0][planeB_row0]                    (2 mots / ligne)
      ...
      [0][0]                                        (terminator)

    SPRxPOS bits : VVVVVVVV HHHHHHHH (V=VSTART low8, H=HSTART bits 8..1)
    SPRxCTL bits : EEEEEEEE A.....VEH (E=VSTOP low8, A=ATTACH, V=VSTART hi,
                                        E=VSTOP hi, H=HSTART bit0)
    """
    H = grid.shape[0]
    vstop = vstart + H

    pos = ((vstart & 0xFF) << 8) | ((hstart >> 1) & 0xFF)
    ctl = (vstop & 0xFF) << 8
    if attached:
        ctl |= 0x80
    if vstart & 0x100:
        ctl |= 0x04
    if vstop & 0x100:
        ctl |= 0x02
    if hstart & 0x01:
        ctl |= 0x01

    out = bytearray()
    out += pos.to_bytes(2, "big")
    out += ctl.to_bytes(2, "big")
    for y in range(H):
        row = grid[y]
        a = b = 0
        for x in range(AMIGA_SPRITE_W):
            v = int(row[x]) & 0x3
            if v & 1:
                a |= 1 << (15 - x)
            if v & 2:
                b |= 1 << (15 - x)
        out += a.to_bytes(2, "big")
        out += b.to_bytes(2, "big")
    out += b"\x00\x00\x00\x00"
    return bytes(out)


# ---------------------------------------------------------------------------
# Génération des fichiers de sortie
# ---------------------------------------------------------------------------

C64_COLOR_NAMES = [
    "BLACK", "WHITE", "RED", "CYAN", "PURPLE", "GREEN", "BLUE", "YELLOW",
    "ORANGE", "BROWN", "LIGHT_RED", "DARK_GREY", "MED_GREY", "LIGHT_GREEN",
    "LIGHT_BLUE", "LIGHT_GREY",
]


def emit_kickass(frames_bytes: list[bytes], label: str, mode: str,
                 colors: dict, src: str, address: int | None,
                 screen_base: int, no_macros: bool) -> str:
    """Émet un .asm KickAssembler directement utilisable via #import.

    Le fichier définit :
      - les constantes <LABEL>_FRAMES, <LABEL>_FRAME_BYTES, couleurs, flag MC
      - un segment .pc (pinné si --address fourni, sinon * pour relocatable)
      - le label <label> aligné sur 64 octets
      - .const <LABEL>_PTR = <label> / 64 (valeur pour le pointeur sprite VIC)
      - macros install_<label>(num) et set_<label>_frame(num, frame) pour câbler
        directement les registres VIC-II ($D000+, $D015, $D01C, $D025/26/27+)
    """
    UP = label.upper()
    is_mc = 1 if mode == "multicolor" else 0
    lines: list[str] = []

    # En-tête + exemple d'intégration --------------------------------------
    lines.append("// " + "=" * 58)
    lines.append(f"// Sprites C64 générés depuis : {os.path.basename(src)}")
    lines.append(f"// Mode      : {mode}    Frames : {len(frames_bytes)}    "
                 f"Octets : {len(frames_bytes) * SPRITE_BYTES}")
    if mode == "multicolor":
        lines.append(f"// Couleurs  : bg=${colors['bg']:02x}  mc1=${colors['mc1']:02x}  "
                     f"mc2=${colors['mc2']:02x}  sprite=${colors['fg']:02x}")
    else:
        lines.append(f"// Couleurs  : bg=${colors['bg']:02x}  fg=${colors['fg']:02x}")
    lines.append("//")
    lines.append("// Intégration dans main.asm :")
    lines.append(f"//     #import \"{os.path.basename('data/' + label)}.asm\"")
    lines.append(f"//     install_{label}(0)              // affecte sprite 0")
    lines.append(f"//     lda #100  : sta $d000           // X")
    lines.append(f"//     lda #120  : sta $d001           // Y")
    lines.append(f"//     set_{label}_frame(0, 2)         // change la frame")
    lines.append("// " + "=" * 58)
    lines.append("")

    # Constantes -----------------------------------------------------------
    lines.append(f".const {UP}_FRAMES        = {len(frames_bytes)}")
    lines.append(f".const {UP}_FRAME_BYTES   = {SPRITE_BYTES}")
    lines.append(f".const {UP}_IS_MULTICOLOR = {is_mc}")
    lines.append(f".const {UP}_BG_COLOR      = ${colors['bg']:02x}")
    lines.append(f".const {UP}_FG_COLOR      = ${colors['fg']:02x}")
    if mode == "multicolor":
        lines.append(f".const {UP}_MC1_COLOR     = ${colors['mc1']:02x}")
        lines.append(f".const {UP}_MC2_COLOR     = ${colors['mc2']:02x}")
    lines.append(f".const {UP}_SCREEN_BASE   = ${screen_base:04x}")
    lines.append("")

    # Segment de données ---------------------------------------------------
    if address is not None:
        lines.append(f".pc = ${address:04x} \"{label} data\"")
    else:
        lines.append(f".pc = * \"{label} data\"")
    lines.append(".align $40")
    lines.append(f"{label}:")
    for i, fb in enumerate(frames_bytes):
        lines.append(f"// --- frame {i} ---")
        for row in range(SPRITE_H):
            chunk = fb[row * 3:(row + 1) * 3]
            lines.append("    .byte " + ", ".join(f"${b:02x}" for b in chunk))
        lines.append("    .byte $00")
    lines.append("")
    lines.append(f".const {UP}_PTR = {label} / 64")
    lines.append("")

    # Macros d'installation ------------------------------------------------
    if not no_macros:
        lines.append("// Active le sprite num (0-7), pose le pointeur, la couleur,")
        lines.append("// le bit multicolor le cas échéant et le bit d'enable.")
        lines.append(f".macro install_{label}(num) {{")
        lines.append(f"    lda #{UP}_PTR")
        lines.append(f"    sta {UP}_SCREEN_BASE + $03f8 + num")
        lines.append(f"    lda #{UP}_FG_COLOR")
        lines.append("    sta $d027 + num")
        if mode == "multicolor":
            lines.append(f"    lda #{UP}_MC1_COLOR")
            lines.append("    sta $d025")
            lines.append(f"    lda #{UP}_MC2_COLOR")
            lines.append("    sta $d026")
            lines.append("    lda $d01c")
            lines.append("    ora #(1 << num)")
            lines.append("    sta $d01c")
        else:
            lines.append("    lda $d01c")
            lines.append("    and #($ff ^ (1 << num))")
            lines.append("    sta $d01c")
        lines.append("    lda $d015")
        lines.append("    ora #(1 << num)")
        lines.append("    sta $d015")
        lines.append("}")
        lines.append("")
        lines.append("// Change la frame courante du sprite num en repointant")
        lines.append(f".macro set_{label}_frame(num, frame) {{")
        lines.append(f"    lda #{UP}_PTR + frame")
        lines.append(f"    sta {UP}_SCREEN_BASE + $03f8 + num")
        lines.append("}")
        lines.append("")
        lines.append("// Variante runtime : A = numéro de frame (0..FRAMES-1), X = num sprite")
        lines.append(f"set_{label}_frame_a:")
        lines.append(f"    clc")
        lines.append(f"    adc #{UP}_PTR")
        lines.append(f"    sta {UP}_SCREEN_BASE + $03f8, x")
        lines.append("    rts")
        lines.append("")
    return "\n".join(lines) + "\n"


def emit_acme(frames_bytes: list[bytes], label: str, mode: str,
              colors: dict, src: str, address: int | None,
              screen_base: int, no_macros: bool) -> str:
    """Émet un .a ACME directement utilisable via !source.

    Pose les constantes équivalentes et un label aligné 64 octets.
    """
    UP = label.upper()
    is_mc = 1 if mode == "multicolor" else 0
    lines: list[str] = []
    lines.append("; " + "=" * 58)
    lines.append(f"; Sprites C64 générés depuis : {os.path.basename(src)}  (mode {mode})")
    lines.append(f"; Frames : {len(frames_bytes)}, {SPRITE_BYTES} octets/frame")
    lines.append(";")
    lines.append(f";   !source \"{label}.a\"")
    lines.append(f";   jsr install_{label}        ; X = num sprite, A = num frame")
    lines.append("; " + "=" * 58)
    lines.append("")
    lines.append(f"{UP}_FRAMES        = {len(frames_bytes)}")
    lines.append(f"{UP}_FRAME_BYTES   = {SPRITE_BYTES}")
    lines.append(f"{UP}_IS_MULTICOLOR = {is_mc}")
    lines.append(f"{UP}_BG_COLOR      = ${colors['bg']:02x}")
    lines.append(f"{UP}_FG_COLOR      = ${colors['fg']:02x}")
    if mode == "multicolor":
        lines.append(f"{UP}_MC1_COLOR     = ${colors['mc1']:02x}")
        lines.append(f"{UP}_MC2_COLOR     = ${colors['mc2']:02x}")
    lines.append(f"{UP}_SCREEN_BASE   = ${screen_base:04x}")
    lines.append("")
    if address is not None:
        lines.append(f"* = ${address:04x}")
    lines.append("!align 63, 0")
    lines.append(f"{label}:")
    for i, fb in enumerate(frames_bytes):
        lines.append(f"; --- frame {i} ---")
        for row in range(SPRITE_H):
            chunk = fb[row * 3:(row + 1) * 3]
            lines.append("    !byte " + ", ".join(f"${b:02x}" for b in chunk))
        lines.append("    !byte $00")
    lines.append("")
    lines.append(f"{UP}_PTR = {label} / 64")
    lines.append("")
    if not no_macros:
        lines.append(f"; X = num sprite (0..7), A = numéro de frame")
        lines.append(f"install_{label}:")
        lines.append(f"    pha")
        lines.append(f"    lda #<{UP}_FG_COLOR")
        lines.append(f"    sta $d027,x")
        if mode == "multicolor":
            lines.append(f"    lda #{UP}_MC1_COLOR : sta $d025")
            lines.append(f"    lda #{UP}_MC2_COLOR : sta $d026")
            lines.append(f"    lda $d01c")
            lines.append(f"    ora bit_table,x")
            lines.append(f"    sta $d01c")
        lines.append(f"    lda $d015")
        lines.append(f"    ora bit_table,x")
        lines.append(f"    sta $d015")
        lines.append(f"    pla")
        lines.append(f"    clc")
        lines.append(f"    adc #{UP}_PTR")
        lines.append(f"    sta {UP}_SCREEN_BASE + $3f8,x")
        lines.append(f"    rts")
        lines.append(f"bit_table: !byte $01,$02,$04,$08,$10,$20,$40,$80")
    return "\n".join(lines) + "\n"


def emit_binary_with_wrapper(frames_bytes: list[bytes], label: str, mode: str,
                             colors: dict, output_bin: str, address: int | None,
                             screen_base: int) -> str:
    """Émet un wrapper KickAssembler qui inclut le binaire via .import binary.

    Sortie : le .bin (à output_bin) + un .asm wrapper retourné en chaîne.
    """
    UP = label.upper()
    is_mc = 1 if mode == "multicolor" else 0
    bin_name = os.path.basename(output_bin)
    lines: list[str] = []
    lines.append("// " + "=" * 58)
    lines.append(f"// Wrapper de sprites — données brutes : {bin_name}")
    lines.append(f"// Frames : {len(frames_bytes)}    Mode : {mode}")
    lines.append("// " + "=" * 58)
    lines.append("")
    lines.append(f".const {UP}_FRAMES        = {len(frames_bytes)}")
    lines.append(f".const {UP}_FRAME_BYTES   = {SPRITE_BYTES}")
    lines.append(f".const {UP}_IS_MULTICOLOR = {is_mc}")
    lines.append(f".const {UP}_BG_COLOR      = ${colors['bg']:02x}")
    lines.append(f".const {UP}_FG_COLOR      = ${colors['fg']:02x}")
    if mode == "multicolor":
        lines.append(f".const {UP}_MC1_COLOR     = ${colors['mc1']:02x}")
        lines.append(f".const {UP}_MC2_COLOR     = ${colors['mc2']:02x}")
    lines.append(f".const {UP}_SCREEN_BASE   = ${screen_base:04x}")
    lines.append("")
    if address is not None:
        lines.append(f".pc = ${address:04x} \"{label} data\"")
    else:
        lines.append(f".pc = * \"{label} data\"")
    lines.append(".align $40")
    lines.append(f"{label}:")
    lines.append(f"    .import binary \"{bin_name}\"")
    lines.append("")
    lines.append(f".const {UP}_PTR = {label} / 64")
    return "\n".join(lines) + "\n"


# ---------------------------------------------------------------------------
# Émetteurs Amiga (vasm/devpac/asmone — syntaxe motorola dc.w)
# ---------------------------------------------------------------------------

def emit_amiga_vasm(frames_bytes: list[bytes], label: str, height: int,
                    palette_ocs: list[int], src: str, address: int | None,
                    sprite_num: int, no_macros: bool) -> str:
    """Émet un .s vasm/devpac directement assemblable.

    Sépare en frames lisibles avec les mots SPRxPOS/SPRxCTL en en-tête,
    les paires plane A / plane B par ligne, puis le terminator 0,0.

    Pose constantes <LABEL>_HEIGHT, _FRAMES, _FRAME_BYTES, _COLOR1/2/3
    (valeurs OCS 12 bits prêtes à mettre dans COLOR17/18/19).

    Sous-routine setup_<label>_spr<n> : set SPRxPT, COLOR17-19, DMACON.
    """
    UP = label.upper()
    bytes_per_frame = 8 + 4 * height
    custom_base = 0xDFF000
    sprpt = custom_base + 0x120 + sprite_num * 4   # SPR0PT=$dff120
    color_base = custom_base + 0x1A2 + sprite_num // 2 * 8   # COLOR17 pour SPR0/1
    # DMACON : SET | DMAEN (bit 9) | SPREN (bit 5) ; le bit SPREN est partagé.
    dmacon_value = 0x8000 | 0x0200 | 0x0020

    lines: list[str] = []
    lines.append("; " + "=" * 58)
    lines.append(f"; Sprite Amiga 500 (OCS) généré depuis : {os.path.basename(src)}")
    lines.append(f"; Taille      : {AMIGA_SPRITE_W} x {height}")
    lines.append(f"; Frames      : {len(frames_bytes)}    "
                 f"Octets/frame : {bytes_per_frame}    "
                 f"Total : {bytes_per_frame * len(frames_bytes)}")
    lines.append(f"; Palette OCS : transparent=${palette_ocs[0]:03x}  "
                 f"col1=${palette_ocs[1]:03x}  col2=${palette_ocs[2]:03x}  "
                 f"col3=${palette_ocs[3]:03x}")
    lines.append(";")
    lines.append("; Intégration :")
    lines.append(f";   include \"{label}.s\"")
    lines.append(f";   bsr     setup_{label}_spr{sprite_num}    ; pose pointeur, palette, DMA")
    lines.append(f";   ; pour changer de frame : ")
    lines.append(f";   move.l  #{label}+{UP}_FRAME_BYTES*N,${sprpt:06x}")
    lines.append("; " + "=" * 58)
    lines.append("")
    lines.append(f"{UP}_WIDTH       equ  {AMIGA_SPRITE_W}")
    lines.append(f"{UP}_HEIGHT      equ  {height}")
    lines.append(f"{UP}_FRAMES      equ  {len(frames_bytes)}")
    lines.append(f"{UP}_FRAME_BYTES equ  {bytes_per_frame}")
    lines.append(f"{UP}_COLOR1      equ  ${palette_ocs[1]:03x}")
    lines.append(f"{UP}_COLOR2      equ  ${palette_ocs[2]:03x}")
    lines.append(f"{UP}_COLOR3      equ  ${palette_ocs[3]:03x}")
    lines.append(f"{UP}_SPRITE_NUM  equ  {sprite_num}")
    lines.append(f"{UP}_SPRPT       equ  ${sprpt:06x}    ; SPR{sprite_num}PT")
    lines.append(f"{UP}_COLOR_REG   equ  ${color_base:06x}")
    lines.append("")
    if address is not None:
        lines.append(f"    org     ${address:08x}")
    lines.append("    even")
    lines.append(f"{label}:")
    for i, fb in enumerate(frames_bytes):
        words = [(fb[j] << 8) | fb[j + 1] for j in range(0, len(fb), 2)]
        lines.append(f"; --- frame {i} ---")
        lines.append(f"    dc.w    ${words[0]:04x},${words[1]:04x}      "
                     f"; SPR{sprite_num}POS, SPR{sprite_num}CTL")
        for r in range(height):
            o = 2 + r * 2
            lines.append(f"    dc.w    ${words[o]:04x},${words[o + 1]:04x}")
        lines.append(f"    dc.w    ${words[-2]:04x},${words[-1]:04x}      ; terminator")
    lines.append("")

    if not no_macros:
        lines.append("; ----------------------------------------------------------")
        lines.append(f"; Installe le sprite : pose SPR{sprite_num}PT, palette,")
        lines.append("; et active le DMA sprite (SPREN dans DMACON).")
        lines.append("; ----------------------------------------------------------")
        lines.append(f"setup_{label}_spr{sprite_num}:")
        lines.append(f"    move.l  #{label},{UP}_SPRPT")
        lines.append(f"    move.w  #{UP}_COLOR1,{UP}_COLOR_REG")
        lines.append(f"    move.w  #{UP}_COLOR2,{UP}_COLOR_REG+2")
        lines.append(f"    move.w  #{UP}_COLOR3,{UP}_COLOR_REG+4")
        lines.append(f"    move.w  #${dmacon_value:04x},$dff096    "
                     f"; DMACON : SET | DMAEN | SPREN")
        lines.append("    rts")
        lines.append("")
        lines.append("; Change de frame : D0.w = numéro de frame (0..FRAMES-1)")
        lines.append(f"set_{label}_frame:")
        lines.append("    mulu.w  #" + UP + "_FRAME_BYTES,d0")
        lines.append(f"    add.l   #{label},d0")
        lines.append(f"    move.l  d0,{UP}_SPRPT")
        lines.append("    rts")
    return "\n".join(lines) + "\n"


def emit_amiga_binary_wrapper(frames_bytes: list[bytes], label: str, height: int,
                              palette_ocs: list[int], output_bin: str,
                              address: int | None, sprite_num: int) -> str:
    """Wrapper vasm qui incbin le binaire Amiga."""
    UP = label.upper()
    bytes_per_frame = 8 + 4 * height
    bin_name = os.path.basename(output_bin)
    custom_base = 0xDFF000
    sprpt = custom_base + 0x120 + sprite_num * 4
    color_base = custom_base + 0x1A2 + sprite_num // 2 * 8

    lines: list[str] = []
    lines.append("; " + "=" * 58)
    lines.append(f"; Wrapper sprite Amiga — données brutes : {bin_name}")
    lines.append(f"; {AMIGA_SPRITE_W}x{height}, {len(frames_bytes)} frames, "
                 f"{bytes_per_frame} octets/frame")
    lines.append("; " + "=" * 58)
    lines.append("")
    lines.append(f"{UP}_WIDTH       equ  {AMIGA_SPRITE_W}")
    lines.append(f"{UP}_HEIGHT      equ  {height}")
    lines.append(f"{UP}_FRAMES      equ  {len(frames_bytes)}")
    lines.append(f"{UP}_FRAME_BYTES equ  {bytes_per_frame}")
    lines.append(f"{UP}_COLOR1      equ  ${palette_ocs[1]:03x}")
    lines.append(f"{UP}_COLOR2      equ  ${palette_ocs[2]:03x}")
    lines.append(f"{UP}_COLOR3      equ  ${palette_ocs[3]:03x}")
    lines.append(f"{UP}_SPRPT       equ  ${sprpt:06x}")
    lines.append(f"{UP}_COLOR_REG   equ  ${color_base:06x}")
    lines.append("")
    if address is not None:
        lines.append(f"    org     ${address:08x}")
    lines.append("    even")
    lines.append(f"{label}:")
    lines.append(f"    incbin  \"{bin_name}\"")
    return "\n".join(lines) + "\n"


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def parse_color(value: str) -> int:
    s = value.strip().lower()
    if s.startswith("$"):
        return int(s[1:], 16)
    if s.startswith("0x"):
        return int(s, 16)
    if s.isalpha() or "_" in s:
        try:
            return C64_COLOR_NAMES.index(s.upper())
        except ValueError:
            raise argparse.ArgumentTypeError(f"Couleur inconnue : {value}")
    n = int(s)
    if not 0 <= n <= 15:
        raise argparse.ArgumentTypeError(f"Index de couleur hors plage 0..15 : {value}")
    return n


def build_argparser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        description="Génère des sprites C64 (24x21) ou Amiga 500 (16xH) depuis un GIF ou MP4.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    p.add_argument("input", help="Fichier source (.gif, .mp4, .png, ...)")
    p.add_argument("-o", "--output", default=None,
                   help="Fichier de sortie. Défaut : <input>.asm/.s (ou .bin si --syntax bin)")
    p.add_argument("--target", choices=("c64", "amiga"), default="c64",
                   help="Plateforme cible : C64 (VIC-II) ou Amiga 500 (OCS)")
    p.add_argument("--mode", choices=("hires", "multicolor"), default="multicolor",
                   help="C64 uniquement : hi-res 1 bpp ou multicolor 2 bpp")
    p.add_argument("--syntax", choices=("kickass", "acme", "vasm", "bin"), default=None,
                   help="Format de sortie. Défaut : kickass pour C64, vasm pour Amiga")
    p.add_argument("--label", default="sprite_data",
                   help="Label assembleur du bloc de données")

    p.add_argument("--bg-color", type=parse_color, default=0,
                   help="Couleur fond/transparent (0..15 ou nom)")
    p.add_argument("--fg-color", type=parse_color, default=1,
                   help="Couleur principale du sprite (hi-res : couleur 1 ; "
                        "multicolor : couleur individuelle du sprite)")
    p.add_argument("--mc1", type=parse_color, default=11,
                   help="Multicolor 0 partagé ($D025) — code 01")
    p.add_argument("--mc2", type=parse_color, default=12,
                   help="Multicolor 1 partagé ($D026) — code 11")

    p.add_argument("--fit", choices=("contain", "cover", "stretch"), default="contain",
                   help="Stratégie de mise à l'échelle")
    p.add_argument("--threshold", type=int, default=128,
                   help="Seuil de luminance (mode hi-res)")
    p.add_argument("--no-dither", action="store_true",
                   help="Désactive le tramage Floyd-Steinberg en hi-res")

    p.add_argument("--max-frames", type=int, default=None,
                   help="Nombre maximum de frames à exporter")
    p.add_argument("--start-frame", type=int, default=0,
                   help="Frame de départ")
    p.add_argument("--frame-step", type=int, default=1,
                   help="Conserver une frame sur N")
    p.add_argument("--fps", type=float, default=None,
                   help="Fréquence de rééchantillonnage (vidéos uniquement)")

    p.add_argument("--preview", default=None,
                   help="Écrit un PNG d'aperçu (toutes frames concaténées)")

    # --- Intégration directe dans le code C64 ---
    p.add_argument("--address", default=None,
                   help="Adresse hexa de pin du segment (ex: $3000). "
                        "Sans cette option, le bloc est relocatable (.pc = *).")
    p.add_argument("--screen-base", default="$0400",
                   help="Adresse RAM écran (où vivent les pointeurs sprites $XXF8-$XXFF). "
                        "Utilisée par les macros install_/set_frame.")
    p.add_argument("--no-macros", action="store_true",
                   help="N'émet pas les macros install_/set_frame, seulement les données.")
    p.add_argument("--bin-wrapper", action="store_true",
                   help="En mode --syntax bin : émet aussi un .asm/.s wrapper qui inclut "
                        "le binaire (.import binary pour C64, incbin pour Amiga).")

    # --- Options spécifiques Amiga 500 ---
    p.add_argument("--height", type=int, default=21,
                   help="Amiga : hauteur du sprite en lignes")
    p.add_argument("--vstart", type=int, default=0,
                   help="Amiga : ligne de départ verticale (SPRxPOS, 0 = laisser au runtime)")
    p.add_argument("--hstart", type=int, default=0,
                   help="Amiga : pixel de départ horizontal (SPRxPOS, 0 = laisser au runtime)")
    p.add_argument("--sprite-num", type=int, default=0, choices=range(8),
                   help="Amiga : numéro du sprite hardware (0..7) pour les macros install/set_frame")
    p.add_argument("--attached", action="store_true",
                   help="Amiga : active le bit ATTACH dans SPRxCTL (sprites pairés 15 couleurs)")
    p.add_argument("--amiga-palette", default=None,
                   help="Amiga : palette explicite, 3 valeurs OCS séparées par virgules. "
                        "Ex : '$f00,$0f0,$00f' ou '#ff0000,#00ff00,#0000ff'")
    return p


def _parse_addr(s: str | None) -> int | None:
    if s is None:
        return None
    s = s.strip().lower()
    if s.startswith("$"):
        return int(s[1:], 16)
    if s.startswith("0x"):
        return int(s, 16)
    return int(s, 0)


def write_preview(path: str, sprites_pixels: list[np.ndarray], colors: dict,
                  mode: str) -> None:
    """sprites_pixels : liste de tableaux (21, 24) avec valeurs codées."""
    n = len(sprites_pixels)
    grid_cols = min(n, 8)
    grid_rows = (n + grid_cols - 1) // grid_cols
    out = np.zeros((grid_rows * (SPRITE_H + 1) + 1,
                    grid_cols * (SPRITE_W + 1) + 1, 3), dtype=np.uint8)
    out[:] = (40, 40, 40)
    for i, sp in enumerate(sprites_pixels):
        gy, gx = divmod(i, grid_cols)
        x0 = 1 + gx * (SPRITE_W + 1)
        y0 = 1 + gy * (SPRITE_H + 1)
        if mode == "hires":
            rgb = np.where(sp[..., None] == 1,
                           C64_PALETTE[colors["fg"]],
                           C64_PALETTE[colors["bg"]]).astype(np.uint8)
        else:
            lookup = np.array([
                C64_PALETTE[colors["bg"]],
                C64_PALETTE[colors["mc1"]],
                C64_PALETTE[colors["fg"]],
                C64_PALETTE[colors["mc2"]],
            ], dtype=np.uint8)
            rgb = lookup[sp]
        out[y0:y0 + SPRITE_H, x0:x0 + SPRITE_W] = rgb
    Image.fromarray(out, "RGB").resize(
        (out.shape[1] * 4, out.shape[0] * 4), Image.NEAREST).save(path)


def _default_syntax(target: str) -> str:
    return "vasm" if target == "amiga" else "kickass"


def _default_ext(syntax: str) -> str:
    return {"bin": ".bin", "vasm": ".s", "kickass": ".asm", "acme": ".a"}[syntax]


def _run_c64(args, frames, address) -> tuple[list[bytes], list[np.ndarray], dict, str]:
    bg_rgb = tuple(int(c) for c in C64_PALETTE[args.bg_color])
    encoded: list[bytes] = []
    grids: list[np.ndarray] = []
    for fr in frames:
        scaled = fit_to_sprite(fr, args.mode, args.fit, bg_rgb)
        if args.mode == "hires":
            grid = quantize_hires(scaled, args.fg_color, args.bg_color,
                                  args.threshold, dither=not args.no_dither)
            encoded.append(encode_hires(grid))
        else:
            grid = quantize_multicolor(scaled, args.bg_color, args.mc1,
                                       args.mc2, args.fg_color)
            encoded.append(encode_multicolor(grid))
        grids.append(grid)
    colors = {"bg": args.bg_color, "fg": args.fg_color,
              "mc1": args.mc1, "mc2": args.mc2}
    summary = f"C64 mode {args.mode}, {len(encoded) * SPRITE_BYTES} octets"
    return encoded, grids, colors, summary


def _run_amiga(args, frames, address):
    if args.bg_color > 15:
        # garde-fou : on accepte les indices C64 comme valeur de fond initiale
        sys.exit("--bg-color invalide pour Amiga (utiliser 0..15 ou redéfinir un RGB)")
    bg_rgb = tuple(int(c) for c in C64_PALETTE[args.bg_color])

    fixed = None
    if args.amiga_palette:
        parts = [s for s in args.amiga_palette.split(",") if s.strip()]
        if len(parts) != 3:
            sys.exit("--amiga-palette attend exactement 3 couleurs séparées par virgules.")
        fixed = [parse_ocs_color(p) for p in parts]

    scaled_frames = [fit_to_amiga(fr, AMIGA_SPRITE_W, args.height, args.fit, bg_rgb)
                     for fr in frames]
    palette_ocs, grids = amiga_palette_from_frames(scaled_frames, bg_rgb, fixed)

    encoded = [encode_amiga_sprite(g, args.vstart, args.hstart, args.attached)
               for g in grids]
    bytes_per_frame = 8 + 4 * args.height
    summary = (f"Amiga OCS {AMIGA_SPRITE_W}x{args.height}, "
               f"{bytes_per_frame * len(encoded)} octets, "
               f"palette ${palette_ocs[0]:03x}/${palette_ocs[1]:03x}/"
               f"${palette_ocs[2]:03x}/${palette_ocs[3]:03x}")
    return encoded, grids, palette_ocs, summary


def main(argv: list[str] | None = None) -> int:
    args = build_argparser().parse_args(argv)

    syntax = args.syntax or _default_syntax(args.target)
    if args.target == "amiga" and syntax in ("kickass", "acme"):
        sys.exit(f"--target amiga incompatible avec --syntax {syntax} "
                 f"(utiliser vasm ou bin).")
    if args.target == "c64" and syntax == "vasm":
        sys.exit("--syntax vasm est réservé à --target amiga.")

    output = args.output
    if output is None:
        base = os.path.splitext(args.input)[0]
        output = base + _default_ext(syntax)

    frames = load_frames(args.input, args.fps, args.max_frames,
                         args.start_frame, args.frame_step)

    address = _parse_addr(args.address)
    screen_base = _parse_addr(args.screen_base) or 0x0400

    if args.target == "c64":
        encoded, grids, colors, summary = _run_c64(args, frames, address)

        if syntax == "bin":
            with open(output, "wb") as f:
                for fb in encoded:
                    f.write(fb)
            if args.bin_wrapper:
                wrapper_path = os.path.splitext(output)[0] + ".asm"
                text = emit_binary_with_wrapper(encoded, args.label, args.mode,
                                                colors, output, address, screen_base)
                with open(wrapper_path, "w", encoding="utf-8") as f:
                    f.write(text)
                print(f"      wrapper KickAssembler : {wrapper_path}")
        elif syntax == "kickass":
            text = emit_kickass(encoded, args.label, args.mode, colors,
                                args.input, address, screen_base, args.no_macros)
            with open(output, "w", encoding="utf-8") as f:
                f.write(text)
        else:  # acme
            text = emit_acme(encoded, args.label, args.mode, colors,
                             args.input, address, screen_base, args.no_macros)
            with open(output, "w", encoding="utf-8") as f:
                f.write(text)

        if args.preview:
            write_preview(args.preview, grids, colors, args.mode)

    else:  # amiga
        encoded, grids, palette_ocs, summary = _run_amiga(args, frames, address)

        if syntax == "bin":
            with open(output, "wb") as f:
                for fb in encoded:
                    f.write(fb)
            if args.bin_wrapper:
                wrapper_path = os.path.splitext(output)[0] + ".s"
                text = emit_amiga_binary_wrapper(encoded, args.label, args.height,
                                                 palette_ocs, output, address,
                                                 args.sprite_num)
                with open(wrapper_path, "w", encoding="utf-8") as f:
                    f.write(text)
                print(f"      wrapper vasm : {wrapper_path}")
        else:  # vasm
            text = emit_amiga_vasm(encoded, args.label, args.height,
                                   palette_ocs, args.input, address,
                                   args.sprite_num, args.no_macros)
            with open(output, "w", encoding="utf-8") as f:
                f.write(text)

        if args.preview:
            write_amiga_preview(args.preview, grids, palette_ocs)

    print(f"OK : {len(encoded)} frame(s) → {output}  ({summary})")
    return 0


def write_amiga_preview(path: str, grids: list[np.ndarray],
                        palette_ocs: list[int]) -> None:
    """Aperçu PNG pour les frames Amiga."""
    pal_rgb = np.array([ocs12_to_rgb(c) for c in palette_ocs], dtype=np.uint8)
    n = len(grids)
    h = grids[0].shape[0]
    cols = min(n, 8)
    rows = (n + cols - 1) // cols
    out = np.full((rows * (h + 1) + 1, cols * (AMIGA_SPRITE_W + 1) + 1, 3),
                  40, dtype=np.uint8)
    for i, g in enumerate(grids):
        gy, gx = divmod(i, cols)
        x0 = 1 + gx * (AMIGA_SPRITE_W + 1)
        y0 = 1 + gy * (h + 1)
        out[y0:y0 + h, x0:x0 + AMIGA_SPRITE_W] = pal_rgb[g]
    Image.fromarray(out, "RGB").resize(
        (out.shape[1] * 4, out.shape[0] * 4), Image.NEAREST).save(path)


if __name__ == "__main__":
    raise SystemExit(main())
