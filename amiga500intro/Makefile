# ============================================================
# Makefile - THE PLACE Amiga 500 Intro
# Assembleur : vasm (motorola syntax)
# Linker     : vlink (format Amiga hunk)
# Emulateur  : FS-UAE ou WinUAE
# ============================================================

VASM    = vasmm68k_mot
VLINK   = vlink
EMU     = fs-uae

ASMFLAGS = -Fhunk -m68000 -kick1hunks -no-opt
LDFLAGS  = -bamigahunk

SRC      = main.s
INCLUDES = hardware.i copper.s scroll.s tables.s
OBJ      = main.o
OUT      = intro

.PHONY: all clean run

all: $(OUT)

$(OBJ): $(SRC) $(INCLUDES)
	$(VASM) $(ASMFLAGS) -o $(OBJ) $(SRC)

$(OUT): $(OBJ)
	$(VLINK) $(LDFLAGS) -o $(OUT) $(OBJ)

run: $(OUT)
	$(EMU) --amiga_model=A500 --kickstart_file=kick.rom --hard_drive_0=. \
	       --automatic_input_grab=0 $(OUT)

clean:
	rm -f $(OBJ) $(OUT) *.sym
