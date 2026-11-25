package baro.app;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Возвращает системе память, которую куча набирает во время расчётов (подбор планов, чтение файлов игры и сейвов).
 * Живых данных у программы ~20 МБ, но без этого Java держит раздутую кучу до выхода — сотни мегабайт.
 */
public final class MemoryTrimmer {
    private static final long MB = 1 << 20;
    private static final MemoryMXBean MEMORY = ManagementFactory.getMemoryMXBean();
    /** Сколько занято после последнего сжатия. */
    private static long live = 32 * MB;

    private MemoryTrimmer() {}

    public static void start() {
        try {
            // после сборки мусора оставлять свободными 10–30 % кучи, а не 40–70 % (по умолчанию)
            var vm = ManagementFactory.getPlatformMXBean(com.sun.management.HotSpotDiagnosticMXBean.class);
            vm.setVMOption("MinHeapFreeRatio", "10");
            vm.setVMOption("MaxHeapFreeRatio", "30");
        } catch (Throwable t) { return; } // нет модуля управления — ничего не делаем
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "memory-trimmer");
            t.setDaemon(true);
            return t;
        }).scheduleWithFixedDelay(MemoryTrimmer::trimIfBloated, 30, 30, TimeUnit.SECONDS);
    }

    private static void trimIfBloated() {
        if (MEMORY.getHeapMemoryUsage().getCommitted() < Math.max(96 * MB, 3 * live)) return;
        // куча уменьшается постепенно (0 → 10 → 40 → 100 % лишнего), поэтому несколько сборок подряд
        for (int i = 0; i < 4; i++) System.gc();
        live = MEMORY.getHeapMemoryUsage().getUsed();
    }
}
