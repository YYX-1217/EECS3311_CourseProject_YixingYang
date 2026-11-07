package ca.yorku.eecs3311.usedcar.ui;

import ca.yorku.eecs3311.usedcar.app.UsedCarApplicationFacade;
import ca.yorku.eecs3311.usedcar.command.GenerateChecklistCommand;
import ca.yorku.eecs3311.usedcar.result.ChecklistEvaluation;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;

/** F10 boundary; generation runs off the EDT, completion stays in the shared session. */
public final class ChecklistPanel extends JPanel {
    private final UsedCarApplicationFacade facade;
    private final JTextArea status=new JTextArea(5,65);
    private final JPanel sections=new JPanel();
    private final JScrollPane sectionScroll=new JScrollPane(sections);
    public ChecklistPanel(UsedCarApplicationFacade facade){
        super(new BorderLayout(6,6));this.facade=facade;
        var generate=new JButton("Generate Inspection Checklist");generate.setName("checklistGenerate");
        var reload=new JButton("Show saved checklist");reload.setName("checklistShow");
        status.setName("checklistStatus");status.setEditable(false);status.setLineWrap(true);status.setWrapStyleWord(true);
        sections.setLayout(new BoxLayout(sections,BoxLayout.Y_AXIS));
        var controls=new JPanel(new FlowLayout(FlowLayout.LEFT));controls.add(generate);controls.add(reload);
        var top=new JPanel(new BorderLayout());top.add(controls,BorderLayout.NORTH);top.add(new JScrollPane(status));add(top,BorderLayout.NORTH);add(sectionScroll);
        reload.addActionListener(event->showSaved());
        generate.addActionListener(event->{
            if(facade.currentVehicle().isEmpty()){clear("Save a vehicle before generating an inspection checklist.");return;}
            generate.setEnabled(false);clear("Collecting vehicle evidence and generating checklist...");
            new SwingWorker<ChecklistEvaluation,Void>(){
                protected ChecklistEvaluation doInBackground(){return new GenerateChecklistCommand(facade).execute();}
                protected void done(){
                    try{get();showSaved();}
                    catch(InterruptedException e){Thread.currentThread().interrupt();clear("Checklist generation interrupted. Reload saved state or retry.");}
                    catch(ExecutionException e){clear("Checklist could not be saved; the vehicle or generation may have changed. Reload saved state or generate again.");}
                    finally{generate.setEnabled(true);}
                }
            }.execute();
        });
        addComponentListener(new java.awt.event.ComponentAdapter(){public void componentShown(java.awt.event.ComponentEvent e){showSaved();}});
        showSaved();
    }
    private void clear(String message){status.setText(message);sections.removeAll();sections.revalidate();sections.repaint();}
    private void showSaved(){
        var current=facade.currentChecklist();
        if(current.isEmpty()){clear("No checklist for the current vehicle. Save a vehicle and select Generate Inspection Checklist. Generation may send one AI request; showing or marking items never does.");return;}
        var result=current.get();clear("F10 "+result.status()+" | "+result.vehicle().year()+" "+result.vehicle().make()+" "+result.vehicle().model()+" ["+result.vehicle().id()+"]\n"+result.message()+"\n"+result.knowledgeStatus()+"\nCompletion records progress only, not verified vehicle condition.");
        result.checklist().sections().forEach((section,items)->{
            var group=new JPanel();group.setLayout(new BoxLayout(group,BoxLayout.Y_AXIS));group.setBorder(BorderFactory.createTitledBorder(section));
            for(var item:items){
                var row=new JPanel(new BorderLayout(4,4));
                var done=new JCheckBox(item.id()+" | "+item.priority(),result.completed().contains(item.id()));done.setName("checklistItem-"+item.id());
                var text=new JTextArea(item.action()+"\nReason: "+item.reason()+"\nSource: "+item.source(),4,60);text.setEditable(false);text.setLineWrap(true);text.setWrapStyleWord(true);text.setCaretPosition(0);
                ((javax.swing.text.DefaultCaret)text.getCaret()).setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);
                row.add(done,BorderLayout.NORTH);row.add(new JScrollPane(text));group.add(row);
                done.addActionListener(event->{
                    try{facade.completeChecklistItem(result.generationId(),item.id(),done.isSelected());}
                    catch(IllegalArgumentException|IllegalStateException e){clear("This checklist is no longer current. Show saved checklist or generate again.");}
                });
            }
            sections.add(group);
        });
        sections.revalidate();sections.repaint();status.setCaretPosition(0);
        SwingUtilities.invokeLater(()->sectionScroll.getViewport().setViewPosition(new Point(0,0)));
    }
}
