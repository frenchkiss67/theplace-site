# ============================================================
#  Makefile — Zoom Scrolltext C64
# ============================================================

KICKASS ?= java -jar $(KICKASS_JAR)
KICKASS_JAR ?= KickAss.jar

SRC := main.asm
OUT := zoomscroll.prg

ASM_FILES := main.asm scroller.asm text.asm tables.asm \
             font_cyber.asm font_neon.asm

.PHONY: all run clean help

all: $(OUT)

$(OUT): $(ASM_FILES)
	@echo "=> Assemblage $< -> $@"
	$(KICKASS) $(SRC) -o $@ -showmem

run: $(OUT)
	x64sc -warp $(OUT)

clean:
	rm -f $(OUT) *.sym *.vs

help:
	@echo "Targets :"
	@echo "  all    - Assemble le PRG (défaut)"
	@echo "  run    - Lance dans VICE (x64sc)"
	@echo "  clean  - Supprime les fichiers générés"
	@echo ""
	@echo "Variables :"
	@echo "  KICKASS_JAR=/path/to/KickAss.jar"
