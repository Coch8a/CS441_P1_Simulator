package isasim;

import java.nio.file.Path;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        if (args.length != 3 || !(args[0].equals("-r") || args[0].equals("-s"))) {
            printUsage();
            System.exit(1);
        }

        Path assemblyFile = Path.of(args[1]);
        Path memoryFile = Path.of(args[2]);

        try {
            Simulator simulator = Simulator.load(assemblyFile, memoryFile);

            if (args[0].equals("-r")) {
                simulator.runThrough();
                return;
            }

            if (args[0].equals("-s")) {
                simulator.stepThrough();
                return;
            }
            
            System.exit(2);
        } catch (SimulatorException e) {
            System.err.println("Simulator error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.err.println("Usage: isaSim -<r|s> <assembly file> <memory file>");
    }
}
