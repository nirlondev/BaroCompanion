package baro.ui.overlay;


import static baro.Tr.tr;
import baro.data.GameCatalog;
import baro.model.CargoMission;
import baro.model.Effect;
import baro.pricing.CargoValuation;
import baro.model.Item;
import baro.model.Medicine;
import baro.pricing.Format;
import baro.pricing.PriceCalculator;
import baro.search.QuickHit;
import baro.search.QuickSearch;
import baro.settings.AppSettings;
import baro.ui.AppIcon;
import baro.ui.Navigator;
import baro.ui.Theme;
import baro.ui.views.Blocks;
import baro.ui.views.PlanView;
import baro.ui.widgets.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Маленькое полупрозрачное окно поверх игры. Пока запрос пуст — строка поиска и, если выбрано в меню, закреплённая панель:
 * список покупок или миссии из сейва и монстры биома; при вводе появляются до 5 результатов и краткая карточка выбранного.
 * ↑↓ — выбор, Enter — открыть в справочнике, Esc — очистить / скрыть, Ctrl+колесо — прозрачность,
 * правая кнопка — меню.
 */
public final class OverlayWindow extends JFrame implements Navigator {
    private static final int ROWS = 5;
    private final int ROW_H = Theme.px(19), CARD_MAX = Theme.px(300);
    /** Что показывать, когда поиск пуст. */
    private enum Panel { NONE, SHOP, SAVE }
    private static final float FONT = 12f;

    private final QuickSearch search;
    private final GameCatalog catalog;
    private final AppSettings settings;
    private final Blocks blocks;
    private final Consumer<Item> openInMain;
    private final Consumer<CargoMission> openMissionInMain;
    private final Consumer<baro.model.Talent> openTalentInMain;
    private final CargoValuation valuation;
    private final baro.planner.PlanCache plans;
    private final baro.app.UserLists lists;
    private final baro.app.SaveWatcher saves;
    private final baro.ui.main.BestiarySource lore;
    private final Consumer<Object> openLoreInMain;
    private final Runnable openShoppingInMain;
    private CargoMission pinnedMission;
    private final SearchField query = new SearchField(tr("Поиск… (Esc — скрыть)"), 10);
    private final JToggleButton medMode = new JToggleButton(tr("Мед"));
    private final DefaultListModel<QuickHit> model = new DefaultListModel<>();
    private final JList<QuickHit> list = new JList<>(model);
    private final JScrollPane listScroll;
    private final JScrollPane cardScroll;
    private final boolean translucent;
    private Item pinned; // предмет, открытый по ссылке из карточки
    private Point dragFrom;

    public OverlayWindow(GameCatalog catalog, PriceCalculator prices, baro.planner.PlanCache plans, AppSettings settings, Consumer<Item> openInMain,
                         Consumer<CargoMission> openMissionInMain, Consumer<baro.model.Talent> openTalentInMain, Runnable openMain,
                         baro.app.UserLists lists, baro.app.SaveWatcher saves, baro.ui.main.BestiarySource lore,
                         Consumer<Object> openLoreInMain, Runnable openShoppingInMain) {
        super(tr("Справочник — поверх игры"));
        this.lists = lists;
        this.saves = saves;
        this.lore = lore;
        this.openLoreInMain = openLoreInMain;
        this.openShoppingInMain = openShoppingInMain;
        this.catalog = catalog;
        this.settings = settings;
        this.openInMain = openInMain;
        this.openMissionInMain = openMissionInMain;
        this.openTalentInMain = openTalentInMain;
        this.valuation = new CargoValuation(prices);
        this.plans = plans;
        this.search = new QuickSearch(catalog, prices);
        this.blocks = new Blocks(prices, this, FONT - 0.5f, new baro.ui.views.CardContext(catalog, lists, saves::last, new baro.ui.views.CardContext.Actions() {
            @Override public void openCompare() { openMain.run(); }
            @Override public void openShopping() { openShoppingInMain.run(); }
            @Override public void compare(Object creature) { }
        }));
        setUndecorated(true);
        setType(Type.UTILITY);
        setAlwaysOnTop(true);
        setIconImage(AppIcon.create(32));
        translucent = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);

