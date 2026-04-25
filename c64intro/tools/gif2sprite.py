#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
gif2sprite.py — Générateur de sprites Commodore 64 à partir de GIF ou MP4.

Convertit chaque image (frame) en sprite hardware C64 :
  - Hi-res     : 24 x 21 pixels, 1 bit/pixel, 63 octets utiles (+1 padding = 64)
  - Multicolor : 12 x 21 pixels logiques, 2 bits/pixel, 63 octets utiles (+1 padding)

Sortie : KickAssembler (.asm), ACME (!byte), ou binaire brut (.bin / .prg).

Usage typique :
    python3 gif2sprite.py logo.gif -o sprites.asm --mode multicolor \
        --bg-color 0 --fg-color 1 --mc1 11 --mc2 12 --label logo_anim
    python3 gif2sprite.py demo.mp4 -o run.bin --syntax bin --fps 12 --max-frames 32
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
        description="Génère des sprites Commodore 64 (24x21) depuis un GIF ou MP4.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    p.add_argument("input", help="Fichier source (.gif, .mp4, .png, ...)")
    p.add_argument("-o", "--output", default=None,
                   help="Fichier de sortie. Défaut : <input>.asm (ou .bin si --syntax bin)")
    p.add_argument("--mode", choices=("hires", "multicolor"), default="multicolor",
                   help="Mode du sprite : hi-res 1 bpp ou multicolor 2 bpp")
    p.add_argument("--syntax", choices=("kickass", "acme", "bin"), default="kickass",
                   help="Format de sortie")
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
                   help="En mode --syntax bin : émet aussi un .asm wrapper qui inclut "
                        "le binaire via .import binary.")
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


def main(argv: list[str] | None = None) -> int:
    args = build_argparser().parse_args(argv)

    output = args.output
    if output is None:
        base = os.path.splitext(args.input)[0]
        output = base + (".bin" if args.syntax == "bin" else ".asm")

    frames = load_frames(args.input, args.fps, args.max_frames,
                         args.start_frame, args.frame_step)

    bg_rgb = tuple(int(c) for c in C64_PALETTE[args.bg_color])
    encoded: list[bytes] = []
    pixel_grids: list[np.ndarray] = []
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
        pixel_grids.append(grid)

    colors = {"bg": args.bg_color, "fg": args.fg_color,
              "mc1": args.mc1, "mc2": args.mc2}

    address = _parse_addr(args.address)
    screen_base = _parse_addr(args.screen_base) or 0x0400

    if args.syntax == "bin":
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
    elif args.syntax == "kickass":
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
        write_preview(args.preview, pixel_grids, colors, args.mode)

    print(f"OK : {len(encoded)} frame(s) → {output} "
          f"({len(encoded) * SPRITE_BYTES} octets, mode {args.mode})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
