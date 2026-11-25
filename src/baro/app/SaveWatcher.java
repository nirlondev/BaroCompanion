package baro.app;


import static baro.Tr.tr;
import baro.pricing.PriceSettings;
import baro.save.SaveReader;
import baro.save.SaveSnapshot;
import baro.settings.AppSettings;

import javax.swing.SwingUtilities;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Подтягивает сложность, станцию, наценки магазинов, репутацию и требуемые товары из сейва игры.
 * Сейв применяется, только когда он новее уже применённого: всё, что поменяли вручную, держится до следующего сохранения в игре.
 * Настройки: save.auto (true) — следить за сейвами, save.file — конкретный сейв (пусто — самый свежий), save.dir — своя папка сейвов.
 */
public final class SaveWatcher {
    private static final long POLL_SECONDS = 5;
    private final AppSettings settings;
    private final PriceSettings prices;
    private final List<Runnable> listeners = new ArrayList<>();
    private ScheduledExecutorService timer;
    private volatile String status = "";
    private volatile SaveSnapshot last;

    public SaveWatcher(AppSettings settings, PriceSettings prices) { this.settings = settings; this.prices = prices; }

    public boolean isAuto() { return settings.getBoolean("save.auto", true); }

    public void setAuto(boolean on) {
        settings.put("save.auto", on);
        settings.save();
        if (on) check(false);
        fire();
    }

    /** Последний прочитанный сейв (биом, миссии, наличие в магазинах) или пусто. */
    public Optional<SaveSnapshot> last() { return Optional.ofNullable(last); }

    /** Последнее сообщение о сейве (что применено или почему не получилось). */
    public String status() { return status; }

    public void addListener(Runnable r) { listeners.add(r); }

    /** Все найденные сейвы, свежие первыми. */
    public List<Path> saves() { return SaveReader.saveFiles(settings.getString("save.dir", "")); }

    /** Выбранный сейв (null — самый свежий). */
    public Path pinned() { String p = settings.getString("save.file", ""); return p.isEmpty() ? null : Paths.get(p); }

    public void pin(Path p) {
        settings.put("save.file", p == null ? "" : p.toString());
        settings.save();
        check(true);
    }

    public void start() {
        if (timer != null) return;
        timer = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "save-watcher"); t.setDaemon(true); return t; });
        timer.scheduleWithFixedDelay(() -> { if (isAuto()) check(false); }, 0, POLL_SECONDS, TimeUnit.SECONDS);
    }

    /** Загрузить сейв сейчас, даже если он уже применялся (кнопка «Из сейва»). */
    public void loadNow() { check(true); }

    private void check(boolean force) {
        Runnable job = () -> {
            Path file = pinned();
            if (file == null || !Files.isRegularFile(file)) {
                List<Path> all = saves();
                if (all.isEmpty()) { setStatus(tr("Сейвы игры не найдены")); return; }
                file = all.get(0);
            }
            String stamp = file + "|" + SaveReader.modified(file);
            boolean seen = stamp.equals(settings.getString("save.applied", ""));
            if (!force && seen && last != null) return;
            Optional<SaveSnapshot> snap;
            try { snap = Optional.of(SaveReader.read(file)); }
            catch (Exception e) { setStatus(tr("Сейв «") + file.getFileName() + tr("» не прочитан: ") + e.getMessage()); return; }
            SaveSnapshot s = snap.get();
            last = s;
            if (!force && seen) { fire(); return; } // уже применённый сейв (после перезапуска): цены не трогаем, только миссии и биом
            settings.put("save.applied", stamp);
            SwingUtilities.invokeLater(() -> {
                prices.apply(s);
                setStatus(tr("Из сейва: ") + s.describe());
                fire();
            });
        };
        if (timer != null && !Thread.currentThread().getName().equals("save-watcher")) timer.execute(job); else job.run();
    }

    private void setStatus(String s) {
        if (s.equals(status)) return;
        status = s;
        fire();
    }

    private void fire() { SwingUtilities.invokeLater(() -> { for (Runnable r : new ArrayList<>(listeners)) r.run(); }); }
}