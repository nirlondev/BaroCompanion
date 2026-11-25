package baro.ui.main;


import static baro.Tr.tr;
import baro.app.UserLists;
import baro.model.Item;
import baro.model.Store;
import baro.planner.CraftTree;
import baro.pricing.Format;
import baro.pricing.StoreOffer;
import baro.ui.Links;
import baro.ui.Theme;
import baro.ui.views.Blocks;
import baro.ui.widgets.*;
import baro.ui.widgets.Stack;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * Вкладка «Покупки»: список предметов с количеством, общая цена в выбранном магазине и по самым дешёвым магазинам,
 * и режим «скрафтить самому» — всё раскладывается до сырья (общий список ресурсов и его цена).
 */
final class ShoppingTab extends JPanel {
    private final UserLists lists;
    private final Blocks blocks;
    private final Stack page = new Stack(6).padding(10, 12, 16, 12);
    private final JToggleButton craft = new JToggleButton(tr("Скрафтить самому — до сырья"));
    private boolean rendering;

    ShoppingTab(UserLists lists, Blocks blocks) {
        super(new BorderLayout());
        this.lists = lists;
        this.blocks = blocks;
        setBackground(Theme.BG);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(Theme.HEADER);
        JButton copy = new JButton(tr("Копировать списком"));
        copy.setToolTipText(tr("Скопировать список в буфер обмена — вставить в чат или заметки"));
        copy.addActionListener(e -> Links.copy(asText()));
        JButton clear = new JButton(tr("Очистить"));
        clear.addActionListener(e -> {
            if (!lists.shopping().isEmpty() && JOptionPane.showConfirmDialog(this, tr("Очистить список покупок?"), tr("Покупки"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
                lists.clearShopping();
        });
        craft.setFocusable(false);
        craft.setToolTipText(tr("Посчитать, сколько сырья нужно, чтобы скрафтить всё из списка самому (что дешевле купить — покупается)"));
        craft.addActionListener(e -> render());
        bar.add(craft); bar.add(copy); bar.add(clear);
        add(bar, BorderLayout.NORTH);
        add(Theme.scroll(page), BorderLayout.CENTER);
        lists.addListener(() -> { if (!rendering) render(); });
        render();
    }

    void pricesChanged() { render(); }

    private void render() {
        rendering = true;
        page.removeAll();
        Map<Item, Integer> list = lists.shopping();
        page.with(blocks.text().text(tr("Список покупок"), Style.TITLE));
        if (list.isEmpty()) {
            page.with(blocks.text().text(tr("Список пуст. Добавляйте предметы кнопкой «+ В список покупок» в карточке предмета. ")
                    + tr("Список можно закрепить в окне поверх игры (правая кнопка по окну → «Когда поиск пуст»)."), Style.MUTED));
        } else {
            Stack rows = new Stack(2);
            for (Map.Entry<Item, Integer> e : list.entrySet()) rows.with(row(e.getKey(), e.getValue()));
            page.with(blocks.section(tr("Предметы (") + list.size() + ")", rows));
            page.with(blocks.section(tr("Итого"), totals(list)));
            if (craft.isSelected()) page.with(blocks.section(tr("Скрафтить самому — сырьё на весь список"), raw(list)));
        }
        page.revalidate();
        page.repaint();
        rendering = false;
    }

    private Component row(Item item, int n) {
        RichText t = blocks.text().link(item);
        Store here = blocks.prices().settings().store();
        Optional<Integer> buy = blocks.prices().buyPrice(item);
        Optional<StoreOffer> best = blocks.prices().cheapestBuy(item);
        if (buy.isPresent()) t.text("  " + here.title() + ": ", Style.MUTED).word(Format.money((long) buy.get() * n), Style.BUY);
        else t.text(tr("  здесь не продаётся"), Style.MUTED);
        if (best.isPresent() && (buy.isEmpty() || best.get().price() < buy.get()))
            t.text(tr(" · дешевле: ") + best.get().store().title() + " ", Style.MUTED).word(Format.money((long) best.get().price() * n), Style.BUY);
        if (best.isEmpty()) t.text(CraftTree.recipe(item) != null ? tr(" · только крафт") : tr(" · не продаётся"), Style.WARN);
        int board = blocks.onBoard(item);
        if (board > 0) t.text(tr(" · на борту ") + board + tr(" шт."), board >= n ? Style.GOOD : Style.MUTED);
        Integer have = blocks.stock().get(item.id());
        if (have != null) t.text(have > 0 ? tr(" · на станции ") + have + tr(" шт.") : tr(" · на станции нет"), have >= n ? Style.GOOD : Style.WARN);

        JSpinner count = new JSpinner(new SpinnerNumberModel(n, 0, 999, 1));
        ((JSpinner.DefaultEditor) count.getEditor()).getTextField().setColumns(3);
        count.addChangeListener(e -> lists.setShopping(item, (Integer) count.getValue()));
        JButton remove = new JButton("×");
        remove.setMargin(new Insets(0, 6, 0, 6));
        remove.setFocusable(false);
        remove.setToolTipText(tr("Убрать из списка"));
        remove.addActionListener(e -> lists.setShopping(item, 0));
        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        east.setOpaque(false);
        east.add(count); east.add(remove);
        return new SideRow(t, east);
    }

    private Stack totals(Map<Item, Integer> list) {
        long here = 0, cheapest = 0;
        int missingHere = 0;
        java.util.List<Item> nowhere = new ArrayList<>();
        Map<Store, Long> byStore = new EnumMap<>(Store.class);
        for (Map.Entry<Item, Integer> e : list.entrySet()) {
            Optional<Integer> b = blocks.prices().buyPrice(e.getKey());
            if (b.isPresent()) here += (long) b.get() * e.getValue(); else missingHere++;
            Optional<StoreOffer> o = blocks.prices().cheapestBuy(e.getKey());
            if (o.isPresent()) { cheapest += (long) o.get().price() * e.getValue(); byStore.merge(o.get().store(), (long) o.get().price() * e.getValue(), Long::sum); }
            else nowhere.add(e.getKey());
        }
        Stack s = new Stack(2);
        s.with(blocks.text().text(tr("В магазине «") + blocks.prices().settings().store().title() + "»: ", Style.MUTED).word(Format.money(here), Style.BUY)
                .text(missingHere > 0 ? tr(" (без ") + missingHere + tr(" — здесь не продаются)") : "", Style.MUTED));
        s.with(blocks.text().text(tr("Если покупать каждое там, где дешевле: "), Style.MUTED).word(Format.money(cheapest), Style.BUY));
        if (byStore.size() > 1) {
            RichText r = blocks.text().text(tr("По магазинам: "), Style.MUTED);
            r.join(byStore.entrySet(), Style.MUTED, e -> r.text(e.getKey().title() + " ", Style.NORMAL).word(Format.money(e.getValue()), Style.BUY));
            s.with(r);
        }
        if (!nowhere.isEmpty()) {
            RichText r = blocks.text().text(tr("Нигде не продаются (крафт или добыча): "), Style.WARN);
            s.with(r.join(nowhere, Style.MUTED, r::link));
        }
        return s;
    }

    private Stack raw(Map<Item, Integer> list) {
        CraftTree ct = new CraftTree(blocks.prices());
        Map<Item, Integer> all = new LinkedHashMap<>();
        long cost = 0;
        boolean complete = true;
        double seconds = 0;
        for (Map.Entry<Item, Integer> e : list.entrySet()) {
            CraftTree.Node n = ct.build(e.getKey(), e.getValue(), true);
            CraftTree.leaves(n).forEach((k, v) -> all.merge(k, v, Integer::sum));
            OptionalLong c = ct.cost(n);
            if (c.isPresent()) cost += c.getAsLong(); else complete = false;
            seconds += CraftTree.seconds(n);
        }
        Stack s = new Stack(2);
        s.with(blocks.rawList(all));
        RichText t = blocks.text();
        if (complete) t.text(tr("Купить всё сырьё ≈ "), Style.MUTED).word(Format.money(cost), Style.BUY);
        else t.text(tr("Часть сырья не продаётся — его нужно добыть или получить разбором."), Style.WARN);
        t.text(tr(" · крафт ≈ ") + Format.seconds(seconds), Style.MUTED);
        s.with(t);
        return s;
    }

    private String asText() {
        StringBuilder sb = new StringBuilder(tr("Список покупок:\n"));
        for (Map.Entry<Item, Integer> e : lists.shopping().entrySet()) {
            sb.append("- ").append(e.getKey().displayName()).append(" ×").append(e.getValue());
            blocks.prices().cheapestBuy(e.getKey()).ifPresent(o -> sb.append(" — ").append(Format.money((long) o.price() * e.getValue())).append(" (").append(o.store().title()).append(")"));
            sb.append('\n');
        }
        return sb.toString();
    }

    /** Строка: текст слева (с переносом), элементы управления справа. */
    static final class SideRow extends JPanel implements WidthAware {
        private final Component text, side;
        SideRow(Component text, Component side) {
            super(null);
            this.text = text; this.side = side;
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            add(text); add(side);
        }
        @Override public int preferredHeight(int width) {
            int sw = side.getPreferredSize().width;
            return Math.max(side.getPreferredSize().height, WidthAware.heightOf(text, Math.max(60, width - sw - 8)));
        }
        @Override public void doLayout() {
            Dimension sp = side.getPreferredSize();
            int w = getWidth(), h = getHeight();
            side.setBounds(w - sp.width, Math.max(0, (h - sp.height) / 2), sp.width, sp.height);
            text.setBounds(0, 0, Math.max(60, w - sp.width - 8), h);
        }
        @Override public Dimension getPreferredSize() { int w = getWidth() > 0 ? getWidth() : 400; return new Dimension(w, preferredHeight(w)); }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
    }
}
