package ca.yorku.eecs3311.usedcar.ui;

import ca.yorku.eecs3311.usedcar.app.UsedCarApplicationFacade;
import ca.yorku.eecs3311.usedcar.command.AnalyzeListingCommand;
import ca.yorku.eecs3311.usedcar.result.ListingAnalysisResult;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

/** F02 boundary: background analysis, local draft editing and explicit confirmation. */
public final class ListingAnalyzerPanel extends JPanel {
    private final JTextArea source = new JTextArea(5,60), result = new JTextArea(9,60);
    private final JButton analyze = new JButton("Analyze Listing"), save = new JButton("Confirm and Save"), cancel = new JButton("Cancel");
    private final Map<String,JTextField> fields = new LinkedHashMap<>();
    private final UsedCarApplicationFacade facade;
    private final Predicate<String> confirm;
    private ListingAnalysisResult draft;
    private SwingWorker<ListingAnalysisResult,Void> worker;
    public ListingAnalyzerPanel(UsedCarApplicationFacade facade) {
        this(facade, message -> JOptionPane.showConfirmDialog(null,new JScrollPane(new JTextArea(message,16,70)),
                "Review final values and conflicts - replace saved vehicle?",JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
    }
    /** Confirmation boundary is injectable for offline Swing verification. */
    public ListingAnalyzerPanel(UsedCarApplicationFacade facade, Predicate<String> confirm) {
        super(new BorderLayout(6,6)); this.facade=facade; this.confirm=confirm;
        source.setName("listingText"); result.setName("listingResult"); result.setEditable(false);
        result.setLineWrap(true); result.setWrapStyleWord(true);
        analyze.setName("listingAnalyze"); save.setName("listingSave"); cancel.setName("listingCancel"); save.setEnabled(false);
        var top = new JPanel(new BorderLayout()); top.add(new JLabel("Untrusted seller listing (max 16000 characters); sends one explicit Gemini request"),BorderLayout.NORTH);
        top.add(new JScrollPane(source)); add(top,BorderLayout.NORTH);
        var form = new JPanel(new GridLayout(0,2,4,4));
        for (var field : PreparationForm.FIELDS) if (field.section().equals("Vehicle")) {
            var value = new JTextField(24); value.setName("listing-"+field.key()); fields.put(field.key(),value);
            form.add(new JLabel(field.label())); form.add(value);
        }
        var middle = new JPanel(new BorderLayout()); middle.add(form,BorderLayout.NORTH); middle.add(new JScrollPane(result)); add(middle);
        var buttons = new JPanel(); buttons.add(analyze);buttons.add(save);buttons.add(cancel);add(buttons,BorderLayout.SOUTH);
        analyze.addActionListener(e -> start()); cancel.addActionListener(e -> cancelDraft()); save.addActionListener(e -> save());
    }
    private void start() {
        String text = source.getText(); draft=null; save.setEnabled(false); fields.values().forEach(f -> f.setText(""));
        analyze.setEnabled(false); result.setText("Analyzing; no vehicle saved. Cancel discards the pending result.");
        worker = new SwingWorker<>() {
            @Override protected ListingAnalysisResult doInBackground() { return new AnalyzeListingCommand(facade,text).execute(); }
            @Override protected void done() {
                if (worker != this) return;
                worker=null; analyze.setEnabled(true);
                try {
                    draft=get(); draft.draft().formValues().forEach((key,value) -> fields.get(key).setText(value));
                    result.setText(draft.draft().describe()+"\n"+ResultFormatter.format(draft.quality())
                            +"\nReview all fields. Unsupported/unknown units and uncertain values are not prefilled. Enter explicit CAD/km/L/100km corrections. Seller claims are unverified.");
                    result.setCaretPosition(0);save.setEnabled(true);
                } catch (CancellationException e) { result.setText("Cancelled; saved vehicle unchanged."); }
                catch (InterruptedException e) { Thread.currentThread().interrupt();result.setText("Analysis interrupted; no save."); }
                catch (ExecutionException e) { result.setText("ERROR: "+e.getCause().getMessage()+"\nNo save. Retry explicitly or use manual vehicle entry."); }
            }
        }; worker.execute();
    }
    private void cancelDraft() {
        var pending=worker;worker=null;if(pending!=null)pending.cancel(true);
        draft=null;save.setEnabled(false);analyze.setEnabled(true);fields.values().forEach(f -> f.setText(""));
        result.setText("Cancelled; saved vehicle and preferences unchanged. A request already sent may still consume quota.");
    }
    private void save() {
        if(draft==null)return;
        var values=new LinkedHashMap<String,String>();fields.forEach((k,v)->values.put(k,v.getText()));
        try {
            var candidate=PreparationForm.vehicle(facade,values);
            String review=candidate+"\n"+ResultFormatter.format(facade.reviewListing(draft,candidate));
            if(!confirm.test(review))return;
            facade.confirmListing(draft,candidate,true);draft=null;save.setEnabled(false);
            result.setText("User-confirmed listing saved. Preferences unchanged. Seller claims are not independently verified.");
        }catch(IllegalArgumentException|IllegalStateException e){result.setText("ERROR: "+e.getMessage()+"\nCorrect draft fields or cancel; previous saved vehicle preserved.");}
    }
}
