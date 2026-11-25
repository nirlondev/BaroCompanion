package baro.ui.widgets;

import javax.swing.*;
import java.awt.*;

/** Тот же значок, но бледный (для выключенного переключателя). */
public final class DimIcon implements Icon {
    private final Icon icon;

    public DimIcon(Icon icon) { this.icon = icon; }

    @Override public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        icon.paintIcon(c, g2, x, y);
        g2.dispose();
    }

    @Override public int getIconWidth() { return icon.getIconWidth(); }
    @Override public int getIconHeight() { return icon.getIconHeight(); }
}
