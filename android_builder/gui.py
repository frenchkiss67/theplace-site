"""Interface graphique Tkinter pour android_builder.

Lancement : `python -m android_builder gui` ou `android-builder gui`.

Tkinter étant inclus dans la distribution standard de Python sur Windows,
aucune dépendance externe n'est nécessaire. Sur Linux/macOS, le paquet
`python3-tk` peut être requis selon la distribution.
"""

from __future__ import annotations

import threading
import traceback
from pathlib import Path

from android_builder import __version__
from android_builder.generator import (
    ProjectConfig,
    add_activity,
    detect_project,
    generate_project,
)
from android_builder.templates import FLAVORS

# Tkinter est importé paresseusement : sur Linux sans `python3-tk`, le simple
# fait d'importer ce module ne doit pas casser le reste du CLI.
try:
    from tkinter import StringVar, BooleanVar, IntVar, Tk, filedialog, messagebox
    from tkinter import ttk, scrolledtext
    _TK_IMPORT_ERROR: Exception | None = None
except Exception as _exc:  # noqa: BLE001 — Tk indisponible
    StringVar = BooleanVar = IntVar = Tk = filedialog = messagebox = None  # type: ignore[assignment]
    ttk = scrolledtext = None  # type: ignore[assignment]
    _TK_IMPORT_ERROR = _exc


PADDING = {"padx": 6, "pady": 4}


