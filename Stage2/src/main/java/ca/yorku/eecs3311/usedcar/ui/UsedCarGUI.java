package ca.yorku.eecs3311.usedcar.ui;

import ca.yorku.eecs3311.usedcar.app.UsedCarApplicationFacade;
import ca.yorku.eecs3311.usedcar.command.Command;
import ca.yorku.eecs3311.usedcar.result.PreparationResult;
import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Minimal Swing boundary. Only local, fast deterministic work runs on the EDT. */
public final class UsedCarGUI {
    private final UsedCarApplicationFacade facade;
    private final Map<String, JTextField> fields = new LinkedHashMap<>();
    private final JTextArea output = new JTextArea(13, 85);
    private final JLabel message = new JLabel("Enter your data or explicitly load the fictional example.");
    private final JPanel panel = new JPanel(new BorderLayout(8, 8));

    public UsedCarGUI(UsedCarApplicationFacade facade) {
        this.facade = facade;
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(new JLabel("AI Used-Car Evaluation Assistant | Initial deterministic foundation | CAD"), BorderLayout.NORTH);
        var tabs = new JTabbedPane();
        for (String section : new String[]{"Vehicle", "Preferences", "Cost assumptions"}) {
            var form = new JPanel(new GridLayout(0, 2, 8, 5));
            for (var field : PreparationForm.FIELDS) {
                if (!field.section().equals(section)) continue;
                JTextField input = new JTextField(24);
                input.setName(field.key());
                input.getAccessibleContext().setAccessibleName(field.label());
                fields.put(field.key(), input);
                JLabel label = new JLabel(field.label()); label.setLabelFor(input);
                form.add(label); form.add(input);
            }
            var holder = new JPanel(new BorderLayout());
            holder.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            holder.add(form, BorderLayout.NORTH);
            if (section.equals("Preferences")) {
                var profileButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
                var save = new JButton("Save preferences"); save.setName("profileSave");
                save.addActionListener(event -> {
                    var values = new LinkedHashMap<String, String>();
                    PreparationForm.FIELDS.stream().filter(f -> f.section().equals("Preferences"))
                            .forEach(f -> values.put(f.key(), fields.get(f.key()).getText()));
                    try {
                        facade.savePreferences(PreparationForm.preferences(values));
                        output.setText(ResultFormatter.format(facade.preferences())); output.setCaretPosition(0);
                        message.setText("Preferences saved independently; current vehicle preserved.");
                    } catch (IllegalArgumentException e) { displayError(e.getMessage()); }
                });
                var load = new JButton("Load saved preferences"); load.setName("profileLoad");
                load.addActionListener(event -> {
                    PreparationForm.preferenceValues(facade.preferences()).forEach((key, value) -> fields.get(key).setText(value));
                    output.setText(ResultFormatter.format(facade.preferences())); output.setCaretPosition(0);
                    message.setText("Saved preferences loaded; unsaved preference edits discarded.");
                });
                profileButtons.add(save); profileButtons.add(load);
                var mode = new JComboBox<String>(); mode.setName("strategyMode");
                mode.addItem("Select a mode"); facade.evaluationModes().forEach(mode::addItem);
                var preview = new JButton("Preview priority guidance"); preview.setName("strategyPreview");
                preview.addActionListener(event -> {
                    try {
                        facade.selectStrategy((String) mode.getSelectedItem());
                        output.setText(facade.previewPriorityGuidance()); output.setCaretPosition(0);
                        message.setText("Guidance uses saved preferences. Save preference edits before previewing. No AI assessment generated.");
                    } catch (IllegalArgumentException | IllegalStateException e) { displayError(e.getMessage()); }
                });
                var strategyControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
                strategyControls.add(new JLabel("Evaluation priority:")); strategyControls.add(mode); strategyControls.add(preview);
                var actions = new JPanel(new GridLayout(0, 1)); actions.add(profileButtons); actions.add(strategyControls);
                holder.add(actions, BorderLayout.SOUTH);
            }
            tabs.addTab(section, new JScrollPane(holder));
        }
        PreparationForm.preferenceValues(facade.preferences()).forEach((key, value) -> fields.get(key).setText(value));
        output.setName("results"); output.setEditable(false); output.setLineWrap(true); output.setWrapStyleWord(true);
        output.setText("Fill the Vehicle, Preferences and Cost assumptions tabs, then select Save and calculate.\n"
                + "Optional values may remain blank. No AI risk assessment is generated.\n" + facade.llmStatus());
        var split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabs, new JScrollPane(output));
        split.setResizeWeight(0.55); panel.add(split, BorderLayout.CENTER);
        var buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        var example = new JButton("Load fictional example"); example.setName("example");
        example.addActionListener(event -> {
            PreparationForm.example().forEach((key, value) -> fields.get(key).setText(value));
            message.setText("Fictional example loaded. Review all tabs; nothing saved yet.");
            output.setText("Example input loaded but not calculated. Click Save and calculate for fresh results.");
        });
        var calculate = new JButton("Save and calculate"); calculate.setName("calculate");
        calculate.addActionListener(event -> calculate());
        var conflicts = new JButton("Check against saved vehicle"); conflicts.setName("conflicts");
        conflicts.setToolTipText("Use the Vehicle tab as a second structured record; does not save or extract listing text.");
        conflicts.addActionListener(event -> {
            var values = new LinkedHashMap<String, String>();
            fields.forEach((key, field) -> values.put(key, field.getText()));
            try {
                output.setText(ResultFormatter.format(facade.checkConflict(PreparationForm.vehicle(facade, values))));
                output.setCaretPosition(0);
                message.setText("Compared saved manual record with Vehicle tab values. Neither source was changed.");
            } catch (IllegalArgumentException | IllegalStateException e) { displayError(e.getMessage()); }
        });
        var knowledge = new JButton("View local knowledge"); knowledge.setName("knowledge");
        knowledge.addActionListener(event -> {
            try {
                output.setText(ResultFormatter.format(facade.retrieveKnowledge())); output.setCaretPosition(0);
                message.setText("Local knowledge for the saved vehicle; unsaved form edits are not used.");
            } catch (IllegalArgumentException | IllegalStateException e) {
                output.setText("ERROR: " + e.getMessage());
                message.setText("Knowledge retrieval failed; saved session preserved.");
            }
        });
        buttons.add(example); buttons.add(calculate); buttons.add(conflicts); buttons.add(knowledge);
        var bottom = new JPanel(new BorderLayout()); bottom.add(buttons, BorderLayout.NORTH); bottom.add(message, BorderLayout.SOUTH);
        panel.add(bottom, BorderLayout.SOUTH);
    }

    private void calculate() {
        var values = new LinkedHashMap<String, String>();
        fields.forEach((key, field) -> values.put(key, field.getText()));
        try {
            displayResult(submit(PreparationForm.command(facade, values)));
            message.setText("Saved to this session and calculated from the submitted values.");
        } catch (IllegalArgumentException | IllegalStateException e) { displayError(e.getMessage()); }
    }

    public <R> R submit(Command<R> command) { return command.execute(); }
    public void displayResult(PreparationResult result) { output.setText(ResultFormatter.format(result)); output.setCaretPosition(0); }
    public void displayError(String text) {
        message.setText("Input rejected; previous saved session preserved.");
        output.setText("ERROR: " + text + "\nCorrect the input and try again. No new results were saved.");
    }
    public JPanel panel() { return panel; }
    public JFrame show() {
        JFrame frame = new JFrame("AI Used-Car Evaluation Assistant - Stage 2 Foundation");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setContentPane(panel); frame.setSize(1000, 850); frame.setMinimumSize(new Dimension(760, 600));
        frame.setLocationByPlatform(true); frame.setVisible(true);
        return frame;
    }
}
