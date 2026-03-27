# ============================================================
# Makefile - THE PLACE C64 Intro
# Assembleur: KickAssembler
# Émulateur: VICE (x64sc)
# ============================================================

KICK = java -jar KickAss.jar
VICE = x64sc
SRC = main.asm
OUT = intro.prg

.PHONY: all clean run

all: $(OUT)

$(OUT): $(SRC) irq.asm logo.asm rasterbars.asm sinscroll.asm tables.asm charset.asm
	$(KICK) $(SRC) -o $(OUT)

run: $(OUT)
	$(VICE) $(OUT)

clean:
	rm -f $(OUT) *.sym *.vs *.dbg