class AndroidBuilderGUI:
    """Fenêtre principale avec onglets : créer un projet / ajouter une Activity."""

    def __init__(self, root) -> None:
        self.root = root
        root.title(f"Android Builder — v{__version__}")
        root.geometry("760x620")
        root.minsize(640, 540)

        self._build_style()
        self._build_layout()

    # --- Construction de l'UI -------------------------------------------------

    def _build_style(self) -> None:
        style = ttk.Style()
        # 'vista' sur Windows si dispo, sinon thème courant.
        if "vista" in style.theme_names():
            style.theme_use("vista")
        elif "clam" in style.theme_names():
            style.theme_use("clam")

    def _build_layout(self) -> None:
        notebook = ttk.Notebook(self.root)
        notebook.pack(fill="both", expand=True, padx=8, pady=(8, 4))

        self.create_tab = ttk.Frame(notebook)
        self.activity_tab = ttk.Frame(notebook)
        notebook.add(self.create_tab, text="Créer un projet")
        notebook.add(self.activity_tab, text="Ajouter une Activity")

        self._build_create_tab(self.create_tab)
        self._build_activity_tab(self.activity_tab)

        # Zone de log partagée en bas.
        log_frame = ttk.LabelFrame(self.root, text="Journal")
        log_frame.pack(fill="both", expand=False, padx=8, pady=(4, 8))
        self.log = scrolledtext.ScrolledText(log_frame, height=10, state="disabled")
        self.log.pack(fill="both", expand=True, padx=4, pady=4)

    # --- Onglet : créer un projet --------------------------------------------

    def _build_create_tab(self, parent) -> None:
        # Variables d'état liées aux champs.
        self.var_app_name = StringVar(value="MonApp")
        self.var_package = StringVar(value="com.exemple.monapp")
        self.var_main_activity = StringVar(value="MainActivity")
        self.var_flavor = StringVar(value="kotlin-compose")
        self.var_min_sdk = IntVar(value=24)
        self.var_target_sdk = IntVar(value=34)
        self.var_compile_sdk = IntVar(value=34)
        self.var_output = StringVar(value=str(Path.cwd()))
        self.var_overwrite = BooleanVar(value=False)

        form = ttk.Frame(parent)
        form.pack(fill="x", **PADDING)
        for col in (1,):
            form.columnconfigure(col, weight=1)

        self._row_entry(form, 0, "Nom de l'application", self.var_app_name)
        self._row_entry(form, 1, "Package (ex: com.exemple.monapp)", self.var_package)
        self._row_entry(form, 2, "Activity principale", self.var_main_activity)

        # Flavor (combobox).
        ttk.Label(form, text="Type de projet").grid(row=3, column=0, sticky="w", **PADDING)
        flavor_combo = ttk.Combobox(
            form,
            textvariable=self.var_flavor,
            values=list(FLAVORS.keys()),
            state="readonly",
        )
        flavor_combo.grid(row=3, column=1, sticky="ew", **PADDING)
        self.flavor_label = ttk.Label(form, text=FLAVORS["kotlin-compose"]["label"])
        self.flavor_label.grid(row=4, column=1, sticky="w", padx=6)
        flavor_combo.bind("<<ComboboxSelected>>", self._on_flavor_change)

        # SDKs.
        sdk_frame = ttk.Frame(form)
        sdk_frame.grid(row=5, column=0, columnspan=2, sticky="ew", **PADDING)
        ttk.Label(sdk_frame, text="min_sdk").pack(side="left")
        ttk.Spinbox(sdk_frame, from_=21, to=35, textvariable=self.var_min_sdk, width=5).pack(side="left", padx=(4, 12))
        ttk.Label(sdk_frame, text="target_sdk").pack(side="left")
        ttk.Spinbox(sdk_frame, from_=21, to=35, textvariable=self.var_target_sdk, width=5).pack(side="left", padx=(4, 12))
        ttk.Label(sdk_frame, text="compile_sdk").pack(side="left")
        ttk.Spinbox(sdk_frame, from_=21, to=35, textvariable=self.var_compile_sdk, width=5).pack(side="left", padx=(4, 12))

        # Dossier de sortie + bouton parcourir.
        ttk.Label(form, text="Dossier de sortie").grid(row=6, column=0, sticky="w", **PADDING)
        out_frame = ttk.Frame(form)
        out_frame.grid(row=6, column=1, sticky="ew", **PADDING)
        out_frame.columnconfigure(0, weight=1)
        ttk.Entry(out_frame, textvariable=self.var_output).grid(row=0, column=0, sticky="ew")
        ttk.Button(out_frame, text="Parcourir…", command=self._browse_output).grid(row=0, column=1, padx=(4, 0))

        ttk.Checkbutton(
            form, text="Écraser les fichiers existants", variable=self.var_overwrite
        ).grid(row=7, column=1, sticky="w", **PADDING)

        # Boutons d'action.
        actions = ttk.Frame(parent)
        actions.pack(fill="x", **PADDING)
        self.create_button = ttk.Button(actions, text="Générer le projet", command=self._on_generate)
        self.create_button.pack(side="right")
        ttk.Button(actions, text="Réinitialiser", command=self._reset_create_form).pack(side="right", padx=(0, 6))

    def _on_flavor_change(self, _event=None) -> None:
        meta = FLAVORS.get(self.var_flavor.get())
        if meta:
            self.flavor_label.config(text=meta["label"])

    def _browse_output(self) -> None:
        chosen = filedialog.askdirectory(
            title="Dossier de sortie", initialdir=self.var_output.get() or str(Path.cwd())
        )
        if chosen:
            self.var_output.set(chosen)

    def _reset_create_form(self) -> None:
        self.var_app_name.set("MonApp")
        self.var_package.set("com.exemple.monapp")
        self.var_main_activity.set("MainActivity")
        self.var_flavor.set("kotlin-compose")
        self.var_min_sdk.set(24)
        self.var_target_sdk.set(34)
        self.var_compile_sdk.set(34)
        self.var_output.set(str(Path.cwd()))
        self.var_overwrite.set(False)
        self._on_flavor_change()

    # --- Onglet : ajouter une Activity ---------------------------------------

    def _build_activity_tab(self, parent) -> None:
        self.var_act_project = StringVar(value=str(Path.cwd()))
        self.var_act_name = StringVar(value="DetailsActivity")
        self.var_act_overwrite = BooleanVar(value=False)
        self.var_detected = StringVar(value="(aucun projet sélectionné)")

        form = ttk.Frame(parent)
        form.pack(fill="x", **PADDING)
        form.columnconfigure(1, weight=1)

        ttk.Label(form, text="Dossier du projet").grid(row=0, column=0, sticky="w", **PADDING)
        proj_frame = ttk.Frame(form)
        proj_frame.grid(row=0, column=1, sticky="ew", **PADDING)
        proj_frame.columnconfigure(0, weight=1)
        ttk.Entry(proj_frame, textvariable=self.var_act_project).grid(row=0, column=0, sticky="ew")
        ttk.Button(proj_frame, text="Parcourir…", command=self._browse_project).grid(row=0, column=1, padx=(4, 0))
        ttk.Button(proj_frame, text="Détecter", command=self._detect_project).grid(row=0, column=2, padx=(4, 0))

        ttk.Label(form, text="Configuration détectée").grid(row=1, column=0, sticky="w", **PADDING)
        ttk.Label(form, textvariable=self.var_detected, foreground="#444").grid(
            row=1, column=1, sticky="w", **PADDING
        )

        self._row_entry(form, 2, "Nom de la nouvelle Activity", self.var_act_name)

        ttk.Checkbutton(
            form, text="Écraser les fichiers existants", variable=self.var_act_overwrite
        ).grid(row=3, column=1, sticky="w", **PADDING)

        actions = ttk.Frame(parent)
        actions.pack(fill="x", **PADDING)
        self.activity_button = ttk.Button(
            actions, text="Ajouter l'Activity", command=self._on_add_activity
        )
        self.activity_button.pack(side="right")

    def _browse_project(self) -> None:
        chosen = filedialog.askdirectory(
            title="Projet Android existant", initialdir=self.var_act_project.get() or str(Path.cwd())
        )
        if chosen:
            self.var_act_project.set(chosen)
            self._detect_project()

    def _detect_project(self) -> None:
        try:
            info = detect_project(Path(self.var_act_project.get()))
        except Exception as exc:
            self.var_detected.set(f"Erreur : {exc}")
            return
        self.var_detected.set(
            f"package={info['package']}  •  flavor={info['flavor']}  •  langage={info['language']}"
        )

    # --- Helpers UI -----------------------------------------------------------

    def _row_entry(self, parent, row: int, label: str, var) -> None:
        ttk.Label(parent, text=label).grid(row=row, column=0, sticky="w", **PADDING)
        ttk.Entry(parent, textvariable=var).grid(row=row, column=1, sticky="ew", **PADDING)

    def _log(self, message: str) -> None:
        self.log.configure(state="normal")
        self.log.insert("end", message + "\n")
        self.log.see("end")
        self.log.configure(state="disabled")

    # --- Actions --------------------------------------------------------------

    def _on_generate(self) -> None:
        try:
            config = ProjectConfig(
                app_name=self.var_app_name.get().strip(),
                package=self.var_package.get().strip(),
                flavor=self.var_flavor.get(),
                main_activity=self.var_main_activity.get().strip() or "MainActivity",
                min_sdk=int(self.var_min_sdk.get()),
                target_sdk=int(self.var_target_sdk.get()),
                compile_sdk=int(self.var_compile_sdk.get()),
                output_dir=Path(self.var_output.get().strip()),
                overwrite=bool(self.var_overwrite.get()),
            ).validate()
        except (ValueError, TypeError) as exc:
            messagebox.showerror("Configuration invalide", str(exc))
            return

        self._log(f"→ Génération du projet « {config.app_name} » dans {config.project_dir}")
        self.create_button.config(state="disabled")
        threading.Thread(
            target=self._run_generate, args=(config,), daemon=True
        ).start()

    def _run_generate(self, config: ProjectConfig) -> None:
        try:
            files = generate_project(config)
        except Exception as exc:  # noqa: BLE001 — on affiche tout
            self.root.after(0, self._on_generate_error, exc)
            return
        self.root.after(0, self._on_generate_success, config, files)

    def _on_generate_success(self, config: ProjectConfig, files: list[Path]) -> None:
        self._log(f"✓ {len(files)} fichiers créés sous {config.project_dir}")
        for file in sorted(files):
            self._log(f"   - {file.relative_to(config.project_dir)}")
        self.create_button.config(state="normal")
        messagebox.showinfo(
            "Projet généré",
            f"Projet « {config.app_name} » créé dans :\n{config.project_dir}",
        )

    def _on_generate_error(self, exc: Exception) -> None:
        self._log(f"✗ Erreur : {exc}")
        self._log(traceback.format_exc().rstrip())
        self.create_button.config(state="normal")
        messagebox.showerror("Échec de la génération", str(exc))

    def _on_add_activity(self) -> None:
        project_dir = Path(self.var_act_project.get().strip())
        name = self.var_act_name.get().strip()
        overwrite = bool(self.var_act_overwrite.get())

        if not name:
            messagebox.showerror("Champ requis", "Le nom de l'Activity est obligatoire.")
            return

        self._log(f"→ Ajout de l'Activity {name} dans {project_dir}")
        self.activity_button.config(state="disabled")
        threading.Thread(
            target=self._run_add_activity,
            args=(project_dir, name, overwrite),
            daemon=True,
        ).start()

    def _run_add_activity(self, project_dir: Path, name: str, overwrite: bool) -> None:
        try:
            files = add_activity(project_dir, name, overwrite=overwrite)
        except Exception as exc:  # noqa: BLE001
            self.root.after(0, self._on_activity_error, exc)
            return
        self.root.after(0, self._on_activity_success, project_dir, files)

    def _on_activity_success(self, project_dir: Path, files: list[Path]) -> None:
        self._log(f"✓ Activity ajoutée — {len(files)} fichiers créés")
        for file in sorted(files):
            self._log(f"   - {file.relative_to(project_dir)}")
        self.activity_button.config(state="normal")
        messagebox.showinfo("Activity ajoutée", "L'Activity a été ajoutée et enregistrée dans le manifest.")

    def _on_activity_error(self, exc: Exception) -> None:
        self._log(f"✗ Erreur : {exc}")
        self._log(traceback.format_exc().rstrip())
        self.activity_button.config(state="normal")
        messagebox.showerror("Échec de l'ajout", str(exc))


def launch() -> int:
    """Crée la fenêtre principale et lance la boucle Tk."""
    if _TK_IMPORT_ERROR is not None:
        print(f"Tkinter indisponible : {_TK_IMPORT_ERROR}")
        print(
            "Sur Linux, installez 'python3-tk'. Sous Windows et macOS, "
            "Tkinter est inclus avec l'installeur Python officiel."
        )
        return 1
    try:
        root = Tk()
    except Exception as exc:  # noqa: BLE001 — pas de display, etc.
        print(f"Impossible d'initialiser Tkinter : {exc}")
        print(
            "Aucun affichage graphique disponible ? Lancer la GUI depuis une "
            "session avec un environnement graphique (Windows, macOS, ou Linux "
            "avec un serveur X/Wayland actif)."
        )
        return 1
    AndroidBuilderGUI(root)
    root.mainloop()
    return 0
