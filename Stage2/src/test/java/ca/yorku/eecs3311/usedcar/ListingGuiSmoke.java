package ca.yorku.eecs3311.usedcar;
import ca.yorku.eecs3311.usedcar.ui.ListingAnalyzerPanel;
import ca.yorku.eecs3311.usedcar.llm.LLMClient;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;

public final class ListingGuiSmoke {
    private static Component named(Container root,String name){for(Component c:root.getComponents()){if(name.equals(c.getName()))return c;if(c instanceof Container box){var found=named(box,name);if(found!=null)return found;}}return null;}
    public static void main(String[] args)throws Exception {
        var started=new CountDownLatch(1);var release=new CountDownLatch(1);var calls=new AtomicInteger();var cancelStarted=new CountDownLatch(1);var cancelRelease=new CountDownLatch(1);
        LLMClient client=new LLMClient(){public String status(){return "OFFLINE";}public String generate(String prompt){
            check(!SwingUtilities.isEventDispatchThread(),"Network work must not run on EDT");
            int call=calls.incrementAndGet();
            if(call==3){cancelStarted.countDown();try{cancelRelease.await(5,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("cancelled");}}
            if(call==1){started.countDown();try{if(!release.await(5,TimeUnit.SECONDS))throw new AssertionError("Release timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("interrupted");}}
            return ListingChecks.fixture().toString();}};
        var facade=ListingChecks.facade(client);var review=new AtomicReference<String>();var panel=new AtomicReference<ListingAnalyzerPanel>();var frame=new AtomicReference<JFrame>();
        SwingUtilities.invokeAndWait(()->{var p=new ListingAnalyzerPanel(facade,message->{review.set(message);return true;});panel.set(p);var f=new JFrame();frame.set(f);f.setContentPane(p);f.pack();f.setVisible(true);((JTextArea)named(p,"listingText")).setText(ListingChecks.SOURCE);((JButton)named(p,"listingAnalyze")).doClick();});
        try{
            check(started.await(5,TimeUnit.SECONDS),"Background analysis not started");
            SwingUtilities.invokeAndWait(()->check(!((JButton)named(panel.get(),"listingAnalyze")).isEnabled()&&facade.currentVehicle().isEmpty(),"EDT must remain responsive without saving"));
            release.countDown();awaitReady(panel.get());
            SwingUtilities.invokeAndWait(()->{
                check(facade.currentVehicle().isEmpty(),"GUI analysis cannot save");
                ((JTextField)named(panel.get(),"listing-year")).setText("-1");((JButton)named(panel.get(),"listingSave")).doClick();
                check(facade.currentVehicle().isEmpty(),"Invalid GUI corrections cannot save");
                ((JTextField)named(panel.get(),"listing-year")).setText("2021");((JButton)named(panel.get(),"listingSave")).doClick();
                check(facade.currentVehicle().orElseThrow().year()==2021&&review.get().contains("2021"),"GUI must review and save corrected candidate");
                ((JButton)named(panel.get(),"listingAnalyze")).doClick();
            });awaitReady(panel.get());
            SwingUtilities.invokeAndWait(()->{((JButton)named(panel.get(),"listingCancel")).doClick();check(facade.currentVehicle().orElseThrow().year()==2021&&!((JButton)named(panel.get(),"listingSave")).isEnabled(),"Cancellation must preserve saved vehicle");});
            SwingUtilities.invokeAndWait(()->((JButton)named(panel.get(),"listingAnalyze")).doClick());
            check(cancelStarted.await(5,TimeUnit.SECONDS),"Pending cancellation request did not start");
            SwingUtilities.invokeAndWait(()->((JButton)named(panel.get(),"listingCancel")).doClick());cancelRelease.countDown();
            SwingUtilities.invokeAndWait(()->check(facade.currentVehicle().orElseThrow().year()==2021
                    &&!((JButton)named(panel.get(),"listingSave")).isEnabled(),"Cancelled background result must not save or enable confirmation"));
            System.out.println("PASS: F02 GUI background analysis, responsive EDT, draft-only results, invalid/corrected confirmation and cancellation.");
        }finally{release.countDown();cancelRelease.countDown();SwingUtilities.invokeAndWait(()->frame.get().dispose());}
    }
    private static void awaitReady(ListingAnalyzerPanel panel)throws Exception{
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);var ready=new AtomicBoolean();
        while(System.nanoTime()<end){SwingUtilities.invokeAndWait(()->ready.set(((JButton)named(panel,"listingSave")).isEnabled()));if(ready.get())return;Thread.sleep(30);}
        throw new AssertionError("GUI draft did not become ready");
    }
}
