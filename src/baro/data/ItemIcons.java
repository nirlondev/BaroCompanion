package baro.data;


import static baro.Tr.tr;
import baro.model.Item;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.Icon;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Иконки предметов из текстур игры. Атласы бывают 8192×8192 — целиком в память (256 МБ) не влезают,
 * поэтому иконки вырезаются полосами (ImageReadParam.setSourceRegion) один раз, в фоне, и складываются
 * в кэш (icons.cache рядом с настройками). Пока кэш строится, иконки пустые; потом окна перерисовываются.
 */
public final class ItemIcons {
    /** Где картинка предмета: файл текстуры и прямоугольник {x, y, w, h} (null — вся текстура). */
    public record Spec(String texture, int[] rect) { }

    private static final int STORED = 64, BAND = 768, MAGIC = 0x42494331; // "BIC1"
    private static final ItemIcons NONE = new ItemIcons(Map.of(), null);

    private final Map<String, Spec> specs;
    private final Path cache;
    private volatile Map<String, byte[]> png = Map.of();
    private final Map<String, BufferedImage> decoded = new HashMap<>();
    private final Map<String, Image> scaled = new HashMap<>();
    private boolean started;

    public ItemIcons(Map<String, Spec> specs, Path cache) { this.specs = Map.copyOf(specs); this.cache = cache; }

    /** Без иконок (данные встроенные). */
    public static ItemIcons none() { return NONE; }

    public boolean isEmpty() { return specs.isEmpty(); }

    /** Иконка предмета размера size×size или null, если картинки у предмета нет. Пока кэш не готов — пустое место. */
    public Icon icon(Item item, int size) {
        if (item == null || !specs.containsKey(item.id())) return null;
        start();
        return new Lazy(item.id(), size);
    }

    /** Картинка предмета или null (если ещё не загружена или её нет). */
    public synchronized Image image(String id, int size) {
        String key = id + "@" + size;
        Image s = scaled.get(key);
        if (s != null) return s;
        BufferedImage src = decoded.get(id);
        if (src == null) {
            byte[] b = png.get(id);
            if (b == null) return null;
            try { src = ImageIO.read(new ByteArrayInputStream(b)); } catch (IOException e) { src = null; }
            if (src == null) return null;
            decoded.put(id, src);
        }
        s = fit(src, size, 2.0);
        scaled.put(key, s);
        return s;
    }

    private final class Lazy implements Icon {
        private final String id; private final int size;
        Lazy(String id, int size) { this.id = id; this.size = size; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Image im = image(id, size);
            if (im != null) g.drawImage(im, x + (size - im.getWidth(null)) / 2, y + (size - im.getHeight(null)) / 2, null);
        }
        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    // ---------- загрузка ----------

