package baro.ui.main;


import static baro.Tr.tr;
import baro.app.UserLists;
import baro.bestiary.Creature;
import baro.data.GameCatalog;
import baro.model.*;
import baro.pricing.Format;
import baro.ui.AppIcon;
import baro.ui.Theme;
import baro.ui.views.Blocks;
import baro.ui.widgets.*;
import baro.ui.widgets.Stack;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;

/**
 * Сравнение рядом: 2–4 предмета (оружие, броня, препараты…) или существа бестиария. Колонки — записи, строки — характеристики;
 * лучшее значение в строке подсвечивается.
 */
final class CompareWindow extends JDialog {
    private static final int MAX_CREATURES = 4;
    private final UserLists lists;
    private final GameCatalog catalog;
    private final Blocks blocks;
    private final List<Creature> creatures = new ArrayList<>();
    private final Stack page = new Stack(8).padding(10, 12, 16, 12);

    CompareWindow(Frame owner, UserLists lists, GameCatalog catalog, Blocks blocks) {
        super(owner, tr("Сравнение"), false);
        this.lists = lists;
        this.catalog = catalog;
        this.blocks = blocks;
        setIconImages(Arrays.asList(AppIcon.create(16), AppIcon.create(32)));
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(Theme.scroll(page), BorderLayout.CENTER);
        setContentPane(root);
        setSize(Theme.px(980), Theme.px(680));
        setLocationRelativeTo(owner);
        lists.addListener(() -> { if (isVisible()) render(); });
    }

    /** Добавить существо к сравнению (или убрать, если уже есть) и показать окно. */
    void toggle(Creature c) {
        if (!creatures.remove(c)) {
            creatures.add(c);
            while (creatures.size() > MAX_CREATURES) creatures.remove(0);
        }
        showWindow();
    }

    void showWindow() {
        render();
        setVisible(true);
        toFront();
    }

    void pricesChanged() { if (isVisible()) render(); }

    private void render() {
        page.removeAll();
        List<Item> items = lists.compare();
        if (items.isEmpty() && creatures.isEmpty())
            page.with(blocks.text().text(tr("Пока нечего сравнивать. В карточке предмета нажмите «Сравнить», в карточке существа бестиария — тоже «Сравнить». ")
                    + tr("Можно сравнить до 4 предметов и до 4 существ."), Style.MUTED));
        if (!items.isEmpty()) page.with(itemsGrid(items));
        if (!creatures.isEmpty()) page.with(creaturesGrid());
        page.revalidate();
        page.repaint();
    }

    // ---------- предметы ----------

