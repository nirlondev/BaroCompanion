package baro.ui.main;


import static baro.Tr.tr;
import baro.app.SubmarineSource;
import baro.app.UserLists;
import baro.bestiary.Creature;
import baro.data.GameCatalog;
import baro.model.Affliction;
import baro.model.Item;
import baro.pricing.PriceCalculator;
import baro.settings.AppSettings;
import baro.ui.AppIcon;
import baro.ui.Navigator;
import baro.ui.Theme;
import baro.ui.WindowChrome;
import baro.ui.views.Blocks;
import baro.ui.views.CardContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Главное окно справочника. */
public final class MainWindow extends JFrame implements Navigator {
    private final GameCatalog catalog;
    private final PriceCalculator prices;
    private final AppSettings settings;
    private final UserLists lists;
    private final JTabbedPane tabs = new JTabbedPane();
    private final ItemsTab items;
    private final MedicinesTab medicines;
    private final baro.ui.talents.TalentsTab talents;
    private final AllMissionsTab allMissions;
    private final BestiaryTab bestiary;
    private final ShoppingTab shopping;
    private final SubmarinesTab submarines;
    private final CompareWindow compare;
    private final Blocks blocks;
    private final Runnable restart;
    private final JLabel status = new JLabel();
    private final JButton compareButton = new JButton();
    private DeconFinderDialog deconFinder;

