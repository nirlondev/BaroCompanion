package baro.ui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Заголовок окна Windows в цветах программы: тёмно-синий вместо белого (DwmSetWindowAttribute).
 * На Windows 10 заголовок становится тёмным, на Windows 11 — именно цвета HEADER. На других системах ничего не делает.
 */
public final class WindowChrome {
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20, DWMWA_BORDER_COLOR = 34, DWMWA_CAPTION_COLOR = 35, DWMWA_TEXT_COLOR = 36;
    private static MethodHandle setAttribute, setWindowPos;
    private static Object nullPtr;
    private static Object intLayout, arena;
    private static Method allocate, setInt, ofAddress;
    private static boolean tried;

    private WindowChrome() {}

    /** Покрасить заголовок, как только окно появится на экране. */
    public static void install(Window w) {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return;
        // применяем при каждом показе и активации: диалоги создаются заново и могут показываться повторно
        w.addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) { applyLater(w); }
            @Override public void windowActivated(WindowEvent e) { applyLater(w); }
        });
        w.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) { applyLater(w); }
        });
        if (w.isDisplayable()) applyLater(w);
    }

    private static void applyLater(Window w) {
        apply(w);
        javax.swing.SwingUtilities.invokeLater(() -> apply(w));
    }

    private static synchronized boolean init() {
        if (tried) return setAttribute != null;
        tried = true;
        try {
            Class<?> linkerC = Class.forName("java.lang.foreign.Linker");
            Class<?> lookupC = Class.forName("java.lang.foreign.SymbolLookup");
            Class<?> arenaC = Class.forName("java.lang.foreign.Arena");
            Class<?> segC = Class.forName("java.lang.foreign.MemorySegment");
            Class<?> layoutC = Class.forName("java.lang.foreign.MemoryLayout");
            Class<?> valueC = Class.forName("java.lang.foreign.ValueLayout");
            Class<?> fdC = Class.forName("java.lang.foreign.FunctionDescriptor");
            Class<?> optC = Class.forName("java.lang.foreign.Linker$Option");
            Object linker = linkerC.getMethod("nativeLinker").invoke(null);
            arena = arenaC.getMethod("global").invoke(null);
            Object dwm = lookupC.getMethod("libraryLookup", String.class, arenaC).invoke(null, "dwmapi", arena);
            Object i32 = valueC.getField("JAVA_INT").get(null), ptr = valueC.getField("ADDRESS").get(null);
            intLayout = i32;
            Object layouts = Array.newInstance(layoutC, 4);
            Array.set(layouts, 0, ptr); Array.set(layouts, 1, i32); Array.set(layouts, 2, ptr); Array.set(layouts, 3, i32);
            Object fd = fdC.getMethod("of", layoutC, layouts.getClass()).invoke(null, i32, layouts);
            Optional<?> sym = (Optional<?>) lookupC.getMethod("find", String.class).invoke(dwm, "DwmSetWindowAttribute");
            if (!sym.isPresent()) return false;
            Method downcall = linkerC.getMethod("downcallHandle", segC, fdC, Array.newInstance(optC, 0).getClass());
            setAttribute = (MethodHandle) downcall.invoke(linker, sym.get(), fd, Array.newInstance(optC, 0));
            // SetWindowPos(SWP_FRAMECHANGED) — чтобы Windows сразу перерисовала рамку окна
            try {
                Object user32 = lookupC.getMethod("libraryLookup", String.class, arenaC).invoke(null, "user32", arena);
                Object l7 = Array.newInstance(layoutC, 7);
                Array.set(l7, 0, ptr); Array.set(l7, 1, ptr);
                for (int k = 2; k < 7; k++) Array.set(l7, k, i32);
                Object fd7 = fdC.getMethod("of", layoutC, l7.getClass()).invoke(null, i32, l7);
                Optional<?> swp = (Optional<?>) lookupC.getMethod("find", String.class).invoke(user32, "SetWindowPos");
                if (swp.isPresent()) setWindowPos = (MethodHandle) downcall.invoke(linker, swp.get(), fd7, Array.newInstance(optC, 0));
                nullPtr = segC.getField("NULL").get(null);
            } catch (Throwable ignored) { }
            allocate = Class.forName("java.lang.foreign.SegmentAllocator").getMethod("allocate", long.class, long.class);
            setInt = segC.getMethod("set", Class.forName("java.lang.foreign.ValueLayout$OfInt"), long.class, int.class);
            ofAddress = segC.getMethod("ofAddress", long.class);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static long hwnd(Window w) throws Exception {
        Field f = Component.class.getDeclaredField("peer");
        f.setAccessible(true);
        Object peer = f.get(w);
        if (peer == null) return 0;
        Method m = peer.getClass().getMethod("getHWnd");
        m.setAccessible(true);
        return (Long) m.invoke(peer);
    }

    private static void apply(Window w) {
        try {
            if (!init()) return;
            long h = hwnd(w);
            if (h == 0) return;
            Object hw = ofAddress.invoke(null, h);
            set(hw, DWMWA_USE_IMMERSIVE_DARK_MODE, 1);
            set(hw, DWMWA_CAPTION_COLOR, colorRef(Theme.HEADER));
            set(hw, DWMWA_TEXT_COLOR, colorRef(Theme.FG));
            set(hw, DWMWA_BORDER_COLOR, colorRef(Theme.HEADER_LINE));
            // SWP_NOSIZE|SWP_NOMOVE|SWP_NOZORDER|SWP_NOACTIVATE|SWP_FRAMECHANGED
            if (setWindowPos != null && !Boolean.TRUE.equals(((javax.swing.RootPaneContainer) w).getRootPane().getClientProperty("baro.chrome")))
                setWindowPos.invokeWithArguments(hw, nullPtr, 0, 0, 0, 0, 0x0001 | 0x0002 | 0x0004 | 0x0010 | 0x0020);
            if (w instanceof javax.swing.RootPaneContainer) ((javax.swing.RootPaneContainer) w).getRootPane().putClientProperty("baro.chrome", Boolean.TRUE);
        } catch (Throwable ignored) {
            // нет доступа к окну (запуск без нужных флагов) — остаётся обычный заголовок
        }
    }

    private static void set(Object hwnd, int attr, int value) throws Throwable {
        Object mem = allocate.invoke(arena, 4L, 4L);
        setInt.invoke(mem, intLayout, 0L, value);
        setAttribute.invokeWithArguments(hwnd, attr, mem, 4);
    }

    /** COLORREF Windows: 0x00BBGGRR. */
    private static int colorRef(Color c) { return c.getRed() | (c.getGreen() << 8) | (c.getBlue() << 16); }
}