    private Stack itemsGrid(List<Item> items) {
        Grid g = new Grid(items.size());
        g.row("", items, it -> {
            RichText t = blocks.text();
            t.link(it).newLine().action(tr("убрать"), tr("Убрать из сравнения"), () -> lists.toggleCompare(it));
            return t;
        });
        g.row(tr("Категория"), items, it -> blocks.text().text(it.category().title(), Style.MUTED));
        g.numeric(tr("Купить (дешевле всего)"), items, it -> blocks.prices().cheapestBuy(it).map(o -> (double) o.price()).orElse(null), v -> Format.money(Math.round(v)), false);
        g.numeric(tr("Продать здесь"), items, it -> blocks.prices().sellPrice(it).map(Integer::doubleValue).orElse(null), v -> Format.money(Math.round(v)), true);
        if (items.stream().anyMatch(i -> i.weapon().isPresent())) {
            g.numeric(tr("Выстрелов в минуту"), items, it -> it.weapon().flatMap(w -> w.fireInterval().or(w::meleeInterval)).filter(t -> t > 0).map(t -> 60 / t).orElse(null), v -> String.valueOf(Math.round(v)), true);
            g.numeric(tr("Урон за выстрел"), items, CompareWindow::hitDamage, Format::num, true);
            g.numeric(tr("Урон в минуту ≈"), items, it -> {
                Double d = hitDamage(it);
                Double rate = it.weapon().flatMap(w -> w.fireInterval().or(w::meleeInterval)).filter(t -> t > 0).map(t -> 60 / t).orElse(null);
                return d == null || rate == null ? null : d * rate;
            }, v -> String.valueOf(Math.round(v)), true);
            g.numeric(tr("Разброс с навыком, °"), items, it -> it.weapon().flatMap(WeaponStats::spread).orElse(null), Format::num, false);
            g.row(tr("Боеприпасы"), items, it -> {
                RichText t = blocks.text();
                List<Item> ammo = it.weapon().map(WeaponStats::ammo).orElse(List.of());
                return ammo.isEmpty() ? t.text("—", Style.MUTED) : t.join(ammo.subList(0, Math.min(3, ammo.size())), Style.MUTED, t::link);
            });
        }
        if (items.stream().anyMatch(i -> catalog.medicineOf(i).isPresent())) {
            g.row(tr("Эффекты"), items, it -> {
                Stack s = new Stack(0);
                catalog.medicineOf(it).ifPresentOrElse(m -> { for (Effect e : m.onSuccess().subList(0, Math.min(5, m.onSuccess().size()))) s.with(blocks.effect(e, false, true)); },
                        () -> s.with(blocks.text().text("—", Style.MUTED)));
                return s;
            });
        }
        g.row(tr("Крафт"), items, it -> it.recipes().isEmpty() ? blocks.text().text(tr("не крафтится"), Style.MUTED) : blocks.recipes(it, true));
        g.row(tr("Разбор"), items, it -> it.deconstruction().isEmpty() ? blocks.text().text(tr("не разбирается"), Style.MUTED) : blocks.decon(it, true));
        g.row(tr("Лучший выбор"), items, it -> blocks.text().text(it.bestReason().orElse("—"), it.isBest() ? Style.ACCENT : Style.MUTED));
        Stack s = new Stack(4);
        RichText head = blocks.text().text(tr("Предметы"), Style.TITLE).text("   ", Style.MUTED).action(tr("очистить"), tr("Убрать все предметы из сравнения"), lists::clearCompare);
        s.with(head, g);
        return s;
    }

    /** Урон одного выстрела (или удара): сумма травм — ожоги, кровотечение, урон — по боеприпасу, если оружие заряжается. */
    private static Double hitDamage(Item it) {
        WeaponStats w = it.weapon().orElse(null);
        if (w == null) return null;
        List<DamageBlock> blocks = !w.roundDamage().isEmpty() ? w.roundDamage() : !w.ownDamage().isEmpty() ? w.ownDamage() : null;
        if (blocks == null && !w.ammo().isEmpty()) blocks = w.ammo().get(0).weapon().map(a -> a.roundDamage().isEmpty() ? a.ownDamage() : a.roundDamage()).orElse(null);
        if (blocks == null) return null;
        double d = 0;
        for (DamageBlock b : blocks) for (AfflictionHit h : b.afflictions()) if (!h.isStun()) d += h.strength() * h.probability().orElse(1.0);
        int rounds = w.rounds().orElse(1);
        return d <= 0 ? null : d * rounds;
    }

    // ---------- существа ----------

    private Stack creaturesGrid() {
        Grid g = new Grid(creatures.size());
        g.row("", creatures, c -> blocks.text().lore(c.name(), c, c.kind().title()).newLine().action(tr("убрать"), tr("Убрать из сравнения"), () -> { creatures.remove(c); render(); }));
        g.row(tr("Вид"), creatures, c -> blocks.text().text(c.kind().title(), Style.MUTED));
        g.numeric(tr("Здоровье"), creatures, c -> c.health().vitality(), Format::num, true);
        g.numeric(tr("Сильнейший удар по человеку"), creatures, c -> c.maxHit() > 0 ? c.maxHit() : null, Format::num, true);
        g.numeric(tr("Удар по корпусу"), creatures, c -> c.maxStructureDamage() > 0 ? c.maxStructureDamage() : null, Format::num, true);
        g.numeric(tr("Скорость, м/с"), creatures, c -> c.topSpeed() > 0 ? c.topSpeed() : null, Format::num, true);
        g.numeric(tr("Атак"), creatures, c -> (double) c.attacks().size(), v -> String.valueOf(Math.round(v)), true);
        g.row(tr("Уязвимые места"), creatures, c -> {
            RichText t = blocks.text();
            List<Creature.LimbMultiplier> weak = c.health().weakSpots().stream().filter(w -> w.multiplier() > 1).toList();
            return weak.isEmpty() ? t.text("—", Style.MUTED) : t.join(weak, Style.MUTED, w -> t.text(w.limb() + " ×" + Format.num(w.multiplier()), Style.NORMAL));
        });
        g.row(tr("Броня"), creatures, c -> blocks.text().text(c.armor().isEmpty() ? tr("нет") : tr("у ") + c.armor().size() + tr(" частей тела"), c.armor().isEmpty() ? Style.MUTED : Style.WARN));
        g.row(tr("Заходит в лодку"), creatures, c -> blocks.text().text(c.canEnterSub() ? tr("да") : tr("нет"), c.canEnterSub() ? Style.WARN : Style.MUTED));
        g.row(tr("Добыча"), creatures, c -> {
            RichText t = blocks.text();
            if (c.drops().isEmpty()) return t.text("—", Style.MUTED);
            List<Creature.Drop> d = c.drops().subList(0, Math.min(4, c.drops().size()));
            return t.join(d, Style.MUTED, x -> catalog.item(x.itemId()).ifPresentOrElse(t::link, () -> t.text(x.itemId(), Style.NORMAL)));
        });
        Stack s = new Stack(4);
        s.with(blocks.text().text(tr("Существа"), Style.TITLE).text("   ", Style.MUTED).action(tr("очистить"), tr("Убрать все существа из сравнения"), () -> { creatures.clear(); render(); }), g);
        return s;
    }

