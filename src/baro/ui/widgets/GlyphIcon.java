package baro.ui.widgets;

import baro.ui.Theme;

import javax.swing.Icon;
import java.awt.*;

/** Значок из одного символа шрифта (★ избранного и т. п.). */
public final class GlyphIcon implements Icon {
    private final String glyph;
    private final Color color;
    private final int size;

    public GlyphIcon(String glyph, Color color, int size) { this.glyph = glyph; this.color = color; this.size = size; }

    @Override public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(Theme.font(size / Theme.scale(), true)); // шрифт с запасными шрифтами системы: ☑, ★ и т. п.
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(glyph, x + (size - fm.stringWidth(glyph)) / 2, y + (size - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }

    @Override public int getIconWidth() { return size; }
    @Override public int getIconHeight() { return size; }
}
