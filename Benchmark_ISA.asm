; Benchmark_ISA.asm
; Testing benchmark that executes every defined ISA instruction

LOADD r0, #0x00F0
LOADD r1, #0x0002
LSR r2, r1, #0x0005
SUB r4, r1, #0x0003
OR r5, r2, #0xFFAD
AND r6, r5, #0xDEAD
XOR r7, r6, r4
LSL r8, r6, #0x0010

STORE r4, r0
ADD r0, r0, #0x0004
STORE r5, r0
ADD r0, r0, #0x0004
STORE r6, r0
ADD r0, r0, #0x0004
STORE r7, r0
ADD r0, r0, #0x0004
STORE r8, r0

LOADD r3, #0x00F0
LOAD r7, r3
MOV r9, r7

LOADD r10, #0x0104
FL r2, r10
ADD r10, r10, #0x0004
FL r3, r10
ADD r10, r10, #0x0004
FL r4, r10
FMA r2, r3, r4
FD r5, r2, r4

LOADD r11, #0x0120
FS r2, r11
ADD r11, r11, #0x0004
FS r5, r11

CMP r6, r6
BZ #0x008C
ADD r1, r1, #0x0001

CMP r9, r6
BN #0x0098
ADD r1, r1, #0x0001

JMP #0x00A0
ADD r1, r1, #0x0001

RBYT r8, r7
RBIT r9, r7
ADD r0, r0, #0x0004
STORE r8, r0
ADD r0, r0, #0x0004
STORE r9, r0
HALT
