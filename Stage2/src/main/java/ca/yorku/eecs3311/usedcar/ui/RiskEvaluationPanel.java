package ca.yorku.eecs3311.usedcar.ui;
import ca.yorku.eecs3311.usedcar.app.UsedCarApplicationFacade;
import ca.yorku.eecs3311.usedcar.command.EvaluateVehicleCommand;
import ca.yorku.eecs3311.usedcar.result.RiskEvaluation;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.*;
/** Explicit saved-vehicle evaluation; provider calls never run on the EDT. */
public final class RiskEvaluationPanel extends WorkspaceUI.Feature {
    private static final String OUTDATED="STALE: Previous risk assessment is outdated because saved evaluation data changed. Please run Evaluate Vehicle again.";
    private final UsedCarApplicationFacade facade;
    private final JTextArea output=new JTextArea(18,70);
    private RiskEvaluation displayed;
    private boolean current(RiskEvaluation result){return facade.lastRiskEvaluation().filter(saved->saved==result).isPresent();}
    private void refreshValidity(){
        if(displayed!=null&&!current(displayed)){displayed=null;output.setText(OUTDATED);output.setCaretPosition(0);}
    }
    public RiskEvaluationPanel(UsedCarApplicationFacade facade){
        super(new BorderLayout(6,6));this.facade=facade;var evaluate=new JButton("Evaluate Vehicle");evaluate.setName("riskEvaluate");
        output.setName("riskResults");output.setEditable(false);output.setLineWrap(true);output.setWrapStyleWord(true);
        output.setText("Save or confirm a vehicle first. Evaluation uses saved preferences and available saved costs. Explicit evaluation may send one Gemini request. Results are advisory.");
        WorkspaceUI.primary(evaluate);workspace(WorkspaceUI.actions(evaluate),WorkspaceUI.resultCard("Risk findings & evidence",output));
        // Read-only validity checks on the EDT. Stop polling when hidden/disposed; refresh immediately on return.
        var timer=new Timer(250,event->refreshValidity());
        addHierarchyListener(event->{
            if((event.getChangeFlags()&java.awt.event.HierarchyEvent.SHOWING_CHANGED)!=0){
                if(isShowing()){refreshValidity();timer.start();}else timer.stop();
            }
        });
        evaluate.addActionListener(event->{
            displayed=null;evaluate.setEnabled(false);output.setText("Collecting evidence and assessing the saved vehicle...");
            new SwingWorker<RiskEvaluation,Void>(){
                @Override protected RiskEvaluation doInBackground(){return new EvaluateVehicleCommand(facade).execute();}
                @Override protected void done(){
                    try{
                        var result=get();
                        // Also closes the gap between backend completion and this queued EDT callback.
                        if(current(result)){displayed=result;output.setText(ResultFormatter.format(result));}
                        else{displayed=null;output.setText(OUTDATED);}
                        output.setCaretPosition(0);
                    }
                    catch(InterruptedException e){Thread.currentThread().interrupt();output.setText("Evaluation interrupted; no AI result displayed.");}
                    catch(ExecutionException e){output.setText("ERROR: Save a valid vehicle before evaluation. No AI result displayed.");}
                    finally{evaluate.setEnabled(true);}
                }
            }.execute();
        });
    }
}
