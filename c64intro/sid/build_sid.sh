#!/bin/bash
# ============================================================
# build_sid.sh - Convertit un WAV en .sid (digi-sample 4-bit)
#
# Usage:
#   ./build_sid.sh input.wav [output.sid] [--rate 8000] [--name "..."]
#
# Necessite python3 (stdlib uniquement).
# Le .sid produit est jouable par VICE/sidplayfp/JSidPlay2.
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TOOL="$SCRIPT_DIR/../../tools/audio2sid.py"

if [ $# -lt 1 ]; then
    echo "Usage: $0 <input.wav> [<output.sid>] [options...]"
    echo ""
    echo "Options principales:"
    echo "  --rate N           Frequence cible en Hz (def: 8000)"
    echo "  --max-bytes N      Taille max donnees packees (def: 32768)"
    echo "  --name 'TITRE'     Champ name PSID"
    echo "  --author 'NOM'     Champ author PSID"
    echo "  --no-loop          Ne pas reboucler"
    echo "  --ntsc             Cibler NTSC (def: PAL)"
    exit 1
fi

INPUT="$1"
shift

if [ $# -ge 1 ] && [[ "$1" != --* ]]; then
    OUTPUT="$1"
    shift
else
    OUTPUT="${INPUT%.*}.sid"
fi

echo "=== audio2sid: $INPUT -> $OUTPUT ==="
python3 "$TOOL" "$INPUT" "$OUTPUT" "$@"
echo "=== Termine ==="

if command -v sidplayfp >/dev/null 2>&1; then
    echo ""
    echo "Pour ecouter:  sidplayfp $OUTPUT"
elif command -v x64sc >/dev/null 2>&1; then
    echo ""
    echo "Pour ecouter (VICE en mode VSID):  x64sc $OUTPUT"
fi
