package ca.yorku.eecs3311.usedcar.ui;
import ca.yorku.eecs3311.usedcar.app.UsedCarApplicationFacade;
import ca.yorku.eecs3311.usedcar.command.EvaluateVehicleCommand;
import ca.yorku.eecs3311.usedcar.result.RiskEvaluation;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.*;
/** Explicit saved-vehicle evaluation; provider calls never run on the EDT. */
public final class RiskEvaluationPanel extends JPanel {
    public RiskEvaluationPanel(UsedCarApplicationFacade facade){
        super(new BorderLayout(6,6));var evaluate=new JButton("Evaluate Vehicle");evaluate.setName("riskEvaluate");
        var output=new JTextArea(18,70);output.setName("riskResults");output.setEditable(false);output.setLineWrap(true);output.setWrapStyleWord(true);
        output.setText("Save or confirm a vehicle first. Evaluation uses saved preferences and available saved costs. Explicit evaluation may send one Gemini request. Results are advisory.");
        add(evaluate,BorderLayout.NORTH);add(new JScrollPane(output));
        evaluate.addActionListener(event->{
            evaluate.setEnabled(false);output.setText("Collecting evidence and assessing the saved vehicle...");
            new SwingWorker<RiskEvaluation,Void>(){
                @Override protected RiskEvaluation doInBackground(){return new EvaluateVehicleCommand(facade).execute();}
                @Override protected void done(){
                    try{output.setText(ResultFormatter.format(get()));output.setCaretPosition(0);}
                    catch(InterruptedException e){Thread.currentThread().interrupt();output.setText("Evaluation interrupted; no AI result displayed.");}
                    catch(ExecutionException e){output.setText("ERROR: Save a valid vehicle before evaluation. No AI result displayed.");}
                    finally{evaluate.setEnabled(true);}
                }
            }.execute();
        });
    }
}
