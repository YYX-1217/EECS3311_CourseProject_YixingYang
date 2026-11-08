package ca.yorku.eecs3311.usedcar.ui;

import ca.yorku.eecs3311.usedcar.app.UsedCarApplicationFacade;
import ca.yorku.eecs3311.usedcar.command.GenerateReportCommand;
import ca.yorku.eecs3311.usedcar.result.ReportEvaluation;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.*;

/** F12 boundary: shared report formatting, asynchronous generation and explicit local export. */
public final class ReportPanel extends JPanel {
    private final UsedCarApplicationFacade facade;
    private final JTextArea output=new JTextArea(20,75);
    private final JButton generate=new JButton("Generate / Regenerate Report");
    private final JButton show=new JButton("Show saved report");
    private final JButton save=new JButton("Export UTF-8 text...");
    private String displayedId;
    public ReportPanel(UsedCarApplicationFacade facade){
        super(new BorderLayout(6,6));this.facade=facade;generate.setName("reportGenerate");show.setName("reportShow");save.setName("reportExport");
        output.setName("reportResults");output.setEditable(false);output.setLineWrap(true);output.setWrapStyleWord(true);
        var controls=new JPanel(new FlowLayout(FlowLayout.LEFT));controls.add(generate);controls.add(show);controls.add(save);add(controls,BorderLayout.NORTH);add(new JScrollPane(output));
        show.addActionListener(e->showSaved());generate.addActionListener(e->generate());save.addActionListener(e->chooseExport());
        addComponentListener(new java.awt.event.ComponentAdapter(){public void componentShown(java.awt.event.ComponentEvent e){if(generate.isEnabled())showSaved();}});
        // A visible report must not remain apparently current after another tab changes evidence.
        var timer=new Timer(250,e->{if(displayedId!=null&&facade.currentReport().filter(r->r.generationId().equals(displayedId)).isEmpty()){displayedId=null;output.setText("Report is no longer current. Saved evidence changed; generate a new report.");save.setEnabled(false);}});
        addHierarchyListener(e->{if((e.getChangeFlags()&java.awt.event.HierarchyEvent.SHOWING_CHANGED)!=0){if(isShowing())timer.start();else timer.stop();}});
        showSaved();
    }
    private void showSaved(){
        facade.currentReport().ifPresentOrElse(r->{displayedId=r.generationId();output.setText(ResultFormatter.format(r));output.setCaretPosition(0);save.setEnabled(true);},()->{
            displayedId=null;output.setText("No current report. Save a vehicle and preferences, then Generate Report. Generation may send one AI request. Show is cached; export writes only a new file you select.");save.setEnabled(false);
        });
    }
    private void generate(){
        if(facade.currentVehicle().isEmpty()){output.setText("ERROR: Save a vehicle before generating a report.");return;}
        generate.setEnabled(false);show.setEnabled(false);save.setEnabled(false);displayedId=null;output.setText("Collecting current evidence and generating advisory report...");
        new SwingWorker<ReportEvaluation,Void>(){
            protected ReportEvaluation doInBackground(){return new GenerateReportCommand(facade).execute();}
            protected void done(){
                try{var result=get();if(facade.currentReport().filter(r->r.generationId().equals(result.generationId())).isEmpty())throw new IllegalStateException();showSaved();}
                catch(InterruptedException e){Thread.currentThread().interrupt();output.setText("Report generation interrupted. Retry explicitly.");}
                catch(ExecutionException|IllegalStateException e){output.setText("Report could not be saved. The evidence may have changed; show the current state or generate again.");}
                finally{generate.setEnabled(true);show.setEnabled(true);}
            }
        }.execute();
    }
    private void chooseExport(){
        if(displayedId==null){output.setText("Show a current report before export.");return;}
        var chooser=new JFileChooser();chooser.setDialogTitle("Choose a new local report .txt file (private data)");
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        exportTo(chooser.getSelectedFile().toPath());
    }
    /** Same export action used after file selection and by the GUI smoke test. */
    public void exportTo(java.nio.file.Path path){
        try{var result=facade.exportReport(displayedId,path);output.append("\nExported UTF-8 report to "+result+"\n");}
        catch(IllegalArgumentException|IllegalStateException e){output.append("\nERROR: "+e.getMessage()+"\n");}
    }
}
