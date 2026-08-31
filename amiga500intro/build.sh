#!/bin/bash
# ============================================================
# build.sh - Compiler et lancer l'intro Amiga 500 THE PLACE
#
# Pre-requis :
#   - vasm (vasmm68k_mot) : http://sun.hasenbraten.de/vasm/
#   - vlink                : http://sun.hasenbraten.de/vlink/
#   - fs-uae (optionnel)   : pour l'execution
# ============================================================

set -e

VASM="${VASM:-vasmm68k_mot}"
VLINK="${VLINK:-vlink}"
EMU="${EMU:-fs-uae}"
SRC="main.s"
OBJ="main.o"
OUT="intro"

echo "=== Compilation de l'intro Amiga 500 THE PLACE ==="

# 1) Assemblage
echo "[1/3] Assemblage avec vasm..."
"$VASM" -Fhunk -m68000 -kick1hunks -no-opt -o "$OBJ" "$SRC"

# 2) Lien (format hunk Amiga)
echo "[2/3] Lien avec vlink..."
"$VLINK" -bamigahunk -o "$OUT" "$OBJ"

if [ -f "$OUT" ]; then
    echo "[OK] $OUT genere avec succes"
    ls -la "$OUT"
else
    echo "[ERREUR] Echec du build"
    exit 1
fi

# 3) Execution optionnelle
if [ "$1" = "run" ]; then
    echo "[3/3] Lancement dans $EMU..."
    "$EMU" --amiga_model=A500 \
           --automatic_input_grab=0 \
           "$OUT"
fi

echo "=== Termine ==="
