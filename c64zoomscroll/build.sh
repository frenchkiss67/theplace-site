#!/usr/bin/env bash
# ============================================================
#  build.sh — Assemble le zoom scrolltext avec KickAssembler
# ============================================================
set -euo pipefail

KICKASS_JAR="${KICKASS_JAR:-KickAss.jar}"
SRC="main.asm"
OUT="zoomscroll.prg"

if [ ! -f "$KICKASS_JAR" ]; then
    echo "Error: $KICKASS_JAR introuvable."
    echo "  Télécharge KickAssembler depuis http://www.theweb.dk/KickAssembler/"
    echo "  ou définis la variable KICKASS_JAR avec le chemin du .jar."
    exit 1
fi

echo "=> Assemblage $SRC -> $OUT"
java -jar "$KICKASS_JAR" "$SRC" -o "$OUT" -showmem

echo ""
echo "Build OK : $OUT"
echo ""
echo "Lancer avec VICE :"
echo "  x64sc -warp $OUT"
echo ""
echo "Ou charger dans l'émulateur :"
echo "  LOAD \"$OUT\",8,1"
echo "  RUN"
