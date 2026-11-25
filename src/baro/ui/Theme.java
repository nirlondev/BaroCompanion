package baro.ui;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.util.Arrays;

/**
 * Тема в цветах справочника: тёмная (по умолчанию) или светлая, и масштаб шрифта.
 * Выбирается до создания окон ({@link #configure}); смена применяется после перезапуска.
 */
public final class Theme {
    public static Color BG, PANEL, SUNK, FG, MUTED, ACCENT, ACCENT_DARK, BUY, SELL, WARN, LINE, HIT, WHITE,
            /** Шапка окна и панелей: приглушённый синий, не ярче остального интерфейса. */
            HEADER, HEADER_LINE,
            /** Значок «избранное». */
            FAVORITE;
    private static final String FAMILY = pickFamily();
    private static boolean light;
    private static float scale = 1f;

    static { palette(false); }

    private Theme() {}

    /** Задать тему (dark/light) и масштаб шрифта (0,8–1,6) — до первого окна. */
    public static void configure(String theme, double fontScale) {
        light = "light".equalsIgnoreCase(theme);
        scale = (float) Math.max(0.8, Math.min(1.6, fontScale));
        palette(light);
    }

    public static boolean isLight() { return light; }
    public static float scale() { return scale; }

    /** Размер в пикселях с учётом масштаба шрифта (высота строки списка и т. п.). */
    public static int px(int base) { return Math.round(base * scale); }

    private static void palette(boolean light) {
        if (light) {
            BG = new Color(0xf4f7f7); PANEL = new Color(0xe6eded); SUNK = new Color(0xffffff);
            FG = new Color(0x1d2b2e); MUTED = new Color(0x5d7174); ACCENT = new Color(0x0b7d72); ACCENT_DARK = new Color(0x9fd8d0);
            BUY = new Color(0xb4501a); SELL = new Color(0x1d7d43); WARN = new Color(0x9a6a00); LINE = new Color(0xc9d6d8);
            HIT = new Color(0xd3f0eb); WHITE = new Color(0x000000);
            HEADER = new Color(0xdce7f0); HEADER_LINE = new Color(0xb9cddd); FAVORITE = new Color(0xc28a00);
        } else {
            BG = new Color(0x0f1b1e); PANEL = new Color(0x14252a); SUNK = new Color(0x132327);
            FG = new Color(0xd5e3e3); MUTED = new Color(0x8aa0a2); ACCENT = new Color(0x46c2b3); ACCENT_DARK = new Color(0x0b746e);
            BUY = new Color(0xf0915c); SELL = new Color(0x6fd496); WARN = new Color(0xe3b25a); LINE = new Color(0x24393f);
            HIT = new Color(0x173b37); WHITE = new Color(0xffffff);
            HEADER = new Color(0x13293d); HEADER_LINE = new Color(0x1d3a52); FAVORITE = new Color(0xf2c94c);
        }
    }

    private static String pickFamily() {
        String[] have = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String f : new String[]{"Segoe UI", "Noto Sans", "DejaVu Sans", "Arial"})
            if (Arrays.asList(have).contains(f)) return f;
        return Font.DIALOG;
    }

    /** Шрифт с запасными шрифтами системы (через StyleContext): символы вроде ★ и ⚖ рисуются, даже если их нет в основном. */
    public static Font font(float size, boolean bold) {
        return javax.swing.text.StyleContext.getDefaultStyleContext().getFont(FAMILY, bold ? Font.BOLD : Font.PLAIN, Math.round(size * scale));
    }

    public static void install() {
        try {
            for (UIManager.LookAndFeelInfo i : UIManager.getInstalledLookAndFeels())
                if ("Nimbus".equals(i.getName())) { UIManager.setLookAndFeel(i.getClassName()); break; }
        } catch (Exception ignored) { }
        put("control", PANEL); put("info", PANEL); put("nimbusBase", light ? new Color(0x5f8f94) : new Color(0x0e2b2e));
        put("nimbusAlertYellow", WARN); put("nimbusDisabledText", MUTED); put("nimbusFocus", ACCENT);
        put("nimbusGreen", SELL); put("nimbusInfoBlue", ACCENT); put("nimbusLightBackground", SUNK);
        put("nimbusOrange", BUY); put("nimbusRed", BUY); put("nimbusSelectedText", light ? FG : WHITE);
        put("nimbusSelectionBackground", ACCENT_DARK); put("text", FG); put("nimbusBorder", LINE);
        put("textForeground", FG); put("textBackground", SUNK); put("background", PANEL);
        put("List.background", SUNK); put("List.foreground", FG); put("Table.background", SUNK);
        put("Table.alternateRowColor", light ? new Color(0xeef3f3) : new Color(0x16292d)); put("Table.foreground", FG);
        put("TableHeader.background", PANEL); put("ScrollPane.background", BG); put("Viewport.background", BG);
        put("ToolTip.background", PANEL); put("ToolTip.foreground", FG);
        Font f = font(13, false);
        for (String k : new String[]{"defaultFont", "Label.font", "Button.font", "ToggleButton.font", "TextField.font", "ComboBox.font",
                "List.font", "Table.font", "TableHeader.font", "TabbedPane.font", "CheckBox.font", "Spinner.font", "MenuItem.font", "PopupMenu.font",
                "ToolTip.font", "Menu.font", "RadioButtonMenuItem.font", "CheckBoxMenuItem.font", "TextArea.font", "RadioButton.font"})
            UIManager.put(k, f);
    }

    private static void put(String key, Color c) { UIManager.put(key, new ColorUIResource(c)); }

    /** Скролл с фоном темы и быстрой прокруткой колесом. */
    public static JScrollPane scroll(Component view) {
        JScrollPane s = new JScrollPane(view);
        s.setBorder(BorderFactory.createEmptyBorder());
        s.getViewport().setBackground(BG);
        s.getVerticalScrollBar().setUnitIncrement(18);
        s.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return s;
    }
}
