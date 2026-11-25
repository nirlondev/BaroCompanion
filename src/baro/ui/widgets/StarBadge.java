package baro.ui.widgets;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Значок «лучший предмет». Картинка берётся из папки проекта image/ (best.svg, отрисованный в best-N.png
 * разных размеров) — Java сама выбирает подходящий размер под масштаб экрана.
 */
public final class StarBadge implements Icon {
    private static final int[] SIZES = {16, 20, 24, 28, 32, 40, 48, 64, 128, 256};
    private static final Image IMAGE = load();
    private static final Map<Integer, StarBadge> CACHE = new HashMap<>();
    private final int size;

    private StarBadge(int size) { this.size = size; }

    public static StarBadge of(int size) { return CACHE.computeIfAbsent(size, StarBadge::new); }

    private static Image load() {
        List<Image> variants = new ArrayList<>();
        for (int s : SIZES) {
            try (InputStream in = StarBadge.class.getResourceAsStream("/image/best-" + s + ".png")) {
                if (in != null) { BufferedImage img = ImageIO.read(in); if (img != null) variants.add(img); }
            } catch (Exception ignored) { }
        }
        return variants.isEmpty() ? null : new BaseMultiResolutionImage(variants.toArray(new Image[0]));
    }

    @Override public void paintIcon(Component c, Graphics g, int x, int y) {
        if (IMAGE == null) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(IMAGE, x, y, size, size, null);
        g2.dispose();
    }

    @Override public int getIconWidth() { return size; }
    @Override public int getIconHeight() { return size; }
}
