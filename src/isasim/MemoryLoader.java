package isasim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class MemoryLoader {
    private MemoryLoader() {}

    public static Memory load(Path path) throws SimulatorException {
        if (!Files.isRegularFile(path)) {
            throw new SimulatorException("Memory file not found: " + path);
        }

        try {
            List<String> lines = Files.readAllLines(path);
            String comment = "// final memory";
            int headerLine = -1;

            for (int i = 0; i < lines.size(); i++) {
                String trimmed = lines.get(i).trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (isComment(trimmed)) {
                    if (headerLine < 0) {
                        comment = trimmed;
                    }
                    continue;
                }
                headerLine = i;
                break;
            }

            if (headerLine < 0) {
                throw new SimulatorException("Memory file has no header: " + path);
            }

            String[] header = lines.get(headerLine).split(",");
            if (header.length != 2) {
                throw new SimulatorException("Invalid memory header at line " + (headerLine + 1));
            }

            int addressBits = Integer.parseInt(header[0].trim());
            int wordBits = Integer.parseInt(header[1].trim());
            if (addressBits <= 0 || addressBits > 30 || wordBits != 32) {
                throw new SimulatorException("Expected address bits <= 30 and 32-bit words");
            }

            Map<Integer, Integer> words = new TreeMap<>();
            Set<Integer> specified = new TreeSet<>();
            int addressWidth = 1;
            int valueWidth = 8;

            for (int i = headerLine + 1; i < lines.size(); i++) {
                String trimmed = lines.get(i).trim();
                if (trimmed.isEmpty() || isComment(trimmed)) {
                    continue;
                }

                String[] entry = trimmed.split(",");
                if (entry.length != 2) {
                    throw new SimulatorException("Invalid memory entry at line " + (i + 1));
                }

                String addressToken = stripHexPrefix(entry[0].trim());
                String valueToken = stripHexPrefix(entry[1].trim());
                int address = parseHexInt(addressToken, i + 1);
                int value = (int) parseUnsignedHex(valueToken, i + 1);

                if ((address & 0x3) != 0) {
                    throw new SimulatorException("Unaligned memory address at line " + (i + 1));
                }
                long maxAddress = (1L << addressBits) - 1L;
                if (Integer.toUnsignedLong(address) > maxAddress) {
                    throw new SimulatorException("Memory address exceeds declared address width at line " + (i + 1));
                }

                words.put(address, value);
                specified.add(address);
                addressWidth = Math.min(Math.max(addressWidth, addressToken.length()), 8);
                valueWidth = Math.max(valueWidth, valueToken.length());
            }

            return new Memory(addressBits, wordBits, addressWidth, valueWidth, comment, words, specified);
        } catch (NumberFormatException e) {
            throw new SimulatorException("Invalid numeric value in memory file: " + path, e);
        } catch (IOException e) {
            throw new SimulatorException("Could not read memory file: " + path, e);
        }
    }

    private static boolean isComment(String line) {
        return line.startsWith("//") || line.startsWith(";") || line.startsWith("#");
    }

    private static String stripHexPrefix(String token) {
        return token.startsWith("0x") || token.startsWith("0X") ? token.substring(2) : token;
    }

    private static int parseHexInt(String token, int line) throws SimulatorException {
        long value = parseUnsignedHex(token, line);
        if (value > 0x7FFFFFFFL) {
            throw new SimulatorException("Address too large at line " + line);
        }
        return (int) value;
    }

    private static long parseUnsignedHex(String token, int line) throws SimulatorException {
        try {
            long value = Long.parseUnsignedLong(token, 16);
            if (Long.compareUnsigned(value, 0xFFFFFFFFL) > 0) {
                throw new SimulatorException("32-bit hex value out of range at line " + line);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new SimulatorException("Invalid hex value at line " + line + ": " + token, e);
        }
    }
}
