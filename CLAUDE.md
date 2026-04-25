# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository overview

Despite the repo name `theplace-site`, the only active codebase here is **`c64intro/`** — a Commodore 64 intro/demo written in 6510 assembler for KickAssembler. It produces a single PAL `intro.prg` with three classic effects: a hi-res bitmap logo at the top, animated raster bars in the middle band, and a sinus scroller across the lower screen.

The repo root also contains **`env_local.txt`**, **`index_js.txt`**, **`package_json.txt`**, and **`theplace-site-code.pdf`** — these are plain-text snapshots of an unrelated Next.js / Supabase / EmailJS site (kept for reference only). They are **not** wired into any build and should generally be left alone unless the user explicitly asks about them.

## Build / run

All commands run from `c64intro/`. The toolchain is **KickAssembler only** (the older docs mentioning ACME are wrong — there is no ACME-compatible build).

```bash
# Compile (entry point is main.asm, which #imports every other .asm file)
make                    # → produces intro.prg
java -jar KickAss.jar main.asm -o intro.prg   # equivalent

# Run in the VICE emulator
make run                # builds then launches x64sc intro.prg
./build.sh run          # equivalent

# Clean
make clean              # removes intro.prg, *.sym, *.vs, *.dbg
```

`KICKASS_JAR` and `VICE_BIN` env vars override the defaults in `build.sh`. `setup-c64-dev.sh` is a one-shot installer that fetches KickAssembler, installs VICE via apt/brew, installs the VS Code KickAss extensions, and writes `.vscode/{settings,tasks}.json` — only run it on a fresh dev machine.

There is no test suite. "Testing" means running the `.prg` in VICE and visually verifying the three effects.

## Architecture

### Memory layout (chosen by `main.asm`)

| Region | Use |
|--------|-----|
| `$0801` | BASIC upstart stub (`BasicUpstart2(start)`) so the user can `LOAD"…",8,1 : RUN` |
| `$0810` | Mutable variables (`frame_flag`, `scroll_x`, `text_ptr`, `sin_phase`, `bar_offset`, `temp_x`, `scroll_buffer[41]`) |
| `$0400`–`$07FF` | Text-mode screen RAM |
| `$0800`–`$0FFF` | Charset copied from char ROM at boot (see `charset.asm`) |
| `$2000`–`$3F3F` | Hi-res bitmap data for the logo (8000 bytes generated at assembly time by `logo.asm`) |
| `$3C00`–`$3FE7` | Bitmap-mode screen RAM (foreground/background colour nibbles) |
| `$C000`+ | Main code, IRQ handlers, all imported modules and tables |
| `$D800`+ | Color RAM |

`$01` is set to `$35` at startup (BASIC + Kernal ROM off, I/O on). `charset.asm` temporarily flips it to `$33` while copying the char ROM, then restores it.

### IRQ chain (the heart of the demo)

`main.asm` initialises everything, enables interrupts, and then spins on `frame_flag`. All real-time work happens inside three chained raster IRQs configured in `irq.asm`. Each handler sets the next handler's vector at `$FFFE/$FFFF` and the next raster line at `$D012` before `RTI`:

1. **`irq_top` @ raster line `$30` (`IRQ1_LINE`)** — switches the VIC-II to **hi-res bitmap** (`$D011=$3B`, `$D018=$F8`, `$D016=$08`). Note this is hi-res, *not* multicolor — bit 4 of `$D016` stays 0.
2. **`irq_mid` @ raster line `$82` (`IRQ2_LINE`)** — switches back to text mode (`$D011=$1B`, `$D018=$12`), applies the current `scroll_x` to `$D016` for smooth horizontal scrolling, then runs a tight cycle-padded loop that writes `bar_colors[bar_offset+i]` into `$D020`/`$D021` for `RASTER_BAR_LINES` (48) consecutive raster lines. The NOP/`bit $ea` padding inside `rbar_loop` is calibrated for ~63 PAL cycles per line — **do not change it without recounting cycles**.
3. **`irq_bottom` @ raster line `$F8` (`IRQ3_LINE`)** — sets `frame_flag=1` to release the main loop, then arms `irq_top` for the next frame.

The main loop reacts to `frame_flag` by calling `update_scroll`, incrementing `bar_offset` (animates the rasters), and adding 2 to `sin_phase` (animates the scroller wave).

### Sinus scroller (`sinscroll.asm`)

- 41-byte `scroll_buffer` holds the on-screen text. `scroll_x` counts 7→0; on underflow the buffer is shifted one column left and the next character of `scroll_text` is appended. `text_ptr` wraps to the start when it hits the trailing `0` byte.
- `place_scroll_chars` clears 7 rows starting at `SCROLL_BASE_ROW=18`, then for each column `c` reads `sin_table[(c*4 + sin_phase) & $FF]` (range 0..6) to pick which of those 7 rows the character goes on. Address-of-row lookup uses pre-computed `row_addr_lo/hi` tables (filled at assembly time, one entry per text row).
- The scroll text uses `.encoding "screencode_upper"`, so the `.text` literals are already screen codes — no PETSCII conversion at runtime.

### Logo (`logo.asm`)

The "THE PLACE" logo is **generated at assembly time**, not loaded from a file. Five `.var logo_lineN` strings of `#`/`.` describe a 5×40 character grid; the `bitmapByte(idx)` function maps each of the 8000 bitmap bytes to either `$FF` (filled cell) or `$00`. To change the logo, edit the five strings in `logo.asm` — there is no `.kla` or `.bin` asset.

### Tables (`tables.asm`, `rasterbars.asm`)

- `sin_table` — 256 bytes, formula `round(3 + 3*sin(i*2π/256))`, range 0..6, used for the scroller's vertical wave.
- `bar_colors` — 256 bytes (4 colour bars × 16 entries × 4 repeats) indexed by the running `bar_offset`. Adding a new bar means appending another 16-byte block inside the `.for (var rep = 0; rep < 4; rep++)` loop and keeping the total a power of two so the `inx` wraparound stays seamless.

## Conventions

- Opcodes lowercase, labels `snake_case`, constants `UPPER_SNAKE_CASE` (declared via `.const`).
- Comments are in **French**.
- KickAssembler-specific syntax is used throughout: `.const`, `.var`, `.pc = …`, `.fill`, `.for`, `.function`, `.byte`, `.text`, `.encoding`, `#import`, `BasicUpstart2(…)`, `!label:` for local labels, `<addr` / `>addr` for low/high byte. Don't translate to ACME or DASM.
- All sub-modules are pulled in by `main.asm` via `#import`. New `.asm` files must be added to both that `#import` list **and** the `Makefile` prerequisite line, otherwise `make` won't rebuild on changes.

## Timing constraints (don't break these)

- Target is **PAL** (312 raster lines, 63 cycles/line). The raster-bar busy loop in `irq_mid` is hand-tuned to consume exactly one scanline per iteration; any added/removed instruction inside `rbar_loop` will produce visible tearing or colour shifts.
- The IRQ handlers do **not** use stable-raster (double-IRQ) tricks — there is some jitter at the mode switches. If a future change requires rock-steady transitions, that has to be added explicitly.
- Only the bitmap region (`$2000`–`$3F3F`) and the bitmap screen RAM (`$3C00`–`$3FE7`) overlap; everything else is laid out so KickAssembler will throw a memory-overlap error if you collide regions. Trust those errors instead of `.pc`-overriding around them.
