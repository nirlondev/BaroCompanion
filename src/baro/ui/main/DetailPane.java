package baro.ui.main;


import static baro.Tr.tr;
import baro.model.CargoMission;
import baro.model.Item;
import baro.ui.Navigator;
import baro.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Правая часть вкладки: основная карточка и, как в плиточном оконном менеджере, боковая карточка справа —
 * ссылка на ингредиент открывается сбоку, а исходный предмет остаётся виден. История «Назад / Вперёд».
 */
final class DetailPane<T> extends JPanel {
    /** Одно состояние истории: что открыто в основной и боковой карточке. */
    private static final class State<T> {
        final T main; final Item side;
        State(T main, Item side) { this.main = main; this.side = side; }
    }

    private final BiFunction<T, Navigator, Component> mainFactory;
    private final Function<Navigator, Component> emptyFactory;
    private final BiFunction<Item, Navigator, Component> sideFactory;
    private final Navigator external;
    private final Function<Item, T> promote;
    private final List<State<T>> history = new ArrayList<>();
    private int index = -1;

    private final JButton back = button(tr("←  Назад"), tr("Назад (Alt+←)"));
    private final JButton forward = button(tr("Вперёд  →"), tr("Вперёд (Alt+→)"));
    private final JPanel center = new JPanel(new BorderLayout());
    private double divider = 0.5;
    private java.util.function.Consumer<T> onShow;

    /**
     * @param promote как сделать предмет основной карточкой (null — открыть его во внешнем навигаторе)
     * @param external куда вести ссылки на миссии и «открыть полностью», если promote нет
     */
    DetailPane(BiFunction<T, Navigator, Component> mainFactory, Function<Navigator, Component> emptyFactory,
               BiFunction<Item, Navigator, Component> sideFactory, Function<Item, T> promote, Navigator external) {
        super(new BorderLayout());
        this.mainFactory = mainFactory; this.emptyFactory = emptyFactory; this.sideFactory = sideFactory;
        this.promote = promote; this.external = external;
        setBackground(Theme.BG);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        bar.setBackground(Theme.HEADER);
        back.addActionListener(e -> go(-1));
        forward.addActionListener(e -> go(1));
        bar.add(back); bar.add(forward);
        center.setBackground(Theme.BG);
        add(bar, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        InputMap im = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        im.put(KeyStroke.getKeyStroke("alt LEFT"), "back"); im.put(KeyStroke.getKeyStroke("alt RIGHT"), "fwd");
        getActionMap().put("back", new AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { go(-1); } });
        getActionMap().put("fwd", new AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { go(1); } });
        refresh();
    }

    private static JButton button(String text, String tip) {
        JButton b = new JButton(text);
        b.setFocusable(false);
        b.setToolTipText(tip);
        return b;
    }

    /** Что сделать, когда в основной карточке открыли запись (запомнить в «недавних»). */
    void onShow(java.util.function.Consumer<T> c) { this.onShow = c; }

    // ---------- навигация ----------

    /** Открыть в основной карточке (выбор в списке слева). */
    void show(T main) {
        State<T> cur = current();
        if (cur != null && cur.main == main && cur.side == null) return;
        push(new State<>(main, null));
    }

    /** Открыть предмет сбоку, не закрывая основную карточку. */
    private void openSide(Item item) {
        State<T> cur = current();
        if (cur == null || cur.main == null) {
            if (promote != null) push(new State<>(promote.apply(item), null)); else external.open(item);
            return;
        }
        if (cur.side == item) return;
        push(new State<>(cur.main, item));
    }

    private void closeSide() { State<T> cur = current(); if (cur != null) push(new State<>(cur.main, null)); }

    private void promoteSide() {
        State<T> cur = current();
        if (cur == null || cur.side == null) return;
        if (promote != null) push(new State<>(promote.apply(cur.side), null)); else external.open(cur.side);
    }

    private void push(State<T> s) {
        while (history.size() > index + 1) history.remove(history.size() - 1);
        history.add(s);
        index = history.size() - 1;
        refresh();
    }

    private void go(int d) {
        int n = index + d;
        if (n < 0 || n >= history.size()) return;
        index = n;
        refresh();
    }

    private State<T> current() { return index >= 0 && index < history.size() ? history.get(index) : null; }

    void clear() { history.clear(); index = -1; refresh(); }

    T current_main() { State<T> c = current(); return c == null ? null : c.main; }

    T currentValue() { return current_main(); }

    // ---------- отрисовка ----------

    private final Navigator inner = new Navigator() {
        @Override public void open(Item item) { openSide(item); }
        @Override public void openMission(CargoMission m) { external.openMission(m); }
        @Override public void openTalent(baro.model.Talent t) { external.openTalent(t); }
        @Override public void openLore(Object entry) { external.openLore(entry); }
    };

    /** Перерисовать (например, после смены цен). */
    void refresh() {
        Component old = center.getComponentCount() > 0 ? center.getComponent(0) : null;
        if (old instanceof JSplitPane) divider = ((JSplitPane) old).getDividerLocation() / (double) Math.max(1, ((JSplitPane) old).getWidth());
        center.removeAll();
        State<T> cur = current();
        JScrollPane mainScroll = Theme.scroll(cur == null || cur.main == null ? emptyFactory.apply(inner) : mainFactory.apply(cur.main, inner));
        if (cur != null && cur.side != null) {
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainScroll, sidePanel(cur.side));
            split.setBorder(null);
            split.setResizeWeight(0.5);
            split.setContinuousLayout(true);
            center.add(split);
            double d = divider > 0.15 && divider < 0.85 ? divider : 0.5;
            SwingUtilities.invokeLater(() -> split.setDividerLocation(d));
        } else center.add(mainScroll);
        if (onShow != null && cur != null && cur.main != null) onShow.accept(cur.main);
        back.setEnabled(index > 0);
        forward.setEnabled(index < history.size() - 1);
        center.revalidate();
        center.repaint();
    }

    private JComponent sidePanel(Item item) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.BG);
        p.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, Theme.ACCENT_DARK));
        JPanel head = new JPanel(new BorderLayout(6, 0));
        head.setBackground(Theme.HEADER);
        head.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 4));
        JLabel title = new JLabel(item.displayName());
        title.setForeground(Theme.ACCENT);
        title.setFont(Theme.font(13, true));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.setOpaque(false);
        JButton full = button(tr("Открыть полностью"), tr("Сделать основной карточкой"));
        full.addActionListener(e -> promoteSide());
        JButton close = button("×", tr("Закрыть боковую карточку"));
        close.addActionListener(e -> closeSide());
        buttons.add(full); buttons.add(close);
        head.add(title, BorderLayout.CENTER);
        head.add(buttons, BorderLayout.EAST);
        p.add(head, BorderLayout.NORTH);
        p.add(Theme.scroll(sideFactory.apply(item, inner)), BorderLayout.CENTER);
        p.setMinimumSize(new Dimension(260, 100));
        return p;
    }
}
