package baro.ui.widgets;

import javax.swing.*;
import java.awt.*;

/**
 * Вертикальная колонка: дети на всю ширину, высота с учётом переноса текста.
 * Если лежит в прокрутке — растягивается по ширине окна.
 */
public class Stack extends JPanel implements WidthAware, Scrollable {
    private final int gap;

    public Stack(int gap) {
        this.gap = gap;
        setLayout(new Layout());
        setOpaque(false);
        setAlignmentX(LEFT_ALIGNMENT);
    }

    public Stack padding(int top, int left, int bottom, int right) {
        setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        return this;
    }

    public Stack with(Component... cs) { for (Component c : cs) super.add(c); return this; }

    @Override public int preferredHeight(int width) {
        Insets in = getInsets();
        int w = width - in.left - in.right, h = 0, n = 0;
        for (Component c : getComponents()) {
            if (!c.isVisible()) continue;
            if (n++ > 0) h += gap;
            h += WidthAware.heightOf(c, w);
        }
        return h + in.top + in.bottom;
    }

    private int currentWidth() {
        Container p = getParent();
        if (p instanceof JViewport && p.getWidth() > 0) return p.getWidth();
        return getWidth() > 0 ? getWidth() : 380;
    }

    @Override public Dimension getPreferredSize() { int w = currentWidth(); return new Dimension(w, preferredHeight(w)); }
    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }

    @Override public void addNotify() {
        super.addNotify();
        if (getParent() instanceof JViewport) {
            getParent().addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override public void componentResized(java.awt.event.ComponentEvent e) { revalidate(); }
            });
        }
    }

    private final class Layout implements LayoutManager {
        @Override public void layoutContainer(Container c) {
            Insets in = getInsets();
            int w = getWidth() - in.left - in.right, y = in.top, n = 0;
            for (Component k : getComponents()) {
                if (!k.isVisible()) continue;
                if (n++ > 0) y += gap;
                int h = WidthAware.heightOf(k, w);
                k.setBounds(in.left, y, w, h);
                y += h;
            }
        }
        @Override public Dimension preferredLayoutSize(Container c) { return getPreferredSize(); }
        @Override public Dimension minimumLayoutSize(Container c) { return new Dimension(50, 10); }
        @Override public void addLayoutComponent(String name, Component comp) { }
        @Override public void removeLayoutComponent(Component comp) { }
    }

    // Scrollable: по ширине — как окно, по высоте — прокрутка.
    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(18, r.height - 40); }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}
