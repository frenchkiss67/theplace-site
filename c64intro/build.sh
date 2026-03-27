#!/bin/bash
# ============================================================
# build.sh - Compiler et lancer l'intro C64 THE PLACE
# ============================================================

set -e

KICKASS_JAR="${KICKASS_JAR:-KickAss.jar}"
VICE_BIN="${VICE_BIN:-x64sc}"
SRC="main.asm"
OUT="intro.prg"

echo "=== Compilation de l'intro C64 THE PLACE ==="

# Compilation avec KickAssembler
echo "[1/2] Assemblage avec KickAssembler..."
java -jar "$KICKASS_JAR" "$SRC" -o "$OUT"

if [ $? -eq 0 ]; then
    echo "[OK] $OUT généré avec succès"
    ls -la "$OUT"
else
    echo "[ERREUR] Échec de l'assemblage"
    exit 1
fi

# Lancer dans VICE si demandé
if [ "$1" = "run" ]; then
    echo "[2/2] Lancement dans VICE ($VICE_BIN)..."
    "$VICE_BIN" "$OUT"
fi

echo "=== Terminé ==="
