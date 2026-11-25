package baro.hotkey;


import static baro.Tr.tr;
import javax.swing.SwingUtilities;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Глобальное сочетание через WinAPI RegisterHotKey (user32.dll).
 * Вызов нативных функций — через Foreign Function API Java (java.lang.foreign), без сторонних библиотек.
 * Доступ к API идёт через отражение, поэтому программа собирается и запускается на Java 21 и новее.
 * Регистрация, снятие и чтение сообщений выполняются в одном отдельном потоке — так требует WinAPI.
 */
final class WindowsHotkey implements GlobalHotkey {
    static final int MOD_SHIFT = 0x0004;
    private static final int MOD_NOREPEAT = 0x4000;
    private static final int WM_HOTKEY = 0x0312;
    private static final int PM_REMOVE = 1;
    private static final int ID = 0xBA70;

    private final int modifiers, key;
    private final String name;
    private final MethodHandle register, unregister, peek;
    private final Object nullPtr, msg;
    private final Method getInt;
    private final Object intLayout;

    private volatile boolean wantActive = true, running;
    private volatile String status;
    private Thread thread;

    WindowsHotkey(int modifiers, int key, String name) throws Exception {
        this.modifiers = modifiers | MOD_NOREPEAT;
        this.key = key;
        this.name = name;
        Class<?> linkerC = Class.forName("java.lang.foreign.Linker");
        Class<?> lookupC = Class.forName("java.lang.foreign.SymbolLookup");
        Class<?> arenaC = Class.forName("java.lang.foreign.Arena");
        Class<?> segC = Class.forName("java.lang.foreign.MemorySegment");
        Class<?> layoutC = Class.forName("java.lang.foreign.MemoryLayout");
        Class<?> valueC = Class.forName("java.lang.foreign.ValueLayout");
        Class<?> fdC = Class.forName("java.lang.foreign.FunctionDescriptor");
        Class<?> optC = Class.forName("java.lang.foreign.Linker$Option");
        Class<?> allocC = Class.forName("java.lang.foreign.SegmentAllocator");

        Object linker = linkerC.getMethod("nativeLinker").invoke(null);
        Object arena = arenaC.getMethod("global").invoke(null);
        Object user32 = lookupC.getMethod("libraryLookup", String.class, arenaC).invoke(null, "user32", arena);
        Object i32 = valueC.getField("JAVA_INT").get(null);
        Object ptr = valueC.getField("ADDRESS").get(null);
        intLayout = i32;
        nullPtr = segC.getField("NULL").get(null);

        Method fdOf = fdC.getMethod("of", layoutC, Array.newInstance(layoutC, 0).getClass());
        Method find = lookupC.getMethod("find", String.class);
        Method downcall = linkerC.getMethod("downcallHandle", segC, fdC, Array.newInstance(optC, 0).getClass());
        Object noOpts = Array.newInstance(optC, 0);

        register = handle(linker, user32, find, downcall, noOpts, fdOf.invoke(null, i32, layouts(layoutC, ptr, i32, i32, i32)), "RegisterHotKey");
        unregister = handle(linker, user32, find, downcall, noOpts, fdOf.invoke(null, i32, layouts(layoutC, ptr, i32)), "UnregisterHotKey");
        peek = handle(linker, user32, find, downcall, noOpts, fdOf.invoke(null, i32, layouts(layoutC, ptr, ptr, i32, i32, i32)), "PeekMessageW");

        msg = allocC.getMethod("allocate", long.class, long.class).invoke(arena, 48L, 8L); // sizeof(MSG) на x64
        getInt = segC.getMethod("get", Class.forName("java.lang.foreign.ValueLayout$OfInt"), long.class);
        status = name;
    }

    private static Object layouts(Class<?> layoutC, Object... l) {
        Object arr = Array.newInstance(layoutC, l.length);
        for (int i = 0; i < l.length; i++) Array.set(arr, i, l[i]);
        return arr;
    }

    private static MethodHandle handle(Object linker, Object lib, Method find, Method downcall, Object opts, Object fd, String fn) throws Exception {
        Optional<?> sym = (Optional<?>) find.invoke(lib, fn);
        if (!sym.isPresent()) throw new IllegalStateException(tr("нет функции ") + fn);
        return (MethodHandle) downcall.invoke(linker, sym.get(), fd, opts);
    }

    @Override public synchronized void start(Runnable onPressed) {
        if (running) return;
        running = true;
        thread = new Thread(() -> loop(onPressed), "global-hotkey");
        thread.setDaemon(true);
        thread.start();
    }

    private void loop(Runnable onPressed) {
        boolean registered = false;
        try {
            while (running) {
                if (wantActive && !registered) {
                    registered = (int) register.invokeWithArguments(nullPtr, ID, modifiers, key) != 0;
                    status = registered ? name : name + tr(" занято другой программой");
                    if (!registered) { sleep(2000); continue; }
                } else if (!wantActive && registered) {
                    unregister.invokeWithArguments(nullPtr, ID);
                    registered = false;
                }
                while ((int) peek.invokeWithArguments(msg, nullPtr, 0, 0, PM_REMOVE) != 0) {
                    int message = (int) getInt.invoke(msg, intLayout, 8L);
                    if (message == WM_HOTKEY) SwingUtilities.invokeLater(onPressed);
                }
                sleep(25);
            }
        } catch (Throwable t) {
            status = name + tr(" не работает: ") + t;
        } finally {
            if (registered) try { unregister.invokeWithArguments(nullPtr, ID); } catch (Throwable ignored) { }
        }
    }

    private static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) { } }

    @Override public void setActive(boolean active) { wantActive = active; }
    @Override public void stop() { running = false; }
    @Override public boolean isSupported() { return true; }
    @Override public String status() { return status; }
}
