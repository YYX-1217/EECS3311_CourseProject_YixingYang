package ca.yorku.eecs3311.usedcar;

import ca.yorku.eecs3311.usedcar.ui.*;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;

/** Real Swing listeners, offline model fixture; no desktop-wide capture or live provider. */
public final class ComparisonGuiSmoke {
    private static Component named(Container parent,String name){
        for(Component c:parent.getComponents()){
            if(name.equals(c.getName()))return c;
            if(c instanceof Container box){var found=named(box,name);if(found!=null)return found;}
        }return null;
    }
    private static JButton button(Container p,String name){return (JButton)named(p,name);}
    private static JTextField field(Container p,String name){return (JTextField)named(p,"comparison_"+name);}
    private static String output(Container p){return ((JTextArea)named(p,"comparisonResults")).getText();}
    public static void main(String[] args)throws Exception{
        var started=new CountDownLatch(1);var release=new CountDownLatch(1);var sequence=new AtomicInteger();
        var llm=new ComparisonChecks.Fake(){public String generateStructured(String p,String s){
            check(!SwingUtilities.isEventDispatchThread(),"Gemini comparison must run off the EDT");
            if(sequence.incrementAndGet()==1){started.countDown();try{check(release.await(5,TimeUnit.SECONDS),"Worker release timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException();}}
            return super.generateStructured(p,s);
        }};
        var f=ComparisonChecks.facade(llm);PreparationForm.command(f,RiskChecks.complete()).execute();
        var first=f.currentVehicle();var prefs=f.preferences();var assumptions=f.costAssumptions();
        var panel=new AtomicReference<JPanel>();var frame=new AtomicReference<JFrame>();
        try{
            SwingUtilities.invokeAndWait(()->{
                var gui=new UsedCarGUI(f);panel.set(gui.panel());frame.set(gui.show());check(frame.get().isShowing(),"GUI must launch");
                var p=panel.get();button(p,"comparisonRun").doClick();check(output(p).contains("Save Vehicle A and comparison Vehicle B"),"Missing B must be actionable");
                ComparisonChecks.secondValues().forEach((key,value)->{if(named(p,"comparison_"+key)!=null)field(p,key).setText(value);});
                button(p,"comparisonSave").doClick();check(f.comparisonVehicle().isPresent()&&f.currentVehicle().equals(first),"GUI save B must preserve A");
                var previous=f.comparisonVehicle();field(p,"price").setText("-1");button(p,"comparisonSave").doClick();
                check(output(p).contains("ERROR:")&&f.comparisonVehicle().equals(previous),"Invalid GUI edits must preserve B");
                button(p,"comparisonCancel").doClick();check(field(p,"price").getText().equals("16000"),"Cancel must reload saved B without saving edits");
                check(f.preferences().equals(prefs)&&f.costAssumptions().equals(assumptions),"GUI B workflow preserves preferences and assumptions");
                button(p,"comparisonRun").doClick();
            });
            check(started.await(5,TimeUnit.SECONDS),"Comparison worker did not start");
            SwingUtilities.invokeAndWait(()->{
                var p=panel.get();check(!button(p,"comparisonRun").isEnabled(),"Compare disabled while EDT remains responsive");
                field(p,"price").setText("15000");button(p,"comparisonSave").doClick();
            });
            release.countDown();awaitReady(panel.get());
            SwingUtilities.invokeAndWait(()->{
                check(output(panel.get()).contains("STALE")&&!output(panel.get()).contains("AI trade-off explanation (advisory):"),"Changing B while pending must withhold stale AI");
                button(panel.get(),"comparisonRun").doClick();
            });
            awaitReady(panel.get());
            SwingUtilities.invokeAndWait(()->{
                String text=output(panel.get());check(text.contains("AI_SUCCESS")&&text.contains("A: 18000 | B: 15000")&&text.contains("AI trade-off explanation")&&text.contains("Vehicle B EVIDENCE"),"GUI must show actual shared comparison and both evidence snapshots");
                llm.fail=true;button(panel.get(),"comparisonRun").doClick();
            });
            awaitReady(panel.get());
            SwingUtilities.invokeAndWait(()->{
                String text=output(panel.get());check(text.contains("AI_UNAVAILABLE")&&text.contains("DETERMINISTIC SIDE-BY-SIDE")&&!text.contains("AI trade-off explanation (advisory):"),"Provider failure preserves metrics and removes prior narrative");
                check(f.currentVehicle().equals(first)&&f.preferences().equals(prefs),"Failed GUI comparison preserves session");
            });
            System.out.println("PASS: F09 GUI entry/edit/cancel, background comparison, responsive EDT, stale-result rejection and evidence-preserving failure.");
        }finally{release.countDown();if(frame.get()!=null)SwingUtilities.invokeAndWait(()->frame.get().dispose());}
    }
    private static void awaitReady(Container panel)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);var ready=new AtomicBoolean();
        while(System.nanoTime()<deadline){SwingUtilities.invokeAndWait(()->ready.set(button(panel,"comparisonRun").isEnabled()));if(ready.get())return;Thread.sleep(30);}
        throw new AssertionError("Comparison GUI did not finish");
    }
}
