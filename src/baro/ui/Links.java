package baro.ui;


import static baro.Tr.tr;
import baro.model.Item;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Внешние ссылки (вики игры, Boosty) и буфер обмена. */
public final class Links {
    private static final String WIKI = "https://barotraumagame.com/wiki/";
    public static final String BOOSTY = "https://boosty.to/nirlond";

    private Links() {}

    /** Страница предмета в официальной вики (по английскому названию; без него — поиск по идентификатору). */
    public static String wiki(Item item) {
        String en = item.englishName().orElse(null);
        if (en == null && item.name().matches("[\\p{IsLatin}\\d\\s\\-'().]+")) en = item.name();
        return en != null ? page(en) : WIKI + "index.php?search=" + URLEncoder.encode(item.id(), StandardCharsets.UTF_8);
    }

    /** Страница вики по английскому названию («Plasma Cutter» → Plasma_Cutter). */
    public static String page(String englishName) {
        return WIKI + URLEncoder.encode(englishName.trim().replace(' ', '_'), StandardCharsets.UTF_8).replace("%2F", "/");
    }

    public static void open(Component parent, String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) { Desktop.getDesktop().browse(URI.create(url)); return; }
        } catch (Exception ignored) { }
        copy(url);
        JOptionPane.showMessageDialog(parent, tr("Браузер не открылся — ссылка скопирована в буфер обмена:\n") + url, tr("Ссылка"), JOptionPane.INFORMATION_MESSAGE);
    }

    public static void copy(String text) {
        try { Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null); } catch (Exception ignored) { }
    }
}
