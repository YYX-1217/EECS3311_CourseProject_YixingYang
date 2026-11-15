package ca.yorku.eecs3311.usedcar;

import ca.yorku.eecs3311.usedcar.ui.*;
import ca.yorku.eecs3311.usedcar.llm.LLMClient;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;

/** Actual workspace, actual listeners, fictional input. Navigation must not make provider calls. */
public final class WorkspaceGuiSmoke {
    private static final String[] SECTIONS={"F01","F02","F03","F04","F05","F06","F07","F08","F09","F10","F11","F12"};
    public static Component named(Container root,String name){
        for(Component c:root.getComponents()){if(name.equals(c.getName()))return c;if(c instanceof Container box){var found=named(box,name);if(found!=null)return found;}}return null;
    }
    public static void select(Container root,String section){
        section = switch(section) {
            case "Recommendation / Report" -> "F12"; case "Clarification" -> "F11";
            case "Inspection Checklist" -> "F10"; case "Comparison" -> "F09";
            case "Risk Assessment" -> "F08"; case "Listing Analyzer" -> "F02"; default -> section;
        };
        var button=(JButton)named(root,"nav-"+section);check(button!=null,"Sidebar must contain "+section);button.doClick();
    }
    private static void capture(JPanel root,String file)throws Exception{
        var image=new BufferedImage(root.getWidth(),root.getHeight(),BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();root.printAll(g);g.dispose();
        var path=Path.of("build","verification","workspace",file);Files.createDirectories(path.getParent());javax.imageio.ImageIO.write(image,"png",path.toFile());
    }
    private static void layoutTree(Container container){container.doLayout();for(Component c:container.getComponents())if(c instanceof Container child)layoutTree(child);}
    private static void offscreen1920(ca.yorku.eecs3311.usedcar.app.UsedCarApplicationFacade facade)throws Exception{
        var view=new java.util.concurrent.atomic.AtomicReference<JPanel>();
        SwingUtilities.invokeAndWait(()->{var panel=new UsedCarGUI(facade).panel();panel.setSize(1920,1080);view.set(panel);layoutTree(panel);});flush();
        for(int i=0;i<SECTIONS.length;i++){
            final int index=i;
            SwingUtilities.invokeAndWait(()->{select(view.get(),SECTIONS[index]);layoutTree(view.get());});flush();
            SwingUtilities.invokeAndWait(()->{
                var root=view.get();layoutTree(root);
                check(root.getWidth()==1920&&root.getHeight()==1080,"Exact offscreen 1920x1080 layout");
                var split=(JSplitPane)named(root,"workspaceSplit");split.setDividerLocation(0.46);layoutTree(root);
                check(split.getOrientation()==JSplitPane.HORIZONTAL_SPLIT,"Wide layout retains three columns");
                try{capture(root,"1920-"+(index+1)+"-offscreen.png");}catch(Exception e){throw new RuntimeException(e);}
            });
        }
    }
    private static void flush() throws Exception { SwingUtilities.invokeAndWait(()->{});SwingUtilities.invokeAndWait(()->{}); }
    public static void main(String[] args)throws Exception{
        var calls=new AtomicInteger();
        var client=new LLMClient(){public String status(){return "NOT CONFIGURED: offline UI verification";}public String generate(String p){calls.incrementAndGet();throw new IllegalStateException("Offline UI verification");}};
        var facade=RiskChecks.facade(client);var ui=new java.util.concurrent.atomic.AtomicReference<UsedCarGUI>();var window=new java.util.concurrent.atomic.AtomicReference<JFrame>();
        SwingUtilities.invokeAndWait(()->{var gui=new UsedCarGUI(facade);ui.set(gui);window.set(gui.show());});flush();
        var frame=window.get();var root=ui.get().panel();
        try{
            SwingUtilities.invokeAndWait(()->{
                check(facade.currentVehicle().isEmpty(),"Blank startup cannot create a vehicle");
                check(((JTextField)named(root,"year")).getText().isEmpty(),"Blank startup cannot load an example");
                check(((JTextArea)named(root,"reportResults")).getText().contains("No current report"),"No fabricated AI result");
                ((JTextField)named(root,"notes")).setText("Unsaved note preserved through navigation");
                ((JTextField)named(root,"comparison_make")).setText("Unsaved comparison input");
                ((JTextField)named(root,"clarificationAnswer")).setText("Unsaved answer");
                ((JTextArea)named(root,"listingText")).setText("Unsaved seller listing");
            });
            for(var size:new Dimension[]{new Dimension(1366,768),new Dimension(920,620)}){
                SwingUtilities.invokeAndWait(()->frame.setSize(size));flush();
                for(int i=0;i<SECTIONS.length;i++){
                    final int index=i;
                    SwingUtilities.invokeAndWait(()->select(root,SECTIONS[index]));flush();
                    SwingUtilities.invokeAndWait(()->{
                        check(((JButton)named(root,"nav-"+SECTIONS[index])).isSelected(),"Active navigation state");
                        check(((JButton)named(root,"nav-"+SECTIONS[index])).isShowing(),"Navigation remains accessible");
                        check(((JTextField)named(root,"notes")).getText().equals("Unsaved note preserved through navigation"),"Navigation cannot lose unsaved fields");
                        check(((JTextField)named(root,"comparison_make")).getText().equals("Unsaved comparison input"),"Comparison edits survive navigation");
                        check(((JTextField)named(root,"clarificationAnswer")).getText().equals("Unsaved answer"),"Clarification answer input survives navigation");
                        check(((JTextArea)named(root,"listingText")).getText().equals("Unsaved seller listing"),"Listing input survives navigation");
                        check(facade.currentVehicle().isEmpty()&&calls.get()==0,"Navigation cannot save, calculate or request AI");
                        check(frame.getSize().equals(size),"Requested window size must actually be applied");
                        var split=(JSplitPane)named(root,"workspaceSplit");
                        check(split.getOrientation()==(size.width<1100?JSplitPane.VERTICAL_SPLIT:JSplitPane.HORIZONTAL_SPLIT),"Responsive orientation must follow actual resize events");
                        var nav=(JScrollPane)named(root,"featureNavigation");
                        check(nav.getWidth()>=300&&nav.getWidth()<=350,"Sidebar has practical readable width");
                        check(nav.getVerticalScrollBar().isVisible(),"Twelve features remain scrollable");
                        var button=(JButton)named(root,"nav-"+SECTIONS[index]);
                        check(button.getVisibleRect().height==button.getHeight(),"Selected navigation row scrolls fully into view");
                        check(button.getAccessibleContext().getAccessibleName().equals(
                                WorkspaceUI.FEATURES[index][0]+" — "+WorkspaceUI.FEATURES[index][1]),"Exact feature identity");
                        try{capture(root,size.width+"-"+(index+1)+"-blank.png");}catch(Exception e){throw new RuntimeException(e);}
                    });
                }
            }
            offscreen1920(facade);
            SwingUtilities.invokeAndWait(()->{select(root,SECTIONS[0]);frame.setSize(1366,768);});flush();
            SwingUtilities.invokeAndWait(()->{
                ((JButton)named(root,"example")).doClick();check(facade.currentVehicle().isEmpty(),"Example still requires explicit save");
                ((JButton)named(root,"calculate")).doClick();
                check(((JTextArea)named(root,"results")).getText().contains("24922.00"),"Existing deterministic output preserved");
                var saved=facade.currentVehicle();((JTextField)named(root,"price")).setText("not-a-number");
                ((JButton)named(root,"calculate")).doClick();check(saved.equals(facade.currentVehicle()),"Invalid numeric form must preserve saved vehicle");
                ((JTextField)named(root,"price")).setText(PreparationForm.example().get("price"));
                ((JButton)named(root,"calculate")).doClick();
                var current=facade.currentVehicle();
                select(root,"F04"); ((JButton)named(root,"qualityCheck")).doClick();
                check(((JTextArea)named(root,"results")).getText().equals(
                        ResultFormatter.format(facade.checkMissingInformation(current.orElseThrow()))),"F04 uses actual saved data-quality service");
                String quality=((JTextArea)named(root,"results")).getText();
                select(root,"F07"); ((JButton)named(root,"constraintCheck")).doClick();
                check(((JTextArea)named(root,"results")).getText().contains("unspecified preferences are skipped")
                        || ((JTextArea)named(root,"results")).getText().contains("expected="),"F07 displays actual checker outcomes");
                select(root,"F04"); check(((JTextArea)named(root,"results")).getText().equals(quality),"Feature result snapshots survive navigation");
                select(root,"F06"); check(((JTextArea)named(root,"results")).getText().contains("24922.00"),"F06 retains real cost breakdown");
                check(current.equals(facade.currentVehicle()),"Read-only feature actions preserve saved vehicle");
                ((JButton)named(root,"costCalculate")).doClick();
                check(((JTextArea)named(root,"results")).getText().contains("24922.00"),"Relocated F06 action uses existing preparation command");
            });flush();
            SwingUtilities.invokeAndWait(()->{try{capture(root,"1366-calculated.png");}catch(Exception e){throw new RuntimeException(e);}check(calls.get()==0,"Layout checks never call Gemini");});
            System.out.println("PASS: Workspace twelve-section navigation, blank startup, real calculations, validation, saved/unsaved state, actual 1366x768 / 920x620 windows and exact 1920x1080 offscreen layout; no AI requests.");
        }finally{SwingUtilities.invokeAndWait(frame::dispose);}
    }
}
