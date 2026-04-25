"""Templates de projet Android, organisés par flavor (kotlin_compose, kotlin_views, java)."""

from android_builder.templates.kotlin_compose import KOTLIN_COMPOSE_FILES
from android_builder.templates.kotlin_views import KOTLIN_VIEWS_FILES
from android_builder.templates.java_views import JAVA_VIEWS_FILES
from android_builder.templates.common import COMMON_FILES, GRADLE_WRAPPER_FILES

FLAVORS = {
    "kotlin-compose": {
        "label": "Kotlin + Jetpack Compose (recommandé)",
        "language": "kotlin",
        "files": KOTLIN_COMPOSE_FILES,
    },
    "kotlin-views": {
        "label": "Kotlin + XML Views (classique)",
        "language": "kotlin",
        "files": KOTLIN_VIEWS_FILES,
    },
    "java-views": {
        "label": "Java + XML Views",
        "language": "java",
        "files": JAVA_VIEWS_FILES,
    },
}

__all__ = [
    "FLAVORS",
    "COMMON_FILES",
    "GRADLE_WRAPPER_FILES",
    "KOTLIN_COMPOSE_FILES",
    "KOTLIN_VIEWS_FILES",
    "JAVA_VIEWS_FILES",
]
