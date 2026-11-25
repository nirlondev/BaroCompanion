package baro.ui.widgets;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

/** Несколько значков в ряд (иконка предмета, звезда «лучший», значок «избранное»); пустые пропускаются. */
public final class IconRowIcon implements Icon {
    private static final int GAP = 3;
    private final List<Icon> icons;

    private IconRowIcon(List<Icon> icons) { this.icons = icons; }

    /** null, если значков нет; один значок — он сам. */
    public static Icon of(Icon... icons) {
        List<Icon> l = new ArrayList<>();
        for (Icon i : icons) if (i != null) l.add(i);
        return l.isEmpty() ? null : l.size() == 1 ? l.get(0) : new IconRowIcon(l);
    }

    @Override public void paintIcon(Component c, Graphics g, int x, int y) {
        int h = getIconHeight();
        for (Icon i : icons) { i.paintIcon(c, g, x, y + (h - i.getIconHeight()) / 2); x += i.getIconWidth() + GAP; }
    }

    @Override public int getIconWidth() {
        int w = -GAP;
        for (Icon i : icons) w += i.getIconWidth() + GAP;
        return w;
    }

    @Override public int getIconHeight() {
        int h = 0;
        for (Icon i : icons) h = Math.max(h, i.getIconHeight());
        return h;
    }
}
