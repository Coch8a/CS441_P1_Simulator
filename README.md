# Run Instructions

# Run Instructions

Open the project folder in VS Code and open a Git Bash terminal.

Make sure the benchmark files are in the root project folder:

```text
MAXFINDER_FP.asm
MAXFINDER_FP.mem

Make the scripts executable:
chmod +x build.sh isaSim

Build the simulator:
./build.sh

Run the simulator in run through mode:
./isaSim -r MAXFINDER_FP.asm MAXFINDER_FP.mem

After the simulator finishes, it will create:
MAXFINDER_FP.instrCount
MAXFINDER_FP.mem.out

View the instruction counts:
cat MAXFINDER_FP.instrCount

View the final memory output:
cat MAXFINDER_FP.mem.out

For the current floating point MAXFINDER benchmark, the output memory file should contain:
0110,53ED8000


## Onyx

From the project directory:

```bash
chmod +x build.sh isaSim
./build.sh
./isaSim -r <assembly file> <memory file>
```

Example:

```bash
./isaSim -r MAXFINDER_FP.asm MAXFINDER_FP.mem
```

The simulator writes `<benchmarkName>.instrCount` and `<benchmarkName>.mem.out` in the current directory.

Step by step mode is reserved for later implementation:

```bash
./isaSim -s <assembly file> <memory file>
```
