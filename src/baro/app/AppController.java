package baro.app;


import static baro.Tr.tr;
import baro.data.GameCatalog;
import baro.hotkey.GlobalHotkey;
import baro.model.Item;
import baro.pricing.PriceCalculator;
import baro.pricing.PriceSettings;
import baro.settings.AppSettings;
import baro.ui.Theme;
import baro.ui.main.MainWindow;
import baro.ui.overlay.OverlayWindow;

import javax.swing.*;

/** Связывает данные, настройки, окна и глобальное сочетание клавиш (по умолчанию Shift+T). */
public final class AppController {
    private final GameCatalog catalog;
    private final AppSettings settings;
    private final PriceCalculator prices;
    private final GlobalHotkey hotkey;
    private final baro.planner.PlanCache plans;
    private final UserLists lists;
    private final baro.ui.main.BestiarySource lore;
    private final SubmarineSource subs;
    private SingleInstance instance;
    private MainWindow main;
    private OverlayWindow overlay;
    private boolean trayInstalled;

    public AppController(GameCatalog catalog, AppSettings settings, GlobalHotkey hotkey) {
        this.catalog = catalog;
        this.settings = settings;
        this.prices = new PriceCalculator(new PriceSettings(settings));
        this.hotkey = hotkey;
        this.plans = new baro.planner.PlanCache(new baro.planner.CargoPlanner(prices), prices.settings());
        this.saves = new SaveWatcher(settings, prices.settings());
        this.lists = new UserLists(catalog, settings);
        this.lore = new baro.ui.main.BestiarySource(settings);
        this.subs = new SubmarineSource(settings);
    }

    /** Одна копия программы: при перезапуске её порт освобождается для новой. */
    public void setInstance(SingleInstance instance) { this.instance = instance; }

    private final SaveWatcher saves;

    /** Запуск в потоке Swing. */
    public void start(boolean overlayOnly) {
        Theme.install();
        saves.start();
        overlay = new OverlayWindow(catalog, prices, plans, settings, this::revealInMain, m -> main().revealMission(m), t -> main().revealTalent(t), this::showMain,
                lists, saves, lore, e -> main().revealLore(e), () -> main().revealShopping());
        trayInstalled = TrayController.install(this::showMain, this::toggleOverlay, this::exit, baro.hotkey.GlobalHotkey.name(settings.getString("hotkey", baro.hotkey.GlobalHotkey.DEFAULT)));
        hotkey.start(this::toggleOverlay);
        if (overlayOnly) overlay.showAndFocus(); else showMain();
        updateStatus();
        // статус сочетания определяется в фоновом потоке — обновим подпись чуть позже
        new Timer(1500, e -> updateStatus()) {{ setRepeats(false); }}.start();
    }

    private MainWindow main() {
        if (main == null) main = new MainWindow(catalog, prices, plans, settings, saves, lists, lore, subs, this::toggleOverlay, this::mainClosed, this::restart);
        return main;
    }

    void showMain() {
        MainWindow m = main();
        m.setVisible(true);
        m.setState(java.awt.Frame.NORMAL);
        m.toFront();
        updateStatus();
    }

    void toggleOverlay() { overlay.toggle(); }

    /** Программу запустили ещё раз: показать нужное окно уже работающей копии. */
    public void onSecondLaunch(String command) {
        if ("exit".equals(command)) { exit(); return; }
        if (overlay == null) return;
        if ("overlay".equals(command)) overlay.showAndFocus(); else showMain();
    }

    private void revealInMain(Item item) { main().reveal(item); }

    private void mainClosed() {
        main.setVisible(false);
        // Без значка в трее программу нельзя было бы вернуть — тогда закрываем полностью.
        if (!trayInstalled && !overlay.isVisible()) exit();
    }

    private void updateStatus() {
        if (main != null) main.setHotkeyStatus(hotkey.isSupported() ? hotkey.status() : hotkey.status() + tr(" — кнопка справа"));
    }

    /**
     * Перезапуск (после смены настроек): та же команда, что запустила программу (exe или java …).
     * Порт «одной копии» освобождается заранее, чтобы новая копия не отдала команду этой и не закрылась.
     */
    void restart() {
        ProcessHandle.Info info = ProcessHandle.current().info();
        java.util.Optional<String> cmd = info.command();
        if (cmd.isEmpty()) {
            JOptionPane.showMessageDialog(main, tr("Перезапустите программу вручную, чтобы применить настройки."), tr("Перезапуск"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        java.util.List<String> line = new java.util.ArrayList<>();
        line.add(cmd.get());
        info.arguments().ifPresent(a -> line.addAll(java.util.Arrays.asList(a)));
        hotkey.stop();
        settings.saveNow();
        if (instance != null) instance.release();
        try { new ProcessBuilder(line).inheritIO().start(); }
        catch (java.io.IOException e) {
            JOptionPane.showMessageDialog(main, tr("Не удалось перезапустить: ") + e.getMessage() + tr(". Запустите программу вручную."), tr("Перезапуск"), JOptionPane.WARNING_MESSAGE);
        }
        System.exit(0);
    }

    void exit() {
        hotkey.stop();
        settings.saveNow();
        System.exit(0);
    }
}
