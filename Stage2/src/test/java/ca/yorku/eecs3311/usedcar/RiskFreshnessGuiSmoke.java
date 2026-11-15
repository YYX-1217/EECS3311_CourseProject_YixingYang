package ca.yorku.eecs3311.usedcar;

import ca.yorku.eecs3311.usedcar.app.*;
import ca.yorku.eecs3311.usedcar.domain.*;
import ca.yorku.eecs3311.usedcar.ui.*;
import javax.swing.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.math.BigDecimal;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;
import static ca.yorku.eecs3311.usedcar.WorkspaceGuiSmoke.*;

/** AUD-03 real workspace listeners, mock provider, no credentials/network. */
public final class RiskFreshnessGuiSmoke {
    private static void edt(Runnable action)throws Exception{SwingUtilities.invokeAndWait(action);}
    private static void await(java.util.function.BooleanSupplier condition,String message)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);var yes=new AtomicBoolean();
        while(System.nanoTime()<deadline){edt(()->yes.set(condition.getAsBoolean()));if(yes.get())return;Thread.sleep(20);}throw new AssertionError(message);
    }
    private static final class Harness implements AutoCloseable {
        final RiskAgeChecks.Fake llm;final UsedCarApplicationFacade facade;JPanel root;JFrame frame;
        Harness(RiskAgeChecks.Fake fake)throws Exception{llm=fake;facade=RiskAgeChecks.saved(fake);edt(()->{var gui=new UsedCarGUI(facade);root=gui.panel();frame=gui.show();select(root,"F01");ReportChecks.corolla().forEach((key,value)->((JTextField)named(root,key)).setText(value));((JButton)named(root,"calculate")).doClick();select(root,"F08");});}
        JButton evaluate(){return (JButton)named(root,"riskEvaluate");}
        String text(){return ((JTextArea)named(root,"riskResults")).getText();}
        void begin()throws Exception{edt(()->evaluate().doClick());}
        void ready()throws Exception{await(()->evaluate().isEnabled(),"F08 worker did not finish");}
        void success()throws Exception{begin();ready();edt(()->check(text().contains("AI_SUCCESS"),"Mock valid result must be visible"));}
        void stale(int calls)throws Exception{await(()->text().contains("outdated")&&!text().contains("AI_SUCCESS"),"Old AI_SUCCESS must be replaced by an outdated warning");edt(()->check(llm.riskCalls==calls&&facade.lastRiskEvaluation().isEmpty(),"Invalidation/navigation must not generate AI or retain cache"));}
        void save(String key,String value)throws Exception{edt(()->{select(root,"F01");((JTextField)named(root,key)).setText(value);((JButton)named(root,"calculate")).doClick();select(root,"F08");});}
        public void close()throws Exception{edt(frame::dispose);}
    }
    public static void price()throws Exception{try(var h=new Harness(new RiskAgeChecks.Fake())){h.success();h.save("price","14000");check(h.facade.currentVehicle().orElseThrow().askingPrice().intValue()==14000,"Real F01 save changed price");h.stale(1);}}
    public static void main(String[] args)throws Exception{
        price();if(args.length>0&&args[0].equals("price-only"))return;
        try(var h=new Harness(new RiskAgeChecks.Fake())){
            h.success();h.save("mileage","130000");h.stale(1);
            h.success();edt(()->h.facade.savePreferences(UserPreference.limits(new BigDecimal("12000"),150000)));h.stale(2);
            h.success();edt(()->h.facade.selectStrategy("budget"));h.stale(3);
            h.success();edt(()->h.facade.estimateCost(h.facade.currentVehicle().orElseThrow(),16000,1.65));h.stale(4);
            h.success();edt(()->{var c=h.facade.revisitClarification(ClarificationField.YEAR);h.facade.submitClarification(c.cycleId(),ClarificationField.YEAR,"2020");h.facade.resolveClarification(c.cycleId(),ClarificationField.YEAR,true);});h.stale(5);
        }
        try(var h=new Harness(new RiskAgeChecks.Fake())){
            h.success();var cached=h.facade.lastRiskEvaluation().orElseThrow();
            edt(()->{String original=h.text();select(h.root,"F01");select(h.root,"F08");check(h.text().equals(original),"Unchanged context/navigation preserves result");
                select(h.root,"F01");((JTextField)named(h.root,"price")).setText("14000");select(h.root,"F08");check(h.text().equals(original),"Unsaved edits preserve evaluated snapshot");
                select(h.root,"F01");((JTextField)named(h.root,"price")).setText("invalid");((JButton)named(h.root,"calculate")).doClick();select(h.root,"F08");check(h.text().equals(original),"Invalid edit preserves result");
                select(h.root,"F01");((JTextField)named(h.root,"price")).setText("12500");select(h.root,"F08");check(h.text().equals(original),"Abandoned form edit preserves result");
                var c=h.facade.revisitClarification(ClarificationField.YEAR);h.facade.submitClarification(c.cycleId(),ClarificationField.YEAR,"2020");h.facade.resolveClarification(c.cycleId(),ClarificationField.YEAR,false);
                check(h.facade.lastRiskEvaluation().orElseThrow()==cached&&h.facade.currentVehicle().orElseThrow().askingPrice().intValue()==12500&&h.llm.riskCalls==1,"Rejected clarification and failed/unsaved edits preserve saved snapshot without requests");});
            await(()->h.text().contains("AI_SUCCESS"),"Valid saved state remains visible");
        }
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        var delayed=new RiskAgeChecks.Fake(){public String generateStructured(String p,String s){check(!SwingUtilities.isEventDispatchThread(),"Provider must stay off EDT");entered.countDown();try{check(release.await(8,TimeUnit.SECONDS),"Release delayed response");}catch(InterruptedException e){throw new AssertionError(e);}return super.generateStructured(p,s);}};
        try(var h=new Harness(delayed)){h.begin();check(entered.await(8,TimeUnit.SECONDS),"Delayed request started");h.save("price","14000");release.countDown();h.ready();h.stale(1);h.success();check(delayed.riskCalls==2,"Only explicit retry requests fresh result");}finally{release.countDown();}
        // Backend has cached success, but done() cannot run until the EDT barrier is released.
        var providerEntered=new CountDownLatch(1);var providerRelease=new CountDownLatch(1);
        var edtBlocked=new CountDownLatch(1);var cacheObserved=new CountDownLatch(1);var changed=new CountDownLatch(1);
        var callbackFailure=new AtomicReference<Throwable>();var staleSuccess=new AtomicBoolean();
        var queued=new RiskAgeChecks.Fake(){public String generateStructured(String p,String schema){
            providerEntered.countDown();try{check(providerRelease.await(8,TimeUnit.SECONDS),"Provider barrier");}catch(InterruptedException e){throw new AssertionError(e);}return super.generateStructured(p,schema);
        }};
        try(var h=new Harness(queued)){
            edt(()->((JTextArea)named(h.root,"riskResults")).getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
                private void observe(){check(SwingUtilities.isEventDispatchThread(),"Render changes must run on EDT");if(h.text().contains("AI_SUCCESS"))staleSuccess.set(true);}
                public void insertUpdate(javax.swing.event.DocumentEvent e){observe();}public void removeUpdate(javax.swing.event.DocumentEvent e){observe();}public void changedUpdate(javax.swing.event.DocumentEvent e){observe();}
            }));
            h.begin();check(providerEntered.await(8,TimeUnit.SECONDS),"Queued request started");
            SwingUtilities.invokeLater(()->{
                edtBlocked.countDown();providerRelease.countDown();
                try{check(cacheObserved.await(8,TimeUnit.SECONDS),"Observe backend completion before UI delivery");
                    var form=ReportChecks.corolla();form.put("price","14000");h.facade.saveVehicle(PreparationForm.vehicle(h.facade,form));
                }catch(Throwable error){callbackFailure.set(error);}finally{changed.countDown();}
            });
            check(edtBlocked.await(8,TimeUnit.SECONDS),"EDT barrier entered");
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
            while(h.facade.lastRiskEvaluation().isEmpty()&&System.nanoTime()<deadline)Thread.sleep(10);
            boolean cached=h.facade.lastRiskEvaluation().filter(result->result.status().equals("AI_SUCCESS")).isPresent();
            cacheObserved.countDown();check(changed.await(8,TimeUnit.SECONDS)&&cached,"Backend success observed before context edit");
            if(callbackFailure.get()!=null)throw new AssertionError("EDT barrier failed",callbackFailure.get());
            h.ready();h.stale(1);check(!staleSuccess.get(),"Queued old success must never appear, even transiently");
        }finally{providerRelease.countDown();cacheObserved.countDown();}
        System.out.println("PASS: AUD-03 price/mileage/preferences/strategy/cost/F11 invalidation, return navigation, unchanged/unsaved/invalid/cancelled edits, delayed snapshot and queued EDT-result rejection, explicit recovery and no automatic requests.");
    }
}
