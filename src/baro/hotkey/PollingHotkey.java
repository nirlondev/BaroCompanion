package baro.hotkey;


import static baro.Tr.tr;
import javax.swing.SwingUtilities;

/**
 * Глобальное сочетание опросом состояния клавиатуры (Linux и macOS): каждые ~30 мс
 * проверяется, зажато ли сочетание, и по фронту нажатия вызывается обработчик.
 * Опрос, а не перехват, — чтобы не мешать игре и не падать из-за занятого сочетания.
 */
abstract class PollingHotkey implements GlobalHotkey {
    private static final long POLL_MS = 30;

    final String name;
    volatile String status;
    private volatile boolean wantActive = true, running;
    private Thread thread;

    PollingHotkey(String name) { this.name = name; this.status = name; }

    /** true, пока сочетание (нужные модификаторы и клавиша) зажато. Может бросить исключение. */
    abstract boolean comboDown() throws Throwable;

    @Override public synchronized void start(Runnable onPressed) {
        if (running) return;
        running = true;
        thread = new Thread(() -> loop(onPressed), "global-hotkey");
        thread.setDaemon(true);
        thread.start();
    }

    private void loop(Runnable onPressed) {
        boolean wasDown = false;
        try {
            while (running) {
                boolean down = wantActive && comboDown();
                if (down && !wasDown) SwingUtilities.invokeLater(onPressed);
                wasDown = down;
                Thread.sleep(POLL_MS);
            }
        } catch (InterruptedException ignored) {
        } catch (Throwable t) {
            status = name + tr(" не работает: ") + t;
        }
    }

    @Override public void setActive(boolean active) { wantActive = active; }
    @Override public void stop() { running = false; if (thread != null) thread.interrupt(); }
    @Override public boolean isSupported() { return true; }
    @Override public String status() { return status; }
}
