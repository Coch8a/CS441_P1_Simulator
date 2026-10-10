; MAXFINDER_FP.asm
; Floating-point MAXFINDER for the team's custom 32-bit ISA
;
; Register usage:
; r0 = pointer to current array element
; r1 = number of elements remaining
; r2 = current maximum
; r3 = current candidate
; r4 = -1.0 floating-point constant
; r5 = candidate - current maximum
; r6 = integer zero
; r7 = output address
; r8 = address of -1.0 constant
;
; Data memory:
; 0x0104 = 0x35980000
; 0x0108 = 0x53BD0000
; 0x010C = 0x53ED8000
; 0x0110 = output location
; 0x0114 = 0xBF800000 (-1.0)
;
; Expected maximum = 0x53ED8000
;
; Concrete control-flow addresses:
; loop = 0x0024
; next = 0x0044
; done = 0x0050

LOADD r0, #0x0104
LOADD r1, #0x0003
LOADD r6, #0x0000
LOADD r7, #0x0110
LOADD r8, #0x0114

FL r4, r8

FL r2, r0
ADD r0, r0, #0x0004
SUB r1, r1, #0x0001

CMP r1, r6
BZ #0x0050

FL r3, r0
MOV r5, r3
FMA r5, r2, r4

CMP r5, r6
BN #0x0044

MOV r2, r3

ADD r0, r0, #0x0004
SUB r1, r1, #0x0001
JMP #0x0024

FS r2, r7
HALT
