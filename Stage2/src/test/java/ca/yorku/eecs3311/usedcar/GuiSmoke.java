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
                ((JButton) named(gui.panel(), "example")).doClick();
                ((JButton) named(gui.panel(), "calculate")).doClick();
                var results = (JTextArea) named(gui.panel(), "results");
                FoundationChecks.check(results.getText().contains("24922.00"), "GUI calculation mismatch.");
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
                System.out.println("PASS: Swing window launch, example calculation, invalid input, session preservation and edited calculation.");
            } catch (Exception e) { throw new RuntimeException(e); }
            finally { window.dispose(); }
        });
    }
}
