package ca.yorku.eecs3311.usedcar.ui;

import ca.yorku.eecs3311.usedcar.app.UsedCarApplicationFacade;
import ca.yorku.eecs3311.usedcar.domain.ClarificationField;
import ca.yorku.eecs3311.usedcar.result.ClarificationState;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.function.Supplier;

/** F11 UI: local actions use the displayed cycle token; network work never runs on the EDT. */
public final class ClarificationPanel extends WorkspaceUI.Feature {
    private final UsedCarApplicationFacade facade;
    private final Runnable changed;
    private final JTextArea status=new JTextArea(17,70);
    private final JComboBox<String> questions=new JComboBox<>();
    private final JComboBox<ClarificationField> revisit=new JComboBox<>(ClarificationField.values());
    private final JTextField answer=new JTextField(18);
    private final JPanel questionCards=WorkspaceUI.column();
    private final java.util.List<JButton> buttons=new ArrayList<>();
    private String cycle;
    private boolean busy;
    public ClarificationPanel(UsedCarApplicationFacade facade,Runnable changed){
        super(new BorderLayout(6,6));this.facade=facade;this.changed=changed;
        status.setName("clarificationStatus");status.setEditable(false);status.setLineWrap(true);status.setWrapStyleWord(true);
        questions.setName("clarificationField");answer.setName("clarificationAnswer");revisit.setName("clarificationRevisitField");
        var controls=WorkspaceUI.column();var top=new JPanel(new WorkspaceUI.WrapLayout());
        top.add(button("Start / Show","clarificationStart",()->background(()->facade.startClarification(false))));
        top.add(button("Next gaps","clarificationNext",()->background(()->facade.startClarification(true))));
        top.add(button("Refresh status","clarificationShow",this::refresh));controls.add(top);controls.add(Box.createVerticalStrut(8));
        var row=new JPanel(new WorkspaceUI.WrapLayout());row.add(new JLabel("Target:"));row.add(questions);row.add(answer);
        row.add(button("Answer","clarificationSubmit",()->local(()->facade.submitClarification(cycle,field(),answer.getText()))));
        row.add(button("Skip","clarificationSkip",()->local(()->facade.skipClarification(cycle,field(),false))));
        row.add(button("Unknown","clarificationUnknown",()->local(()->facade.skipClarification(cycle,field(),true))));controls.add(WorkspaceUI.card("Answer a question",row));controls.add(Box.createVerticalStrut(12));
        var conflict=new JPanel(new WorkspaceUI.WrapLayout());
        conflict.add(button("Keep existing","clarificationKeep",()->local(()->facade.resolveClarification(cycle,field(),false))));
        conflict.add(button("Accept new","clarificationAccept",()->local(()->facade.resolveClarification(cycle,field(),true))));
        conflict.add(revisit);conflict.add(button("Revisit field","clarificationRevisit",()->local(()->facade.revisitClarification((ClarificationField)revisit.getSelectedItem()))));controls.add(WorkspaceUI.card("Resolve or revisit",conflict));controls.add(Box.createVerticalStrut(12));
        var resume=new JPanel(new WorkspaceUI.WrapLayout());
        resume.add(button("Continue Risk Evaluation","clarificationRisk",()->{String token=cycle;background(()->facade.continueClarification(token,false));}));
        resume.add(button("Continue Checklist","clarificationChecklist",()->{String token=cycle;background(()->facade.continueClarification(token,true));}));controls.add(WorkspaceUI.card("Continue with saved answers",resume));
        workspace(controls,WorkspaceUI.split(JSplitPane.VERTICAL_SPLIT,WorkspaceUI.card("Current questions",WorkspaceUI.columnScroll(questionCards)),WorkspaceUI.resultCard("Clarification status & full history",status),0.38));
        addComponentListener(new java.awt.event.ComponentAdapter(){public void componentShown(java.awt.event.ComponentEvent e){if(!busy)refresh();}});
        refresh();
    }
    private JButton button(String label,String name,Runnable action){var b=new JButton(label);b.setName(name);b.addActionListener(e->action.run());buttons.add(b);return b;}
    private ClarificationField field(){if(questions.getSelectedItem()==null)throw new IllegalStateException("Start clarification and select a target first.");return ClarificationField.fromKey((String)questions.getSelectedItem());}
    private void display(ClarificationState state){
        cycle=state.cycleId();var selected=questions.getSelectedItem();questions.removeAllItems();state.batch().questions().forEach(q->questions.addItem(q.id()));
        if(selected!=null&&state.batch().questions().stream().anyMatch(q->q.id().equals(selected)))questions.setSelectedItem(selected);
        status.setText(ResultFormatter.format(state));status.setCaretPosition(0);
        questionCards.removeAll();
        for(var q:state.batch().questions()){
            var response=state.responses().get(q.field());
            var text=new WorkspaceUI.WrappingText(q.question()+"\n"+q.reason()+(response.answer().isBlank()?"":"\nAnswer: "+response.answer()));text.setFont(WorkspaceUI.FONT);
            var card=WorkspaceUI.card(q.field().label()+" / "+q.priority()+" / "+state.responses().get(q.field()).status(),text);
            questionCards.add(card);questionCards.add(Box.createVerticalStrut(12));WorkspaceUI.style(card);
        }
        questionCards.revalidate();questionCards.repaint();
    }
    private void refresh(){facade.currentClarification().ifPresentOrElse(this::display,()->{cycle=null;questions.removeAllItems();questionCards.removeAll();questionCards.revalidate();questionCards.repaint();status.setText("Save a vehicle, then Start clarification. Start/Next may send one AI request; answers and Refresh are local. Finish each cycle before Next, Revisit or Continue. Unknown and Skip preserve data.");});}
    private void local(Supplier<ClarificationState> action){try{display(action.get());answer.setText("");changed.run();}catch(IllegalArgumentException|IllegalStateException e){status.setText("ERROR: "+e.getMessage()+"\nRefresh status to review the saved cycle.");}}
    private void background(Supplier<Object> action){
        var expected=facade.currentVehicle();
        busy=true;buttons.forEach(b->b.setEnabled(false));status.setText("Working with saved vehicle context...");
        new SwingWorker<Object,Void>(){
            protected Object doInBackground(){if(!facade.currentVehicle().equals(expected))throw new IllegalStateException("Vehicle changed before request.");return action.get();}
            protected void done(){
                try{Object result=get();if(!facade.currentVehicle().equals(expected))throw new IllegalStateException("Vehicle changed; refresh clarification.");if(result instanceof ClarificationState state){
                    if(facade.currentClarification().filter(s->s.cycleId().equals(state.cycleId())).isEmpty())throw new IllegalStateException("Vehicle changed; refresh clarification.");display(state);
                }else{refresh();status.append("\n"+ResultFormatter.formatClarificationContinuation(result));}changed.run();}
                catch(Exception e){status.setText("ERROR: Clarification could not complete. Save a vehicle, finish pending questions/conflicts, or refresh after a vehicle change.");}
                finally{busy=false;buttons.forEach(b->b.setEnabled(true));}
            }
        }.execute();
    }
}
