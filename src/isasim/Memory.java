package isasim;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class Memory {
    private final int addressBits;
    private final int wordBits;
    private final int addressHexWidth;
    private final int valueHexWidth;
    private final String comment;
    private final Map<Integer, Integer> words;
    private final Set<Integer> outputAddresses;

    public Memory(
            int addressBits,
            int wordBits,
            int addressHexWidth,
            int valueHexWidth,
            String comment,
            Map<Integer, Integer> words,
            Set<Integer> specifiedAddresses) {
        this.addressBits = addressBits;
        this.wordBits = wordBits;
        this.addressHexWidth = addressHexWidth;
        this.valueHexWidth = valueHexWidth;
        this.comment = comment;
        this.words = new TreeMap<>(words);
        this.outputAddresses = new TreeSet<>(specifiedAddresses);
    }

    public int readWord(int address) throws SimulatorException {
        validateWordAddress(address);
        return words.getOrDefault(address, 0);
    }

    public void writeWord(int address, int value) throws SimulatorException {
        validateWordAddress(address);
        words.put(address, value);
        outputAddresses.add(address);
    }

    public int addressBits() {
        return addressBits;
    }

    public int wordBits() {
        return wordBits;
    }

    public int addressHexWidth() {
        return addressHexWidth;
    }

    public int valueHexWidth() {
        return valueHexWidth;
    }

    public String comment() {
        return comment;
    }

    public Map<Integer, Integer> wordsForOutput() {
        Map<Integer, Integer> result = new TreeMap<>();
        for (int address : outputAddresses) {
            result.put(address, words.getOrDefault(address, 0));
        }
        return result;
    }

    private void validateWordAddress(int address) throws SimulatorException {
        long unsigned = Integer.toUnsignedLong(address);
        long maxAddress = (1L << addressBits) - 1L;
        if (unsigned > maxAddress) {
            throw new SimulatorException(String.format("Memory address out of range: 0x%X", unsigned));
        }
        if ((address & 0x3) != 0) {
            throw new SimulatorException(String.format("Unaligned 32-bit memory access: 0x%X", unsigned));
        }
    }
}
