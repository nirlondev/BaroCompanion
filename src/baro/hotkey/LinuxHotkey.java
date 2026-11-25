package baro.hotkey;


import static baro.Tr.tr;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.invoke.MethodHandle;

/**
 * Глобальное сочетание на Linux (X11) через libX11 — опросом состояния клавиатуры.
 * XQueryPointer даёт текущие модификаторы, XQueryKeymap — нажатые клавиши; грабы не используются,
 * поэтому занятое другой программой сочетание не приводит к ошибке и падению.
 * Под чистым Wayland клавиши, уходящие в нативные окна, могут не читаться — тогда окно всё равно
 * открывается из самой программы.
 */
final class LinuxHotkey extends PollingHotkey {
    // Маски модификаторов X11
    private static final int SHIFT = 1, CONTROL = 4, MOD1 = 8, MOD4 = 64; // Alt=Mod1, Super=Mod4
    private static final int MOD_MASK = SHIFT | CONTROL | MOD1 | MOD4;

    private final Ffm ffm;
    private final MethodHandle queryPointer, queryKeymap;
    private final Object display;
    private final long root;
    private final int keycode, wantMods;
    private final Object keymapBuf, rootRet, childRet, rxB, ryB, wxB, wyB, maskB;

    LinuxHotkey(int awtKey, int javaMods, String name) throws Throwable {
        super(name);
        int keysym = keysym(awtKey);
        if (keysym == 0) throw new IllegalStateException(tr("клавиша не поддерживается"));
        wantMods = mods(javaMods);
        ffm = new Ffm();
        Object x11 = ffm.library("X11");
        MethodHandle openDisplay = ffm.fn(x11, "XOpenDisplay", ffm.PTR, ffm.PTR);
        MethodHandle defaultRoot = ffm.fn(x11, "XDefaultRootWindow", ffm.LONG, ffm.PTR);
        MethodHandle toKeycode = ffm.fn(x11, "XKeysymToKeycode", ffm.INT, ffm.PTR, ffm.LONG);
        queryPointer = ffm.fn(x11, "XQueryPointer", ffm.INT,
                ffm.PTR, ffm.LONG, ffm.PTR, ffm.PTR, ffm.PTR, ffm.PTR, ffm.PTR, ffm.PTR, ffm.PTR);
        queryKeymap = ffm.fn(x11, "XQueryKeymap", ffm.INT, ffm.PTR, ffm.PTR);

        display = openDisplay.invoke(ffm.NULL);
        if (ffm.address(display) == 0) throw new IllegalStateException(tr("нет дисплея X11 (DISPLAY)"));
        root = (long) defaultRoot.invoke(display);
        keycode = (int) toKeycode.invoke(display, (long) keysym) & 0xFF;
        if (keycode == 0) throw new IllegalStateException(tr("клавиша не найдена в раскладке"));

        keymapBuf = ffm.alloc(32);
        rootRet = ffm.alloc(8); childRet = ffm.alloc(8);
        rxB = ffm.alloc(4); ryB = ffm.alloc(4); wxB = ffm.alloc(4); wyB = ffm.alloc(4); maskB = ffm.alloc(4);
    }

    @Override boolean comboDown() throws Throwable {
        queryPointer.invoke(display, root, rootRet, childRet, rxB, ryB, wxB, wyB, maskB);
        if ((ffm.getInt(maskB, 0) & MOD_MASK) != wantMods) return false;
        queryKeymap.invoke(display, keymapBuf);
        return (ffm.getByte(keymapBuf, keycode >> 3) & (1 << (keycode & 7))) != 0;
    }

    private static int mods(int javaMods) {
        int m = 0;
        if ((javaMods & InputEvent.SHIFT_DOWN_MASK) != 0) m |= SHIFT;
        if ((javaMods & InputEvent.CTRL_DOWN_MASK) != 0) m |= CONTROL;
        if ((javaMods & InputEvent.ALT_DOWN_MASK) != 0) m |= MOD1;
        if ((javaMods & InputEvent.META_DOWN_MASK) != 0) m |= MOD4;
        return m;
    }

    /** AWT-код клавиши → keysym X11 (буквы, цифры, F1–F24). 0 — не поддерживается. */
    private static int keysym(int vk) {
        if (vk >= KeyEvent.VK_A && vk <= KeyEvent.VK_Z) return vk + 0x20; // строчная буква: XK_a..XK_z
        if (vk >= KeyEvent.VK_0 && vk <= KeyEvent.VK_9) return vk;         // XK_0..XK_9 == '0'..'9'
        if (vk >= KeyEvent.VK_F1 && vk <= KeyEvent.VK_F12) return 0xFFBE + (vk - KeyEvent.VK_F1); // XK_F1..
        return 0;
    }
}
