package baro.ui;

import java.awt.*;
import java.awt.image.BufferedImage;

/** Значок программы, нарисованный кодом (никаких файлов картинок). */
public final class AppIcon {
    private AppIcon() {}

    public static Image create(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float k = size / 32f;
        g.setColor(Theme.ACCENT_DARK);
        g.fillRoundRect(0, 0, size, size, Math.round(8 * k), Math.round(8 * k));
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3f * k));
        g.drawOval(Math.round(7 * k), Math.round(7 * k), Math.round(18 * k), Math.round(18 * k));
        g.drawLine(Math.round(16 * k), Math.round(2 * k), Math.round(16 * k), Math.round(30 * k));
        g.drawLine(Math.round(2 * k), Math.round(16 * k), Math.round(30 * k), Math.round(16 * k));
        g.dispose();
        return img;
    }
}
