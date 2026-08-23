# Maquettes UI — Tickets de caisse

Reconstitution des 8 écrans de l'app à partir des tokens et composants
réels du code (`ui/theme/Color.kt`, `Theme.kt`, `Type.kt` et les écrans
`ui/*.kt`) — pas d'approximation, les valeurs sont relevées.

Chaque `.dc.html` est un écran autonome dessiné à **390 × 844** (phone).
`canvas.json` décrit leur disposition sur le canvas.

| Fichier                | Écran                                   |
|------------------------|-----------------------------------------|
| `Empty.dc.html`        | Liste vide (premier lancement)           |
| `Main.dc.html`         | Liste remplie — écran principal          |
| `Selection.dc.html`    | Mode sélection multiple                  |
| `Detail.dc.html`       | Détail d'un ticket + garantie            |
| `Stats.dc.html`        | Statistiques (donut + 12 mois)           |
| `Onboarding.dc.html`   | Onboarding, page 1/2                     |
| `Settings.dc.html`     | Dialogue Réglages                        |
| `Lock.dc.html`         | Écran de verrouillage biométrique        |

## Tokens repris du code

| Rôle                  | Valeur    | Source                          |
|-----------------------|-----------|---------------------------------|
| `primary`             | `#0E7C66` | `Color.kt` — `Teal40`           |
| `secondary`           | `#466A60` | `Color.kt` — `TealGrey40`       |
| `tertiary`            | `#7A5C2E` | `Color.kt` — `Sand40`           |
| `primaryContainer`    | `#EADDFF` | baseline M3 (non surchargé)     |
| `secondaryContainer`  | `#E8DEF8` | baseline M3 (non surchargé)     |
| Typographie           | Roboto    | `Type.kt` — `Typography()` M3   |

## Deux points relevés en dessinant

1. **Les rôles `*Container` sont violets.** `Theme.kt` ne surcharge que
   `primary` / `secondary` / `tertiary` ; tous les containers gardent la
   palette M3 baseline. Le FAB, la barre de sélection et les cartes
   sélectionnées sortent donc en lavande. Le dynamic color Android 12+
   masque le problème, pas les versions antérieures.
2. **La TopAppBar du détail déborde.** Retour + titre + 4 actions ne
   tiennent pas dans 390 dp : le titre est tronqué. Idem pour
   « %d sélectionné(s) ». Dans `MetadataRow`, le bouton catégorie
   (40 dp) et le champ montant (56 dp) sont alignés en haut faute de
   `verticalAlignment`.

## Régénérer le canvas

Les `.dc.html` sont la source ; le canvas publié est un artefact
régénéré à la demande (~2 Mo, non versionné — cf. `.gitignore`) :

```bash
cd receipt-scanner/mockups
node "<skill>/seed-canvas.mjs" \
  --template "<skill>/payload.template.html" \
  --out tickets-de-caisse-ui.html \
  --title "Tickets de caisse — UI" \
  --artboard Main.dc.html --artboard Empty.dc.html \
  --artboard Selection.dc.html --artboard Detail.dc.html \
  --artboard Stats.dc.html --artboard Onboarding.dc.html \
  --artboard Settings.dc.html --artboard Lock.dc.html \
  --canvas canvas.json
```

Les maquettes sont **statiques** : elles documentent l'UI, elles ne la
pilotent pas. Toute divergence avec le code se tranche en faveur du code.
