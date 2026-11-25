package baro.hotkey;


import static baro.Tr.tr;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/** Сочетание клавиш, которое работает во всей системе (в том числе поверх игры). */
public interface GlobalHotkey {
    /** Сочетание по умолчанию (в формате KeyStroke). */
    String DEFAULT = "shift T";

    /** Начать слушать. Обработчик вызывается в потоке Swing. */
    void start(Runnable onPressed);

    /** Временно отпустить сочетание (например, пока в окне набирают текст) или снова захватить. */
    void setActive(boolean active);

    void stop();

    boolean isSupported();

    /** Текст для пользователя: «Shift+T» или почему не работает. */
    String status();

    static GlobalHotkey shiftT() { return of(DEFAULT); }

    /** Сочетание из настроек («shift T», «ctrl alt F1»…); неверное — Shift+T. */
    static GlobalHotkey of(String spec) {
        KeyStroke ks = parse(spec);
        String name = name(ks);
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("win")) return new WindowsHotkey(windowsModifiers(ks.getModifiers()), ks.getKeyCode(), name);
            if (os.contains("mac")) return new MacHotkey(ks.getKeyCode(), ks.getModifiers(), name);
            if (os.contains("nux") || os.contains("nix") || os.contains("aix")) return new LinuxHotkey(ks.getKeyCode(), ks.getModifiers(), name);
            return new NoHotkey(name + tr(" на этой системе недоступно"));
        } catch (Throwable t) {
            return new NoHotkey(name + tr(" недоступно: ") + t.getMessage());
        }
    }

    static KeyStroke parse(String spec) {
        KeyStroke ks = spec == null ? null : KeyStroke.getKeyStroke(spec.trim());
        return ks == null || ks.getKeyCode() == KeyEvent.VK_UNDEFINED ? KeyStroke.getKeyStroke(DEFAULT) : ks;
    }

    /** «Shift+T», «Ctrl+Alt+F1». */
    static String name(KeyStroke ks) {
        String mods = InputEvent.getModifiersExText(ks.getModifiers());
        String key = KeyEvent.getKeyText(ks.getKeyCode());
        return mods.isEmpty() ? key : mods + "+" + key;
    }

    /** Текущее сочетание из строки настроек — для подписей. */
    static String name(String spec) { return name(parse(spec)); }

    /** Модификаторы Java → WinAPI (MOD_ALT 1, MOD_CONTROL 2, MOD_SHIFT 4, MOD_WIN 8). */
    private static int windowsModifiers(int javaMods) {
        int m = 0;
        if ((javaMods & InputEvent.ALT_DOWN_MASK) != 0) m |= 0x0001;
        if ((javaMods & InputEvent.CTRL_DOWN_MASK) != 0) m |= 0x0002;
        if ((javaMods & InputEvent.SHIFT_DOWN_MASK) != 0) m |= 0x0004;
        if ((javaMods & InputEvent.META_DOWN_MASK) != 0) m |= 0x0008;
        return m;
    }
}
