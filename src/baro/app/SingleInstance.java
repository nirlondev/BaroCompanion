package baro.app;

import javax.swing.SwingUtilities;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Одна копия программы: повторный запуск не открывает вторую, а просит уже работающую показать окно.
 * Связь через локальный порт (только 127.0.0.1).
 */
public final class SingleInstance {
    private static final int PORT = 47613;
    private static final String HELLO = "baro-spravochnik";
    private final String version;
    private ServerSocket server;

    /** @param version версия программы: старая работающая копия уступает место новой */
    public SingleInstance(String version) { this.version = version; }

    /** @return true — мы первые; false — сообщение передано работающей копии, нужно выйти. */
    public boolean acquire(String command) {
        if (bind()) return true;
        String reply;
        try (Socket s = new Socket(InetAddress.getLoopbackAddress(), PORT)) {
            s.setSoTimeout(2000);
            Writer w = new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8);
            w.write(HELLO + " " + version + " " + command + "\n");
            w.flush();
            reply = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8)).readLine();
        } catch (IOException other) {
            return true; // порт занят чем-то посторонним — просто работаем
        }
        if ("ok".equals(reply)) return false; // та же версия уже работает и покажет окно
        if (!"exit".equals(reply)) terminateOldCopies(); // очень старая версия не умеет уступать — закрываем её
        // работала старая версия — она закрывается, занимаем её место
        for (int i = 0; i < 50; i++) {
            if (bind()) return true;
            try { Thread.sleep(100); } catch (InterruptedException e) { break; }
        }
        return true;
    }

    /** Закрыть другие запущенные копии программы (exe или javaw с BaroCompanion.jar; и прежние BaroSpravochnik). */
    private static void terminateOldCopies() {
        long self = ProcessHandle.current().pid();
        ProcessHandle.allProcesses().filter(p -> p.pid() != self).forEach(p -> {
            String cmd = p.info().command().orElse("").toLowerCase();
            String line = p.info().commandLine().orElse("").toLowerCase();
            if (cmd.endsWith("barocompanion.exe") || line.contains("barocompanion.jar")
                    || cmd.endsWith("barospravochnik.exe") || line.contains("barospravochnik.jar")) {
                p.destroyForcibly();
                try { p.onExit().get(3, java.util.concurrent.TimeUnit.SECONDS); } catch (Exception ignored) { }
            }
        });
    }

    /** Освободить порт (перед перезапуском — новая копия займёт его). */
    public void release() {
        try { if (server != null) server.close(); } catch (IOException ignored) { }
    }

    private boolean bind() {
        try { server = new ServerSocket(PORT, 8, InetAddress.getLoopbackAddress()); return true; }
        catch (IOException busy) { return false; }
    }

    /** Слушать команды от повторных запусков; обработчик вызывается в потоке Swing. */
    public void listen(Consumer<String> handler) {
        if (server == null) return;
        Thread t = new Thread(() -> {
            while (!server.isClosed()) {
                try (Socket s = server.accept()) {
                    s.setSoTimeout(2000);
                    BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                    Writer w = new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8);
                    String line = r.readLine();
                    if (line == null || !line.startsWith(HELLO + " ")) continue;
                    String[] parts = line.substring(HELLO.length() + 1).trim().split(" ", 2);
                    if (parts.length == 2 && !parts[0].equals(version)) {
                        w.write("exit\n"); w.flush();
                        server.close();
                        SwingUtilities.invokeLater(() -> handler.accept("exit"));
                        return;
                    }
                    w.write("ok\n"); w.flush();
                    String cmd = parts.length == 2 ? parts[1] : parts[0];
                    SwingUtilities.invokeLater(() -> handler.accept(cmd));
                } catch (IOException ignored) { }
            }
        }, "single-instance");
        t.setDaemon(true);
        t.start();
    }
}
