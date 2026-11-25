package baro.hotkey;


import java.lang.invoke.MethodHandle;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Тонкая обёртка над Foreign Function API Java (java.lang.foreign) через отражение —
 * чтобы программа собиралась и запускалась на Java 21 и новее без флага --enable-preview.
 * Используется глобальными сочетаниями на Linux (libX11) и macOS (CoreGraphics).
 * Типы раскладок (int/long/byte/указатель) задаются константами {@link #INT} и т.п.
 */
final class Ffm {
    final Object INT, LONG, BYTE, PTR, NULL;

    private final Object linker, arena;
    private final Class<?> linkerC, lookupC, segC, layoutC, fdC, optC, allocC;
    private final Class<?> ofIntC, ofLongC, ofByteC;
    private final Method fdOf, find, downcall, allocate, getI, getL, getB, address;
    private final Object noOpts;

    Ffm() throws Exception {
        linkerC = Class.forName("java.lang.foreign.Linker");
        lookupC = Class.forName("java.lang.foreign.SymbolLookup");
        Class<?> arenaC = Class.forName("java.lang.foreign.Arena");
        segC = Class.forName("java.lang.foreign.MemorySegment");
        layoutC = Class.forName("java.lang.foreign.MemoryLayout");
        Class<?> valueC = Class.forName("java.lang.foreign.ValueLayout");
        fdC = Class.forName("java.lang.foreign.FunctionDescriptor");
        optC = Class.forName("java.lang.foreign.Linker$Option");
        allocC = Class.forName("java.lang.foreign.SegmentAllocator");
        ofIntC = Class.forName("java.lang.foreign.ValueLayout$OfInt");
        ofLongC = Class.forName("java.lang.foreign.ValueLayout$OfLong");
        ofByteC = Class.forName("java.lang.foreign.ValueLayout$OfByte");

        linker = linkerC.getMethod("nativeLinker").invoke(null);
        arena = arenaC.getMethod("global").invoke(null);
        INT = valueC.getField("JAVA_INT").get(null);
        LONG = valueC.getField("JAVA_LONG").get(null);
        BYTE = valueC.getField("JAVA_BYTE").get(null);
        PTR = valueC.getField("ADDRESS").get(null);
        NULL = segC.getField("NULL").get(null);

        fdOf = fdC.getMethod("of", layoutC, Array.newInstance(layoutC, 0).getClass());
        find = lookupC.getMethod("find", String.class);
        downcall = linkerC.getMethod("downcallHandle", segC, fdC, Array.newInstance(optC, 0).getClass());
        allocate = allocC.getMethod("allocate", long.class, long.class);
        getI = segC.getMethod("get", ofIntC, long.class);
        getL = segC.getMethod("get", ofLongC, long.class);
        getB = segC.getMethod("get", ofByteC, long.class);
        address = segC.getMethod("address");
        noOpts = Array.newInstance(optC, 0);
    }

    /** Загрузить библиотеку по имени («X11») или полному пути (фреймворк macOS). */
    Object library(String name) throws Exception {
        return lookupC.getMethod("libraryLookup", String.class, Class.forName("java.lang.foreign.Arena"))
                .invoke(null, name, arena);
    }

    /** Указатель на функцию: тип результата, затем типы аргументов. */
    MethodHandle fn(Object lib, String name, Object ret, Object... args) throws Exception {
        Optional<?> sym = (Optional<?>) find.invoke(lib, name);
        if (!sym.isPresent()) throw new IllegalStateException("нет функции " + name);
        Object layouts = Array.newInstance(layoutC, args.length);
        for (int i = 0; i < args.length; i++) Array.set(layouts, i, args[i]);
        Object fd = fdOf.invoke(null, ret, layouts);
        return (MethodHandle) downcall.invoke(linker, sym.get(), fd, noOpts);
    }

    /** Блок памяти (для возвращаемых по указателю значений). */
    Object alloc(long bytes) throws Exception { return allocate.invoke(arena, bytes, 8L); }

    int getInt(Object seg, long off) throws Exception { return (int) getI.invoke(seg, INT, off); }
    long getLong(Object seg, long off) throws Exception { return (long) getL.invoke(seg, LONG, off); }
    int getByte(Object seg, long off) throws Exception { return (byte) getB.invoke(seg, BYTE, off) & 0xFF; }
    long address(Object seg) throws Exception { return (long) address.invoke(seg); }
}
