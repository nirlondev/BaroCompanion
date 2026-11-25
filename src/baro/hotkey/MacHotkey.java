package baro.hotkey;


import static baro.Tr.tr;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.invoke.MethodHandle;

/**
 * Глобальное сочетание на macOS через CoreGraphics (CGEventSourceKeyState / CGEventSourceFlagsState) —
 * опросом состояния клавиатуры. Чтение состояния клавиш не требует разрешения «Универсальный доступ»
 * (в отличие от перехвата событий).
 */
final class MacHotkey extends PollingHotkey {
    // Флаги модификаторов CoreGraphics
    private static final long SHIFT = 0x20000L, CONTROL = 0x40000L, ALT = 0x80000L, CMD = 0x100000L;
    private static final long MOD_MASK = SHIFT | CONTROL | ALT | CMD;
    private static final int COMBINED = 0; // kCGEventSourceStateCombinedSessionState
    private static final String CG = "/System/Library/Frameworks/CoreGraphics.framework/CoreGraphics";

    private final MethodHandle keyState, flagsState;
    private final int keycode;
    private final long wantMods;

    MacHotkey(int awtKey, int javaMods, String name) throws Exception {
        super(name);
        keycode = keycode(awtKey);
        if (keycode < 0) throw new IllegalStateException(tr("клавиша не поддерживается"));
        wantMods = mods(javaMods);
        Ffm ffm = new Ffm();
        Object cg = ffm.library(CG);
        keyState = ffm.fn(cg, "CGEventSourceKeyState", ffm.INT, ffm.INT, ffm.INT);
        flagsState = ffm.fn(cg, "CGEventSourceFlagsState", ffm.LONG, ffm.INT);
    }

    @Override boolean comboDown() throws Throwable {
        if (((long) flagsState.invoke(COMBINED) & MOD_MASK) != wantMods) return false;
        return ((int) keyState.invoke(COMBINED, keycode) & 0xFF) != 0;
    }

    private static long mods(int javaMods) {
        long m = 0;
        if ((javaMods & InputEvent.SHIFT_DOWN_MASK) != 0) m |= SHIFT;
        if ((javaMods & InputEvent.CTRL_DOWN_MASK) != 0) m |= CONTROL;
        if ((javaMods & InputEvent.ALT_DOWN_MASK) != 0) m |= ALT;
        if ((javaMods & InputEvent.META_DOWN_MASK) != 0) m |= CMD;
        return m;
    }

    /** AWT-код клавиши → виртуальный код macOS (ANSI). -1 — не поддерживается. */
    private static int keycode(int vk) {
        switch (vk) {
            case KeyEvent.VK_A: return 0;  case KeyEvent.VK_B: return 11; case KeyEvent.VK_C: return 8;
            case KeyEvent.VK_D: return 2;  case KeyEvent.VK_E: return 14; case KeyEvent.VK_F: return 3;
            case KeyEvent.VK_G: return 5;  case KeyEvent.VK_H: return 4;  case KeyEvent.VK_I: return 34;
            case KeyEvent.VK_J: return 38; case KeyEvent.VK_K: return 40; case KeyEvent.VK_L: return 37;
            case KeyEvent.VK_M: return 46; case KeyEvent.VK_N: return 45; case KeyEvent.VK_O: return 31;
            case KeyEvent.VK_P: return 35; case KeyEvent.VK_Q: return 12; case KeyEvent.VK_R: return 15;
            case KeyEvent.VK_S: return 1;  case KeyEvent.VK_T: return 17; case KeyEvent.VK_U: return 32;
            case KeyEvent.VK_V: return 9;  case KeyEvent.VK_W: return 13; case KeyEvent.VK_X: return 7;
            case KeyEvent.VK_Y: return 16; case KeyEvent.VK_Z: return 6;
            case KeyEvent.VK_0: return 29; case KeyEvent.VK_1: return 18; case KeyEvent.VK_2: return 19;
            case KeyEvent.VK_3: return 20; case KeyEvent.VK_4: return 21; case KeyEvent.VK_5: return 23;
            case KeyEvent.VK_6: return 22; case KeyEvent.VK_7: return 26; case KeyEvent.VK_8: return 28;
            case KeyEvent.VK_9: return 25;
            case KeyEvent.VK_F1: return 122; case KeyEvent.VK_F2: return 120; case KeyEvent.VK_F3: return 99;
            case KeyEvent.VK_F4: return 118; case KeyEvent.VK_F5: return 96;  case KeyEvent.VK_F6: return 97;
            case KeyEvent.VK_F7: return 98;  case KeyEvent.VK_F8: return 100; case KeyEvent.VK_F9: return 101;
            case KeyEvent.VK_F10: return 109; case KeyEvent.VK_F11: return 103; case KeyEvent.VK_F12: return 111;
            default: return -1;
        }
    }
}
