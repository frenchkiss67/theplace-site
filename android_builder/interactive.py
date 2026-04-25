"""Mode interactif : pose les questions à l'utilisateur pour configurer un projet."""

from __future__ import annotations

from pathlib import Path

from android_builder.generator import ProjectConfig
from android_builder.templates import FLAVORS
from android_builder.utils import (
    validate_app_name,
    validate_class_name,
    validate_package,
)


def _ask(prompt: str, default: str | None = None) -> str:
    suffix = f" [{default}]" if default else ""
    while True:
        response = input(f"{prompt}{suffix}: ").strip()
        if response:
            return response
        if default is not None:
            return default
        print("  ↳ une valeur est requise.")


def _ask_validated(prompt: str, validator, default: str | None = None) -> str:
    while True:
        value = _ask(prompt, default)
        try:
            return validator(value)
        except ValueError as exc:
            print(f"  ↳ {exc}")


def _ask_int(prompt: str, default: int, *, min_value: int, max_value: int) -> int:
    while True:
        value = _ask(prompt, str(default))
        try:
            num = int(value)
        except ValueError:
            print("  ↳ entier attendu.")
            continue
        if not (min_value <= num <= max_value):
            print(f"  ↳ valeur hors plage ({min_value}-{max_value}).")
            continue
        return num


def _ask_choice(prompt: str, choices: dict[str, str], default: str) -> str:
    print(prompt)
    keys = list(choices.keys())
    for index, key in enumerate(keys, start=1):
        marker = " (défaut)" if key == default else ""
        print(f"  {index}. {key} — {choices[key]}{marker}")
    while True:
        raw = input(f"Choix [1-{len(keys)}, défaut={default}]: ").strip()
        if not raw:
            return default
        if raw in choices:
            return raw
        try:
            idx = int(raw)
        except ValueError:
            print("  ↳ saisir un numéro ou le nom du choix.")
            continue
        if 1 <= idx <= len(keys):
            return keys[idx - 1]
        print("  ↳ choix hors plage.")


def prompt_project_config(output_dir: Path) -> ProjectConfig:
    """Affiche les questions interactives et renvoie une configuration validée."""
    print("=== Création d'un nouveau projet Android ===")
    app_name = _ask_validated("Nom de l'application", validate_app_name, default="MonApp")
    package = _ask_validated(
        "Package (ex: com.exemple.monapp)",
        validate_package,
        default=f"com.exemple.{app_name.lower().replace(' ', '').replace('-', '')[:20] or 'app'}",
    )
    main_activity = _ask_validated(
        "Nom de l'Activity principale", validate_class_name, default="MainActivity"
    )
    flavor_labels = {key: meta["label"] for key, meta in FLAVORS.items()}
    flavor = _ask_choice("Type de projet :", flavor_labels, default="kotlin-compose")
    min_sdk = _ask_int("min_sdk", default=24, min_value=21, max_value=35)
    target_sdk = _ask_int("target_sdk", default=34, min_value=min_sdk, max_value=35)
    compile_sdk = _ask_int(
        "compile_sdk", default=max(34, target_sdk), min_value=target_sdk, max_value=35
    )

    return ProjectConfig(
        app_name=app_name,
        package=package,
        flavor=flavor,
        main_activity=main_activity,
        min_sdk=min_sdk,
        target_sdk=target_sdk,
        compile_sdk=compile_sdk,
        output_dir=output_dir,
    )
