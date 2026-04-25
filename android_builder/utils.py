"""Utilitaires partagés (validation, formatage, écriture de fichiers)."""

from __future__ import annotations

import re
import sys
from pathlib import Path
from typing import Iterable

PACKAGE_RE = re.compile(r"^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$")
CLASS_RE = re.compile(r"^[A-Z][A-Za-z0-9]*$")
APP_NAME_RE = re.compile(r"^[A-Za-z][A-Za-z0-9 _-]{0,62}$")
JAVA_KEYWORDS = {
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
    "class", "const", "continue", "default", "do", "double", "else", "enum",
    "extends", "final", "finally", "float", "for", "goto", "if", "implements",
    "import", "instanceof", "int", "interface", "long", "native", "new",
    "package", "private", "protected", "public", "return", "short", "static",
    "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
    "transient", "try", "void", "volatile", "while", "true", "false", "null",
}


def validate_package(package: str) -> str:
    """Valide un identifiant de package Android (ex: com.example.app)."""
    if not PACKAGE_RE.match(package):
        raise ValueError(
            f"Package invalide: '{package}'. Attendu: minuscules séparées par "
            "des points, ex. 'com.exemple.monapp'."
        )
    for segment in package.split("."):
        if segment in JAVA_KEYWORDS:
            raise ValueError(
                f"Segment '{segment}' est un mot-clé Java/Kotlin réservé."
            )
    return package


def validate_class_name(name: str) -> str:
    """Valide un nom de classe (PascalCase)."""
    if not CLASS_RE.match(name):
        raise ValueError(
            f"Nom de classe invalide: '{name}'. Attendu: PascalCase, ex. "
            "'MainActivity'."
        )
    if name in JAVA_KEYWORDS:
        raise ValueError(f"'{name}' est un mot-clé réservé.")
    return name


def validate_app_name(name: str) -> str:
    """Valide un nom d'application affichable."""
    if not APP_NAME_RE.match(name):
        raise ValueError(
            f"Nom d'application invalide: '{name}'. Lettres, chiffres, espaces, "
            "tirets et underscores autorisés (1 à 63 caractères)."
        )
    return name


def to_snake(name: str) -> str:
    """Convertit PascalCase / camelCase en snake_case."""
    s1 = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", s1).lower()


def to_resource_name(name: str) -> str:
    """Convertit un nom en identifiant de ressource Android valide."""
    snake = to_snake(name)
    cleaned = re.sub(r"[^a-z0-9_]", "_", snake)
    return re.sub(r"_+", "_", cleaned).strip("_") or "resource"


def package_to_path(package: str) -> Path:
    """Transforme com.example.app en com/example/app."""
    return Path(*package.split("."))


def write_file(path: Path, content: str, *, overwrite: bool = False) -> None:
    """Écrit un fichier, en créant les dossiers parents."""
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists() and not overwrite:
        raise FileExistsError(f"Le fichier existe déjà: {path}")
    path.write_text(content, encoding="utf-8")


def render(template: str, mapping: dict[str, str]) -> str:
    """Rendu basique de template via les jetons {{cle}}."""
    output = template
    for key, value in mapping.items():
        output = output.replace("{{" + key + "}}", str(value))
    return output


def info(msg: str) -> None:
    print(f"[android-builder] {msg}", file=sys.stderr)


def success(msg: str) -> None:
    print(f"[android-builder] OK — {msg}")


def list_tree(root: Path, files: Iterable[Path]) -> str:
    """Représentation textuelle d'une liste de fichiers générés."""
    rels = sorted(str(p.relative_to(root)) for p in files)
    return "\n".join(f"  - {r}" for r in rels)
