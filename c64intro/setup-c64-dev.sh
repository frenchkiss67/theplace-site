#!/bin/bash
# ============================================================
# setup-c64-dev.sh
# Installation automatique de l'environnement de développement
# C64 pour Visual Studio Code (Linux/macOS)
# ============================================================

set -e

INSTALL_DIR="$HOME/c64dev"
KICKASS_DIR="$INSTALL_DIR/kickassembler"
VICE_ROMS_DIR="$INSTALL_DIR/vice-roms"

echo "============================================"
echo "  Installation environnement C64 pour VS Code"
echo "============================================"
echo ""

# --- Détecter l'OS ---
OS="$(uname -s)"
echo "[*] Système détecté: $OS"

# ============================================================
# 1. Vérifier Java
# ============================================================
echo ""
echo "[1/4] Vérification de Java..."
if command -v java &> /dev/null; then
    JAVA_VER=$(java -version 2>&1 | head -n1)
    echo "  OK: $JAVA_VER"
else
    echo "  Java non trouvé. Installation..."
    if [ "$OS" = "Linux" ]; then
        sudo apt-get update && sudo apt-get install -y default-jre
    elif [ "$OS" = "Darwin" ]; then
        brew install openjdk
    fi
    echo "  OK: Java installé"
fi

# ============================================================
# 2. Installer KickAssembler
# ============================================================
echo ""
echo "[2/4] Installation de KickAssembler..."
mkdir -p "$KICKASS_DIR"

if [ -f "$KICKASS_DIR/KickAss.jar" ]; then
    echo "  KickAssembler déjà installé dans $KICKASS_DIR"
else
    echo "  Téléchargement depuis theweb.dk..."
    cd /tmp
    curl -L -o KickAssembler.zip "http://theweb.dk/KickAssembler/KickAssembler.zip"
    unzip -o KickAssembler.zip -d "$KICKASS_DIR"
    rm -f KickAssembler.zip

    # KickAss.jar peut être dans un sous-dossier
    if [ ! -f "$KICKASS_DIR/KickAss.jar" ]; then
        find "$KICKASS_DIR" -name "KickAss.jar" -exec cp {} "$KICKASS_DIR/" \;
    fi

    echo "  OK: KickAssembler installé dans $KICKASS_DIR/KickAss.jar"
fi

# Créer un script wrapper pour KickAss
cat > "$INSTALL_DIR/kickass" << WRAPPER
#!/bin/bash
java -jar "$KICKASS_DIR/KickAss.jar" "\$@"
WRAPPER
chmod +x "$INSTALL_DIR/kickass"
echo "  Script wrapper créé: $INSTALL_DIR/kickass"

# ============================================================
# 3. Installer VICE (émulateur C64)
# ============================================================
echo ""
echo "[3/4] Installation de VICE (émulateur C64)..."

if command -v x64sc &> /dev/null; then
    echo "  OK: VICE déjà installé ($(which x64sc))"
elif [ "$OS" = "Linux" ]; then
    echo "  Installation via apt..."
    sudo apt-get update && sudo apt-get install -y vice
    echo "  OK: VICE installé"
elif [ "$OS" = "Darwin" ]; then
    echo "  Installation via Homebrew..."
    brew install vice
    echo "  OK: VICE installé"
else
    echo "  [!] OS non supporté pour l'installation automatique."
    echo "      Télécharger VICE depuis: https://vice-emu.sourceforge.io/"
fi

# ============================================================
# 4. Extensions VS Code
# ============================================================
echo ""
echo "[4/4] Installation des extensions VS Code..."

if command -v code &> /dev/null; then
    echo "  Installation de l'extension KickAss (C64)..."
    code --install-extension CaptainJiNX.kickass-c64 2>/dev/null || true

    echo "  Installation de l'extension Kick Assembler 8-Bit Retro Studio..."
    code --install-extension paulhocker.kick-assembler-vscode-ext 2>/dev/null || true

    echo "  OK: Extensions installées"
else
    echo "  [!] VS Code ('code') non trouvé dans le PATH."
    echo "      Installer manuellement ces extensions dans VS Code :"
    echo "        - CaptainJiNX.kickass-c64"
    echo "        - paulhocker.kick-assembler-vscode-ext"
fi

# ============================================================
# 5. Configurer les settings VS Code pour le projet
# ============================================================
echo ""
echo "[*] Création de la configuration VS Code pour le projet..."

VSCODE_DIR="$(dirname "$0")/.vscode"
mkdir -p "$VSCODE_DIR"

cat > "$VSCODE_DIR/settings.json" << SETTINGS
{
    "kickassembler.kickAssemblerJarPath": "$KICKASS_DIR/KickAss.jar",
    "kickassembler.vicePath": "$(command -v x64sc 2>/dev/null || echo '/usr/bin/x64sc')",
    "kickassembler.outputDirectory": ".",
    "editor.tabSize": 8,
    "files.associations": {
        "*.asm": "kickassembler"
    }
}
SETTINGS

echo "  OK: .vscode/settings.json créé"

# ============================================================
# 6. Créer le script de lancement rapide
# ============================================================
cat > "$VSCODE_DIR/tasks.json" << TASKS
{
    "version": "2.0.0",
    "tasks": [
        {
            "label": "Compiler l'intro C64",
            "type": "shell",
            "command": "java",
            "args": [
                "-jar",
                "$KICKASS_DIR/KickAss.jar",
                "\${workspaceFolder}/main.asm",
                "-o",
                "\${workspaceFolder}/intro.prg"
            ],
            "group": {
                "kind": "build",
                "isDefault": true
            },
            "problemMatcher": []
        },
        {
            "label": "Lancer dans VICE",
            "type": "shell",
            "command": "$(command -v x64sc 2>/dev/null || echo 'x64sc')",
            "args": ["\${workspaceFolder}/intro.prg"],
            "dependsOn": "Compiler l'intro C64",
            "problemMatcher": []
        }
    ]
}
TASKS

echo "  OK: .vscode/tasks.json créé"

# ============================================================
# Résumé
# ============================================================
echo ""
echo "============================================"
echo "  Installation terminée !"
echo "============================================"
echo ""
echo "  KickAssembler : $KICKASS_DIR/KickAss.jar"
echo "  VICE          : $(command -v x64sc 2>/dev/null || echo 'non trouvé')"
echo "  Wrapper       : $INSTALL_DIR/kickass"
echo ""
echo "  Pour utiliser dans VS Code :"
echo "    1. Ouvrir le dossier c64intro/ dans VS Code"
echo "    2. Ctrl+Shift+B  → Compiler"
echo "    3. Ctrl+Shift+P  → 'Tasks: Run Task' → 'Lancer dans VICE'"
echo "    4. Ou appuyer sur F6 (si extension KickAss configurée)"
echo ""
echo "  Depuis le terminal :"
echo "    cd c64intro"
echo "    $INSTALL_DIR/kickass main.asm -o intro.prg"
echo "    x64sc intro.prg"
echo ""
