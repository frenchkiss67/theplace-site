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
# C64    : make sprites        SPRITE_INPUT=anim.gif SPRITE_LABEL=ball
# Amiga  : make sprites-amiga  SPRITE_INPUT=anim.gif SPRITE_LABEL=ball SPRITE_ADDRESS=\$$30000
SPRITE_TOOL     = python3 tools/gif2sprite.py
SPRITE_INPUT   ?=
SPRITE_LABEL   ?= sprite_data
SPRITE_ADDRESS ?= $$3000
SPRITE_SCREEN  ?= $$0400
SPRITE_MODE    ?= multicolor

SPRITE_C64_OUT  ?= data/$(SPRITE_LABEL).asm
SPRITE_C64_OPTS ?= --target c64 --mode $(SPRITE_MODE) --label $(SPRITE_LABEL) \
                   --address '$(SPRITE_ADDRESS)' --screen-base '$(SPRITE_SCREEN)'

SPRITE_AMIGA_OUT  ?= data/$(SPRITE_LABEL).s
SPRITE_AMIGA_HEIGHT ?= 21
SPRITE_AMIGA_NUM    ?= 0
SPRITE_AMIGA_OPTS ?= --target amiga --label $(SPRITE_LABEL) \
                     --height $(SPRITE_AMIGA_HEIGHT) --sprite-num $(SPRITE_AMIGA_NUM) \
                     --address '$(SPRITE_ADDRESS)'

.PHONY: all clean run sprites sprites-amiga

all: $(OUT)

$(OUT): $(SRC) irq.asm logo.asm rasterbars.asm sinscroll.asm tables.asm charset.asm
	$(KICK) $(SRC) -o $(OUT)

run: $(OUT)
	$(VICE) $(OUT)

sprites:
	@if [ -z "$(SPRITE_INPUT)" ]; then \
		echo "Usage : make sprites SPRITE_INPUT=fichier.gif|.mp4 [SPRITE_LABEL=...] [SPRITE_ADDRESS=...]"; \
		exit 1; \
	fi
	@mkdir -p $(dir $(SPRITE_C64_OUT))
	$(SPRITE_TOOL) "$(SPRITE_INPUT)" -o "$(SPRITE_C64_OUT)" $(SPRITE_C64_OPTS)

sprites-amiga:
	@if [ -z "$(SPRITE_INPUT)" ]; then \
		echo "Usage : make sprites-amiga SPRITE_INPUT=fichier.gif|.mp4 [SPRITE_LABEL=...] [SPRITE_AMIGA_HEIGHT=...] [SPRITE_AMIGA_NUM=0..7]"; \
		exit 1; \
	fi
	@mkdir -p $(dir $(SPRITE_AMIGA_OUT))
	$(SPRITE_TOOL) "$(SPRITE_INPUT)" -o "$(SPRITE_AMIGA_OUT)" $(SPRITE_AMIGA_OPTS)

clean:
	rm -f $(OUT) *.sym *.vs *.dbg
