package isasim;

public final class Cpu {
    private static final int ADDRESSABLE_REGISTERS = 16;

    private final int[] registers = new int[ADDRESSABLE_REGISTERS];
    private int pc;
    private int comparison;
    private boolean halted;

    public int getRegister(int index) throws SimulatorException {
        checkRegister(index);
        return registers[index];
    }

    public void setRegister(int index, int value) throws SimulatorException {
        checkRegister(index);
        registers[index] = value;
    }

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public int getComparison() {
        return comparison;
    }

    public void setComparison(int comparison) {
        this.comparison = comparison;
    }

    public boolean isHalted() {
        return halted;
    }

    public void halt() {
        halted = true;
    }

    private static void checkRegister(int index) throws SimulatorException {
        if (index < 0 || index >= ADDRESSABLE_REGISTERS) {
            throw new SimulatorException("Register out of range: r" + index);
        }
    }
}
