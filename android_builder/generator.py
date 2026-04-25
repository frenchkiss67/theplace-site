"""Génération de projets Android et de composants supplémentaires."""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path

from android_builder.templates import (
    COMMON_FILES,
    FLAVORS,
    GRADLE_WRAPPER_FILES,
)
from android_builder.utils import (
    info,
    package_to_path,
    render,
    success,
    to_resource_name,
    validate_app_name,
    validate_class_name,
    validate_package,
    write_file,
)


@dataclass
class ProjectConfig:
    """Configuration validée pour la génération d'un projet."""
    app_name: str
    package: str
    flavor: str
    main_activity: str = "MainActivity"
    min_sdk: int = 24
    target_sdk: int = 34
    compile_sdk: int = 34
    output_dir: Path = field(default_factory=lambda: Path.cwd())
    overwrite: bool = False

    def validate(self) -> "ProjectConfig":
        validate_app_name(self.app_name)
        validate_package(self.package)
        validate_class_name(self.main_activity)
        if not (1 <= self.min_sdk <= 99):
            raise ValueError(f"min_sdk doit être entre 1 et 99 (reçu {self.min_sdk}).")
        if self.target_sdk < self.min_sdk:
            raise ValueError("target_sdk doit être ≥ min_sdk.")
        if self.compile_sdk < self.target_sdk:
            raise ValueError("compile_sdk doit être ≥ target_sdk.")
        if self.flavor not in FLAVORS:
            raise ValueError(
                f"Flavor inconnu: '{self.flavor}'. "
                f"Choix possibles: {', '.join(FLAVORS)}."
            )
        return self

    @property
    def project_dir(self) -> Path:
        # Dossier racine du projet généré, basé sur le nom d'app slugifié.
        slug = to_resource_name(self.app_name)
        return self.output_dir / slug

    @property
    def theme_name(self) -> str:
        # Pour Theme.<theme_name> dans les XML : on enlève espaces et tirets.
        cleaned = "".join(part.capitalize() for part in self.app_name.replace("-", " ").replace("_", " ").split())
        return cleaned or "App"

    def template_mapping(self) -> dict[str, str]:
        return {
            "app_name": self.app_name,
            "package": self.package,
            "package_path": str(package_to_path(self.package)),
            "main_activity": self.main_activity,
            "theme_name": self.theme_name,
            "min_sdk": str(self.min_sdk),
            "target_sdk": str(self.target_sdk),
            "compile_sdk": str(self.compile_sdk),
        }


def generate_project(config: ProjectConfig) -> list[Path]:
    """Génère un projet Android complet et retourne la liste des fichiers créés."""
    config.validate()
    mapping = config.template_mapping()
    flavor = FLAVORS[config.flavor]
    files = list(COMMON_FILES) + list(flavor["files"]) + list(GRADLE_WRAPPER_FILES)

    project_dir = config.project_dir
    if project_dir.exists() and any(project_dir.iterdir()) and not config.overwrite:
        raise FileExistsError(
            f"Le dossier '{project_dir}' n'est pas vide. "
            "Utiliser --overwrite pour écraser."
        )

    written: list[Path] = []
    for relative_path, template in files:
        rendered_path = render(relative_path, mapping)
        rendered_content = render(template, mapping)
        target = project_dir / rendered_path
        write_file(target, rendered_content, overwrite=config.overwrite)
        written.append(target)

    info(
        f"Projet '{config.app_name}' généré dans {project_dir} "
        f"(flavor: {config.flavor}, package: {config.package})."
    )
    return written


# ---------------------------------------------------------------------------
# Helpers pour ajouter des composants à un projet existant.
# ---------------------------------------------------------------------------


def detect_project(project_dir: Path) -> dict[str, str]:
    """Inspecte un projet Android existant pour récupérer package et flavor."""
    manifest = project_dir / "app/src/main/AndroidManifest.xml"
    app_gradle_kts = project_dir / "app/build.gradle.kts"
    app_gradle = project_dir / "app/build.gradle"
    if not manifest.exists():
        raise FileNotFoundError(
            f"Aucun AndroidManifest.xml trouvé sous {manifest}. "
            "Le dossier ne semble pas être un projet Android."
        )

    gradle_path = app_gradle_kts if app_gradle_kts.exists() else app_gradle
    if not gradle_path.exists():
        raise FileNotFoundError("app/build.gradle(.kts) introuvable.")
    text = gradle_path.read_text(encoding="utf-8")

    package = _extract_namespace(text)
    language = "kotlin" if "kotlin.android" in text or "kotlinOptions" in text else "java"
    uses_compose = "compose = true" in text or "buildFeatures.compose" in text
    flavor = (
        "kotlin-compose" if (language == "kotlin" and uses_compose)
        else "kotlin-views" if language == "kotlin"
        else "java-views"
    )
    return {"package": package, "flavor": flavor, "language": language}


