# ============================================================
# Makefile - THE PLACE C64 Intro
# Assembleur: KickAssembler
# Émulateur: VICE (x64sc)
# ============================================================

KICK = java -jar KickAss.jar
VICE = x64sc
SRC = main.asm
OUT = intro.prg

# --- Générateur de sprites ---
# Surcharger via : make sprites SPRITE_INPUT=anim.gif SPRITE_LABEL=ball SPRITE_ADDRESS=\$$3000
SPRITE_TOOL    = python3 tools/gif2sprite.py
SPRITE_INPUT  ?=
SPRITE_LABEL  ?= sprite_data
SPRITE_OUT    ?= data/$(SPRITE_LABEL).asm
SPRITE_ADDRESS?= $$3000
SPRITE_SCREEN ?= $$0400
SPRITE_MODE   ?= multicolor
SPRITE_OPTS   ?= --mode $(SPRITE_MODE) --label $(SPRITE_LABEL) \
                 --address '$(SPRITE_ADDRESS)' --screen-base '$(SPRITE_SCREEN)'

.PHONY: all clean run sprites

all: $(OUT)

$(OUT): $(SRC) irq.asm logo.asm rasterbars.asm sinscroll.asm tables.asm charset.asm
	$(KICK) $(SRC) -o $(OUT)

run: $(OUT)
	$(VICE) $(OUT)

sprites:
	@if [ -z "$(SPRITE_INPUT)" ]; then \
		echo "Usage : make sprites SPRITE_INPUT=fichier.gif|.mp4 [SPRITE_OUT=...] [SPRITE_OPTS=...]"; \
		exit 1; \
	fi
	@mkdir -p $(dir $(SPRITE_OUT))
	$(SPRITE_TOOL) "$(SPRITE_INPUT)" -o "$(SPRITE_OUT)" $(SPRITE_OPTS)

clean:
	rm -f $(OUT) *.sym *.vs *.dbg
