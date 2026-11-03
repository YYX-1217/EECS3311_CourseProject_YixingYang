package ca.yorku.eecs3311.usedcar.ui;

import ca.yorku.eecs3311.usedcar.app.UsedCarApplicationFacade;
import ca.yorku.eecs3311.usedcar.command.CompareVehiclesCommand;
import ca.yorku.eecs3311.usedcar.result.ComparisonEvaluation;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

/** Two saved records; editable B form is separate from the current-vehicle form. */
public final class ComparisonPanel extends JPanel {
    private final UsedCarApplicationFacade facade;
    private final Map<String,JTextField> fields=new LinkedHashMap<>();
    private final JTextArea summary=new JTextArea(3,65);
    private final JTextArea output=new JTextArea(15,70);
    public ComparisonPanel(UsedCarApplicationFacade facade) {
        super(new BorderLayout(6,6)); this.facade=facade;
        summary.setName("comparisonSummary"); summary.setEditable(false); summary.setLineWrap(true);
        output.setName("comparisonResults"); output.setEditable(false); output.setLineWrap(true); output.setWrapStyleWord(true);
        var form=new JPanel(new GridLayout(0,2,6,4));
        for(var field:PreparationForm.FIELDS) {
            if(!field.section().equals("Vehicle")) continue;
            var input=new JTextField(20); input.setName("comparison_"+field.key());
            fields.put(field.key(),input); var label=new JLabel("B: "+field.label()); label.setLabelFor(input);
            form.add(label); form.add(input);
        }
        var save=new JButton("Save Vehicle B"); save.setName("comparisonSave");
        var cancel=new JButton("Cancel edits / reload saved B"); cancel.setName("comparisonCancel");
        var compare=new JButton("Compare saved vehicles"); compare.setName("comparisonRun");
        var refresh=new JButton("Refresh saved summaries"); refresh.setName("comparisonRefresh");
        save.addActionListener(event->{
            var values=new LinkedHashMap<String,String>(); fields.forEach((key,input)->values.put(key,input.getText()));
            try {
                facade.saveComparisonVehicle(PreparationForm.comparisonVehicle(facade,values)); refresh();
                output.setText("Vehicle B saved. Vehicle A, preferences and common cost assumptions preserved. Compare uses saved records only.");
            } catch(IllegalArgumentException|IllegalStateException e) { output.setText("ERROR: "+e.getMessage()+" Saved records preserved."); }
        });
        cancel.addActionListener(event->{
            fields.values().forEach(f->f.setText(""));
            facade.comparisonVehicle().ifPresent(v->PreparationForm.vehicleValues(v).forEach((key,value)->fields.get(key).setText(value)));
            refresh(); output.setText("Unsaved B edits discarded; saved records preserved.");
        });
        refresh.addActionListener(event->refresh());
        compare.addActionListener(event->{
            refresh();
            if(facade.currentVehicle().isEmpty() || facade.comparisonVehicle().isEmpty()) {
                output.setText("ERROR: Save Vehicle A and comparison Vehicle B before comparing."); return;
            }
            compare.setEnabled(false); output.setText("Comparing saved records; collecting evidence and requesting AI only when sufficient...");
            new SwingWorker<ComparisonEvaluation,Void>() {
                protected ComparisonEvaluation doInBackground() { return new CompareVehiclesCommand(facade).execute(); }
                protected void done() {
                    try { output.setText(ResultFormatter.format(get())); output.setCaretPosition(0); }
                    catch(InterruptedException e) { Thread.currentThread().interrupt(); output.setText("Comparison interrupted; saved records preserved."); }
                    catch(ExecutionException e) { output.setText("Comparison failed. Review both saved records and retry. No AI narrative displayed; saved records preserved."); }
                    finally { compare.setEnabled(true); refresh(); }
                }
            }.execute();
        });
        var buttons=new JPanel(new FlowLayout(FlowLayout.LEFT)); buttons.add(save); buttons.add(cancel); buttons.add(compare); buttons.add(refresh);
        var upper=new JPanel(new BorderLayout()); upper.add(new JScrollPane(summary),BorderLayout.NORTH);
        upper.add(new JScrollPane(form)); upper.add(buttons,BorderLayout.SOUTH);
        var split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,upper,new JScrollPane(output)); split.setResizeWeight(0.55); add(split);
        addComponentListener(new java.awt.event.ComponentAdapter(){ public void componentShown(java.awt.event.ComponentEvent e){refresh();} });
        output.setText("Save A in the existing Vehicle workflow, enter/save B here, then compare. CAD/km only; save common cost assumptions using Save and calculate. Comparison may send one Gemini request.");
        refresh();
    }
    private void refresh() {
        summary.setText("A: "+facade.currentVehicle().map(v->v.year()+" "+v.make()+" "+v.model()+" ["+v.id()+"]").orElse("not saved")
                +"\nB: "+facade.comparisonVehicle().map(v->v.year()+" "+v.make()+" "+v.model()+" ["+v.id()+"]").orElse("not saved")
                +"\nCommon cost assumptions: "+facade.costAssumptions().map(Object::toString).orElse("UNAVAILABLE"));
    }
}
