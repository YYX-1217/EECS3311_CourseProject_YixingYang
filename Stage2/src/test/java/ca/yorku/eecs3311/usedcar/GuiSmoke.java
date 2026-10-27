package ca.yorku.eecs3311.usedcar;

import ca.yorku.eecs3311.usedcar.app.ApplicationFactory;
import ca.yorku.eecs3311.usedcar.ui.UsedCarGUI;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;

/** Exercises actual Swing button listeners on the EDT; never captures unrelated desktop content. */
public final class GuiSmoke {
    private static Component named(Container parent, String name) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) return child;
            if (child instanceof Container container) {
                Component found = named(container, name); if (found != null) return found;
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) throw new IllegalStateException("GUI smoke test requires a desktop display.");
        Path destination = Path.of("build", "verification"); Files.createDirectories(destination);
        SwingUtilities.invokeAndWait(() -> {
            var facade = ApplicationFactory.create(); var gui = new UsedCarGUI(facade); JFrame window = gui.show();
            try {
                FoundationChecks.check(window.isShowing(), "Swing window did not launch.");
                ((JTextField) named(gui.panel(), "budget")).setText("25000");
                ((JTextField) named(gui.panel(), "preferredBodyType")).setText("SUV");
                ((JTextField) named(gui.panel(), "reliabilityPriority")).setText("5");
                ((JButton) named(gui.panel(), "profileSave")).doClick();
                FoundationChecks.check(facade.currentVehicle().isEmpty() && facade.preferences().reliabilityPriority() == 5
                        && facade.preferences().preferredBodyType().equals("SUV"), "GUI must save preferences without vehicle entry.");
                ((JTextField) named(gui.panel(), "reliabilityPriority")).setText("6");
                ((JButton) named(gui.panel(), "profileSave")).doClick();
                FoundationChecks.check(((JTextArea) named(gui.panel(), "results")).getText().startsWith("ERROR:")
                        && facade.preferences().reliabilityPriority() == 5, "Rejected GUI profile edit must preserve prior values.");
                ((JButton) named(gui.panel(), "profileLoad")).doClick();
                FoundationChecks.check(((JTextField) named(gui.panel(), "reliabilityPriority")).getText().equals("5"), "GUI must reload saved preferences.");
                ((JButton) named(gui.panel(), "example")).doClick();
                ((JButton) named(gui.panel(), "calculate")).doClick();
                var results = (JTextArea) named(gui.panel(), "results");
                FoundationChecks.check(results.getText().contains("24922.00"), "GUI calculation mismatch.");
                ((JTextField) named(gui.panel(), "price")).setText("19000");
                ((JButton) named(gui.panel(), "conflicts")).doClick();
                FoundationChecks.check(results.getText().contains("Asking price | manual=18000 | extracted=19000")
                        && results.getText().contains("Missing fields:") && results.getText().contains("Uncertain fields:")
                        && results.getText().contains("Sufficient essential data: false"), "GUI must display source conflicts and data quality.");
                FoundationChecks.check(facade.currentVehicle().orElseThrow().askingPrice().intValueExact() == 18000, "GUI comparison must preserve the saved record.");
                System.out.println("PASS: F04 GUI source comparison and session preservation.");
                ((JButton) named(gui.panel(), "knowledge")).doClick();
                FoundationChecks.check(results.getText().contains("Fictional sedan scenario")
                        && results.getText().contains("Instructional sample only"), "GUI must display knowledge for the saved vehicle.");
                var image = new BufferedImage(gui.panel().getWidth(), gui.panel().getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();
                try { gui.panel().printAll(graphics); } finally { graphics.dispose(); }
                ImageIO.write(image, "png", destination.resolve("gui.png").toFile());
                ((JTextField) named(gui.panel(), "price")).setText("-1");
                ((JButton) named(gui.panel(), "calculate")).doClick();
                FoundationChecks.check(results.getText().startsWith("ERROR:"), "GUI must show validation errors.");
                FoundationChecks.check(facade.currentVehicle().orElseThrow().askingPrice().intValueExact() == 18000, "GUI error corrupted session.");
                ((JTextField) named(gui.panel(), "price")).setText("16000");
                ((JButton) named(gui.panel(), "calculate")).doClick();
                FoundationChecks.check(results.getText().contains("22662.00"), "Edited input must change real computed total.");
                ((JTextField) named(gui.panel(), "acceptedTransmissions")).setText("manual");
                ((JTextField) named(gui.panel(), "accidentTolerance")).setText("NO_ACCIDENTS");
                ((JTextField) named(gui.panel(), "accident")).setText("YES");
                ((JButton) named(gui.panel(), "calculate")).doClick();
                FoundationChecks.check(results.getText().contains("CONFLICT: Transmission")
                        && results.getText().contains("CONFLICT: Accident history"), "GUI must invoke both new checks.");
                ((JTextField) named(gui.panel(), "transmission")).setText("");
                ((JTextField) named(gui.panel(), "accident")).setText("");
                ((JButton) named(gui.panel(), "calculate")).doClick();
                FoundationChecks.check(results.getText().contains("UNKNOWN: Transmission")
                        && results.getText().contains("UNKNOWN: Accident history"), "GUI must distinguish unknown data from conflicts.");
                ((JTextField) named(gui.panel(), "model")).setText("Unlisted model");
                ((JButton) named(gui.panel(), "calculate")).doClick();
                ((JButton) named(gui.panel(), "knowledge")).doClick();
                FoundationChecks.check(results.getText().contains("No local vehicle knowledge available."), "GUI must show the no-data result.");
                var savedVehicle = facade.currentVehicle().orElseThrow();
                ((JTextField) named(gui.panel(), "budget")).setText("1");
                ((JButton) named(gui.panel(), "profileSave")).doClick();
                FoundationChecks.check(facade.currentVehicle().orElseThrow().equals(savedVehicle)
                        && facade.checkConstraints(savedVehicle).stream().anyMatch(v -> v.constraint().equals("Maximum budget")), "GUI preference update must retain vehicle and affect subsequent constraints.");
                System.out.println("PASS: F03 independent profile save, rejection, reload and F07 use.");
                System.out.println("PASS: F05 GUI knowledge and no-data results.");
                System.out.println("PASS: F07 GUI conflict and uncertainty checks.");
                System.out.println("PASS: Swing window launch, example calculation, invalid input, session preservation and edited calculation.");
            } catch (Exception e) { throw new RuntimeException(e); }
            finally { window.dispose(); }
        });
    }
}
