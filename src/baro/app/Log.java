package baro.app;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;

/**
 * Журнал программы: всё, что пишется в System.out и System.err, дублируется в log.txt рядом с настройками
 * (в exe консоли нет — без журнала ошибки пропадали). Файл больше 1 МБ при запуске переносится в log.old.txt.
 * Необработанные исключения тоже попадают в журнал. Последние строки прикладываются к отчёту о неточности.
 */
public final class Log {
    private static final long MAX = 1 << 20;
    private static final int TAIL = 80;
    private static final Deque<String> tail = new ArrayDeque<>();
    private static Path file;

    private Log() {}

    /** Начать журнал в папке dir (до создания окон). */
    public static synchronized void start(Path dir) {
        if (file != null) return;
        try {
            Files.createDirectories(dir);
            file = dir.resolve("log.txt");
            if (Files.isRegularFile(file) && Files.size(file) > MAX)
                Files.move(file, dir.resolve("log.old.txt"), StandardCopyOption.REPLACE_EXISTING);
            OutputStream out = new BufferedOutputStream(new FileOutputStream(file.toFile(), true), 8192);
            PrintStream fileStream = new PrintStream(out, true, StandardCharsets.UTF_8);
            fileStream.println();
            fileStream.println("=== " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " · версия " + baro.App.VERSION
                    + " · Java " + System.getProperty("java.version") + " · " + System.getProperty("os.name"));
            System.setOut(tee(System.out, fileStream));
            System.setErr(tee(System.err, fileStream));
        } catch (IOException e) {
            file = null;
            return;
        }
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("Необработанная ошибка в потоке «" + t.getName() + "»:");
            e.printStackTrace();
        });
    }

    /** Файл журнала или null, если журнал не ведётся. */
    public static Path file() { return file; }

    /** Последние строки журнала (для отчёта). */
    public static synchronized String tail() { return String.join("\n", tail); }

    private static synchronized void remember(String line) {
        tail.addLast(line);
        while (tail.size() > TAIL) tail.removeFirst();
    }

    /** Поток, который пишет и в исходный поток, и в файл, а строки запоминает для отчёта. */
    private static PrintStream tee(PrintStream original, PrintStream file) {
        OutputStream both = new OutputStream() {
            private final ByteArrayOutputStream line = new ByteArrayOutputStream();
            @Override public void write(int b) { original.write(b); file.write(b); collect(b); }
            @Override public void write(byte[] b, int off, int len) { original.write(b, off, len); file.write(b, off, len); for (int i = off; i < off + len; i++) collect(b[i]); }
            @Override public void flush() { original.flush(); file.flush(); }
            private void collect(int b) {
                if (b == '\n') { remember(line.toString(StandardCharsets.UTF_8).stripTrailing()); line.reset(); }
                else if (line.size() < 2000) line.write(b);
            }
        };
        return new PrintStream(both, true, StandardCharsets.UTF_8);
    }
}
