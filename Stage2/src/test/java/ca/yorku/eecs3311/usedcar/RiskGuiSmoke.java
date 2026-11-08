package ca.yorku.eecs3311.usedcar;
import ca.yorku.eecs3311.usedcar.ui.*;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;
public final class RiskGuiSmoke {
    private static Component named(Container root,String name){for(Component c:root.getComponents()){if(name.equals(c.getName()))return c;if(c instanceof Container box){var f=named(box,name);if(f!=null)return f;}}return null;}
    public static void main(String[] args)throws Exception{
        var start=new CountDownLatch(1);var release=new CountDownLatch(1);var requestSequence=new AtomicInteger();var invalidAge=new AtomicBoolean();
        var llm=new RiskChecks.Fake(){@Override public String generateStructured(String p,String schema){
            check(!SwingUtilities.isEventDispatchThread(),"F08 request must be off EDT");
            if(requestSequence.incrementAndGet()==1){start.countDown();try{if(!release.await(5,TimeUnit.SECONDS))throw new AssertionError("Worker release timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException();}}
            String result=super.generateStructured(p,schema);
            if(invalidAge.get()){var json=com.google.gson.JsonParser.parseString(result).getAsJsonObject();json.getAsJsonArray("risks").add(RiskAgeChecks.BAD);return json.toString();}
            return result;
        }};
        var facade=RiskChecks.facade(llm);RiskChecks.save(facade);var panel=new AtomicReference<RiskEvaluationPanel>();var window=new AtomicReference<JFrame>();
        SwingUtilities.invokeAndWait(()->{var p=new RiskEvaluationPanel(facade);panel.set(p);var f=new JFrame();window.set(f);f.setContentPane(p);f.pack();f.setVisible(true);((JButton)named(p,"riskEvaluate")).doClick();});
        try{
            check(start.await(5,TimeUnit.SECONDS),"Risk worker did not start");
            SwingUtilities.invokeAndWait(()->{check(!((JButton)named(panel.get(),"riskEvaluate")).isEnabled(),"Button must disable while UI remains responsive");
                facade.selectStrategy("budget");});
            release.countDown();awaitReady(panel.get());
            SwingUtilities.invokeAndWait(()->{check(((JTextArea)named(panel.get(),"riskResults")).getText().contains("STALE")&&facade.lastRiskEvaluation().isEmpty(),"Changed context must not store/display stale AI category");((JButton)named(panel.get(),"riskEvaluate")).doClick();});
            awaitReady(panel.get());
            SwingUtilities.invokeAndWait(()->{String text=((JTextArea)named(panel.get(),"riskResults")).getText();check(text.contains("Risk level: Moderate")&&text.contains("Budget Focused")&&text.contains("DETERMINISTIC EVIDENCE"),"GUI must show shared risk result and tool evidence");llm.fail=true;((JButton)named(panel.get(),"riskEvaluate")).doClick();});
            awaitReady(panel.get());
            SwingUtilities.invokeAndWait(()->{String text=((JTextArea)named(panel.get(),"riskResults")).getText();check(text.contains("AI_UNAVAILABLE")&&text.contains("DETERMINISTIC EVIDENCE")&&!text.contains("Risk level: Moderate"),"Failure must retain tools, remove stale AI output and restore controls");});
            SwingUtilities.invokeAndWait(()->{llm.fail=false;invalidAge.set(true);((JButton)named(panel.get(),"riskEvaluate")).doClick();});
            awaitReady(panel.get());
            SwingUtilities.invokeAndWait(()->{String text=((JTextArea)named(panel.get(),"riskResults")).getText();
                check(text.contains("AI_UNAVAILABLE")&&text.contains("[RISK_AGE_EVIDENCE]")&&text.contains("risks[")&&text.contains("Model-year difference:")&&!text.contains(RiskAgeChecks.BAD),"GUI withholds bad age claim while showing safe reason and Java year context");
                invalidAge.set(false);((JButton)named(panel.get(),"riskEvaluate")).doClick();});
            awaitReady(panel.get());
            SwingUtilities.invokeAndWait(()->{check(((JTextArea)named(panel.get(),"riskResults")).getText().contains("AI_SUCCESS"),"Explicit retry can recover after a sound response");});
            System.out.println("PASS: F08 GUI background evaluation, responsive EDT, stale-context rejection, structured result, safe provider failure, quantified-age rejection and explicit recovery.");
        }finally{release.countDown();SwingUtilities.invokeAndWait(()->window.get().dispose());}
    }
    private static void awaitReady(RiskEvaluationPanel p)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);var ready=new AtomicBoolean();while(System.nanoTime()<end){SwingUtilities.invokeAndWait(()->ready.set(((JButton)named(p,"riskEvaluate")).isEnabled()));if(ready.get())return;Thread.sleep(30);}throw new AssertionError("Risk GUI did not finish");}
}

