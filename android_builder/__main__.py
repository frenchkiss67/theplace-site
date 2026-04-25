"""Permet de lancer le CLI via `python -m android_builder`."""

from android_builder.cli import main

if __name__ == "__main__":
    raise SystemExit(main())
