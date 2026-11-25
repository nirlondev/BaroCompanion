package baro;

import baro.app.AppController;
import baro.app.SingleInstance;
import baro.data.GameCatalog;
import baro.hotkey.GlobalHotkey;
import baro.settings.AppSettings;

import javax.swing.SwingUtilities;

/**
 * Baro Companion — справочник Barotrauma.
 * Данные берутся прямо из установленной игры и включённых модов (пакет baro.data.gamefiles);
 * если игра не найдена — используются встроенные данные (пакет baro.data.catalog).
 *
 * Запуск: java -jar BaroCompanion.jar            — главное окно (+ Shift+T или своё сочетание для окна поверх игры)
 *         java -jar BaroCompanion.jar --overlay  — сразу маленькое окно поверх игры
 */
public final class App {
    /** Версия программы (меняется при каждом выпуске — новая копия заменяет запущенную старую). */
    public static final String VERSION = "1.0";

    private App() {}

    public static void main(String[] args) {
        // Рисование через Direct3D держит ~100 МБ памяти видеодрайвера; обычного (GDI) справочнику хватает.
        // Задаётся здесь, а не только в launcher.c: запускатель в готовом exe без clang не пересобрать.
        System.setProperty("sun.java2d.d3d", "false");
        boolean overlayOnly = false;
        for (String a : args) if ("--overlay".equalsIgnoreCase(a) || "-o".equals(a)) overlayOnly = true;
        SingleInstance instance = new SingleInstance(VERSION);
        if (!instance.acquire(overlayOnly ? "overlay" : "main")) return;
        baro.app.MemoryTrimmer.start();
        AppSettings settings = AppSettings.openDefault();
        baro.app.Log.start(settings.dataDir());
        baro.Tr.configure(settings.getString("ui.language", baro.Tr.systemDefault()));
        baro.ui.Theme.configure(settings.getString("ui.theme", "dark"), settings.getDouble("ui.scale", 1.0));
        GameCatalog catalog = GameCatalog.load(settings);
        AppController controller = new AppController(catalog, settings, GlobalHotkey.of(settings.getString("hotkey", GlobalHotkey.DEFAULT)));
        controller.setInstance(instance);
        boolean ov = overlayOnly;
        SwingUtilities.invokeLater(() -> controller.start(ov));
        instance.listen(controller::onSecondLaunch);
    }
}
