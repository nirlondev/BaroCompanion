package baro.app;


import static baro.Tr.tr;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Данные, которые загружаются один раз, в фоне, когда впервые понадобились (бестиарий, подлодки). */
public class LazyData<T> {
    private final String name;
    private final Supplier<T> loader;
    private final List<Consumer<T>> waiting = new ArrayList<>();
    private T data;
    private boolean loading, done;

    public LazyData(String name, Supplier<T> loader) { this.name = name; this.loader = loader; }

    /** Отдать данные (в потоке Swing), загрузив при первом обращении; null — данных нет. */
    public void get(Consumer<T> c) {
        if (done) { c.accept(data); return; }
        waiting.add(c);
        if (loading) return;
        loading = true;
        Thread t = new Thread(() -> {
            T b = null;
            try { b = loader.get(); } catch (Throwable e) { System.err.println(name + tr(": не загружено — ") + e); e.printStackTrace(); }
            T result = b;
            SwingUtilities.invokeLater(() -> {
                data = result;
                done = true;
                List<Consumer<T>> l = new ArrayList<>(waiting);
                waiting.clear();
                for (Consumer<T> x : l) x.accept(result);
            });
        }, name);
        t.setDaemon(true);
        t.start();
    }

    /** Уже загружено (или null, если ещё нет). */
    public T now() { return done ? data : null; }
}
