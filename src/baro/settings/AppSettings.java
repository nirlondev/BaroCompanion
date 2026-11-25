package baro.settings;


import static baro.Tr.tr;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Настройки программы. Хранятся в файле settings.properties рядом с программой (внутри папки проекта),
 * а не в реестре или профиле пользователя.
 */
public final class AppSettings {
    /** Через сколько записывать изменения: перетаскивание окна или прокрутка дают десятки save() в секунду. */
    private static final long SAVE_DELAY_MS = 400;
    private static final Set<AppSettings> PENDING = ConcurrentHashMap.newKeySet();
    private static final ScheduledExecutorService WRITER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "settings-writer");
        t.setDaemon(true);
        return t;
    });
    static { Runtime.getRuntime().addShutdownHook(new Thread(AppSettings::flushAll, "settings-flush")); }

    private final Path file;
    private final Properties props = new Properties();

    private AppSettings(Path file) { this.file = file; }

    /**
     * Файл настроек: путь из свойства baro.settings (его задаёт .exe — %LOCALAPPDATA%\\BaroCompanion),
     * иначе settings.properties рядом с программой.
     */
    public static AppSettings openDefault() {
        String custom = System.getProperty("baro.settings");
        Path file = custom != null && !custom.isEmpty() ? Paths.get(custom) : appDirectory().resolve("settings.properties");
        try { if (file.getParent() != null) Files.createDirectories(file.getParent()); } catch (IOException ignored) { }
        migrateOldName(file);
        for (AppSettings p : PENDING.toArray(new AppSettings[0])) if (p.file.equals(file)) p.flush(); // отложенные изменения этого файла должны быть видны
        AppSettings s = new AppSettings(file);
        s.load();
        return s;
    }

    /**
     * Программа называлась BaroSpravochnik: при первом запуске под новым именем переносим из старой папки настроек
     * (…\BaroSpravochnik рядом с …\BaroCompanion) файлы настроек, журнал и кэш иконок. Старая папка остаётся как была.
     */
    private static void migrateOldName(Path file) {
        Path dir = file.getParent();
        if (Files.exists(file) || dir == null || dir.getParent() == null || !dir.getFileName().toString().equals("BaroCompanion")) return;
        Path old = dir.resolveSibling("BaroSpravochnik");
        if (!Files.isRegularFile(old.resolve(file.getFileName()))) return;
        try (var files = Files.list(old)) {
            for (Path f : (Iterable<Path>) files::iterator)
                if (Files.isRegularFile(f) && !f.getFileName().toString().endsWith(".tmp"))
                    Files.copy(f, dir.resolve(f.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) { System.err.println(tr("Настройки не перенесены из ") + old + " — " + e); }
    }

    /** Папка, где лежит программа (jar) или каталог классов. */
    public static Path appDirectory() {
        try {
            Path p = Paths.get(AppSettings.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return Files.isDirectory(p) ? p : p.getParent();
        } catch (URISyntaxException | NullPointerException e) {
            return Paths.get(".").toAbsolutePath();
        }
    }

    private void load() {
        if (!Files.exists(file)) return;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { props.load(r); }
        catch (IOException e) { System.err.println(tr("Настройки не прочитаны: ") + file + " — " + e); }
    }

    /** Записать изменения на диск — чуть позже и в фоне; частые вызовы подряд дают одну запись. */
    public void save() {
        if (PENDING.add(this)) WRITER.schedule(this::flush, SAVE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /** Записать на диск сейчас (при выходе из программы). */
    public void saveNow() {
        PENDING.remove(this);
        write();
    }

    private void flush() { if (PENDING.remove(this)) write(); }

    private static void flushAll() { for (AppSettings s : PENDING.toArray(new AppSettings[0])) s.flush(); }

    /** Через временный файл: если запись прервётся, прежние настройки останутся целыми. */
    private synchronized void write() {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) { props.store(w, "Baro Companion"); }
            try { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) { System.err.println(tr("Настройки не записаны: ") + file + " — " + e); }
    }

    public String getString(String key, String def) { return props.getProperty(key, def); }
    public int getInt(String key, int def) {
        try { return Integer.parseInt(props.getProperty(key, String.valueOf(def)).trim()); } catch (NumberFormatException e) { return def; }
    }
    public double getDouble(String key, double def) {
        try { return Double.parseDouble(props.getProperty(key, String.valueOf(def)).trim()); } catch (NumberFormatException e) { return def; }
    }
    public boolean getBoolean(String key, boolean def) { return Boolean.parseBoolean(props.getProperty(key, String.valueOf(def))); }

    public void put(String key, Object value) { props.setProperty(key, String.valueOf(value)); }

    /** Список строк, записанный через запятую (пустые элементы пропускаются). */
    public java.util.List<String> getList(String key) {
        java.util.List<String> l = new java.util.ArrayList<>();
        for (String s : getString(key, "").split(",")) if (!s.isBlank()) l.add(s.trim());
        return l;
    }

    public void putList(String key, java.util.Collection<String> values) { put(key, String.join(",", values)); }

    /** Папка с настройками: туда же кладутся кэш иконок и другие файлы программы. */
    public Path dataDir() { Path p = file.toAbsolutePath().getParent(); return p != null ? p : Paths.get(".").toAbsolutePath(); }
}
