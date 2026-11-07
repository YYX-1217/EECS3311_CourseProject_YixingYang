package ca.yorku.eecs3311.usedcar;

import ca.yorku.eecs3311.usedcar.ui.*;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;

/** Real Swing event handlers with offline model; only this application's panel is rendered. */
public final class ChecklistGuiSmoke {
    private static Component named(Container p,String name){for(Component c:p.getComponents()){if(name.equals(c.getName()))return c;if(c instanceof Container box){var found=named(box,name);if(found!=null)return found;}}return null;}
    private static JButton button(Container p,String name){return (JButton)named(p,name);}
    private static String status(Container p){return ((JTextArea)named(p,"checklistStatus")).getText();}
    private static void selectTab(Container p){for(Component c:p.getComponents()){if(c instanceof JTabbedPane tabs){for(int i=0;i<tabs.getTabCount();i++)if(tabs.getTitleAt(i).equals("Inspection Checklist"))tabs.setSelectedIndex(i);}if(c instanceof Container box)selectTab(box);}}
    public static void main(String[] args)throws Exception{
        var started=new CountDownLatch(1);var release=new CountDownLatch(1);var sequence=new AtomicInteger();
        var llm=new ChecklistChecks.Fake(){public String generateStructured(String p,String s){
            check(!SwingUtilities.isEventDispatchThread(),"F10 provider call must run off EDT");
            if(sequence.incrementAndGet()==1){started.countDown();try{check(release.await(5,TimeUnit.SECONDS),"Worker release timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException();}}
            return super.generateStructured(p,s);
        }};
        var f=ChecklistChecks.facade(llm);var root=new AtomicReference<JPanel>();var frame=new AtomicReference<JFrame>();
        try{
            SwingUtilities.invokeAndWait(()->{
                var gui=new UsedCarGUI(f);root.set(gui.panel());frame.set(gui.show());selectTab(root.get());
                check(frame.get().isShowing(),"Actual GUI must launch");button(root.get(),"checklistGenerate").doClick();
                check(status(root.get()).contains("Save a vehicle"),"Missing vehicle must be actionable");
                f.saveVehicle(PreparationForm.vehicle(f,PreparationForm.example()));button(root.get(),"checklistGenerate").doClick();
            });
            check(started.await(5,TimeUnit.SECONDS),"Checklist worker did not start");
            SwingUtilities.invokeAndWait(()->{
                check(!button(root.get(),"checklistGenerate").isEnabled(),"Generation button must disable without blocking EDT");
                var changed=PreparationForm.example();changed.put("mileage","90000");f.saveVehicle(PreparationForm.vehicle(f,changed));
            });
            release.countDown();ready(root.get());
            SwingUtilities.invokeAndWait(()->{
                check(f.currentChecklist().isEmpty()&&status(root.get()).contains("could not be saved"),"Changed vehicle must not receive old checklist");
                button(root.get(),"checklistGenerate").doClick();
            });
            ready(root.get());
            SwingUtilities.invokeAndWait(()->{
                check(status(root.get()).contains("AI_SUCCESS"),"GUI must display successful checklist status");
                var item=(JCheckBox)named(root.get(),"checklistItem-maintenance-records");check(item!=null,"Grouped priority items must be rendered");item.doClick();
                check(f.currentChecklist().orElseThrow().completed().contains("maintenance-records"),"Checkbox must update shared completion");
                int requests=llm.calls;button(root.get(),"checklistShow").doClick();
                check(((JCheckBox)named(root.get(),"checklistItem-maintenance-records")).isSelected()&&llm.calls==requests,"Reload retains completion without AI");
                ((JCheckBox)named(root.get(),"checklistItem-maintenance-records")).doClick();
                check(f.currentChecklist().orElseThrow().completed().isEmpty()&&llm.calls==requests,"Unmark is local");
                llm.fail=true;button(root.get(),"checklistGenerate").doClick();
            });
            ready(root.get());
            SwingUtilities.invokeAndWait(()->{
                check(status(root.get()).contains("FALLBACK")&&named(root.get(),"checklistItem-accident-disclosures")!=null,"Provider failure retains practical grouped checklist and restores controls");
                ((JCheckBox)named(root.get(),"checklistItem-documents")).doClick();check(f.currentChecklist().orElseThrow().completed().contains("documents"),"Fallback completion also works");
                try{
                    var path=java.nio.file.Path.of("build","verification","f10-checklist.png");java.nio.file.Files.createDirectories(path.getParent());
                    var image=new java.awt.image.BufferedImage(root.get().getWidth(),root.get().getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                    var graphics=image.createGraphics();root.get().paintAll(graphics);graphics.dispose();javax.imageio.ImageIO.write(image,"png",path.toFile());
                }catch(java.io.IOException e){throw new IllegalStateException(e);}
            });
            System.out.println("PASS: F10 actual GUI launch, grouped items, background generation, stale rejection, local check/uncheck/reload and usable fallback.");
        }finally{release.countDown();if(frame.get()!=null)SwingUtilities.invokeAndWait(()->frame.get().dispose());}
    }
    private static void ready(Container p)throws Exception{
        var ready=new AtomicBoolean();long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<end){SwingUtilities.invokeAndWait(()->ready.set(button(p,"checklistGenerate").isEnabled()));if(ready.get())return;Thread.sleep(30);}throw new AssertionError("GUI did not finish");
    }
}
