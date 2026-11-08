package ca.yorku.eecs3311.usedcar;

import ca.yorku.eecs3311.usedcar.ui.*;
import ca.yorku.eecs3311.usedcar.domain.*;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.nio.file.*;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;

/** Integrated real Swing frame, offline provider, real button handlers and shared export action. */
public final class ReportGuiSmoke {
    private static Component named(Container p,String name){for(Component c:p.getComponents()){if(name.equals(c.getName()))return c;if(c instanceof Container box){var found=named(box,name);if(found!=null)return found;}}return null;}
    private static JButton button(Container p,String name){return (JButton)named(p,name);}
    private static String text(Container p){return ((JTextArea)named(p,"reportResults")).getText();}
    private static ReportPanel reportPanel(Container p){if(p instanceof ReportPanel report)return report;for(Component c:p.getComponents())if(c instanceof Container box){var found=reportPanel(box);if(found!=null)return found;}return null;}
    private static void select(Container p){for(Component c:p.getComponents()){if(c instanceof JTabbedPane tabs){for(int i=0;i<tabs.getTabCount();i++)if(tabs.getTitleAt(i).equals("Recommendation / Report"))tabs.setSelectedIndex(i);}if(c instanceof Container box)select(box);}}
    private static void ready(Container p)throws Exception{var ready=new AtomicBoolean();long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);while(System.nanoTime()<end){SwingUtilities.invokeAndWait(()->ready.set(button(p,"reportGenerate").isEnabled()));if(ready.get())return;Thread.sleep(30);}throw new AssertionError("F12 Swing worker timeout");}
    public static void main(String[] args)throws Exception{
        var started=new CountDownLatch(1);var release=new CountDownLatch(1);var count=new AtomicInteger();
        var llm=new ReportChecks.Fake(){public String generateStructured(String p,String s){if(p.startsWith("Generate an advisory")){
            check(!SwingUtilities.isEventDispatchThread(),"Report network must run off EDT");if(count.incrementAndGet()==1){started.countDown();try{check(release.await(5,TimeUnit.SECONDS),"Worker release timeout");}catch(InterruptedException e){throw new IllegalStateException(e);}}
        }return super.generateStructured(p,s);}};
        var f=RiskChecks.facade(llm);var root=new AtomicReference<JPanel>();var frame=new AtomicReference<JFrame>();
        Files.createDirectories(Path.of("build","verification"));var export=Files.createTempDirectory(Path.of("build","verification"),"f12-gui-").resolve("Report Review.txt");
        try{
            SwingUtilities.invokeAndWait(()->{var gui=new UsedCarGUI(f);root.set(gui.panel());frame.set(gui.show());select(root.get());check(frame.get().isShowing(),"Actual report tab launches");button(root.get(),"reportGenerate").doClick();check(text(root.get()).contains("Save a vehicle"),"No-vehicle error is actionable");
                ReportChecks.corolla().forEach((key,value)->((JTextField)named(root.get(),key)).setText(value));button(root.get(),"calculate").doClick();check(f.currentVehicle().orElseThrow().make().equals("Toyota"),"Existing GUI saves fictional Corolla");button(root.get(),"reportGenerate").doClick();});
            check(started.await(5,TimeUnit.SECONDS),"Report worker started");SwingUtilities.invokeAndWait(()->{check(!button(root.get(),"reportGenerate").isEnabled(),"Repeated generation disabled while EDT remains responsive");f.savePreferences(UserPreference.limits(null,null));});release.countDown();ready(root.get());
            SwingUtilities.invokeAndWait(()->{check(f.currentReport().isEmpty()&&text(root.get()).contains("could not be saved"),"Late report rejected after preference change");f.savePreferences(PreparationForm.preferences(ReportChecks.corolla()));button(root.get(),"reportGenerate").doClick();});ready(root.get());
            SwingUtilities.invokeAndWait(()->{
                check(text(root.get()).contains("AI_SUCCESS")&&text(root.get()).contains("18707.00")&&text(root.get()).contains("A. DETERMINISTIC")&&text(root.get()).contains("B. GEMINI"),"Report displays category, sections and exact costs with separated responsibilities");
                int calls=llm.reportCalls;button(root.get(),"reportShow").doClick();check(llm.reportCalls==calls,"Show is cached");reportPanel(root.get()).exportTo(export);check(text(root.get()).contains("Exported UTF-8"),"GUI export uses shared service");reportPanel(root.get()).exportTo(export);check(text(root.get()).contains("already exists"),"GUI rejects silent overwrite");
                var s=f.revisitClarification(ClarificationField.MILEAGE);f.submitClarification(s.cycleId(),ClarificationField.MILEAGE,"130000");f.resolveClarification(s.cycleId(),ClarificationField.MILEAGE,true);button(root.get(),"reportShow").doClick();check(text(root.get()).contains("No current report"),"Accepted F11 answer invalidates report view");button(root.get(),"reportGenerate").doClick();
            });ready(root.get());
            SwingUtilities.invokeAndWait(()->{check(text(root.get()).contains("Mileage (km): 130000"),"New report uses accepted F11 value");llm.fail=true;button(root.get(),"reportGenerate").doClick();});ready(root.get());
            SwingUtilities.invokeAndWait(()->{check(text(root.get()).contains("AI_UNAVAILABLE")&&text(root.get()).contains("[REQUEST_FAILURE]")&&!text(root.get()).contains("fictional-private-provider-error")&&text(root.get()).contains("18707.00"),"Provider failure retains deterministic evidence");
                var v=ReportChecks.corolla();v.put("maintenance","UNKNOWN");f.saveVehicle(PreparationForm.vehicle(f,v));button(root.get(),"reportGenerate").doClick();});ready(root.get());
            SwingUtilities.invokeAndWait(()->{check(text(root.get()).contains("INSUFFICIENT_INFORMATION"),"Unknown essential evidence remains insufficient");
                try{var image=new java.awt.image.BufferedImage(root.get().getWidth(),root.get().getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();root.get().paintAll(g);g.dispose();javax.imageio.ImageIO.write(image,"png",Path.of("build","verification","f12-report.png").toFile());}catch(java.io.IOException e){throw new IllegalStateException(e);}});
            check(Files.readString(export).contains("18707.00"),"Export retains exact report text");
            System.out.println("PASS: F12 actual Swing report tab, fictional Corolla, background generation, stale rejection, cached show, UTF-8/no-overwrite export, F11 update, AI unavailable and insufficient evidence.");
        }finally{release.countDown();if(frame.get()!=null)SwingUtilities.invokeAndWait(()->frame.get().dispose());}
    }
}