    private synchronized void start() {
        if (started || specs.isEmpty()) return;
        started = true;
        Thread t = new Thread(() -> {
            try {
                Map<String, byte[]> m = load();
                png = m;
                java.awt.EventQueue.invokeLater(() -> { for (Window w : Window.getWindows()) w.repaint(); });
            } catch (Throwable e) {
                System.err.println(tr("Иконки предметов не загружены: ") + e);
            }
        }, "item-icons");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    /** Подпись набора: какие текстуры и прямоугольники, и когда менялись файлы текстур. */
    private long signature() {
        long h = 1125899906842597L;
        for (Map.Entry<String, Spec> e : new TreeMap<>(specs).entrySet()) {
            h = 31 * h + e.getKey().hashCode();
            h = 31 * h + e.getValue().texture().hashCode();
            h = 31 * h + Arrays.hashCode(e.getValue().rect());
        }
        for (String tex : new TreeSet<>(textures().keySet())) {
            File f = new File(tex);
            h = 31 * h + f.lastModified();
            h = 31 * h + f.length();
        }
        return h;
    }

    private Map<String, List<String>> textures() {
        Map<String, List<String>> byTex = new LinkedHashMap<>();
        for (Map.Entry<String, Spec> e : specs.entrySet()) byTex.computeIfAbsent(e.getValue().texture(), k -> new ArrayList<>()).add(e.getKey());
        return byTex;
    }

    private Map<String, byte[]> load() throws IOException {
        long sig = signature();
        if (cache != null && Files.isRegularFile(cache)) {
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(cache))))) {
                if (in.readInt() == MAGIC && in.readLong() == sig) {
                    int n = in.readInt();
                    Map<String, byte[]> m = new HashMap<>(n * 2);
                    for (int i = 0; i < n; i++) { String id = in.readUTF(); byte[] b = new byte[in.readInt()]; in.readFully(b); m.put(id, b); }
                    return m;
                }
            } catch (IOException e) { /* кэш испорчен — построим заново */ }
        }
        long t0 = System.currentTimeMillis();
        Map<String, byte[]> m = new HashMap<>();
        for (Map.Entry<String, List<String>> e : textures().entrySet()) {
            try { cut(new File(e.getKey()), e.getValue(), m); }
            catch (Exception ex) { System.err.println(tr("Текстура не прочитана: ") + e.getKey() + " — " + ex); }
        }
        if (cache != null) {
            Path tmp = cache.resolveSibling(cache.getFileName() + ".tmp");
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(tmp))))) {
                out.writeInt(MAGIC); out.writeLong(sig); out.writeInt(m.size());
                for (Map.Entry<String, byte[]> x : m.entrySet()) { out.writeUTF(x.getKey()); out.writeInt(x.getValue().length); out.write(x.getValue()); }
            }
            Files.move(tmp, cache, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        System.err.println(tr("Иконки предметов: ") + m.size() + " (" + (System.currentTimeMillis() - t0) + tr(" мс)"));
        return m;
    }

    /** Вырезать иконки одной текстуры: прямоугольники группируются в горизонтальные полосы не выше BAND строк. */
    private void cut(File texture, List<String> ids, Map<String, byte[]> out) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(texture)) {
            Iterator<ImageReader> it = ImageIO.getImageReaders(iis);
            if (!it.hasNext()) return;
            ImageReader reader = it.next();
            try {
                reader.setInput(iis, true, true);
                int w = reader.getWidth(0), h = reader.getHeight(0);
                List<Object[]> rects = new ArrayList<>(); // {id, Rectangle}
                for (String id : ids) {
                    int[] r = specs.get(id).rect();
                    Rectangle rc = (r == null ? new Rectangle(0, 0, w, h) : new Rectangle(r[0], r[1], r[2], r[3])).intersection(new Rectangle(0, 0, w, h));
                    if (!rc.isEmpty()) rects.add(new Object[]{id, rc});
                }
                rects.sort(Comparator.comparingInt(o -> ((Rectangle) o[1]).y));
                int i = 0;
                while (i < rects.size()) {
                    Rectangle band = new Rectangle((Rectangle) rects.get(i)[1]);
                    int j = i + 1;
                    while (j < rects.size()) {
                        Rectangle next = band.union((Rectangle) rects.get(j)[1]);
                        if (next.height > BAND) break;
                        band = next;
                        j++;
                    }
                    ImageReadParam p = reader.getDefaultReadParam();
                    p.setSourceRegion(band);
                    BufferedImage img = reader.read(0, p);
                    for (int k = i; k < j; k++) {
                        Rectangle rc = (Rectangle) rects.get(k)[1];
                        BufferedImage part = img.getSubimage(rc.x - band.x, rc.y - band.y, rc.width, rc.height);
                        ByteArrayOutputStream bo = new ByteArrayOutputStream();
                        ImageIO.write(fit(part, STORED, 1.0), "png", bo);
                        out.put((String) rects.get(k)[0], bo.toByteArray());
                    }
                    i = j;
                }
            } finally { reader.dispose(); }
        }
    }

    /** Вписать в квадрат size с сохранением пропорций (обрезав прозрачные поля); увеличивать не больше чем в maxUp раз. */
    private static BufferedImage fit(BufferedImage src, int size, double maxUp) {
        src = trim(src);
        double k = Math.min(maxUp, Math.min(1.0 * size / src.getWidth(), 1.0 * size / src.getHeight()));
        int w = Math.max(1, (int) Math.round(src.getWidth() * k)), h = Math.max(1, (int) Math.round(src.getHeight() * k));
        BufferedImage cur = src;
        // уменьшение в несколько шагов — без «лесенки»
        while (cur.getWidth() / 2 >= w && cur.getHeight() / 2 >= h) cur = scale(cur, cur.getWidth() / 2, cur.getHeight() / 2);
        return scale(cur, w, h);
    }

    private static BufferedImage scale(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    /** Обрезать прозрачные края (в атласах иконки часто с запасом). */
    private static BufferedImage trim(BufferedImage img) {
        if (!img.getColorModel().hasAlpha()) return img;
        int w = img.getWidth(), h = img.getHeight(), x0 = w, y0 = h, x1 = -1, y1 = -1;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if ((img.getRGB(x, y) >>> 24) > 8) { if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y; }
        if (x1 < 0) return img;
        return x0 == 0 && y0 == 0 && x1 == w - 1 && y1 == h - 1 ? img : img.getSubimage(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
    }
}
