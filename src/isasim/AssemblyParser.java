package isasim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AssemblyParser {
    private AssemblyParser() {}

    public static Program parse(Path path) throws SimulatorException {
        if (!Files.isRegularFile(path)) {
            throw new SimulatorException("Assembly file not found: " + path);
        }

        try {
            List<String> lines = Files.readAllLines(path);
            Map<Integer, Instruction> instructions = new LinkedHashMap<>();
            int address = 0;

            for (int i = 0; i < lines.size(); i++) {
                String line = stripComment(lines.get(i)).trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] firstSplit = line.split("\\s+", 2);
                String mnemonic = firstSplit[0].toUpperCase(Locale.ROOT);
                List<String> operands = new ArrayList<>();

                if (firstSplit.length == 2) {
                    for (String operand : firstSplit[1].split(",")) {
                        String cleaned = operand.trim();
                        if (!cleaned.isEmpty()) {
                            operands.add(cleaned);
                        }
                    }
                }

                instructions.put(address, new Instruction(address, mnemonic, List.copyOf(operands), i + 1));
                address += 4;
            }

            if (instructions.isEmpty()) {
                throw new SimulatorException("Assembly file contains no instructions: " + path);
            }

            return new Program(instructions);
        } catch (IOException e) {
            throw new SimulatorException("Could not read assembly file: " + path, e);
        }
    }

    private static String stripComment(String line) {
        int semicolon = line.indexOf(';');
        int slash = line.indexOf("//");
        int cut = line.length();

        if (semicolon >= 0) {
            cut = Math.min(cut, semicolon);
        }
        if (slash >= 0) {
            cut = Math.min(cut, slash);
        }
        return line.substring(0, cut);
    }
}
