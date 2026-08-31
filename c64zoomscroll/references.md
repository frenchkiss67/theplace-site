# Références — Sources code & documentation C64

Documents et dépôts de code consultés pour la conception du zoom scrolltext.
Tous les liens ont été vérifiés le 2026-04-19.

## Sites de référence généraux

- [Codebase64 wiki](https://codebase.c64.org/) — Le wiki de référence de la scène C64 (routines, tutos, tricks VIC-II).
- [The Raistlin Papers](https://c64demo.com/) — Articles détaillés sur les techniques demo (DXYCP, big scrollers, side-border).
- [Antimon code dumps](http://www.antimon.org/code/) — Archive historique de routines 6502 (DYCP, raster, FLD).
- [Lemon64 forum](https://www.lemon64.com/forum/) — Discussions techniques actives avec code partagé.
- [CSDb](https://csdb.dk/) — Base de données scène C64, sources de démos libres.

## Double IRQ stable (obligatoire pour zoom cycle-exact)

- [Codebase64 — The double IRQ method](https://codebase.c64.org/doku.php?id=base:the_double_irq_method)
- [Codebase64 — Double IRQ explained](https://codebase.c64.org/doku.php?id=base:double_irq_explained)
- [Antimon — Making stable raster routines (Pasi Ojala)](https://www.antimon.org/dl/c64/code/stable.txt)
- [Bumbershoot — Stabilizing the VIC-II Raster](https://bumbershootsoft.wordpress.com/2015/12/29/stabilizing-the-vic-ii-raster/)
- [C64-Wiki — Raster interrupt](https://www.c64-wiki.com/wiki/Raster_interrupt)

Principe-clé retenu : premier IRQ à la ligne N, puis un second IRQ ré-armé pour N+1 précédé de NOPs → jitter ramené à ±1 cycle. Le pattern `lda $d012 / cmp $d012 / beq +1` teste si la lecture a eu lieu en N ou N+1 et ajoute 1 cycle si besoin.

## YSCROLL trick / FLD (cœur du zoom)

- [Codebase64 — FPD (Flexible Pixel Distance)](https://codebase.c64.org/doku.php?id=base:fpd)
- [0xc64 — Simple FLD effect + code](http://www.0xc64.com/2015/11/17/simple-fld-effect/)
- [GitHub 0xc64/c64 — flexlinedistance.asm](https://github.com/0xc64/c64/blob/master/raster/flexlinedistance.asm)
- [Bumbershoot — Flexible Line Distance](https://bumbershootsoft.wordpress.com/2015/09/17/flexible-line-distance-fld/)
- [Antimon — FLD by Marek Klampar](http://www.antimon.org/dl/c64/code/fld.txt)
- [nurpax — Bad lines and flexible line distance (BINTRIS part 5)](https://nurpax.github.io/posts/2018-06-19-bintris-on-c64-part-5.html)

Principe-clé retenu : écrire `(YSCROLL+1) & 7 | $18` dans `$D011` juste avant chaque bad line potentielle → le VIC répète la ligne de char matrix et décale tout le bas.

## Scrolltext (horizontal + DYCP/DXYCP)

- [Slarti64 — 1x2 Font Scroller with Music (code complet)](https://github.com/Slarti64/C64-Code-Hacking/wiki/1x2-Font-Scroller-with-Music)
- [Raistlin Papers — DXYCP Scrollers](https://c64demo.com/dxycp-scrollers/)
- [Raistlin Papers — Side Border Bitmap Scroller](https://c64demo.com/side-border-bitmap-scroller/)
- [Antimon — DYCP Horizontal Scrolling by Pasi Ojala](http://www.antimon.org/dl/c64/code/dycp.txt)
- [0xc64 — 1×1 Smooth Text Scroller](http://www.0xc64.com/2013/11/24/1x1-smooth-text-scroller/)
- [1amstudios — How to implement smooth full-screen scrolling on C64](http://1amstudios.com/2014/12/07/c64-smooth-scrolling/)
- [Retro64 — Simple text scroller in assembly](https://retro64.altervista.org/blog/simple-text-scroller-coded-assemby-language-for-the-commodore-64/)
- [Covert Bitops — scroll rant (Cadaver)](https://cadaver.github.io/rants/scroll.html)

Technique retenue pour le scroll horizontal de base : `$D016` bits 0-2 (XSCROLL) décrémenté chaque frame, puis `shift_screen_left` + injection du prochain char à la colonne 39 quand XSCROLL passe à 7 (wrap).

## Raster bars & multi-couleurs

- [Codebase64 — Overlapping raster bars](https://codebase.c64.org/doku.php?id=base:overlapping_raster_bars)
- [Antimon — Rasters](http://www.antimon.org/dl/c64/code/raster.txt)

## Tutoriels complets 6502 / C64

- [Digitalerr0r — Commodore 64 Programming tutorial series](https://digitalerr0r.net/2011/03/19/commodore-64-programming-a-quick-start-guide-to-c-64-assembly-programming-on-windows/) — Série complète (part 10 = multiple interrupts).
- [6502.org Source Code Library](https://6502.org/source/) — Routines 6502 génériques.
- [digitsensitive/c64 GitHub](https://github.com/digitsensitive/c64) — Exemples 6502 pour C64.
- [cityxen/Commodore64_Programming](https://github.com/cityxen/Commodore64_Programming) — Lib KickAssembler + exemples.
- [nurpax — C64jasm (macros sinus + pré-shift font)](https://nurpax.github.io/posts/2018-11-08-c64jasm.html)
- [DusteD — C64 assembly programming in 2020](https://dusted.dk/pages/c64/C64-programming/)

## Références VIC-II officielles

- Christian Bauer — *The MOS 6567/6569 Video Controller* (standard de fait pour la documentation VIC-II).
- [KickAssembler Reference Manual (Mads Nielsen)](https://theweb.dk/KickAssembler/KickAssembler.pdf)

## Emulateurs / outils

- **VICE** (`x64sc`) — Émulateur C64 de référence, cycle-accurate.
- **KickAssembler** — Assembleur recommandé (macros, `.fill`, maths natives).
- **ACME** — Assembleur cross-platform alternatif.
- **C64 Studio** (Windows) — IDE avec débogueur intégré.

## Notes de cycles PAL retenues pour le projet

- 312 lignes raster × 63 cycles = **19 656 cycles/frame** (~50 Hz).
- Bad line = toutes les 8 lignes quand `$D012 & 7 == YSCROLL`, vole **40-43 cycles**.
- Un IRQ 6510 prend **7 cycles** pour entrer, puis push A/X/Y = 9 cycles avant handler utile.
- Entrée IRQ + ack + `rti` ≈ 25-30 cycles minimum incompressibles.
- Pour un zoom cycle-exact on vise **≤ 63 cycles entre deux écritures `$D011`** pour rester dans une ligne raster.
