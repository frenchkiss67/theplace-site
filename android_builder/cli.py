"""Interface en ligne de commande pour android_builder."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

from android_builder import __version__
from android_builder.generator import (
    ProjectConfig,
    add_activity,
    detect_project,
    generate_project,
)
from android_builder.interactive import prompt_project_config
from android_builder.templates import FLAVORS
from android_builder.utils import list_tree, success


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="android-builder",
        description=(
            "Outil Python pour scaffolder rapidement des applications Android "
            "(Kotlin Compose, Kotlin Views ou Java)."
        ),
    )
    parser.add_argument("--version", action="version", version=f"android-builder {__version__}")

    sub = parser.add_subparsers(dest="command", required=True)

    # create
    p_create = sub.add_parser("create", help="Créer un nouveau projet Android.")
    p_create.add_argument("--name", help="Nom de l'application (ex: 'Mon App').")
    p_create.add_argument("--package", help="Package Android (ex: com.exemple.monapp).")
    p_create.add_argument(
        "--flavor",
        choices=list(FLAVORS.keys()),
        help="Type de projet (kotlin-compose, kotlin-views, java-views).",
    )
    p_create.add_argument("--main-activity", default="MainActivity", help="Nom de l'Activity principale.")
    p_create.add_argument("--min-sdk", type=int, default=24)
    p_create.add_argument("--target-sdk", type=int, default=34)
    p_create.add_argument("--compile-sdk", type=int, default=34)
    p_create.add_argument(
        "--output", type=Path, default=Path.cwd(),
        help="Dossier dans lequel créer le projet (défaut: répertoire courant).",
    )
    p_create.add_argument(
        "--interactive", "-i", action="store_true",
        help="Pose les questions au terminal au lieu d'utiliser uniquement les flags.",
    )
    p_create.add_argument("--overwrite", action="store_true", help="Écrase les fichiers existants.")

    # list-flavors
    sub.add_parser("list-flavors", help="Lister les types de projets disponibles.")

    # add-activity
    p_act = sub.add_parser(
        "add-activity",
        help="Ajouter une nouvelle Activity à un projet existant.",
    )
    p_act.add_argument("name", help="Nom de l'Activity à créer (PascalCase).")
    p_act.add_argument(
        "--project", type=Path, default=Path.cwd(),
        help="Dossier racine du projet Android (défaut: courant).",
    )
    p_act.add_argument("--overwrite", action="store_true")

    # info
    p_info = sub.add_parser("info", help="Détecter et afficher la configuration d'un projet.")
    p_info.add_argument(
        "--project", type=Path, default=Path.cwd(),
        help="Dossier racine du projet Android.",
    )

    # gui
    sub.add_parser(
        "gui",
        help="Lancer l'interface graphique Tkinter (recommandé sous Windows).",
    )

    return parser


def _cmd_create(args: argparse.Namespace) -> int:
    if args.interactive:
        config = prompt_project_config(output_dir=args.output)
    else:
        missing = [
            flag for flag, value in
            [("--name", args.name), ("--package", args.package), ("--flavor", args.flavor)]
            if not value
        ]
        if missing:
            print(
                "Arguments requis manquants : " + ", ".join(missing) +
                "\nAjoutez --interactive pour un mode guidé.",
                file=sys.stderr,
            )
            return 2
        config = ProjectConfig(
            app_name=args.name,
            package=args.package,
            flavor=args.flavor,
            main_activity=args.main_activity,
            min_sdk=args.min_sdk,
            target_sdk=args.target_sdk,
            compile_sdk=args.compile_sdk,
            output_dir=args.output,
            overwrite=args.overwrite,
        )

    try:
        config.overwrite = args.overwrite
        files = generate_project(config)
    except (ValueError, FileExistsError) as exc:
        print(f"Erreur : {exc}", file=sys.stderr)
        return 1

    print(f"\nFichiers créés ({len(files)}) :")
    print(list_tree(config.project_dir, files))
    print("\nProchaines étapes :")
    print(f"  cd {config.project_dir}")
    print("  # Ouvrir le dossier dans Android Studio (qui finalisera le wrapper Gradle)")
    print("  # ou exécuter : gradle wrapper --gradle-version 8.7 && ./gradlew assembleDebug")
    return 0


def _cmd_list_flavors(_: argparse.Namespace) -> int:
    print("Flavors disponibles :")
    for key, meta in FLAVORS.items():
        print(f"  - {key:<16} {meta['label']}")
    return 0


def _cmd_add_activity(args: argparse.Namespace) -> int:
    try:
        files = add_activity(args.project, args.name, overwrite=args.overwrite)
    except (FileNotFoundError, FileExistsError, ValueError) as exc:
        print(f"Erreur : {exc}", file=sys.stderr)
        return 1
    print("Fichiers créés :")
    print(list_tree(args.project, files))
    return 0


def _cmd_gui(_: argparse.Namespace) -> int:
    # Import paresseux : Tkinter n'est chargé que si on lance la GUI.
    from android_builder.gui import launch
    return launch()


def _cmd_info(args: argparse.Namespace) -> int:
    try:
        info = detect_project(args.project)
    except (FileNotFoundError, ValueError) as exc:
        print(f"Erreur : {exc}", file=sys.stderr)
        return 1
    print(f"Projet : {args.project.resolve()}")
    print(f"  package  : {info['package']}")
    print(f"  flavor   : {info['flavor']}")
    print(f"  langage  : {info['language']}")
    return 0


COMMANDS = {
    "create": _cmd_create,
    "list-flavors": _cmd_list_flavors,
    "add-activity": _cmd_add_activity,
    "info": _cmd_info,
    "gui": _cmd_gui,
}


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    handler = COMMANDS[args.command]
    return handler(args)


if __name__ == "__main__":
    raise SystemExit(main())