        // --- строка поиска ---
        JLabel grip = new JLabel(new GripIcon());
        grip.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 4));
        grip.setForeground(Theme.MUTED);
        grip.setFont(Theme.font(FONT + 2, true));
        grip.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        grip.setToolTipText(tr("Перетащить · правая кнопка — меню"));
        query.setFont(Theme.font(FONT, false));
        query.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        query.setBackground(Theme.SUNK);
        query.setForeground(Theme.FG);
        query.setCaretColor(Theme.FG);
        medMode.setFont(Theme.font(FONT - 1, true));
        medMode.setMargin(new Insets(0, 4, 0, 4));
        medMode.setFocusable(false);
        medMode.setToolTipText(tr("Всё — все предметы · Мед — только медикаменты (поиск и по эффекту: «кровь», «скорость», «хаск»)"));
        medMode.setSelected(settings.getBoolean("overlay.med", false));
        medMode.setText(medMode.isSelected() ? tr("Мед") : tr("Всё"));
        JButton close = tinyButton("×", tr("Скрыть (Esc)"));
        close.addActionListener(e -> setVisible(false));

        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        east.setOpaque(false);
        east.add(medMode); east.add(close);
        JPanel bar = new JPanel(new BorderLayout(2, 0));
        bar.setBackground(Theme.PANEL);
        bar.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 2));
        bar.add(grip, BorderLayout.WEST);
        bar.add(query, BorderLayout.CENTER);
        bar.add(east, BorderLayout.EAST);

        // --- результаты ---
        Rows row = new Rows(FONT);
        list.setFixedCellHeight(ROW_H);
        list.setFixedCellWidth(40);
        list.setFocusable(false);
        list.setCellRenderer((l, h, i, sel, f) -> row.set(h.title(), h.mission().isPresent() ? Theme.ACCENT : Theme.FG,
                rightText(h, prices), h.effect().isPresent() || h.mission().isPresent() ? Theme.ACCENT : Theme.SELL, sel,
                IconRowIcon.of(h.item() != null ? blocks.icon(h.item(), 16) : null, h.isBest() ? StarBadge.of(13) : null)));
        // (у талантов и миссий значок не ставится)
        list.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) { pinned = null; pinnedMission = null; updateCard(); } });
        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) openSelected(); }
        });
        listScroll = Theme.scroll(list);
        listScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.LINE));

        cardScroll = Theme.scroll(null);
        cardScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.LINE));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createLineBorder(Theme.ACCENT_DARK));
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(listScroll, BorderLayout.NORTH);
        body.add(cardScroll, BorderLayout.CENTER);
        root.add(bar, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        setContentPane(root);

        // --- поведение ---
        query.onChange(this::refill);
        medMode.addActionListener(e -> { medMode.setText(medMode.isSelected() ? tr("Мед") : tr("Всё")); settings.put("overlay.med", medMode.isSelected()); settings.save(); refill(); query.requestFocusInWindow(); });
        prices.settings().addListener(() -> { list.repaint(); updateCard(); });
        lists.addListener(() -> { if (query.getText().trim().isEmpty() && panel() == Panel.SHOP) updateCard(); });
        saves.addListener(() -> { if (query.getText().trim().isEmpty() && panel() == Panel.SAVE && isVisible()) updateCard(); });
        bindKeys();
        MouseAdapter drag = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { if (SwingUtilities.isLeftMouseButton(e)) dragFrom = e.getLocationOnScreen(); maybeMenu(e); }
            @Override public void mouseReleased(MouseEvent e) { dragFrom = null; saveBounds(); maybeMenu(e); }
            @Override public void mouseDragged(MouseEvent e) {
                if (dragFrom == null) return;
                Point p = e.getLocationOnScreen();
                setLocation(getX() + p.x - dragFrom.x, getY() + p.y - dragFrom.y);
                dragFrom = p;
            }
            private void maybeMenu(MouseEvent e) { if (e.isPopupTrigger()) menu(openMain).show(e.getComponent(), e.getX(), e.getY()); }
        };
        for (Component c : new Component[]{grip, bar, east}) { c.addMouseListener(drag); c.addMouseMotionListener(drag); }
        MouseWheelListener wheel = e -> { if (e.isControlDown()) { setOpacityLevel(opacityLevel() - (float) e.getPreciseWheelRotation() * 0.05f); e.consume(); } };
        root.addMouseWheelListener(wheel);
        query.addMouseWheelListener(wheel);

        setLocation(settings.getInt("overlay.x", 40), settings.getInt("overlay.y", 40));
        setOpacityLevel((float) settings.getDouble("overlay.opacity", 0.82));
        resizeToContent();
    }

    // ---------- публичное ----------

    /** Показать и сразу поставить курсор в поиск. */
    public void showAndFocus() {
        if (query.getText().trim().isEmpty()) updateCard(); // закреплённая панель могла устареть
        setVisible(true);
        toFront();
        requestFocus();
        SwingUtilities.invokeLater(() -> { query.requestFocusInWindow(); query.selectAll(); });
    }

    public void toggle() { if (isVisible()) setVisible(false); else showAndFocus(); }

    /** Ссылка из карточки: показать предмет прямо в окне. */
    @Override public void open(Item item) { pinned = item; pinnedMission = null; updateCard(); }

    // ---------- поиск и карточка ----------

    private static String rightText(QuickHit h, PriceCalculator prices) {
        if (h.mission().isPresent()) return tr("миссия");
        if (h.talent().isPresent()) return tr("талант · ") + h.talent().get().job().name();
        if (h.effect().isPresent()) return Format.signed(h.effect().get().value().orElse(0.0)) + " · " + prices.sellPrice(h.item()).map(Format::money).orElse("—");
        return prices.sellPrice(h.item()).map(Format::money).orElse("");
    }

    private void refill() {
        List<QuickHit> hits = search.find(query.getText(), medMode.isSelected(), 40);
        model.clear();
        for (QuickHit h : hits) model.addElement(h);
        pinned = null;
        pinnedMission = null;
        if (!hits.isEmpty()) list.setSelectedIndex(0);
        updateCard();
    }

    private CargoMission currentMission() {
        if (pinned != null) return null;
        if (pinnedMission != null) return pinnedMission;
        return list.getSelectedValue() == null ? null : list.getSelectedValue().mission().orElse(null);
    }

    private Item currentItem() {
        if (pinned != null) return pinned;
        if (pinnedMission != null) return null;
        return list.getSelectedValue() == null ? null : list.getSelectedValue().item();
    }

    private void updateCard() {
        Item item = currentItem();
        CargoMission mission = currentMission();
        baro.model.Talent talent = pinned == null && pinnedMission == null && list.getSelectedValue() != null ? list.getSelectedValue().talent().orElse(null) : null;
        boolean empty = query.getText().trim().isEmpty();
        Component card = talent != null ? talentCard(talent) : mission != null ? missionCard(mission) : item != null ? card(item)
                : empty ? pinnedPanel() : new Stack(0).padding(4, 8, 4, 8).with(blocks.text().text(tr("Ничего не найдено"), Style.MUTED));
        cardScroll.setViewportView(card);
        cardScroll.setVisible(card != null);
        resizeToContent();
    }

    private Panel panel() {
        try { return Panel.valueOf(settings.getString("overlay.panel", "NONE")); } catch (IllegalArgumentException e) { return Panel.NONE; }
    }

    /** Закреплённая панель при пустом поиске: список покупок или миссии из сейва и монстры биома. */
    private Component pinnedPanel() {
        Panel p = panel();
        if (p == Panel.SHOP) {
            java.util.Map<Item, Integer> shop = lists.shopping();
            Stack s = new Stack(1).padding(4, 8, 6, 8);
            s.with(label(tr("Список покупок")));
            if (shop.isEmpty()) return s.with(blocks.text().text(tr("пуст — «+ В список покупок» в карточке предмета"), Style.MUTED));
            long total = 0;
            for (java.util.Map.Entry<Item, Integer> e : shop.entrySet()) {
                RichText r = blocks.text().word(e.getValue() + "× ", Style.NORMAL).link(e.getKey());
                java.util.Optional<baro.pricing.StoreOffer> o = blocks.prices().cheapestBuy(e.getKey());
                if (o.isPresent()) { long v = (long) o.get().price() * e.getValue(); total += v; r.text(" " + o.get().store().title(), Style.MUTED); }
                s.with(r);
            }
            s.with(blocks.text().text(tr("Итого ≈ "), Style.MUTED).word(Format.money(total), Style.BUY).text("   ", Style.MUTED)
                    .action(tr("открыть"), tr("Список покупок в справочнике"), openShoppingInMain));
            return s;
        }
        if (p == Panel.SAVE) {
            Stack s = new Stack(2).padding(4, 8, 6, 8);
            baro.save.SaveSnapshot snap = saves.last().orElse(null);
            if (snap == null) return s.with(blocks.text().text(tr("Сейв ещё не прочитан (кнопка «Сейв ▾» в справочнике)."), Style.MUTED));
            baro.bestiary.Bestiary best = lore.now();
            if (best == null) { lore.get(b -> { if (query.getText().trim().isEmpty()) updateCard(); }); }
            s.with(label(tr("Миссии — ") + snap.locationName()));
            s.with(baro.ui.views.SaveViews.missions(snap, best, blocks, true));
            if (!snap.biome().isEmpty() && best != null) {
                s.with(label(tr("Опасные в биоме «") + baro.ui.views.SaveViews.biomeName(snap, best) + "»"));
                s.with(baro.ui.views.SaveViews.biomeMonsters(snap, best, blocks, 6));
            }
            return s;
        }
        return null;
    }

    /** Ссылка на существо, миссию или недуг — открыть в справочнике. */
    @Override public void openLore(Object entry) { openLoreInMain.accept(entry); }

    private Stack talentCard(baro.model.Talent talent) {
        Stack s = new Stack(2).padding(4, 8, 6, 8);
        s.with(blocks.text().word(talent.name(), Style.BOLD).text(tr("  талант · ") + talent.owner(), Style.MUTED));
        s.with(blocks.text().text(talent.description(), Style.NORMAL));
        if (!talent.recipes().isEmpty()) s.with(label(tr("Открывает рецепты")), blocks.links(talent.recipes(), 8));
        return s.with(blocks.text().text(tr("Enter — открыть дерево талантов"), Style.MUTED));
    }

    private Stack card(Item item) {
        Stack s = new Stack(2).padding(4, 8, 6, 8);
        RichText head = blocks.text().word(item.displayName(), Style.BOLD).text("  " + item.category().title(), Style.MUTED);
        if (pinned != null) head.text(tr("   (Enter — в справочник)"), Style.MUTED);
        s.with(head);
        if (!item.description().isEmpty()) s.with(blocks.text().text(shorten(item.description(), 160), Style.MUTED));
        s.with(blocks.priceLine(item));
        int board = blocks.onBoard(item);
        if (board > 0) s.with(blocks.text().text(tr("На борту: "), Style.MUTED).word(board + tr(" шт."), Style.GOOD));
        Medicine m = catalog.medicineOf(item).orElse(null);
        item.bestReason().ifPresent(r -> s.with(blocks.text().badge().text(r, Style.ACCENT)));
        Stack ws = blocks.weaponStats(item);
        if (ws != null) s.with(label(tr("Характеристики")), ws);
        if (m != null) {
            m.useSkill().ifPresent(k -> s.with(blocks.text().text(tr("Для применения нужен навык "), Style.MUTED)
                    .skill(baro.model.Skill.MEDICAL, k, baro.model.Skill.useNote(k))));
            Effect hit = list.getSelectedValue() != null && pinned == null ? list.getSelectedValue().effect().orElse(null) : null;
            Stack fx = new Stack(0);
            int shown = 0;
            for (Effect e : m.onSuccess()) { if (shown++ == 7) break; fx.with(blocks.effect(e, e == hit, true)); }
            if (m.onSuccess().size() > 7) fx.with(blocks.text().text(tr("… ещё ") + (m.onSuccess().size() - 7) + " — Enter", Style.MUTED));
            s.with(fx);
        }
        if (!item.recipes().isEmpty()) s.with(label(tr("Крафт")), blocks.recipes(item, true));
        if (item.deconstruction().isPresent()) s.with(label(tr("Разбор")), blocks.decon(item, true));
        if (!item.usedIn().isEmpty()) s.with(label(tr("Используется в крафте")), blocks.links(item.usedIn(), 6));
        if (!item.obtainedFrom().isEmpty()) s.with(label(tr("Получают разбором")), blocks.links(item.obtainedFrom(), 6));
        return s;
    }

    /** Первые ~max символов текста, по границе слова. */
    private static String shorten(String text, int max) {
        if (text.length() <= max) return text;
        int cut = text.lastIndexOf(' ', max);
        return text.substring(0, cut > max / 2 ? cut : max).replaceAll("[\\s,.;:—-]+$", "") + "…";
    }

    private Stack missionCard(CargoMission m) {
        Stack s = new Stack(2).padding(4, 8, 6, 8);
        s.with(blocks.text().word(m.name(), Style.BOLD));
        s.with(blocks.text().text(m.crateName() + tr(" · ящиков ") + (m.minCrates() == m.maxCrates() ? "" + m.maxCrates() : m.minCrates() + "–" + m.maxCrates())
                + tr(" · награда ≈ "), Style.MUTED).word(Format.money(valuation.reward(m)) + tr("/ящ."), Style.SELL));
        s.with(label(tr("В ящике")), PlanView.list(blocks.text(), m.contents()));
        CargoValuation.DeconResult d = valuation.deconstruct(m);
        RichText dec = d.outputs().isEmpty() ? blocks.text().text(tr("ничего не разбирается"), Style.MUTED)
                : PlanView.list(blocks.text(), d.outputs()).text("  → ", Style.MUTED).word(Format.money(d.sellValue()), Style.SELL);
        s.with(label(tr("Разбор ящика")), dec);
        java.util.Optional<java.util.List<baro.planner.PlanResult>> ready = plans.get(m, 1);
        if (ready.isEmpty()) {
            s.with(blocks.text().text(tr("Подбираю лучший план…"), Style.MUTED));
            plans.request(m, 1, l -> { if (currentMission() == m) updateCard(); });
        } else plans.planner().best(ready.get()).ifPresent(r -> {
            int v = plans.planner().value(r.plan());
            RichText t = blocks.text();
            if (v > valuation.reward(m)) t.badge();
            s.with(t.text(tr("Лучше всего: ") + r.plan().kind().title() + " ", Style.MUTED).word((v >= 0 ? "+" : "−") + Format.money(Math.abs(v)), v > 0 ? Style.GOOD : Style.BAD)
                    .text(v > valuation.reward(m) ? tr(" — выгоднее доставки") : tr(" — доставка выгоднее"), Style.MUTED));
            s.with(PlanView.list(blocks.text().text(tr("Продать: "), Style.MUTED), r.plan().sell()));
        });
        s.with(blocks.text().text(tr("Enter — план переработки в справочнике"), Style.MUTED));
        return s;
    }

    @Override public void openMission(CargoMission m) { pinned = null; pinnedMission = m; updateCard(); }

    /** Ссылка на талант из карточки — открыть дерево талантов в справочнике. */
    @Override public void openTalent(baro.model.Talent t) { openTalentInMain.accept(t); }

    private RichText label(String t) { return blocks.text().word(t, Style.ACCENT); }

    private void resizeToContent() {
        int width = settings.getInt("overlay.width", 330);
        int h = getContentPane().getComponent(0).getPreferredSize().height + 2;
        boolean hasRows = !model.isEmpty();
        listScroll.setVisible(hasRows);
        if (hasRows) {
            int lh = Math.min(ROWS, model.size()) * ROW_H + 1;
            listScroll.setPreferredSize(new Dimension(width, lh));
            h += lh;
        }
        if (cardScroll.isVisible() && cardScroll.getViewport().getView() != null) {
            Component v = cardScroll.getViewport().getView();
            int full = WidthAware.heightOf(v, width - 2);
            int ch = (full <= CARD_MAX ? full : Math.min(CARD_MAX, WidthAware.heightOf(v, width - 2 - cardScroll.getVerticalScrollBar().getPreferredSize().width))) + 2;
            h += ch;
        }
        setSize(width, h);
        revalidate();
        if (cardScroll.isVisible()) SwingUtilities.invokeLater(() -> cardScroll.getVerticalScrollBar().setValue(0));
    }

    private void openSelected() {
        CargoMission m = currentMission();
        if (m != null) { openMissionInMain.accept(m); return; }
        if (pinned == null && list.getSelectedValue() != null && list.getSelectedValue().talent().isPresent()) {
            openTalentInMain.accept(list.getSelectedValue().talent().get()); return;
        }
        Item item = currentItem();
        if (item != null) openInMain.accept(item);
    }

    // ---------- клавиши, меню, прозрачность ----------

    private void bindKeys() {
        InputMap im = query.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = query.getActionMap();
        bind(im, am, "DOWN", () -> move(1));
        bind(im, am, "UP", () -> move(-1));
        bind(im, am, "PAGE_DOWN", () -> cardScroll.getVerticalScrollBar().setValue(cardScroll.getVerticalScrollBar().getValue() + 60));
        bind(im, am, "PAGE_UP", () -> cardScroll.getVerticalScrollBar().setValue(cardScroll.getVerticalScrollBar().getValue() - 60));
        bind(im, am, "ENTER", this::openSelected);
        bind(im, am, "ctrl M", medMode::doClick);
        InputMap wim = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        bind(wim, getRootPane().getActionMap(), "ESCAPE", () -> {
            if (pinned != null || pinnedMission != null) { pinned = null; pinnedMission = null; updateCard(); }
            else if (!query.getText().isEmpty()) query.setText("");
            else setVisible(false);
        });
    }

    private static void bind(InputMap im, ActionMap am, String key, Runnable r) {
        im.put(KeyStroke.getKeyStroke(key), key);
        am.put(key, new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { r.run(); } });
    }

    private void move(int d) {
        if (model.isEmpty()) return;
        int i = Math.max(0, Math.min(model.size() - 1, list.getSelectedIndex() + d));
        list.setSelectedIndex(i);
        list.ensureIndexIsVisible(i);
    }

    private JPopupMenu menu(Runnable openMain) {
        JPopupMenu m = new JPopupMenu();
        JMenu op = new JMenu(tr("Прозрачность"));
        for (int p : new int[]{40, 55, 70, 82, 100}) {
            JMenuItem i = new JMenuItem((100 - p) + "%" + (p == 100 ? tr(" (непрозрачно)") : ""));
            i.addActionListener(e -> setOpacityLevel(p / 100f));
            op.add(i);
        }
        op.setEnabled(translucent);
        JMenu w = new JMenu(tr("Ширина"));
        String[] names = {tr("Узкое"), tr("Среднее"), tr("Широкое"), tr("Очень широкое")};
        int[] widths = {270, 330, 420, 520};
        for (int k = 0; k < names.length; k++) {
            int width = widths[k];
            JMenuItem i = new JMenuItem(names[k]);
            i.addActionListener(e -> { settings.put("overlay.width", width); settings.save(); resizeToContent(); });
            w.add(i);
        }
        JMenu pin = new JMenu(tr("Когда поиск пуст"));
        ButtonGroup pg = new ButtonGroup();
        String[] pnames = {tr("Только строка поиска"), tr("Список покупок"), tr("Миссии из сейва и монстры биома")};
        for (Panel pv : Panel.values()) {
            JRadioButtonMenuItem i = new JRadioButtonMenuItem(pnames[pv.ordinal()], panel() == pv);
            i.addActionListener(e -> { settings.put("overlay.panel", pv.name()); settings.save(); if (query.getText().trim().isEmpty()) updateCard(); });
            pg.add(i);
            pin.add(i);
        }
        JMenuItem main = new JMenuItem(tr("Открыть справочник"));
        main.addActionListener(e -> openMain.run());
        JMenuItem hide = new JMenuItem(tr("Скрыть (Esc / ") + baro.hotkey.GlobalHotkey.name(settings.getString("hotkey", baro.hotkey.GlobalHotkey.DEFAULT)) + ")");
        hide.addActionListener(e -> setVisible(false));
        JMenuItem exit = new JMenuItem(tr("Выход из программы"));
        exit.addActionListener(e -> System.exit(0));
        m.add(op); m.add(w); m.add(pin); m.addSeparator(); m.add(main); m.add(hide); m.addSeparator(); m.add(exit);
        return m;
    }

    private float opacityLevel() { return translucent ? getOpacity() : 1f; }

    private void setOpacityLevel(float v) {
        if (!translucent) return;
        v = Math.max(0.25f, Math.min(1f, v));
        setOpacity(v);
        settings.put("overlay.opacity", Math.round(v * 100) / 100.0);
        settings.save();
    }

    private void saveBounds() { settings.put("overlay.x", getX()); settings.put("overlay.y", getY()); settings.save(); }

    private static JButton tinyButton(String text, String tip) {
        JButton b = new JButton(text);
        b.setFont(Theme.font(FONT + 1, true));
        b.setMargin(new Insets(0, 5, 0, 5));
        b.setFocusable(false);
        b.setToolTipText(tip);
        return b;
    }
}
