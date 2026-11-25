package baro.ui.main;


import static baro.Tr.tr;
import baro.bestiary.Bestiary;
import baro.data.GameCatalog;
import baro.search.ItemSearch;
import baro.ui.Theme;
import baro.ui.views.Blocks;
import baro.ui.views.LoreViews;
import baro.ui.widgets.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.List;

/**
 * Общая основа вкладок «Бестиарий» и «Миссии»: поиск, переключатель разделов, список с заголовками групп
 * и карточка справа (с историей и боковой карточкой предмета). Данные подгружаются, когда вкладку впервые открыли.
 */
abstract class LoreTab<S> extends JSplitPane {
    /** Заголовок группы в списке (не выбирается). */
    record Header(String title) { @Override public String toString() { return title; } }

    protected final GameCatalog catalog;
    private final BestiarySource source;
    private final Blocks blocks;
    private final SearchField query;
    protected final Toggles<S> sections;
    private final DefaultListModel<Object> model = new DefaultListModel<>();
    private final JList<Object> list = new JList<>(model);
    private final DetailPane<Object> detail;
    private final Stack top;
    protected Bestiary data;
    private boolean updating, requested, failed;

    LoreTab(GameCatalog catalog, BestiarySource source, Blocks blocks, baro.ui.Navigator external, String hint, int columns) {
        super(HORIZONTAL_SPLIT);
        this.catalog = catalog;
        this.source = source;
        this.blocks = blocks;
        this.query = new SearchField(hint, 16);
        this.sections = new Toggles<>(columns);
        this.detail = new DetailPane<>((e, nav) -> card(e, blocks.withNavigator(nav)),
                nav -> data == null ? loading() : overview(new LoreViews(data, catalog, blocks.withNavigator(nav))),
                (item, nav) -> baro.ui.views.ItemViews.forItem(item, catalog, blocks.withNavigator(nav)), null, external);

        Rows row = new Rows(13f);
        JLabel header = new JLabel();
        header.setFont(Theme.font(12f, true));
        header.setForeground(Theme.ACCENT);
        header.setOpaque(true);
        header.setBackground(Theme.PANEL);
        header.setBorder(BorderFactory.createEmptyBorder(6, 6, 2, 6));
        list.setFixedCellWidth(40); // ширина по окну, а не по самой длинной строке
        list.setCellRenderer((l, v, i, sel, focus) -> {
            if (v instanceof Header h) { header.setText(h.title().toUpperCase(java.util.Locale.ROOT)); return header; }
            String[] r = right(v);
            String tag = r.length > 1 ? r[1] : "";
            return row.set(v.toString(), Theme.FG, r[0], "warn".equals(tag) ? Theme.BUY : tag.startsWith("sell") ? Theme.SELL : Theme.MUTED, sel,
                    "sell*".equals(tag) ? StarBadge.of(14) : null);
        });
        list.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || updating) return;
            Object v = list.getSelectedValue();
            if (v != null && !(v instanceof Header)) detail.show(v);
        });
        query.onChange(this::refill);

        top = new Stack(4).padding(6, 6, 4, 6);
        query.setAlignmentX(LEFT_ALIGNMENT);
        sections.setAlignmentX(LEFT_ALIGNMENT);
        top.with(query, sections);

        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(Theme.PANEL);
        left.add(top, BorderLayout.NORTH);
        left.add(Theme.scroll(list), BorderLayout.CENTER);
        left.setMinimumSize(new Dimension(300, 200));
        setLeftComponent(left);
        setRightComponent(detail);
        setDividerLocation(400);
        setBorder(null);
        // загрузить, когда вкладку впервые показали
        addHierarchyListener(e -> { if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) ensureLoaded(null); });
    }

    /** Ещё один фильтр под переключателем разделов. */
    protected void addFilter(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        top.with(c);
    }

    /** Карточка записи справа. */
    protected Component card(Object entry, Blocks b) { return new LoreViews(data, catalog, b).view(entry); }

    /** Записи не видно и в своём разделе — снять дополнительные фильтры наследника. */
    protected void reveal(Object entry) { }

    /** Перечитать список (фильтр наследника изменился). */
    protected void refilter() { refill(); if (list.getSelectedValue() == null) detail.clear(); }

    /** Перерисовать строки списка и обзор (подписи справа пересчитались). */
    protected void repaintList() { list.repaint(); if (detail.currentValue() == null) detail.refresh(); }

    /** Подключить переключатель разделов (после того, как наследник добавил варианты). */
    protected void sectionsReady() {
        sections.onSelect(s -> { refill(); if (list.getSelectedValue() == null) detail.clear(); });
    }

    private Component loading() {
        Stack s = new Stack(4).padding(10, 12, 16, 12);
        s.with(blocks.text().text(failed ? tr("Нет данных: игра не найдена, а встроенная копия не прочиталась. Укажите папку игры в settings.properties (game.dir=…).")
                : tr("Загружаю данные из файлов игры…"), Style.MUTED));
        return s;
    }

    private void ensureLoaded(Object open) {
        if (data != null) { if (open != null) open(open); return; }
        if (requested && open == null) return;
        requested = true;
        source.get(b -> {
            boolean first = data == null;
            data = b;
            failed = b == null;
            if (first) { refill(); detail.refresh(); }
            if (open != null && b != null) open(open);
        });
    }

    /** Строки списка: заголовки групп и записи раздела (section == null — все разделы, идёт поиск). */
    protected abstract List<Object> entries(S section, String normalizedQuery);

    /** Подпись справа в строке списка: {текст} или {текст, "warn"}. */
    protected abstract String[] right(Object entry);

    protected abstract Component overview(LoreViews v);

    /** Раздел, где есть запись (для перехода по ссылке). */
    protected abstract S sectionOf(Object entry);

    private void refill() {
        if (data == null) return;
        updating = true;
        Object keep = list.getSelectedValue();
        model.clear();
        String q = ItemSearch.normalize(query.getText());
        // поиск — по всем разделам сразу (null), иначе — выбранный раздел
        for (Object o : entries(q.isEmpty() ? sections.selected() : null, q)) model.addElement(o);
        if (keep != null && model.contains(keep)) list.setSelectedValue(keep, true);
        updating = false;
    }

    /** Открыть запись (по ссылке из другой карточки или вкладки). */
    void open(Object entry) {
        if (data == null) { ensureLoaded(entry); return; }
        updating = true;
        if (!model.contains(entry)) {
            S s = sectionOf(entry);
            if (s != sections.selected()) { sections.select(s); updating = false; refill(); updating = true; }
            if (!model.contains(entry)) { reveal(entry); updating = false; refill(); updating = true; }
            if (!model.contains(entry)) { query.setText(""); updating = false; refill(); updating = true; }
        }
        list.setSelectedValue(entry, true);
        updating = false;
        detail.show(entry);
    }

    void pricesChanged() { detail.refresh(); }
}
