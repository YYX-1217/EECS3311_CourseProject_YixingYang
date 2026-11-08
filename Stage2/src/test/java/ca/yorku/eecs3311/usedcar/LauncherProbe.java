package ca.yorku.eecs3311.usedcar;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.swing.*;
import java.awt.*;
import ca.yorku.eecs3311.usedcar.app.ApplicationFactory;
import ca.yorku.eecs3311.usedcar.ui.UsedCarGUI;
/** Offline child process; only fixed success markers are printed. */
public final class LauncherProbe {
 private static Component named(Container root,String name){for(var c:root.getComponents()){if(name.equals(c.getName()))return c;if(c instanceof Container box){var found=named(box,name);if(found!=null)return found;}}return null;}
 public static void main(String[] args)throws Exception{
  String key=System.getenv("GEMINI_API_KEY");
  String actual=key==null||key.isEmpty()?"NONE":HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)));
  if(!actual.equals(System.getenv("STAGE2_TEST_EXPECTED_HASH")))throw new IllegalStateException("Child credential inheritance mismatch.");
  String model=System.getenv("STAGE2_TEST_EXPECTED_MODEL");
  if(model!=null&&!model.equals(System.getenv("GEMINI_MODEL")))throw new IllegalStateException("Model inheritance mismatch.");
  if(args.length>0&&args[0].equals("--inspect-gui"))SwingUtilities.invokeAndWait(()->{
   var gui=new UsedCarGUI(ApplicationFactory.create());var frame=gui.show();
   try{String text=((JTextArea)named(gui.panel(),"results")).getText();if(!text.contains("CONFIGURED: not live-verified")||text.contains(key))throw new IllegalStateException("GUI initial status mismatch.");}finally{frame.dispose();}
  });
  Thread.sleep(700);
  if("FAIL".equals(System.getenv("STAGE2_TEST_CHILD_MODE")))System.exit(17);
  System.out.println("PROBE_OK");
 }
}
