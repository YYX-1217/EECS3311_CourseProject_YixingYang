package ca.yorku.eecs3311.usedcar.ui;

import ca.yorku.eecs3311.usedcar.app.UsedCarApplicationFacade;
import ca.yorku.eecs3311.usedcar.command.Command;
import ca.yorku.eecs3311.usedcar.result.PreparationResult;
import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Workspace presentation boundary. Existing commands and background feature workers retain behavior. */
public final class UsedCarGUI {
    private final UsedCarApplicationFacade facade;
    private final Map<String, JTextField> fields = new LinkedHashMap<>();
    private final JTextArea output = new JTextArea(13, 85);
    private final JLabel message = new WorkspaceUI.StatusLabel("Enter your data or explicitly load the fictional example.");
    private final JPanel panel = new JPanel(new BorderLayout(8, 8));

    private final Map<String, javax.swing.text.Document> resultDocuments = new LinkedHashMap<>();
    private java.util.function.Consumer<String> navigate;
    private String selectedFeature = "F01";

    public UsedCarGUI(UsedCarApplicationFacade facade) {
        this.facade = facade;
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        var heading = new JPanel(new BorderLayout(8, 8));
        var title = new WorkspaceUI.StatusLabel("AI Used-Car Evaluation Assistant"); title.setFont(WorkspaceUI.FONT.deriveFont(Font.BOLD, 23));
        heading.add(title, BorderLayout.CENTER);
        var currency = new JLabel("EECS3311  /  STAGE 2  /  CAD"); currency.setForeground(WorkspaceUI.MUTED);
        heading.add(currency, BorderLayout.SOUTH); heading.setBorder(BorderFactory.createEmptyBorder(0, 4, 16, 4));
        panel.add(heading, BorderLayout.NORTH);
        var forms = new LinkedHashMap<String, JPanel>();
        for (String section : new String[]{"Vehicle", "Preferences", "Cost assumptions"}) {
            var form = new WorkspaceUI.FormGrid();
            for (var field : PreparationForm.FIELDS) {
                if (!field.section().equals(section)) continue;
                JTextField input = new JTextField(24);
                input.setName(field.key());
                input.getAccessibleContext().setAccessibleName(field.label());
                fields.put(field.key(), input);
                form.field(field.label(), input);
            }
            var holder = new JPanel(new BorderLayout());
            holder.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            holder.add(form, BorderLayout.NORTH);
            if (section.equals("Preferences")) {
                var profileButtons = new JPanel(new WorkspaceUI.WrapLayout());
                var save = new JButton("Save preferences"); save.setName("profileSave");
                save.addActionListener(event -> {
                    navigate.accept("F03");
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
                    navigate.accept("F03");
                    PreparationForm.preferenceValues(facade.preferences()).forEach((key, value) -> fields.get(key).setText(value));
                    output.setText(ResultFormatter.format(facade.preferences())); output.setCaretPosition(0);
                    message.setText("Saved preferences loaded; unsaved preference edits discarded.");
                });
                profileButtons.add(save); profileButtons.add(load);
                var mode = new JComboBox<String>(); mode.setName("strategyMode");
                mode.addItem("Select a mode"); facade.evaluationModes().forEach(mode::addItem);
                mode.setToolTipText("auto selects from saved preferences; other modes explicitly override automatic selection.");
                var preview = new JButton("Preview priority guidance"); preview.setName("strategyPreview");
                preview.addActionListener(event -> {
                    navigate.accept("F03");
                    try {
                        facade.selectStrategy((String) mode.getSelectedItem());
                        output.setText(facade.previewPriorityGuidance()); output.setCaretPosition(0);
                        message.setText("Guidance uses saved preferences. Save preference edits before previewing. No AI assessment generated.");
                    } catch (IllegalArgumentException | IllegalStateException e) { displayError(e.getMessage()); }
                });
                var strategyControls = new JPanel(new WorkspaceUI.WrapLayout());
                strategyControls.add(new JLabel("Evaluation priority:")); strategyControls.add(mode); strategyControls.add(preview);
                var actions = WorkspaceUI.column(profileButtons, strategyControls);
                holder.add(actions, BorderLayout.SOUTH);
            }
            forms.put(section, holder);
        }
        PreparationForm.preferenceValues(facade.preferences()).forEach((key, value) -> fields.get(key).setText(value));
        var report = new ReportPanel(facade);
        var clarification = new ClarificationPanel(facade,()->{
            facade.currentVehicle().ifPresent(v->PreparationForm.vehicleValues(v).forEach((key,value)->fields.get(key).setText(value)));
            setResult("F01", "Clarification uses the saved vehicle; Vehicle fields refreshed. Previous displayed calculations are historical. Use Continue Risk Evaluation or Continue Checklist for current evidence.");
        });
        var checklist = new ChecklistPanel(facade);
        var comparison = new ComparisonPanel(facade);
        var risk = new RiskEvaluationPanel(facade);
        var listing = new ListingAnalyzerPanel(facade);
        output.setName("results"); output.setEditable(false); output.setLineWrap(true); output.setWrapStyleWord(true);
        output.setText("Fill F01 Vehicle, F03 Preferences and F06 Cost assumptions, then select Save and calculate.\n"
                + "Optional values may remain blank. No AI risk assessment is generated.\n" + facade.llmStatus());

        var example = new JButton("Load fictional example"); example.setName("example");
        example.addActionListener(event -> {
            navigate.accept("F01");
            PreparationForm.example().forEach((key, value) -> fields.get(key).setText(value));
            message.setText("Fictional example loaded. Review F01, F03 and F06; nothing saved yet.");
            output.setText("Example input loaded but not calculated. Click Save and calculate for fresh results.");
        });
        var calculate = new JButton("Save and calculate"); calculate.setName("calculate");
        calculate.addActionListener(event -> { navigate.accept("F01"); calculate(); });
        var conflicts = new JButton("Check against saved vehicle"); conflicts.setName("conflicts");
        conflicts.setToolTipText("Use the F01 vehicle form as a second structured record; does not save or extract listing text.");
        conflicts.addActionListener(event -> {
            navigate.accept("F04");
            var values = new LinkedHashMap<String, String>();
            fields.forEach((key, field) -> values.put(key, field.getText()));
            try {
                output.setText(ResultFormatter.format(facade.checkConflict(PreparationForm.vehicle(facade, values))));
                output.setCaretPosition(0);
                message.setText("Compared saved manual record with F01 vehicle form values. Neither source was changed.");
            } catch (IllegalArgumentException | IllegalStateException e) { displayError(e.getMessage()); }
        });
        var knowledge = new JButton("View local knowledge"); knowledge.setName("knowledge");
        knowledge.addActionListener(event -> {
            navigate.accept("F05");
            try {
                output.setText(ResultFormatter.format(facade.retrieveKnowledge())); output.setCaretPosition(0);
                message.setText("Local knowledge for the saved vehicle; unsaved form edits are not used.");
            } catch (IllegalArgumentException | IllegalStateException e) {
                output.setText("ERROR: " + e.getMessage());
                message.setText("Knowledge retrieval failed; saved session preserved.");
            }
        });
        WorkspaceUI.primary(calculate);
        var quality = new JButton("Check saved vehicle data quality"); quality.setName("qualityCheck");
        quality.addActionListener(event -> {
            navigate.accept("F04");
            try {
                output.setText(ResultFormatter.format(facade.checkMissingInformation(
                        facade.currentVehicle().orElseThrow(() -> new IllegalStateException("Save a vehicle first.")))));
                output.setCaretPosition(0);
                message.setText("Data-quality findings for the saved vehicle; unsaved F01 edits are not used.");
            } catch (IllegalArgumentException | IllegalStateException e) { displayError(e.getMessage()); }
        });
        var constraints = new JButton("Check saved buyer constraints"); constraints.setName("constraintCheck");
        constraints.addActionListener(event -> {
            navigate.accept("F07");
            try {
                var vehicle = facade.currentVehicle().orElseThrow(() -> new IllegalStateException("Save a vehicle first."));
                output.setText(constraintText(facade.checkConstraints(vehicle)));
                output.setCaretPosition(0);
                message.setText("Checked saved vehicle against saved preferences; no input was saved or changed.");
            } catch (IllegalArgumentException | IllegalStateException e) { displayError(e.getMessage()); }
        });
        var costCalculate = new JButton("Save and calculate"); costCalculate.setName("costCalculate");
        WorkspaceUI.primary(costCalculate);
        costCalculate.addActionListener(event -> { navigate.accept("F06"); calculate(); });
        String combined = "Save and calculate submits all F01 vehicle, F03 preference and F06 cost inputs together. "
                + "Review all three before saving. Optional values may remain blank. No AI assessment is generated.";
        var controls = new LinkedHashMap<String, JComponent>();
        controls.put("F01", WorkspaceUI.column(new WorkspaceUI.WrappingText(combined),
                WorkspaceUI.actions(example, calculate), forms.get("Vehicle")));
        controls.put("F03", forms.get("Preferences"));
        controls.put("F04", WorkspaceUI.column(new WorkspaceUI.WrappingText(
                "Check the saved record, or compare it with unsaved F01 vehicle fields. Missing, uncertain and conflicting values remain distinct. Neither action saves data."),
                WorkspaceUI.actions(quality, conflicts)));
        controls.put("F05", WorkspaceUI.column(new WorkspaceUI.WrappingText(
                "Retrieve local knowledge for the saved vehicle. Source labels and no-match results are preserved. Unsaved F01 edits are not used."),
                WorkspaceUI.actions(knowledge)));
        controls.put("F06", WorkspaceUI.column(new WorkspaceUI.WrappingText(combined),
                WorkspaceUI.actions(costCalculate), forms.get("Cost assumptions")));
        controls.put("F07", WorkspaceUI.column(new WorkspaceUI.WrappingText(
                "Uses the saved vehicle and F03 preferences. Existing checks cover budget, mileage, transmission and accident tolerance. Unspecified preferences are skipped; unknown is not a pass."),
                WorkspaceUI.actions(constraints)));
        var advanced = Map.of("F02", listing, "F08", risk, "F09", comparison,
                "F10", checklist, "F11", clarification, "F12", report);
        var controlLayout = new CardLayout(); var controlDeck = new WorkspaceUI.ControlDeck(controlLayout);
        var resultLayout = new CardLayout(); var resultDeck = new JPanel(resultLayout);
        controlDeck.setName("workspaceControls"); resultDeck.setName("workspaceResults");
        var inputColumn = WorkspaceUI.columnScroll(controlDeck); inputColumn.setName("workspaceInputScroll");
        var context = new WorkspaceUI.StatusLabel("");
        var outputTitle = new WorkspaceUI.StatusLabel("Last action output (snapshot)");
        var basicResults = new JPanel(new BorderLayout(0, 12));
        basicResults.add(WorkspaceUI.card("Saved session context", context), BorderLayout.NORTH);
        var outputCard = WorkspaceUI.resultCard("Last action output (snapshot)", output);
        outputCard.remove(((BorderLayout)outputCard.getLayout()).getLayoutComponent(BorderLayout.NORTH));
        outputCard.add(outputTitle, BorderLayout.NORTH); basicResults.add(outputCard);
        resultDeck.add(basicResults, "basic");
        var navigation = new LinkedHashMap<String, JButton>();
        var sidebar = new JPanel(); sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 8));
        for (var feature : WorkspaceUI.FEATURES) {
            String id = feature[0], name = feature[1];
            if (advanced.containsKey(id)) {
                var page = advanced.get(id);
                controls.put(id, page.takeWorkspaceControls()); resultDeck.add(page, id);
            } else {
                var document = new javax.swing.text.PlainDocument();
                resultDocuments.put(id, document);
                setResult(id, id.equals("F01") ? output.getText() : id.equals("F03")
                        ? ResultFormatter.format(facade.preferences()) : "No output yet. Run this feature's explicit action.\n" +
                        "Results are snapshots; rerun after changing the saved vehicle or preferences.");
            }
            controlDeck.add(WorkspaceUI.card(id + " — " + name, controls.get(id)), id);
            var button = WorkspaceUI.navigationButton(id, name);
            button.addActionListener(event -> navigate.accept(id));
            navigation.put(id, button); sidebar.add(button); sidebar.add(Box.createVerticalStrut(8));
        }
        var sidebarScroll = WorkspaceUI.columnScroll(sidebar); sidebarScroll.setName("featureNavigation");
        sidebarScroll.setPreferredSize(new Dimension(330, 400));
        navigate = id -> {
            selectedFeature = id;
            controlLayout.show(controlDeck, id);
            if (resultDocuments.containsKey(id)) {
                output.setDocument(resultDocuments.get(id)); output.setCaretPosition(0);
                outputTitle.setText(id + " — Last action output (snapshot; rerun after saved inputs change)");
                context.setText(id.equals("F03") ? "Saved preferences are independent of vehicle entry. Load saved preferences explicitly to replace edits."
                        : facade.currentVehicle().map(v -> v.year() + " " + v.make() + " " + v.model()
                        + " | " + v.mileage() + " km | CAD " + v.askingPrice()
                        + ". Unsaved F01 edits are separate from this record.").orElse("No saved vehicle in this session."));
                resultLayout.show(resultDeck, "basic");
            } else resultLayout.show(resultDeck, id);
            navigation.forEach((key, button) -> button.setSelected(key.equals(id)));
            var button = navigation.get(id);
            sidebar.scrollRectToVisible(button.getBounds());
            controlDeck.revalidate();
            SwingUtilities.invokeLater(() -> inputColumn.getViewport().setViewPosition(new Point(0, 0)));
        };
        navigate.accept("F01");
        var workspace = WorkspaceUI.adaptive(inputColumn, WorkspaceUI.resultViewport(resultDeck));
        workspace.setName("workspaceSplit");
        var body = new JPanel(new BorderLayout(12, 0)); body.add(sidebarScroll, BorderLayout.WEST); body.add(workspace);
        panel.add(body, BorderLayout.CENTER);
        message.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 8));
        panel.add(message, BorderLayout.SOUTH);
        WorkspaceUI.style(panel); panel.setOpaque(true); panel.setBackground(WorkspaceUI.BACKGROUND);
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
    public void displayResult(PreparationResult result) {
        String text = ResultFormatter.format(result);
        setResult("F01", text); setResult("F06", text);
        setResult("F04", ResultFormatter.format(result.missing()));
        setResult("F07", constraintText(result.violations()));
        navigate.accept(selectedFeature);
    }
    private void setResult(String id, String text) {
        var document = resultDocuments.get(id);
        if (document == null) return;
        try { document.remove(0, document.getLength()); document.insertString(0, text, null); }
        catch (javax.swing.text.BadLocationException e) { throw new IllegalStateException(e); }
    }
    /** Presentation of existing checker output only; no rules or calculations are duplicated here. */
    private static String constraintText(java.util.List<ca.yorku.eecs3311.usedcar.result.ConstraintViolation> violations) {
        var text = new StringBuilder("Checks cover budget, mileage, accepted transmissions and accident tolerance. UNKNOWN means not evaluable, not a confirmed conflict or pass.\n");
        if (violations.isEmpty()) text.append("No conflicts or unresolved checks among the supplied constraints; unspecified preferences are skipped.\n");
        violations.forEach(item -> text.append(item.severity()).append(": ").append(item.constraint())
                .append(" | expected=").append(item.expected()).append(" | actual=").append(item.actual()).append('\n'));
        return text.toString();
    }
    public void displayError(String text) {
        message.setText("Input rejected; previous saved session preserved.");
        output.setText("ERROR: " + text + "\nCorrect the input and try again. No new results were saved.");
    }
    public JPanel panel() { return panel; }
    public JFrame show() {
        JFrame frame = new JFrame("AI Used-Car Evaluation Assistant | Workspace");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setContentPane(panel);
        var available = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setSize(Math.min(1366, available.width), Math.min(900, available.height));
        frame.setMinimumSize(new Dimension(920, 620));
        frame.setLocationByPlatform(true); frame.setVisible(true);
        return frame;
    }
}
