package ca.yorku.eecs3311.usedcar.app;

import ca.yorku.eecs3311.usedcar.ui.*;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.Scanner;

public final class Main {
    private Main() { }
    public static void main(String[] args) {
        if (args.length > 1) { usage(); System.exit(2); }
        String mode = args.length == 0 ? "--cli" : args[0];
        if (mode.equals("--help")) { usage(); return; }
        if (!mode.equals("--demo") && !mode.equals("--cli") && !mode.equals("--gui")) {
            System.err.println("Unknown mode: " + mode); usage(); System.exit(2);
        }
        var facade = ApplicationFactory.create();
        if (mode.equals("--gui")) {
            if (GraphicsEnvironment.isHeadless()) {
                System.err.println("GUI needs a desktop display. Use --demo or --cli in this environment.");
                System.exit(2);
            }
            SwingUtilities.invokeLater(() -> new UsedCarGUI(facade).show());
        } else {
            var cli = new UsedCarCLI(facade, new Scanner(System.in), System.out);
            if (mode.equals("--demo")) cli.demo(); else cli.run();
        }
    }
    private static void usage() {
        System.out.println("Usage: java -jar build/used-car-assistant.jar [--demo | --cli | --gui | --help]");
        System.out.println("Stage 2 foundation; no API key required. Default: interactive CLI.");
    }
}
