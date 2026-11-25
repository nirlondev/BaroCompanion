package baro.ui.widgets;

import baro.ui.Theme;

import javax.swing.*;
import java.awt.*;

/** Значок «потяни, чтобы переместить»: 2×3 точки. Рисуется кодом, не зависит от шрифта. */
public final class GripIcon implements Icon {
    @Override public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.MUTED);
        for (int col = 0; col < 2; col++) for (int row = 0; row < 3; row++) g2.fillOval(x + col * 5, y + row * 5, 3, 3);
        g2.dispose();
    }

    @Override public int getIconWidth() { return 8; }
    @Override public int getIconHeight() { return 13; }
}
