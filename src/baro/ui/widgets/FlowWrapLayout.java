package baro.ui.widgets;

import javax.swing.JComponent;
import java.awt.*;

/** Раскладка «как текст»: слева направо с переносом на новую строку. */
public final class FlowWrapLayout implements LayoutManager {
    /** Принудительный перенос строки. */
    static final class Break extends Component { }

    private final int vgap, hgap;

    FlowWrapLayout(int vgap) { this(0, vgap); }

    public FlowWrapLayout(int hgap, int vgap) { this.hgap = hgap; this.vgap = vgap; }

    public int heightFor(Container c, int width) { return run(c, width, false); }

    /** Компонент, который нельзя переносить отдельно от предыдущего (запятая, скобка). */
    static final String GLUE = "flowwrap.glue";

    private int run(Container c, int width, boolean apply) {
        Insets in = c.getInsets();
        int max = Math.max(1, width - in.left - in.right);
        int x = 0, y = 0, lineH = 0, n = c.getComponentCount(), lineStart = 0;
        boolean any = false;
        for (int i = 0; i < n; i++) {
            Component k = c.getComponent(i);
            if (!k.isVisible()) continue;
            if (k instanceof Break) {
                if (apply) centerLine(c, lineStart, i, y, lineH, in);
                y += Math.max(lineH, 1) + vgap; x = 0; lineH = 0; lineStart = i + 1; continue;
            }
            Dimension d = k.getPreferredSize();
            boolean glued = k instanceof JComponent && Boolean.TRUE.equals(((JComponent) k).getClientProperty(GLUE));
            if (x > 0 && x + d.width > max && !glued) {
                if (apply) centerLine(c, lineStart, i, y, lineH, in);
                y += lineH + vgap; x = 0; lineH = 0; lineStart = i;
            }
            if (apply) k.setBounds(in.left + x, in.top + y, Math.min(d.width, max), d.height);
            x += d.width + hgap; lineH = Math.max(lineH, d.height); any = true;
        }
        if (apply) centerLine(c, lineStart, n, y, lineH, in);
        return in.top + in.bottom + (any ? y + lineH : y);
    }

    /** Выровнять по вертикали компоненты строки разной высоты. */
    private static void centerLine(Container c, int from, int to, int y, int lineH, Insets in) {
        for (int i = from; i < to; i++) {
            Component k = c.getComponent(i);
            if (k instanceof Break || !k.isVisible()) continue;
            k.setLocation(k.getX(), in.top + y + (lineH - k.getHeight()) / 2);
        }
    }

    @Override public void layoutContainer(Container c) { run(c, c.getWidth(), true); }
    @Override public Dimension preferredLayoutSize(Container c) {
        int w = c.getWidth() > 0 ? c.getWidth() : 360;
        return new Dimension(w, heightFor(c, w));
    }
    @Override public Dimension minimumLayoutSize(Container c) { return new Dimension(10, heightFor(c, Math.max(c.getWidth(), 10))); }
    @Override public void addLayoutComponent(String name, Component comp) { }
    @Override public void removeLayoutComponent(Component comp) { }
}
