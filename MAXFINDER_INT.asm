; MAXFINDER_INT.asm
; Integer MAXFINDER for the team's custom 32-bit ISA
;
; Register usage:
; r0 = pointer to current array element
; r1 = number of elements remaining
; r2 = current maximum
; r3 = current candidate
; r6 = integer zero
; r7 = output address

;
; Data memory:
; 0x0104 = 0x67676767
; 0x0108 = 0x00110011
; 0x010C = 0x04FFFFFF
; 0x0110 = output location
;
; Expected maximum = 0x67676767
;
; Concrete control-flow addresses:
; loop = 0x001C
; next = 0x0034
; done = 0x0040

LOADD r0, #0x0104
LOADD r1, #0x0003
LOADD r6, #0x0000
LOADD r7, #0x0110

; initialization
LOAD r2, r0
ADD r0, r0, #0x0004
SUB r1, r1, #0x0001

; loop (0x001C)
CMP r1, r6
BZ #0x0040

LOAD r3, r0
CMP r3, r2
BN #0x0034 ; (branch negative)

MOV r2, r3 ; update if biggest so far

; next (0x0034)
ADD r0, r0, #0x0004
SUB r1, r1, #0x0001
JMP #0x001C

; done (0x0040)
STORE r2, r7
HALT
