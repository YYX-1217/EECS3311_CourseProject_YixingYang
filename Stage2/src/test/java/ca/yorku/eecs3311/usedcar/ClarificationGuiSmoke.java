package ca.yorku.eecs3311.usedcar;

import ca.yorku.eecs3311.usedcar.ui.*;
import ca.yorku.eecs3311.usedcar.domain.ClarificationField;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;

/** Actual integrated Swing frame and event listeners, with fictional input and an offline provider. */
public final class ClarificationGuiSmoke {
    private static Component named(Container p,String n){for(Component c:p.getComponents()){if(n.equals(c.getName()))return c;if(c instanceof Container box){var found=named(box,n);if(found!=null)return found;}}return null;}
    private static JButton button(Container p,String n){return (JButton)named(p,n);}
    private static String text(Container p){return ((JTextArea)named(p,"clarificationStatus")).getText();}
    private static void select(Container p){for(Component c:p.getComponents()){if(c instanceof JTabbedPane tabs){for(int i=0;i<tabs.getTabCount();i++)if(tabs.getTitleAt(i).equals("Clarification"))tabs.setSelectedIndex(i);}if(c instanceof Container box)select(box);}}
    private static void target(Container p,String field){((JComboBox<?>)named(p,"clarificationField")).setSelectedItem(field);}
    private static void answer(Container p,String value){((JTextField)named(p,"clarificationAnswer")).setText(value);button(p,"clarificationSubmit").doClick();}
    private static void ready(Container p)throws Exception{var done=new AtomicBoolean();long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(6);while(System.nanoTime()<end){SwingUtilities.invokeAndWait(()->done.set(button(p,"clarificationStart").isEnabled()));if(done.get())return;Thread.sleep(25);}throw new AssertionError("F11 worker timeout");}
    public static void main(String[] args)throws Exception{
        var started=new CountDownLatch(1);var release=new CountDownLatch(1);var count=new AtomicInteger();
        var llm=new ClarificationChecks.Fake(){public String generateStructured(String p,String s){check(!SwingUtilities.isEventDispatchThread(),"F11 network must run off EDT");if(count.incrementAndGet()==1){started.countDown();try{check(release.await(5,TimeUnit.SECONDS),"Worker release timeout");}catch(InterruptedException e){throw new IllegalStateException(e);}}return super.generateStructured(p,s);}};llm.fail=true;
        var f=RiskChecks.facade(llm);var root=new AtomicReference<JPanel>();var frame=new AtomicReference<JFrame>();
        try{
            SwingUtilities.invokeAndWait(()->{var gui=new UsedCarGUI(f);root.set(gui.panel());frame.set(gui.show());select(root.get());check(frame.get().isShowing(),"Actual GUI must launch");button(root.get(),"clarificationStart").doClick();});ready(root.get());
            SwingUtilities.invokeAndWait(()->{check(text(root.get()).contains("Save a vehicle"),"No-vehicle error must be helpful");ClarificationChecks.corolla().forEach((key,value)->((JTextField)named(root.get(),key)).setText(value));button(root.get(),"calculate").doClick();check(f.currentVehicle().orElseThrow().make().equals("Toyota"),"Save fictional Corolla through existing GUI");button(root.get(),"clarificationStart").doClick();});
            check(started.await(5,TimeUnit.SECONDS),"Worker did not start");SwingUtilities.invokeAndWait(()->check(!button(root.get(),"clarificationStart").isEnabled(),"GUI stays responsive and disables repeated requests"));release.countDown();ready(root.get());
            SwingUtilities.invokeAndWait(()->{
                check(text(root.get()).contains("FALLBACK")&&text(root.get()).contains("accident"),"Useful fallback questions must appear");
                target(root.get(),"accident");answer(root.get(),"probably");check(text(root.get()).contains("ERROR:"),"Invalid enum shown without saving");button(root.get(),"clarificationShow").doClick();target(root.get(),"accident");answer(root.get(),"NO");
                target(root.get(),"maintenance");button(root.get(),"clarificationUnknown").doClick();target(root.get(),"consumption");button(root.get(),"clarificationSkip").doClick();
                check(f.currentVehicle().orElseThrow().accidentHistory().equals("NO")&&f.currentVehicle().orElseThrow().maintenanceHistory().equals("UNKNOWN"),"Answer changes only target and unknown remains unknown");
                check(((JTextField)named(root.get(),"accident")).getText().equals("NO"),"Vehicle form refreshes clarified value");
                int calls=llm.calls;button(root.get(),"clarificationStart").doClick();check(calls==1,"Exactly one generation so far");
            });ready(root.get());
            SwingUtilities.invokeAndWait(()->{check(llm.calls==1&&text(root.get()).contains("maintenance -> UNKNOWN"),"Reopen retains history without another call");button(root.get(),"clarificationRisk").doClick();});ready(root.get());
            SwingUtilities.invokeAndWait(()->{
                check(text(root.get()).contains("INSUFFICIENT_INFORMATION")&&llm.calls==1,"Continue executes real sufficiency gate without invented history");
                ((JComboBox<?>)named(root.get(),"clarificationRevisitField")).setSelectedItem(ClarificationField.MILEAGE);button(root.get(),"clarificationRevisit").doClick();target(root.get(),"mileage");answer(root.get(),"130000");
                check(text(root.get()).contains("existing=125000")&&text(root.get()).contains("proposed=130000")&&f.currentVehicle().orElseThrow().mileage()==125000,"Visible conflict must preserve old value");button(root.get(),"clarificationKeep").doClick();
                button(root.get(),"clarificationRevisit").doClick();answer(root.get(),"130000");button(root.get(),"clarificationAccept").doClick();check(f.currentVehicle().orElseThrow().mileage()==130000,"Accept explicitly applies value");
                button(root.get(),"clarificationChecklist").doClick();
            });ready(root.get());
            SwingUtilities.invokeAndWait(()->{
                check(text(root.get()).contains("F10 INSPECTION CHECKLIST")&&f.currentChecklist().orElseThrow().vehicle().mileage()==130000,"Continue Checklist uses fresh evidence");
                button(root.get(),"clarificationShow").doClick();
                try{var path=java.nio.file.Path.of("build","verification","f11-clarification.png");java.nio.file.Files.createDirectories(path.getParent());var image=new java.awt.image.BufferedImage(root.get().getWidth(),root.get().getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();root.get().paintAll(g);g.dispose();javax.imageio.ImageIO.write(image,"png",path.toFile());}catch(java.io.IOException e){throw new IllegalStateException(e);}
                var before=f.currentClarification().orElseThrow();var v=ClarificationChecks.corolla();v.put("mileage","140000");f.saveVehicle(PreparationForm.vehicle(f,v));button(root.get(),"clarificationAccept").doClick();check(text(root.get()).contains("ERROR:")&&f.currentVehicle().orElseThrow().mileage()==140000,"Old UI token cannot alter changed vehicle");
            });
            System.out.println("PASS: F11 actual Swing launch, fictional Corolla entry, background fallback, answer/skip/unknown/history, cached reopen, keep/accept, real F08/F10 continuation and stale-response rejection.");
        }finally{release.countDown();if(frame.get()!=null)SwingUtilities.invokeAndWait(()->frame.get().dispose());}
    }
}
