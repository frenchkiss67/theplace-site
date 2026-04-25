# ============================================================
# Makefile - THE PLACE C64 Intro
# Assembleur: KickAssembler
# Émulateur: VICE (x64sc)
# ============================================================

KICK = java -jar KickAss.jar
VICE = x64sc
SIDPLAY = sidplayfp
PY = python3

SRC = main.asm
OUT = intro.prg

# --- SID digi-sample ---
AUDIO2SID = ../tools/audio2sid.py
SID_WAV ?= sample.wav
SID_OUT ?= sample.sid
SID_RATE ?= 8000

.PHONY: all clean run sid sid-run

all: $(OUT)

$(OUT): $(SRC) irq.asm logo.asm rasterbars.asm sinscroll.asm tables.asm charset.asm
	$(KICK) $(SRC) -o $(OUT)

run: $(OUT)
	$(VICE) $(OUT)

# Convertit un WAV en .sid digi-sample. Usage: make sid SID_WAV=foo.wav SID_OUT=foo.sid
sid: $(AUDIO2SID)
	$(PY) $(AUDIO2SID) $(SID_WAV) $(SID_OUT) --rate $(SID_RATE)

sid-run: sid
	$(SIDPLAY) $(SID_OUT)

clean:
	rm -f $(OUT) *.sym *.vs *.dbg
