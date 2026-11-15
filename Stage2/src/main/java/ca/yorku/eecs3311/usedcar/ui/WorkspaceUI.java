package ca.yorku.eecs3311.usedcar.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.*;
import java.awt.*;
import java.awt.event.*;

/** Presentation only: shared workspace surfaces, responsive forms and accessible native controls. */
public final class WorkspaceUI {
    public static final Color BACKGROUND = Color.decode("#F6F7F9"), SURFACE = Color.WHITE;
    public static final Color INK = Color.decode("#172235"), MUTED = Color.decode("#687385");
    public static final Color ACCENT = Color.decode("#EF5B52"), LINE = Color.decode("#E6E9EF");
    public static final Font FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private WorkspaceUI() {}
    public static final String[][] FEATURES = {
        {"F01", "Vehicle Information Entry and Management"},
        {"F02", "Used-Car Listing Text Analysis"},
        {"F03", "Buyer Preference Profile"},
        {"F04", "Missing Information and Data-Quality Detection"},
        {"F05", "Local Vehicle Knowledge Retrieval"},
        {"F06", "Ownership Cost Estimation"},
        {"F07", "Buyer Constraint Conflict Detection"},
        {"F08", "AI-Assisted Vehicle Risk Assessment"},
        {"F09", "Two-Vehicle Comparison"},
        {"F10", "Vehicle-Specific Inspection Checklist Generation"},
        {"F11", "Clarification Question and Follow-Up Management"},
        {"F12", "Personalized Purchase Recommendation and Evaluation Report"}
    };
    public static JButton navigationButton(String id, String name) {
        var button = new JButton("<html><b>" + id + "</b><br><span style='font-weight:normal'>" + escape(name) + "</span></html>");
        button.setName("nav-" + id); button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setPreferredSize(new Dimension(300, 82));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        button.setMinimumSize(new Dimension(200, 82));
        button.getAccessibleContext().setAccessibleName(id + " — " + name);
        button.getAccessibleContext().setAccessibleDescription("Open feature; navigation preserves unsaved inputs and makes no AI request.");
        button.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { button.scrollRectToVisible(new Rectangle(button.getSize())); }
        });
        return button;
    }


    public static JPanel column(JComponent... children) {
        var panel = new JPanel(); panel.setOpaque(false); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (var child : children) {
            child.setAlignmentX(Component.LEFT_ALIGNMENT); panel.add(child); panel.add(Box.createVerticalStrut(16));
        }
        return panel;
    }
    public static JPanel actions(Component... children) {
        var panel = new JPanel(new WrapLayout()); panel.setOpaque(false);
        for (var child : children) panel.add(child);
        return panel;
    }
    public static JButton primary(JButton button) { button.putClientProperty("workspace.primary", true); return button; }
    public static JPanel card(String title, JComponent body) {
        var panel = new RoundedCard(); panel.setLayout(new BorderLayout(0, 12));
        var label = new StatusLabel(title); label.setFont(FONT.deriveFont(Font.BOLD, 15)); label.setForeground(INK);
        panel.add(label, BorderLayout.NORTH); panel.add(body); return panel;
    }
    public static JScrollPane scroll(Component content) {
        if (content instanceof JComponent c) c.setMinimumSize(new Dimension(0, 0));
        var pane = new JScrollPane(content); pane.setBorder(BorderFactory.createEmptyBorder());
        pane.getViewport().setBackground(SURFACE); pane.getVerticalScrollBar().setUnitIncrement(20);
        pane.setMinimumSize(new Dimension(0, 0)); pane.setPreferredSize(new Dimension(360, 260)); return pane;
    }
    public static JScrollPane columnScroll(JComponent content) {
        var track = new TrackingPanel(content);
        var scroll = scroll(track); scroll.getViewport().setBackground(BACKGROUND);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); return scroll;
    }
    /** Nested result cards retain readable height in the narrow, stacked workspace. */
    public static JScrollPane resultViewport(JComponent content) {
        var panel=new ResultViewportPanel(content);var pane=scroll(panel);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);pane.getViewport().setBackground(BACKGROUND);
        return pane;
    }
    private static final class ResultViewportPanel extends JPanel implements Scrollable {
        ResultViewportPanel(JComponent content){super(new BorderLayout());setOpaque(false);add(content);}
        @Override public Dimension getPreferredSize(){return new Dimension(450,720);}
        public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
        public int getScrollableUnitIncrement(Rectangle r,int o,int d){return 20;}
        public int getScrollableBlockIncrement(Rectangle r,int o,int d){return Math.max(20,r.height-32);}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return getParent()!=null&&getParent().getHeight()>=420;}
    }
    public static JPanel resultCard(String title, JTextArea text) {
        text.setColumns(0); text.setRows(0); text.setLineWrap(true); text.setWrapStyleWord(true);
        return card(title, scroll(text));
    }
    public static JSplitPane split(int orientation, Component first, Component second, double weight) {
        var split = new JSplitPane(orientation, first, second); split.setBorder(BorderFactory.createEmptyBorder());
        split.setOpaque(false); split.setContinuousLayout(true); split.setDividerSize(12); split.setResizeWeight(weight);
        first.setMinimumSize(new Dimension(0, 0)); second.setMinimumSize(new Dimension(0, 0));
        split.setUI(new BasicSplitPaneUI(){
            @Override public BasicSplitPaneDivider createDefaultDivider(){
                return new BasicSplitPaneDivider(this){
                    @Override public void paint(Graphics g){
                        g.setColor(BACKGROUND);g.fillRect(0,0,getWidth(),getHeight());g.setColor(LINE);
                        if(orientation==JSplitPane.HORIZONTAL_SPLIT)g.drawLine(getWidth()/2,8,getWidth()/2,getHeight()-8);
                        else g.drawLine(8,getHeight()/2,getWidth()-8,getHeight()/2);
                    }
                };
            }
        });
        split.addHierarchyListener(e -> { if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && split.isShowing()) SwingUtilities.invokeLater(() -> split.setDividerLocation(weight)); });
        return split;
    }
    public static JSplitPane adaptive(Component first, Component second) {
        var split = split(JSplitPane.HORIZONTAL_SPLIT, first, second, 0.46);
        split.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                int orientation = split.getWidth() < 860 ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT;
                if (split.getOrientation() != orientation) { split.setOrientation(orientation); split.setDividerLocation(0.5); }
            }
        });
        return split;
    }
    /** Feature panels also remain usable as standalone Swing panels in the existing smoke tests. */
    public static class Feature extends JPanel {
        private JComponent controls, results;
        public Feature(LayoutManager layout) { super(layout); }
        protected final void workspace(JComponent controls, JComponent results) {
            this.controls = controls; this.results = results; removeAll(); setLayout(new BorderLayout());
            add(adaptive(columnScroll(controls), results)); setPreferredSize(new Dimension(1000, 680));
            style(this);
        }
        public final JComponent takeWorkspaceControls() {
            removeAll(); add(results, BorderLayout.CENTER); setMinimumSize(new Dimension(0, 0));
            setPreferredSize(new Dimension(400, 400)); return controls;
        }
    }
    /** Only the active control page contributes height; long hidden forms must not create blank space. */
    public static final class ControlDeck extends JPanel {
        public ControlDeck(CardLayout layout) { super(layout); }
        @Override public Dimension getPreferredSize() {
            for(Component child:getComponents()) if(child.isVisible()) return child.getPreferredSize();
            return new Dimension(0,0);
        }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE,getPreferredSize().height); }
    }
    public static final class FormGrid extends JPanel {
        public FormGrid() { setOpaque(false); setLayout(new GridLayout(0, 2, 16, 12)); }
        public void field(String label, JTextField input) {
            input.setColumns(1); input.getAccessibleContext().setAccessibleName(label);
            var cell = new JPanel(new BorderLayout(0, 6)); cell.setOpaque(false);
            var caption = new WrappingText(label); caption.setFont(FONT.deriveFont(12f)); caption.setForeground(MUTED);
            cell.add(caption, BorderLayout.NORTH); cell.add(input, BorderLayout.SOUTH); add(cell);
        }
        @Override public void doLayout() {
            var grid = (GridLayout)getLayout(); int columns = getWidth() < 390 ? 1 : 2;
            if (grid.getColumns() != columns) { grid.setColumns(columns); revalidate(); }
            super.doLayout();
        }
        @Override public Dimension getPreferredSize() {
            int columns = getWidth() > 0 && getWidth() < 390 ? 1 : 2;
            int rows = (getComponentCount() + columns - 1) / columns;
            return new Dimension(420, rows * 88 - (rows > 0 ? 12 : 0));
        }
    }
    public static final class WrappingText extends JTextArea {
        public WrappingText(String text) { super(text); setEditable(false); setFocusable(false); setOpaque(false); setLineWrap(true); setWrapStyleWord(true); }
        @Override public Dimension getPreferredSize() {
            var fm = getFontMetrics(getFont()); int width = Math.max(100, getWidth() > 0 ? getWidth() : 200), lines = 0;
            for(String paragraph:getText().split("\\R",-1)){
                lines++;int used=0;
                for(String word:paragraph.split("\\s+")){int size=fm.stringWidth(word+" ");if(used>0&&used+size>width){lines++;used=0;}used+=size;}
            }
            return new Dimension(width, lines * fm.getHeight() + 2);
        }
    }
    public static final class StatusLabel extends JLabel {
        public StatusLabel(String text) { super(text); }
        @Override public void setText(String text) {
            super.setText("<html>" + escape(text == null ? "" : text) + "</html>");
        }
    }
    private static String escape(String text) { return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    public static final class RoundedCard extends JPanel {
        public RoundedCard() { setOpaque(false); setBorder(new EmptyBorder(16, 16, 16, 16)); }
        @Override protected void paintComponent(Graphics g) {
            var paint = (Graphics2D)g.create(); paint.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            paint.setColor(SURFACE); paint.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
            paint.setColor(LINE); paint.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20); paint.dispose();
            super.paintComponent(g);
        }
    }
    private static final class TrackingPanel extends JPanel implements Scrollable {
        TrackingPanel(JComponent child) { super(new BorderLayout()); setOpaque(false); add(child, BorderLayout.NORTH); }
        public Dimension getPreferredScrollableViewportSize() { return new Dimension(450, 500); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 20; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(20, r.height - 32); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }
    public static void style(Component component) {
        if (!(component instanceof JComponent c)) return;
        if (!(c instanceof JLabel label && label.getFont().isBold()) && !(c instanceof WrappingText)) c.setFont(FONT);
        c.setForeground(INK);
        if (c instanceof JPanel panel) {
            c.setOpaque(false);
            if(panel.getLayout() instanceof WrapLayout && panel.getClientProperty("workspace.wrapListener")==null){
                panel.putClientProperty("workspace.wrapListener",true);
                panel.addComponentListener(new ComponentAdapter(){public void componentResized(ComponentEvent e){SwingUtilities.invokeLater(()->{panel.revalidate();if(panel.getParent()!=null)panel.getParent().revalidate();});}});
            }
        }
        if (c instanceof JTextField input) {
            input.setBackground(SURFACE); input.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), new EmptyBorder(8, 10, 8, 10)));
            input.setMinimumSize(new Dimension(60, 36));
        } else if (c instanceof JTextArea text && !(c instanceof WrappingText)) {
            text.setBackground(SURFACE); text.setMargin(new Insets(8, 8, 8, 8)); text.setFont(FONT.deriveFont(14f));
            if(text.isEditable())text.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),new EmptyBorder(8,8,8,8)));
        } else if (c instanceof JComboBox<?>) {
            c.setBackground(SURFACE); c.setBorder(BorderFactory.createLineBorder(LINE));
            c.setMaximumSize(new Dimension(280, 40));
        } else if (c instanceof JButton button) {
            button.setUI(new RoundedButtonUI()); button.setOpaque(false); button.setContentAreaFilled(false);
            button.setFocusPainted(false); button.setBorder(new EmptyBorder(10, 14, 10, 14)); button.setRolloverEnabled(true);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setFont(FONT.deriveFont(Font.BOLD));
        } else if (c instanceof JCheckBox) { c.setOpaque(false); }
        else if (c instanceof JTabbedPane tabs) {
            tabs.setUI(new BasicTabbedPaneUI(){
                @Override protected void installDefaults(){super.installDefaults();tabInsets=new Insets(10,12,10,12);contentBorderInsets=new Insets(12,0,0,0);}
                @Override protected void paintTabBackground(Graphics g,int place,int index,int x,int y,int w,int h,boolean selected){
                    var p=(Graphics2D)g.create();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    p.setColor(selected?Color.decode("#FFF0EE"):SURFACE);p.fillRoundRect(x,y,w,h,12,12);p.dispose();
                }
                @Override protected void paintTabBorder(Graphics g,int place,int index,int x,int y,int w,int h,boolean selected){}
                @Override protected void paintContentBorder(Graphics g,int place,int selected){
                    int y=calculateTabAreaHeight(place,runCount,maxTabHeight);g.setColor(SURFACE);g.fillRect(0,y,tabPane.getWidth(),tabPane.getHeight()-y);
                }
            });
            tabs.setOpaque(true);tabs.setBackground(SURFACE); tabs.setForeground(INK); tabs.setFont(FONT.deriveFont(Font.BOLD));
        }
        for (Component child : c.getComponents()) style(child);
    }
    private static final class RoundedButtonUI extends BasicButtonUI {
        @Override public void paint(Graphics g, JComponent c) {
            var button = (AbstractButton)c; var p = (Graphics2D)g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean primary = Boolean.TRUE.equals(c.getClientProperty("workspace.primary"));
            Color fill = primary ? ACCENT : SURFACE;
            if (button.isSelected()) fill = Color.decode("#FFF0EE");
            if (button.getModel().isRollover() || button.getModel().isPressed()) fill = primary ? Color.decode("#E45148") : Color.decode("#F0F2F6");
            if (!button.isEnabled()) fill = BACKGROUND;
            p.setColor(fill); p.fillRoundRect(0, 0, c.getWidth()-1, c.getHeight()-1, 14, 14);
            p.setColor(button.hasFocus() || button.isSelected() ? ACCENT : LINE);
            p.setStroke(new BasicStroke(button.hasFocus() ? 2 : 1));
            p.drawRoundRect(1, 1, c.getWidth()-3, c.getHeight()-3, 14, 14); p.dispose(); super.paint(g, c);
        }
    }
    /** FlowLayout with a height that accounts for wrapping in a narrow viewport. */
    public static final class WrapLayout extends FlowLayout {
        public WrapLayout() { super(FlowLayout.LEFT, 8, 8); }
        @Override public Dimension preferredLayoutSize(Container target) {
            synchronized (target.getTreeLock()) {
                int width = target.getWidth();
                if (width <= 0 && target.getParent() != null) width = target.getParent().getWidth();
                if (width <= 0) width = 420;
                var insets = target.getInsets(); int limit = Math.max(100, width - insets.left - insets.right - 16);
                int rowWidth = 0, rowHeight = 0, height = 8;
                for (Component child : target.getComponents()) if (child.isVisible()) {
                    var size = child.getPreferredSize();
                    if (rowWidth > 0 && rowWidth + 8 + size.width > limit) { height += rowHeight + 8; rowWidth = 0; rowHeight = 0; }
                    rowWidth += (rowWidth > 0 ? 8 : 0) + size.width; rowHeight = Math.max(rowHeight, size.height);
                }
                return new Dimension(width, height + rowHeight + 8 + insets.top + insets.bottom);
            }
        }
        @Override public Dimension minimumLayoutSize(Container target) { return new Dimension(0, preferredLayoutSize(target).height); }
    }
}
