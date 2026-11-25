package baro.planner;

import baro.model.CargoMission;
import baro.model.Item;
import baro.pricing.PriceSettings;

import javax.swing.SwingUtilities;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Подбор планов в фоне с запоминанием результата. Пересчёт — когда меняются цены (магазин, репутация и т. п.).
 * Колбэки вызываются в потоке Swing.
 */
public final class PlanCache {
    private static final long BUDGET_PER_MODE_MS = 3000;
    private final CargoPlanner planner;
    private final PriceSettings settings;
    /** Очередь задач: то, что открыл пользователь, — в начало, фоновый предрасчёт — в конец. */
    private final LinkedBlockingDeque<Runnable> queue = new LinkedBlockingDeque<>();
    private final Map<String, List<PlanResult>> done = new ConcurrentHashMap<>();
    /** Ключ → кто ждёт результат; доступ под synchronized (waiting). */
    private final Map<String, List<Consumer<List<PlanResult>>>> waiting = new HashMap<>();
    private final Map<String, Runnable> jobs = new HashMap<>();
    private final List<Runnable> listeners = new ArrayList<>();

    public PlanCache(CargoPlanner planner, PriceSettings settings) {
        this.planner = planner;
        this.settings = settings;
        settings.addListener(() -> { done.clear(); listeners.forEach(Runnable::run); });
        Thread t = new Thread(() -> {
            while (true) {
                try { queue.take().run(); } catch (InterruptedException e) { return; } catch (Throwable ignored) { }
            }
        }, "cargo-planner");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    public CargoPlanner planner() { return planner; }

    /** Сообщить, когда готов любой новый результат (для обновления списков). */
    public void addListener(Runnable r) { listeners.add(r); }

    public Optional<List<PlanResult>> get(CargoMission m, int crates) { return Optional.ofNullable(done.get(key(m, crates))); }

    /** Подобрать планы для миссии (вне очереди — пользователь ждёт). */
    public void request(CargoMission m, int crates, Consumer<List<PlanResult>> callback) { request(m, crates, callback, true); }

    /** Заранее посчитать все миссии (по одному ящику), чтобы список показывал выручку. */
    public void prefetch(List<CargoMission> missions) { for (CargoMission m : missions) request(m, 1, null, false); }

    private String key(CargoMission m, int crates) { return m.id() + "|" + crates + "|" + settings.signature(); }

    private void request(CargoMission m, int crates, Consumer<List<PlanResult>> callback, boolean urgent) {
        String key = key(m, crates);
        List<PlanResult> ready = done.get(key);
        if (ready != null) { if (callback != null) callback.accept(ready); return; }
        synchronized (waiting) {
            List<Consumer<List<PlanResult>>> list = waiting.get(key);
            if (list != null) { // уже в очереди
                if (callback != null) list.add(callback);
                Runnable queued = jobs.get(key);
                if (urgent && queued != null && queue.remove(queued)) queue.addFirst(queued); // поднять в начало очереди
                return;
            }
            list = new ArrayList<>();
            if (callback != null) list.add(callback);
            waiting.put(key, list);
        }
        Map<Item, Integer> cargo = new LinkedHashMap<>();
        m.contents().forEach((i, n) -> cargo.put(i, n * crates));
        Runnable job = () -> {
            List<PlanResult> res;
            try { res = planner.all(cargo, BUDGET_PER_MODE_MS); }
            catch (Throwable t) { res = Collections.emptyList(); }
            done.put(key, res);
            List<Consumer<List<PlanResult>>> cbs;
            synchronized (waiting) { cbs = waiting.remove(key); jobs.remove(key); }
            List<PlanResult> fin = res;
            SwingUtilities.invokeLater(() -> {
                cbs.forEach(c -> c.accept(fin));
                listeners.forEach(Runnable::run);
            });
        };
        synchronized (waiting) { jobs.put(key, job); }
        if (urgent) queue.addFirst(job); else queue.addLast(job);
    }
}
