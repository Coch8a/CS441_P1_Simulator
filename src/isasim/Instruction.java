package isasim;

import java.util.List;

public record Instruction(int address, String mnemonic, List<String> operands, int sourceLine) {}
