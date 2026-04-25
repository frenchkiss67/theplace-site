# SID Digi-Sample - Convertisseur audio -> .sid

Convertit un WAV en fichier `.sid` (PSID v2) jouable par VICE, sidplayfp,
JSidPlay2, etc., en utilisant la technique digi-sample 4-bit pilotee par
le registre de volume du SID (`$D418`). C'est la methode oldskool des
classiques C64 (Ghostbusters, Arkanoid, Cinemaware...).

## Principe

Le SID dispose d'un convertisseur N/A 4 bits sur son registre de volume
maitre. En l'ecrivant a frequence audio (4-8 kHz typiquement), pilote par
le timer A de la CIA1, on obtient une voix numerique sans utiliser les
trois voies oscillateurs.

```
WAV --> mono --> reechantillonnage --> normalisation
    --> quantification 4-bit --> packing nibbles
    --> player 6510 (init+play) --> entete PSID v2 --> .sid
```

## Disposition memoire (charge a $1000)

| Adresse  | Role                                |
| -------- | ----------------------------------- |
| `$1000`  | `init` - configure CIA et pointeurs |
| `$1080`  | `play` - sort un nibble dans `$D418`|
| `$1100+` | donnees nibble-packees              |

Variables zero page : `$02` (flag nibble), `$FB/$FC` (pointeur sample).

## Utilisation

### Via Python directement
```bash
python3 tools/audio2sid.py voice.wav voice.sid --rate 8000 --name "Hello"
```

### Via le wrapper shell
```bash
c64intro/sid/build_sid.sh voice.wav voice.sid --rate 8000
```

### Via Make
```bash
cd c64intro
make sid SID_WAV=voice.wav SID_OUT=voice.sid SID_RATE=8000
make sid-run SID_WAV=voice.wav    # convertit + joue avec sidplayfp
```

### Lecture
```bash
sidplayfp voice.sid     # lecteur ligne de commande
x64sc voice.sid         # VICE en mode VSID
```

## Options

| Option              | Defaut         | Effet                                   |
| ------------------- | -------------- | --------------------------------------- |
| `--rate N`          | 8000           | Frequence d'echantillonnage cible (Hz)  |
| `--max-bytes N`     | 32768          | Taille max des donnees packees          |
| `--name "TITRE"`    | "Audio Sample" | Champ name du PSID                      |
| `--author "NOM"`    | "audio2sid"    | Champ author du PSID                    |
| `--released "..."`  | "2026 ..."     | Champ released du PSID                  |
| `--no-loop`         | (boucle)       | Joue une fois puis silence              |
| `--no-preemphasis`  | (active)       | Desactive le filtre HF de compensation  |
| `--ntsc`            | (PAL)          | Cible NTSC (CPU 1.022 MHz au lieu de 0.985) |

## Compromis taille/qualite

A 4 bits par echantillon, donnees packees a 2 echantillons par octet :

| Frequence | Qualite       | Octets/sec | Duree max ~30 KB |
| --------- | ------------- | ---------- | ---------------- |
| 4 kHz     | telephonique  | 2000       | ~15 s            |
| 6 kHz     | radio AM      | 3000       | ~10 s            |
| 8 kHz     | recommandee   | 4000       | ~7.5 s           |
| 11 kHz    | tres bonne    | 5500       | ~5.5 s           |

Au-dela de ~12 kHz le timing CIA devient critique et la qualite
audible n'augmente plus vraiment (le DAC volume reste 4-bit).

## Source assembleur

`sidplayer.asm` reproduit exactement le code emis par `audio2sid.py`,
en syntaxe KickAssembler. Il est a titre documentaire et pour
personnalisation manuelle (changement de frequence en dur, ajout d'un
fade-in, etc.).

## Limites connues

- Le DAC `$D418` du SID 8580 (revision tardive) attenue beaucoup
  les digi-samples ; les SID 6581 sont nettement meilleurs pour cela.
  Le PSID est marque pour 6581 par defaut.
- Le timer CIA limite la frequence pratique a ~16 kHz.
- Pour de la 8-bit qualite "Mahoney" (technique 2014), il faudrait
  un encodage par table inverse beaucoup plus complexe ; la 4-bit
  reste le standard portable.
