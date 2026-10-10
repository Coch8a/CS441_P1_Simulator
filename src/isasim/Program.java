package isasim;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Program {
    private final Map<Integer, Instruction> instructions;

    public Program(Map<Integer, Instruction> instructions) {
        this.instructions = Collections.unmodifiableMap(new LinkedHashMap<>(instructions));
    }

    public Instruction instructionAt(int address) {
        return instructions.get(address);
    }
}