    public MainWindow(GameCatalog catalog, PriceCalculator prices, baro.planner.PlanCache plans, AppSettings settings, baro.app.SaveWatcher saves,
                      UserLists lists, BestiarySource lore, SubmarineSource subs, Runnable toggleOverlay, Runnable onClose, Runnable restart) {
        super("Baro Companion");
        this.catalog = catalog;
        this.prices = prices;
        this.settings = settings;
        this.lists = lists;
        this.restart = restart;
        setIconImages(java.util.Arrays.asList(AppIcon.create(16), AppIcon.create(32), AppIcon.create(64)));
        CardContext ctx = new CardContext(catalog, lists, saves::last, new CardContext.Actions() {
            @Override public void openCompare() { MainWindow.this.compare.showWindow(); }
            @Override public void openShopping() { tabs.setSelectedComponent(shopping); }
            @Override public void compare(Object creature) { if (creature instanceof Creature c) MainWindow.this.compare.toggle(c); }
        });
        blocks = new Blocks(prices, this, 13f, ctx);
        items = new ItemsTab(catalog, prices, blocks, this, lists, settings);
        medicines = new MedicinesTab(catalog, blocks, this);
        tabs.addTab(tr("Предметы"), items);
        tabs.addTab(tr("Медицина"), medicines);
        talents = new baro.ui.talents.TalentsTab(catalog, settings, blocks, this);
        tabs.addTab(tr("Таланты"), talents);
        allMissions = new AllMissionsTab(catalog, lore, blocks, this, saves, new baro.pricing.CargoValuation(prices), plans);
        tabs.addTab(tr("Миссии"), allMissions);
        bestiary = new BestiaryTab(catalog, lore, blocks, this);
        tabs.addTab(tr("Бестиарий"), bestiary);
        submarines = new SubmarinesTab(subs, blocks);
        tabs.addTab(tr("Подлодки"), submarines);
        tabs.addTab(tr("Проводка"), new WiringTab(subs, catalog, blocks));
        shopping = new ShoppingTab(lists, blocks);
        tabs.addTab(tr("Покупки"), shopping);
        compare = new CompareWindow(this, lists, catalog, blocks);
        lists.addListener(this::updateCounters);
        saves.addListener(() -> { items.saveChanged(); shopping.pricesChanged(); });

        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(Theme.HEADER);
        south.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        status.setForeground(Theme.MUTED);
        JButton overlay = button(tr("Окно поверх игры"), tr("Маленькое окно поверх игры (") + baro.hotkey.GlobalHotkey.name(settings.getString("hotkey", baro.hotkey.GlobalHotkey.DEFAULT)) + ")", toggleOverlay);
        JButton prefs = button(tr("Настройки"), tr("Папка игры, моды, язык, сейвы, тема, размер шрифта"), this::openSettings);
        JButton boosty = button("Boosty", tr("Поддержать справочник на Boosty"), () -> baro.ui.Links.open(this, baro.ui.Links.BOOSTY));
        compareButton.setFocusable(false);
        compareButton.setToolTipText(tr("Сравнение предметов и существ рядом"));
        compareButton.addActionListener(e -> compare().showWindow());
        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        east.setOpaque(false);
        JButton update = new JButton();
        update.setFocusable(false);
        update.setForeground(Theme.SELL);
        update.setVisible(false);
        east.add(update); east.add(compareButton); east.add(boosty); east.add(prefs); east.add(overlay);
        if (settings.getBoolean("update.check", true)) baro.app.UpdateCheck.start((version, page) -> {
            update.setText(tr("Доступна версия ") + version);
            update.setToolTipText(tr("Открыть страницу загрузки новой версии"));
            update.addActionListener(e -> baro.ui.Links.open(this, page));
            update.setVisible(true);
        });
        south.add(status, BorderLayout.CENTER);
        south.add(east, BorderLayout.EAST);

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(new PriceBar(prices.settings(), saves, this::openDeconFinder), BorderLayout.NORTH);
        if (!catalog.fromGame()) north.add(embeddedBanner(), BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(north, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);

        prices.settings().addListener(() -> {
            items.pricesChanged(); medicines.pricesChanged(); allMissions.pricesChanged();
            shopping.pricesChanged(); submarines.pricesChanged(); compare.pricesChanged();
        });
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        WindowChrome.install(this);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { saveBounds(); onClose.run(); }
            @Override public void windowOpened(WindowEvent e) { SwingUtilities.invokeLater(() -> WelcomeDialog.maybeShow(MainWindow.this, settings, blocks)); }
        });
        setBounds(settings.getInt("main.x", 80), settings.getInt("main.y", 60), settings.getInt("main.w", 1280), settings.getInt("main.h", 800));
        if (!onScreen(getBounds())) setLocationRelativeTo(null);
        restoreState();
        tabs.addChangeListener(e -> { settings.put("main.view", tabs.getSelectedComponent().getClass().getSimpleName()); settings.save(); });
        updateCounters();
    }

    private static JButton button(String text, String tip, Runnable r) {
        JButton b = new JButton(text);
        b.setFocusable(false);
        b.setToolTipText(tip);
        b.addActionListener(e -> r.run());
        return b;
    }

    /** Плашка: игра не найдена — данные встроенные (могут отставать от версии игры). */
    private JComponent embeddedBanner() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(Theme.isLight() ? new Color(0xfff3d6) : new Color(0x3a2f14));
        p.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        JLabel l = new JLabel(tr("Игра не найдена — показаны встроенные данные: цены и рецепты могут отличаться от вашей версии игры, нет иконок, модов и подлодок."));
        l.setForeground(Theme.WARN);
        JButton b = button(tr("Указать папку игры…"), tr("Открыть настройки"), this::openSettings);
        p.add(l, BorderLayout.CENTER);
        p.add(b, BorderLayout.EAST);
        return p;
    }

    private void updateCounters() {
        int n = lists.compare().size();
        compareButton.setText(n > 0 ? tr("Сравнение (") + n + ")" : tr("Сравнение"));
        int s = lists.shopping().size();
        tabs.setTitleAt(tabs.indexOfComponent(shopping), s > 0 ? tr("Покупки (") + s + ")" : tr("Покупки"));
    }

    /** Открыть вкладку и предмет, на которых закрыли программу. */
    private void restoreState() {
        // вкладка запоминается по классу, а не по номеру: номера меняются, когда вкладки добавляются или объединяются
        String view = settings.getString("main.view", "");
        for (int i = 0; i < tabs.getTabCount(); i++)
            if (tabs.getComponentAt(i).getClass().getSimpleName().equals(view)) tabs.setSelectedIndex(i);
        catalog.item(settings.getString("items.last", "")).ifPresent(it -> {
            Component sel = tabs.getSelectedComponent();
            items.open(it);
            if (sel != items) tabs.setSelectedComponent(sel);
        });
    }

    private CompareWindow compare() { return compare; }

    private void openSettings() { new SettingsDialog(this, settings, restart).setVisible(true); }

    public void setHotkeyStatus(String text) {
        status.setText(tr("Окно поверх игры: ") + text + tr("   ·   Esc в окне — скрыть   ·   Данные: ") + (catalog.fromGame() ? tr("из файлов игры") : tr("встроенные")));
        status.setToolTipText(catalog.source());
    }

    private static boolean onScreen(Rectangle r) {
        for (GraphicsDevice d : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
            if (d.getDefaultConfiguration().getBounds().intersects(r)) return true;
        return false;
    }

    private void saveBounds() {
        Rectangle b = getBounds();
        settings.put("main.x", b.x); settings.put("main.y", b.y); settings.put("main.w", b.width); settings.put("main.h", b.height);
        settings.save();
    }

    @Override public void open(Item item) {
        if (tabs.getSelectedComponent() == medicines && medicines.contains(item)) medicines.open(item);
        else { tabs.setSelectedComponent(items); items.open(item); }
    }

    @Override public void openTalent(baro.model.Talent t) { tabs.setSelectedComponent(talents); talents.show(t); }

    @Override public void openLore(Object entry) {
        if (entry instanceof Affliction a) { tabs.setSelectedComponent(medicines); medicines.open(a); }
        else if (entry instanceof baro.bestiary.MissionInfo) { tabs.setSelectedComponent(allMissions); allMissions.open(entry); }
        else { tabs.setSelectedComponent(bestiary); bestiary.open(entry); }
    }

    /** Показать талант (из окна поверх игры). */
    public void revealTalent(baro.model.Talent t) {
        bringUp();
        openTalent(t);
    }

    /** Показать запись бестиария, миссию или недуг (из окна поверх игры). */
    public void revealLore(Object entry) {
        bringUp();
        openLore(entry);
    }

    /** Показать список покупок (из окна поверх игры). */
    public void revealShopping() {
        bringUp();
        tabs.setSelectedComponent(shopping);
    }

    @Override public void openMission(baro.model.CargoMission m) { tabs.setSelectedComponent(allMissions); allMissions.open(m); }

    /** Показать миссию (из окна поверх игры). */
    public void revealMission(baro.model.CargoMission m) {
        bringUp();
        openMission(m);
    }

    /** Показать предмет (из окна поверх игры): препараты — на вкладке «Медицина». */
    public void reveal(Item item) {
        bringUp();
        if (medicines.contains(item)) { tabs.setSelectedComponent(medicines); medicines.open(item); }
        else { tabs.setSelectedComponent(items); items.open(item); }
    }

    private void bringUp() {
        setVisible(true);
        setState(Frame.NORMAL);
        toFront();
    }

    private void openDeconFinder() {
        if (deconFinder == null) deconFinder = new DeconFinderDialog(this, catalog, prices, this);
        Item sel = items.selected();
        if (sel != null && !sel.obtainedFrom().isEmpty()) deconFinder.select(sel);
        deconFinder.setVisible(true);
        deconFinder.toFront();
    }
}
