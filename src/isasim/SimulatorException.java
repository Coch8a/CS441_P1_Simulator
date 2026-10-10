package isasim;

public final class SimulatorException extends Exception {
    public SimulatorException(String message) {
        super(message);
    }

    public SimulatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
