package isasim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public final class Simulator {
    private static final List<String> SUPPORTED_INSTRUCTIONS = List.of(
            "ADD", "SUB", "AND", "OR", "XOR", "LSL", "LSR",
            "LOAD", "STORE", "LOADD", "MOV",
            "FL", "FS", "FMA", "FD",
            "JMP", "BZ", "BN", "CMP",
            "RBYT", "RBIT", "HALT");

    private final Program program;
    private final Memory memory;
    private final Cpu cpu = new Cpu();
    private final Path assemblyFile;
    private final Map<String, Long> instructionCounts = new LinkedHashMap<>();
    private long totalInstructions;

    private Simulator(Program program, Memory memory, Path assemblyFile) {
        this.program = program;
        this.memory = memory;
        this.assemblyFile = assemblyFile;
        for (String mnemonic : SUPPORTED_INSTRUCTIONS) {
            instructionCounts.put(mnemonic, 0L);
        }
    }

    public static Simulator load(Path assemblyFile, Path memoryFile) throws SimulatorException {
        return new Simulator(
                AssemblyParser.parse(assemblyFile),
                MemoryLoader.load(memoryFile),
                assemblyFile);
    }

    public void runThrough() throws SimulatorException {
        while (!cpu.isHalted()) {
            executeOne();
        }
        printInstructionCounts();
        writeOutputs();
    }

    /**
     * Runs the simulator in step-by-step mode, allowing the user to execute one instruction at a time or specify a number of steps to execute.
     * @throws SimulatorException
     */
    public void stepThrough() throws SimulatorException {
        Scanner scanner = new Scanner(System.in);
        int stepsToRun = 0;
        while (!cpu.isHalted()) {
            displayState();
            if (stepsToRun <= 0) {
                System.out.print("Press enter to execute next instruction (or enter step count number): ");
                String input = scanner.nextLine().trim();
                if (!input.isEmpty()) {
                    try {
                        stepsToRun = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number, stepping 1 instruction.");
                        stepsToRun = 1;
                    }
                } else { 
                    stepsToRun = 1;
                }
            }
            executeOne();
            stepsToRun--;
        }
        System.out.println("\n--- Program Halted ---");
        displayState();
        printInstructionCounts();
        writeOutputs();
    }

    /**
     * Displays the current state of the CPU, including the program counter, executed instruction count, comparison value, and register values.
     * @throws SimulatorException
     */
    private void displayState() throws SimulatorException {
        System.out.println("--------------------------------------------------");
        System.out.printf("PC: 0x%08X  |  Executed Instructions: %d  |  Comparison: %d%n", cpu.getPc(), totalInstructions, cpu.getComparison());
        System.out.println("--------------------------------------------------");
        System.out.println("Registers:");
        for (int i = 0; i < 16; i += 4) {
            System.out.printf("  r%-2d: 0x%08X (%-10d) | r%-2d: 0x%08X (%-10d) | r%-2d: 0x%08X (%-10d) | r%-2d: 0x%08X (%-10d)%n",
                    i,     cpu.getRegister(i),     cpu.getRegister(i),
                    i + 1, cpu.getRegister(i + 1), cpu.getRegister(i + 1),
                    i + 2, cpu.getRegister(i + 2), cpu.getRegister(i + 2),
                    i + 3, cpu.getRegister(i + 3), cpu.getRegister(i + 3));
        }
        System.out.println("--------------------------------------------------");
    }

    public void executeOne() throws SimulatorException {
        Instruction instruction = program.instructionAt(cpu.getPc());
        if (instruction == null) {
            throw new SimulatorException(String.format("No instruction at PC 0x%08X", cpu.getPc()));
        }

        String mnemonic = instruction.mnemonic();
        if (!instructionCounts.containsKey(mnemonic)) {
            throw error(instruction, "Unknown instruction: " + mnemonic);
        }

        int nextPc = cpu.getPc() + 4;

        switch (mnemonic) {
            case "ADD" -> executeBinaryAlu(instruction, AluOp.ADD);
            case "SUB" -> executeBinaryAlu(instruction, AluOp.SUB);
            case "AND" -> executeBinaryAlu(instruction, AluOp.AND);
            case "OR" -> executeBinaryAlu(instruction, AluOp.OR);
            case "XOR" -> executeBinaryAlu(instruction, AluOp.XOR);
            case "LSL" -> executeShift(instruction, true);
            case "LSR" -> executeShift(instruction, false);
            case "LOAD" -> executeLoad(instruction);
            case "STORE" -> executeStore(instruction);
            case "LOADD" -> executeLoadDirect(instruction);
            case "MOV" -> executeMove(instruction);
            case "FL" -> executeFloatLoad(instruction);
            case "FS" -> executeFloatStore(instruction);
            case "FMA" -> executeFma(instruction);
            case "FD" -> executeFloatDivide(instruction);
            case "JMP" -> nextPc = branchTarget(instruction);
            case "BZ" -> {
                requireOperands(instruction, 1);
                if (cpu.getComparison() == 0) {
                    nextPc = branchTarget(instruction);
                }
            }
            case "BN" -> {
                requireOperands(instruction, 1);
                if (cpu.getComparison() < 0) {
                    nextPc = branchTarget(instruction);
                }
            }
            case "CMP" -> executeCompare(instruction);
            case "RBYT" -> executeReverseBytes(instruction);
            case "RBIT" -> executeReverseBits(instruction);
            case "HALT" -> {
                requireOperands(instruction, 0);
                cpu.halt();
            }
            default -> throw error(instruction, "Unsupported instruction: " + mnemonic);
        }

        instructionCounts.merge(mnemonic, 1L, Long::sum);
        totalInstructions++;

        if (!cpu.isHalted()) {
            cpu.setPc(nextPc);
        }
    }

    private void executeBinaryAlu(Instruction instruction, AluOp op) throws SimulatorException {
        requireOperands(instruction, 3);
        int destination = parseRegister(instruction, 0);
        int left = cpu.getRegister(parseRegister(instruction, 1));
        int right = readRegisterOrImmediate(instruction, 2);
        int result = switch (op) {
            case ADD -> left + right;
            case SUB -> left - right;
            case AND -> left & right;
            case OR -> left | right;
            case XOR -> left ^ right;
        };
        cpu.setRegister(destination, result);
    }

    private void executeShift(Instruction instruction, boolean leftShift) throws SimulatorException {
        requireOperands(instruction, 3);
        int destination = parseRegister(instruction, 0);
        int value = cpu.getRegister(parseRegister(instruction, 1));
        int amount = readRegisterOrImmediate(instruction, 2) & 0x1F;
        cpu.setRegister(destination, leftShift ? value << amount : value >>> amount);
    }

    private void executeLoad(Instruction instruction) throws SimulatorException {
        requireOperands(instruction, 2);
        int destination = parseRegister(instruction, 0);
        int address = cpu.getRegister(parseRegister(instruction, 1));
        cpu.setRegister(destination, memory.readWord(address));
    }

    private void executeStore(Instruction instruction) throws SimulatorException {
        requireOperands(instruction, 2);
        int source = parseRegister(instruction, 0);
        int address = cpu.getRegister(parseRegister(instruction, 1));
        memory.writeWord(address, cpu.getRegister(source));
    }

    private void executeLoadDirect(Instruction instruction) throws SimulatorException {
        requireOperands(instruction, 2);
        int destination = parseRegister(instruction, 0);
        cpu.setRegister(destination, parseImmediate(instruction.operands().get(1), instruction));
    }

    private void executeMove(Instruction instruction) throws SimulatorException {
        requireOperands(instruction, 2);
        int destination = parseRegister(instruction, 0);
        int source = parseRegister(instruction, 1);
        cpu.setRegister(destination, cpu.getRegister(source));
    }

    private void executeFloatLoad(Instruction instruction) throws SimulatorException {
        executeLoad(instruction);
    }

    private void executeFloatStore(Instruction instruction) throws SimulatorException {
        executeStore(instruction);
    }

    private void executeFma(Instruction instruction) throws SimulatorException {
        requireOperands(instruction, 3);
        int destination = parseRegister(instruction, 0);
        int source1 = parseRegister(instruction, 1);
        int source2 = parseRegister(instruction, 2);

        float accumulator = Float.intBitsToFloat(cpu.getRegister(destination));
        float left = Float.intBitsToFloat(cpu.getRegister(source1));
        float right = Float.intBitsToFloat(cpu.getRegister(source2));
        float result = left * right + accumulator;
        cpu.setRegister(destination, Float.floatToRawIntBits(result));
    }

    private void executeFloatDivide(Instruction instruction) throws SimulatorException {
        requireOperands(instruction, 3);
        int destination = parseRegister(instruction, 0);
        float numerator = Float.intBitsToFloat(cpu.getRegister(parseRegister(instruction, 1)));
        float denominator = Float.intBitsToFloat(cpu.getRegister(parseRegister(instruction, 2)));
        cpu.setRegister(destination, Float.floatToRawIntBits(numerator / denominator));
    }

    private void executeCompare(Instruction instruction) throws SimulatorException {
        requireOperands(instruction, 2);
        int left = cpu.getRegister(parseRegister(instruction, 0));
        int right = cpu.getRegister(parseRegister(instruction, 1));
        cpu.setComparison(left - right);
    }

    private void executeReverseBytes(Instruction instruction) throws SimulatorException {
        requireOperands(instruction, 2);
        int destination = parseRegister(instruction, 0);
        int source = cpu.getRegister(parseRegister(instruction, 1));
        cpu.setRegister(destination, Integer.reverseBytes(source));
    }

    private void executeReverseBits(Instruction instruction) throws SimulatorException {
        requireOperands(instruction, 2);
        int destination = parseRegister(instruction, 0);
        int source = cpu.getRegister(parseRegister(instruction, 1));
        cpu.setRegister(destination, Integer.reverse(source));
    }

    private int branchTarget(Instruction instruction) throws SimulatorException {
        requireOperands(instruction, 1);
        int target = parseImmediate(instruction.operands().get(0), instruction);
        if ((target & 0x3) != 0) {
            throw error(instruction, "Control-flow target must be 4-byte aligned");
        }
        if (target < 0 || Integer.toUnsignedLong(target) > 0x0FFFFFFFL) {
            throw error(instruction, "Control-flow target exceeds 28-bit address space");
        }
        if (program.instructionAt(target) == null) {
            throw error(instruction, String.format("No instruction at target 0x%X", target));
        }
        return target;
    }

    private int readRegisterOrImmediate(Instruction instruction, int operandIndex) throws SimulatorException {
        String token = instruction.operands().get(operandIndex).trim();
        if (token.toLowerCase(Locale.ROOT).startsWith("r")) {
            return cpu.getRegister(parseRegisterToken(token, instruction));
        }
        return parseImmediate(token, instruction);
    }

    private int parseRegister(Instruction instruction, int operandIndex) throws SimulatorException {
        return parseRegisterToken(instruction.operands().get(operandIndex), instruction);
    }

    private int parseRegisterToken(String token, Instruction instruction) throws SimulatorException {
        String normalized = token.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("r\\d+")) {
            throw error(instruction, "Expected register but found: " + token);
        }
        try {
            int index = Integer.parseInt(normalized.substring(1));
            if (index < 0 || index > 15) {
                throw error(instruction, "Only r0 through r15 are directly addressable");
            }
            return index;
        } catch (NumberFormatException e) {
            throw error(instruction, "Invalid register: " + token);
        }
    }

    private int parseImmediate(String token, Instruction instruction) throws SimulatorException {
        String value = token.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }

        try {
            if (value.startsWith("0x") || value.startsWith("0X")) {
                long parsed = Long.parseUnsignedLong(value.substring(2), 16);
                if (parsed > 0xFFFFFFFFL) {
                    throw error(instruction, "Immediate exceeds 32 bits: " + token);
                }
                return (int) parsed;
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw error(instruction, "Invalid immediate: " + token);
        }
    }

    private static void requireOperands(Instruction instruction, int expected) throws SimulatorException {
        if (instruction.operands().size() != expected) {
            throw error(instruction,
                    instruction.mnemonic() + " expects " + expected + " operand(s), found " + instruction.operands().size());
        }
    }

    private void printInstructionCounts() {
        System.out.println("Instruction Counts");
        for (Map.Entry<String, Long> entry : instructionCounts.entrySet()) {
            System.out.printf("%s: %d%n", entry.getKey(), entry.getValue());
        }
        System.out.println("Total: " + totalInstructions);
    }

    private void writeOutputs() throws SimulatorException {
        String benchmarkName = benchmarkName(assemblyFile);
        Path countPath = Path.of(benchmarkName + ".instrCount");
        Path memoryPath = Path.of(benchmarkName + ".mem.out");

        try {
            StringBuilder counts = new StringBuilder();
            for (Map.Entry<String, Long> entry : instructionCounts.entrySet()) {
                counts.append(entry.getKey()).append(": ").append(entry.getValue()).append(System.lineSeparator());
            }
            counts.append("Total: ").append(totalInstructions).append(System.lineSeparator());
            Files.writeString(countPath, counts.toString());

            StringBuilder memoryOutput = new StringBuilder();
            memoryOutput.append(memory.comment()).append(System.lineSeparator());
            memoryOutput.append(memory.addressBits()).append(',').append(memory.wordBits()).append(System.lineSeparator());
            for (Map.Entry<Integer, Integer> entry : memory.wordsForOutput().entrySet()) {
                memoryOutput.append(String.format(
                        "%0" + memory.addressHexWidth() + "X,%0" + memory.valueHexWidth() + "X%n",
                        entry.getKey(), entry.getValue()));
            }
            Files.writeString(memoryPath, memoryOutput.toString());

            System.out.println("Wrote " + countPath);
            System.out.println("Wrote " + memoryPath);
        } catch (IOException e) {
            throw new SimulatorException("Could not write output files", e);
        }
    }

    private static String benchmarkName(Path assemblyFile) {
        String filename = assemblyFile.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private static SimulatorException error(Instruction instruction, String message) {
        return new SimulatorException("Assembly line " + instruction.sourceLine() + ": " + message);
    }

    private enum AluOp {
        ADD, SUB, AND, OR, XOR
    }
}