def _extract_namespace(gradle_text: str) -> str:
    for line in gradle_text.splitlines():
        line = line.strip()
        if line.startswith("namespace"):
            # namespace = "com.example.app"  ou  namespace "com.example.app"
            quote_open = line.find('"')
            quote_close = line.rfind('"')
            if quote_open != -1 and quote_close > quote_open:
                return line[quote_open + 1 : quote_close]
    raise ValueError("Impossible de trouver 'namespace' dans build.gradle(.kts).")


_COMPOSE_ACTIVITY_TMPL = """\
package {{package}}

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

class {{name}} : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            {{name}}Screen()
        }
    }
}

@Composable
fun {{name}}Screen() {
    Text(text = "Écran {{name}}")
}
"""

_KOTLIN_ACTIVITY_TMPL = """\
package {{package}}

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class {{name}} : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.{{layout}})
    }
}
"""

_JAVA_ACTIVITY_TMPL = """\
package {{package}};

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class {{name}} extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.{{layout}});
    }
}
"""

_LAYOUT_TMPL = """\
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".{{name}}">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Écran {{name}}"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
"""


def add_activity(
    project_dir: Path,
    name: str,
    *,
    overwrite: bool = False,
) -> list[Path]:
    """Ajoute une nouvelle Activity à un projet existant."""
    validate_class_name(name)
    detected = detect_project(project_dir)
    package = detected["package"]
    flavor = detected["flavor"]
    pkg_path = package_to_path(package)
    base = to_resource_name(name).removeprefix("activity_").removesuffix("_activity")
    layout_name = f"activity_{base or to_resource_name(name)}"

    written: list[Path] = []
    if flavor == "kotlin-compose":
        target = project_dir / "app/src/main/java" / pkg_path / f"{name}.kt"
        write_file(
            target,
            render(_COMPOSE_ACTIVITY_TMPL, {"package": package, "name": name}),
            overwrite=overwrite,
        )
        written.append(target)
    elif flavor == "kotlin-views":
        kt_target = project_dir / "app/src/main/java" / pkg_path / f"{name}.kt"
        layout_target = project_dir / "app/src/main/res/layout" / f"{layout_name}.xml"
        mapping = {"package": package, "name": name, "layout": layout_name}
        write_file(kt_target, render(_KOTLIN_ACTIVITY_TMPL, mapping), overwrite=overwrite)
        write_file(layout_target, render(_LAYOUT_TMPL, mapping), overwrite=overwrite)
        written.extend([kt_target, layout_target])
    else:  # java-views
        java_target = project_dir / "app/src/main/java" / pkg_path / f"{name}.java"
        layout_target = project_dir / "app/src/main/res/layout" / f"{layout_name}.xml"
        mapping = {"package": package, "name": name, "layout": layout_name}
        write_file(java_target, render(_JAVA_ACTIVITY_TMPL, mapping), overwrite=overwrite)
        write_file(layout_target, render(_LAYOUT_TMPL, mapping), overwrite=overwrite)
        written.extend([java_target, layout_target])

    _register_activity_in_manifest(project_dir, name)
    success(f"Activity {name} ajoutée ({flavor}).")
    return written


def _register_activity_in_manifest(project_dir: Path, activity_name: str) -> None:
    """Insère la nouvelle Activity dans AndroidManifest.xml si absente."""
    manifest_path = project_dir / "app/src/main/AndroidManifest.xml"
    text = manifest_path.read_text(encoding="utf-8")
    if f'android:name=".{activity_name}"' in text:
        return
    snippet = (
        f'        <activity\n'
        f'            android:name=".{activity_name}"\n'
        f'            android:exported="false" />\n'
    )
    closing = "    </application>"
    if closing not in text:
        info("Manifest: balise </application> introuvable, insertion ignorée.")
        return
    new_text = text.replace(closing, snippet + closing, 1)
    manifest_path.write_text(new_text, encoding="utf-8")