    // ---------- сетка ----------

    /** Таблица: первая колонка — подписи, остальные — записи равной ширины. Высота строки — по самой высокой ячейке. */
    private final class Grid extends JPanel implements WidthAware {
        private final int columns;
        private final List<Component[]> rows = new ArrayList<>();

        Grid(int columns) {
            super(null);
            this.columns = columns;
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
        }

        <T> void row(String title, List<T> values, Function<T, Component> cell) {
            Component[] r = new Component[columns + 1];
            r[0] = blocks.text().word(title, Style.MUTED);
            for (int i = 0; i < columns; i++) r[i + 1] = cell.apply(values.get(i));
            add(r);
        }

        /** Числовая строка: лучшее значение (наибольшее или наименьшее) — зелёным. */
        <T> void numeric(String title, List<T> values, Function<T, Double> value, Function<Double, String> fmt, boolean higherBetter) {
            Double best = null;
            int known = 0;
            for (T v : values) {
                Double d = value.apply(v);
                if (d == null) continue;
                known++;
                if (best == null || (higherBetter ? d > best : d < best)) best = d;
            }
            if (known == 0) return;
            Double b = best;
            int n = known;
            row(title, values, v -> {
                Double d = value.apply(v);
                if (d == null) return blocks.text().text("—", Style.MUTED);
                boolean win = n > 1 && Math.abs(d - b) < 1e-9;
                return blocks.text().word(fmt.apply(d), win ? Style.GOOD : Style.BOLD);
            });
        }

        private void add(Component[] r) {
            rows.add(r);
            for (Component c : r) super.add(c);
        }

        private int labelWidth(int width) { return Math.min(Theme.px(190), width / (columns + 2)); }

        @Override public int preferredHeight(int width) {
            int lw = labelWidth(width), cw = (width - lw) / columns, h = 0;
            for (Component[] r : rows) h += rowHeight(r, lw, cw) + 6;
            return h;
        }

        private int rowHeight(Component[] r, int lw, int cw) {
            int h = WidthAware.heightOf(r[0], lw - 8);
            for (int i = 1; i < r.length; i++) h = Math.max(h, WidthAware.heightOf(r[i], cw - 10));
            return h;
        }

        @Override public void doLayout() {
            int width = getWidth(), lw = labelWidth(width), cw = (width - lw) / columns, y = 0;
            for (Component[] r : rows) {
                int h = rowHeight(r, lw, cw);
                r[0].setBounds(0, y, lw - 8, h);
                for (int i = 1; i < r.length; i++) r[i].setBounds(lw + (i - 1) * cw, y, cw - 10, h);
                y += h + 6;
            }
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Theme.LINE);
            int width = getWidth(), lw = labelWidth(width), cw = (width - lw) / columns, y = 0;
            for (Component[] r : rows) { y += rowHeight(r, lw, cw) + 6; g.drawLine(0, y - 3, width, y - 3); }
        }

        @Override public Dimension getPreferredSize() { int w = getWidth() > 0 ? getWidth() : 900; return new Dimension(w, preferredHeight(w)); }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
    }
}
