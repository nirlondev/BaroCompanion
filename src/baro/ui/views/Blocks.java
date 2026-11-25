package baro.ui.views;


import static baro.Tr.tr;
import baro.data.BuffDescriptions;
import baro.model.*;
import baro.pricing.Format;
import baro.pricing.PriceCalculator;
import baro.pricing.StoreOffer;
import baro.save.SaveSnapshot;
import baro.ui.Navigator;
import baro.ui.Theme;
import baro.ui.widgets.RichText;
import baro.ui.widgets.Stack;
import baro.ui.widgets.Style;

import javax.swing.*;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Строительные блоки карточек: цена, крафт, разбор, ссылки, эффекты. */
public final class Blocks {
    private final PriceCalculator prices;
    private final Navigator navigator;
    private final float size;
    private final CardContext ctx;

    public Blocks(PriceCalculator prices, Navigator navigator, float size) { this(prices, navigator, size, null); }

    /** @param ctx иконки, личные списки, сейв и действия окна (null — карточки без них) */
    public Blocks(PriceCalculator prices, Navigator navigator, float size, CardContext ctx) {
        this.prices = prices; this.navigator = navigator; this.size = size; this.ctx = ctx;
    }

    public PriceCalculator prices() { return prices; }
    public Navigator navigator() { return navigator; }
    /** Иконки, списки и сейв (может быть null). */
    public CardContext context() { return ctx; }

    /** Те же блоки, но ссылки ведут в другой навигатор (например, в боковую карточку). */
    public Blocks withNavigator(Navigator n) { return new Blocks(prices, n, size, ctx); }

    public RichText text() {
        RichText t = new RichText(size, navigator).withPrices(prices::sellPrice);
        if (ctx != null && !ctx.catalog().icons().isEmpty()) {
            int px = Theme.px(Math.round(size + 4));
            t.withIcons(i -> ctx.catalog().icons().icon(i, px));
        }
        return t;
    }

    /** Иконка предмета (size — в пикселях без учёта масштаба) или null. */
    public Icon icon(Item item, int size) { return ctx == null ? null : ctx.catalog().icons().icon(item, Theme.px(size)); }

    public RichText heading(String title) {
        RichText t = text().word(title, Style.ACCENT);
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE), BorderFactory.createEmptyBorder(8, 0, 2, 0)));
        return t;
    }

    public Stack section(String title, java.awt.Component... body) {
        Stack s = new Stack(2);
        s.with(heading(title));
        s.with(body);
        return s;
    }

    public java.awt.Component title(Item item, String chip) {
        RichText right = text().text(item.displayName(), Style.TITLE).newLine().word(chip.toUpperCase(), Style.MUTED);
        item.englishName().ifPresent(en -> right.text("  · " + en, Style.MUTED));
        Icon ic = icon(item, 48);
        if (ic == null) return right;
        return new IconRow(ic, right);
    }

    /** Иконка слева, текст справа (с переносом по ширине). */
    private static final class IconRow extends JPanel implements baro.ui.widgets.WidthAware {
        private final Icon icon; private final RichText text; private final JLabel label;
        IconRow(Icon icon, RichText text) {
            super(null);
            this.icon = icon; this.text = text; this.label = new JLabel(icon);
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            add(label); add(text);
        }
        @Override public int preferredHeight(int width) {
            return Math.max(icon.getIconHeight(), baro.ui.widgets.WidthAware.heightOf(text, Math.max(50, width - icon.getIconWidth() - 10)));
        }
        @Override public void doLayout() {
            int w = getWidth(), iw = icon.getIconWidth();
            label.setBounds(0, 0, iw, icon.getIconHeight());
            text.setBounds(iw + 10, 0, Math.max(50, w - iw - 10), getHeight());
        }
        @Override public java.awt.Dimension getPreferredSize() { int w = getWidth() > 0 ? getWidth() : 360; return new java.awt.Dimension(w, preferredHeight(w)); }
        @Override public java.awt.Dimension getMaximumSize() { return new java.awt.Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
    }

    // ---------- действия с предметом ----------

    /** Строка действий под заголовком: избранное, список покупок, сравнение, вики, сообщить о неточности. */
    public RichText actions(Item item) {
        RichText t = text();
        if (ctx == null) return t.action(tr("Вики"), tr("Открыть страницу предмета в официальной вики игры"), () -> baro.ui.Links.open(t, baro.ui.Links.wiki(item)));
        baro.app.UserLists l = ctx.lists();
        Runnable redraw = () -> javax.swing.SwingUtilities.invokeLater(() -> {
            java.awt.Container p = t.getParent();
            if (p == null) return;
            int at = java.util.Arrays.asList(p.getComponents()).indexOf(t);
            if (at < 0) return;
            p.remove(at);
            p.add(actions(item), at);
            p.revalidate();
            p.repaint();
        });
        if (l.isFavorite(item)) t.word("★ ", Style.FAVORITE).action(tr("В избранном"), tr("Убрать из избранного"), () -> { l.toggleFavorite(item); redraw.run(); });
        else t.word("★ ", Style.MUTED).action(tr("В избранное"), tr("Добавить в избранное — список «Избранное» на вкладке «Предметы»"), () -> { l.toggleFavorite(item); redraw.run(); });
        t.word("   ", Style.MUTED);
        int n = l.shoppingCount(item);
        if (n == 0) t.action(tr("+ В список покупок"), tr("Добавить в список покупок (вкладка «Покупки»)"), () -> { l.addToShopping(item, 1); redraw.run(); });
        else {
            t.text(tr("В покупках: "), Style.MUTED).word(n + tr(" шт."), Style.BOLD).word(" ", Style.MUTED)
                    .action("+1", tr("Добавить ещё"), () -> { l.addToShopping(item, 1); redraw.run(); }).word(" ", Style.MUTED)
                    .action("−1", tr("Убрать одну"), () -> { l.addToShopping(item, -1); redraw.run(); }).word(" ", Style.MUTED)
                    .action(tr("список…"), tr("Открыть список покупок"), () -> ctx.actions().openShopping());
        }
        t.word("   ", Style.MUTED);
        if (l.inCompare(item)) t.action(tr("В сравнении"), tr("Открыть сравнение"), () -> ctx.actions().openCompare());
        else t.action(tr("Сравнить"), tr("Добавить к сравнению (до 4 предметов)"), () -> { l.toggleCompare(item); redraw.run(); ctx.actions().openCompare(); });
        t.word("   ", Style.MUTED);
        t.action(tr("Вики"), tr("Открыть страницу предмета в официальной вики игры"), () -> baro.ui.Links.open(t, baro.ui.Links.wiki(item)));
        t.word("   ", Style.MUTED);
        t.action(tr("Неточность?"), tr("Сообщить о неточных данных: отчёт скопируется в буфер обмена"), () -> baro.ui.ReportDialog.show(t,
                tr("Предмет: ") + item.displayName() + " (" + item.id() + ")", tr("Данные: ") + ctx.catalog().source()));
        int board = onBoard(item);
        if (board > 0) t.word("   ", Style.MUTED).text(tr("На борту") + (boardName().isEmpty() ? "" : " «" + boardName() + "»") + ": ", Style.MUTED).word(board + tr(" шт."), Style.GOOD);
        return t;
    }

    // ---------- дерево крафта ----------

    /** «Из чего с нуля»: рецепт, раскрытый до сырья, итог сырья и его цена; null — если предмет не крафтится. */
    public Stack craftTree(Item item, int count, boolean buyIfCheaper) {
        if (baro.planner.CraftTree.recipe(item) == null) return null;
        baro.planner.CraftTree ct = new baro.planner.CraftTree(prices);
        baro.planner.CraftTree.Node root = ct.build(item, count, buyIfCheaper);
        Stack s = new Stack(1);
        Stack tree = new Stack(0);
        for (baro.planner.CraftTree.Node k : root.children()) treeLine(tree, k, 0);
        s.with(tree);
        s.with(text().word(tr("Итого сырья"), Style.BOLD).text(tr(" (на ") + count + tr(" шт. · крафт ≈ ") + Format.seconds(baro.planner.CraftTree.seconds(root)) + "):", Style.MUTED));
        s.with(rawList(baro.planner.CraftTree.leaves(root)));
        java.util.OptionalLong cost = ct.cost(root);
        RichText total = text();
        if (cost.isPresent()) {
            long sell = (long) prices.sellPrice(item).orElse(0) * count;
            total.text(tr("Купить всё сырьё ≈ "), Style.MUTED).word(Format.money(cost.getAsLong()), Style.BUY)
                    .text(tr(" · продать готовое "), Style.MUTED).word(Format.money(sell), Style.SELL);
            prices.cheapestBuy(item).ifPresent(o -> total.text(tr(" · купить готовое "), Style.MUTED).word(Format.money((long) o.price() * count), Style.BUY));
        } else total.text(tr("Часть сырья не продаётся — цену всего не посчитать."), Style.MUTED);
        s.with(total);
        return s;
    }

    /** Список «что купить или добыть»: количество, где дешевле купить, наличие на станции из сейва. */
    public Stack rawList(java.util.Map<Item, Integer> leaves) {
        Stack sum = new Stack(1).padding(0, 14, 0, 0);
        java.util.Map<String, Integer> stock = stock();
        for (java.util.Map.Entry<Item, Integer> e : leaves.entrySet()) {
            RichText r = text().word(e.getValue() + "× ", Style.NORMAL).link(e.getKey());
            Optional<StoreOffer> o = prices.cheapestBuy(e.getKey());
            if (o.isPresent()) r.text(tr(" · купить "), Style.MUTED).word(Format.money((long) o.get().price() * e.getValue()), Style.BUY).text(" (" + o.get().store().title() + ")", Style.MUTED);
            else if (!e.getKey().obtainedFrom().isEmpty())
                r.text(tr(" · не продаётся — разбором из "), Style.WARN).join(e.getKey().obtainedFrom().subList(0, Math.min(3, e.getKey().obtainedFrom().size())), Style.MUTED, r::link);
            else r.text(tr(" · не продаётся — добыча"), Style.WARN);
            int board = onBoard(e.getKey());
            if (board > 0) {
                r.text(tr("  · на борту ") + board + tr(" шт."), board >= e.getValue() ? Style.GOOD : Style.WARN);
                if (board < e.getValue()) r.text(tr(" — докупить ") + (e.getValue() - board), Style.WARN);
            }
            Integer have = stock.get(e.getKey().id());
            if (have != null) r.text(have > 0 ? tr("  · на станции ") + have + tr(" шт.") : tr("  · на станции нет"), have >= e.getValue() ? Style.GOOD : Style.WARN);
            sum.with(r);
        }
        return sum;
    }

    private void treeLine(Stack tree, baro.planner.CraftTree.Node n, int depth) {
        RichText r = text();
        r.setBorder(BorderFactory.createEmptyBorder(0, 14 + depth * 18, 0, 0));
        r.word(depth > 0 ? "– " : "• ", Style.MUTED).word(n.count() + "× ", Style.NORMAL).link(n.item());
        if (n.options() != null) r.text(" (" + n.options() + ")", Style.MUTED);
        switch (n.mode()) {
            case CRAFT -> {
                r.text(tr(" — скрафтить") + (n.crafts() > 1 ? " ×" + n.crafts() : "") + " · " + n.recipe().fabricator().title(), Style.MUTED);
                for (SkillRequirement sk : n.recipe().skills()) { r.text(" · ", Style.MUTED); r.skill(sk.skill(), sk.level(), Skill.recipeNote(sk.level())); }
                if (n.recipe().needsUnlock()) r.text(tr(" · нужен рецепт"), Style.WARN);
            }
            case BUY -> prices.cheapestBuy(n.item()).ifPresent(o -> r.text(tr(" — купить "), Style.MUTED).word(Format.money((long) o.price() * n.count()), Style.BUY));
            case GATHER -> r.text(tr(" — не продаётся: добыть или разобрать"), Style.WARN);
            case CYCLE -> r.text(tr(" — по кругу в рецептах"), Style.WARN);
        }
        tree.with(r);
        for (baro.planner.CraftTree.Node k : n.children()) treeLine(tree, k, depth + 1);
    }

    /** Сколько предмета на борту (контейнеры подлодки и инвентарь экипажа из сейва); 0 — нет или сейва нет. */
    public int onBoard(Item item) {
        if (ctx == null || ctx.save() == null) return 0;
        return ctx.save().get().map(s -> s.onBoard(item.id())).orElse(0);
    }

    /** Подлодка из сейва или пусто. */
    public String boardName() {
        if (ctx == null || ctx.save() == null) return "";
        return ctx.save().get().map(SaveSnapshot::submarine).orElse("");
    }

    /** Наличие товаров в магазине текущей станции из сейва (id → штук) или пусто. */
    public java.util.Map<String, Integer> stock() {
        if (ctx == null || ctx.save() == null) return java.util.Map.of();
        return ctx.save().get().map(s -> {
            SaveSnapshot.StoreState st = s.stores().get(prices.settings().store());
            return st == null ? java.util.Map.<String, Integer>of() : st.stock();
        }).orElse(java.util.Map.of());
    }

    public RichText description(Item item) {
        return item.description().isEmpty() ? null : text().text(item.description(), Style.MUTED);
    }

    // ---------- цена ----------
    public Stack priceBlock(Item item) {
        Stack s = new Stack(1);
        if (item.price().isEmpty()) { s.with(text().text(tr("Нет цены — не продаётся и не покупается"), Style.MUTED)); return s; }
        Store here = prices.settings().store();
        Optional<Integer> buy = prices.buyPrice(item);
        s.with(text().word(tr("База  "), Style.MUTED).word(Format.money(item.price().get().basePrice()), Style.NORMAL));
        RichText b = text().word(tr("Купить  "), Style.MUTED);
        if (buy.isPresent()) b.word(Format.money(buy.get()), Style.BUY); else b.text(tr("нет в продаже здесь"), Style.MUTED);
        if (buy.isPresent() && prices.isDailySpecial(item)) b.text(tr("  — скидка дня ×0,5"), Style.GOOD);
        s.with(b);
        RichText sell = text().word(tr("Продать  "), Style.MUTED).word(Format.money(prices.sellPrice(item).orElse(0)), Style.SELL);
        if (prices.isRequested(item)) sell.text(tr("  — станция требует, ×2"), Style.GOOD);
        s.with(sell);
        Optional<StoreOffer> best = prices.cheapestBuy(item);
        if (best.isEmpty()) s.with(text().text(tr("Нигде не продаётся"), Style.MUTED));
        else if (!(buy.isPresent() && best.get().store() == here) && best.get().price() < buy.orElse(Integer.MAX_VALUE))
            s.with(text().text(tr("Дешевле всего: ") + best.get().store().title() + " — ", Style.MUTED).word(Format.money(best.get().price()), Style.BUY));
        return s;
    }

    /** Одна строка: «Купить X · Продать Y». */
    public RichText priceLine(Item item) {
        RichText t = text();
        if (item.price().isEmpty()) return t.text(tr("без цены"), Style.MUTED);
        Optional<Integer> buy = prices.buyPrice(item);
        t.word(tr("Купить "), Style.MUTED);
        if (buy.isPresent()) t.word(Format.money(buy.get()), Style.BUY);
        else {
            Optional<StoreOffer> o = prices.cheapestBuy(item);
            if (o.isPresent()) t.word(Format.money(o.get().price()), Style.BUY).text(" (" + o.get().store().title() + ")", Style.MUTED);
            else t.word(tr("нигде"), Style.MUTED);
        }
        if (buy.isPresent() && prices.isDailySpecial(item)) t.text(tr("  — скидка дня ×0,5"), Style.GOOD);
        t.text(tr(" · Продать "), Style.MUTED).word(Format.money(prices.sellPrice(item).orElse(0)), Style.SELL);
        return prices.isRequested(item) ? t.text(tr("  — станция требует, ×2"), Style.GOOD) : t;
    }

    // ---------- крафт ----------
    public Stack recipes(Item item, boolean compact) {
        Stack s = new Stack(compact ? 2 : 6);
        if (item.recipes().isEmpty()) { s.with(text().text(tr("Не крафтится"), Style.MUTED)); return s; }
        for (Recipe r : item.recipes()) s.with(recipe(item, r, compact));
        return s;
    }

    private Stack recipe(Item item, Recipe r, boolean compact) {
        Stack s = new Stack(1);
        RichText head = text().word(r.fabricator().title(), Style.BOLD);
        if (r.isVending()) head.text(tr(" · купить за ") + Format.money(r.vendingPrice()), Style.MUTED);
        else {
            head.text(" · " + Format.seconds(r.seconds()), Style.MUTED);
            for (SkillRequirement sk : r.skills()) { head.text(" · ", Style.MUTED); head.skill(sk.skill(), sk.level(), Skill.recipeNote(sk.level())); }
            if (r.needsUnlock()) {
                if (item.unlockedBy().isEmpty()) head.text(tr(" · нужен рецепт (чертёж)"), Style.WARN);
                else head.text(tr(" · рецепт открывает талант "), Style.WARN)
                        .join(item.unlockedBy(), Style.MUTED, t -> head.link(t).text(" (" + t.owner() + ")", Style.MUTED));
            }
        }
        if (r.outputCount() > 1) head.word("  → " + r.outputCount() + tr(" шт."), Style.ACCENT);
        s.with(head);
        if (r.ingredients().isEmpty()) return s;
        if (compact) {
            RichText line = text();
            s.with(line.join(merged(r), Style.NORMAL, g -> ingredient(line, g, 3)));
        } else {
            Stack list = new Stack(1).padding(0, 14, 0, 0);
            for (Ingredient g : merged(r)) list.with(ingredient(text(), g, 5));
            s.with(list);
            costLine(item, r).ifPresent(s::with);
        }
        return s;
    }

    /** В файлах игры один и тот же ингредиент иногда записан несколькими строками — показываем одной: «2× …». */
    private static List<Ingredient> merged(Recipe r) {
        List<Ingredient> out = new java.util.ArrayList<>();
        java.util.Map<Item, Integer> idx = new java.util.HashMap<>();
        for (Ingredient g : r.ingredients()) {
            if (g instanceof ItemIngredient && g.conditionNote().isEmpty()) {
                Item it = ((ItemIngredient) g).item();
                Integer at = idx.get(it);
                if (at != null) {
                    Ingredient prev = out.get(at);
                    out.set(at, new ItemIngredient(it, prev.count() + g.count(), null, null, false));
                    continue;
                }
                idx.put(it, out.size());
            }
            out.add(g);
        }
        return out;
    }

    private RichText ingredient(RichText t, Ingredient g, int maxOptions) {
        t.word(g.count() + "× ", Style.NORMAL);
        if (g instanceof ItemIngredient) t.link(((ItemIngredient) g).item());
        else {
            TagIngredient tag = (TagIngredient) g;
            t.text(tag.displayName(), Style.NORMAL);
            if (!tag.options().isEmpty()) {
                limited(t.text(tr(" (подойдёт: "), Style.MUTED), tag.options(), maxOptions).word(")", Style.MUTED);
            }
        }
        String note = g.conditionNote();
        if (!note.isEmpty()) t.text(" " + note, Style.WARN);
        return t;
    }

    /** Стоимость покупки ингредиентов и выгода от продажи готового. */
    private Optional<RichText> costLine(Item item, Recipe r) {
        if (r.isVending()) return Optional.empty();
        long cost = 0;
        for (Ingredient g : r.ingredients()) {
            Optional<Integer> unit = cheapestFor(g);
            if (unit.isEmpty()) return Optional.empty();
            cost += (long) unit.get() * g.count();
        }
        long sell = (long) prices.sellPrice(item).orElse(0) * r.outputCount();
        long profit = sell - cost;
        return Optional.of(text().text(tr("Ингредиенты купить ≈ "), Style.MUTED).word(Format.money(cost), Style.BUY)
                .text(tr(" · продать готовое "), Style.MUTED).word(Format.money(sell), Style.SELL)
                .text(tr(" · выгода "), Style.MUTED).word((profit >= 0 ? "+" : "−") + Format.money(Math.abs(profit)), profit >= 0 ? Style.GOOD : Style.BAD));
    }

    private Optional<Integer> cheapestFor(Ingredient g) {
        if (g instanceof ItemIngredient) return prices.cheapestBuy(((ItemIngredient) g).item()).map(StoreOffer::price);
        Integer best = null;
        for (Item o : ((TagIngredient) g).options()) {
            Optional<StoreOffer> p = prices.cheapestBuy(o);
            if (p.isPresent() && (best == null || p.get().price() < best)) best = p.get().price();
        }
        return Optional.ofNullable(best);
    }

    // ---------- разбор ----------
    public Stack decon(Item item, boolean compact) {
        Stack s = new Stack(1);
        Optional<Deconstruction> od = item.deconstruction();
        if (od.isEmpty()) { s.with(text().text(tr("Не разбирается"), Style.MUTED)); return s; }
        Deconstruction d = od.get();
        RichText head = text().word(tr("Разборщик"), Style.BOLD).text(" · " + Format.seconds(d.seconds()), Style.MUTED);
        if (d.isRandomSingle()) head.text(tr(" · выпадает случайно: ") + d.picks() + tr(" шт. за разбор"), Style.MUTED);
        if (d.isGeneticStationOnly()) head.text(tr(" · только на станции генетических исследований"), Style.WARN);

        s.with(head);
        if (compact) {
            RichText line = text();
            s.with(line.join(d.outputs(), Style.NORMAL, o -> output(line, o, d)));
        } else {
            Stack list = new Stack(1).padding(0, 14, 0, 0);
            for (DeconOutput o : d.outputs()) list.with(output(text(), o, d));
            s.with(list);
            s.with(text().text(tr("Продать полученное: "), Style.MUTED).word(Format.money(prices.sellValue(d)), Style.SELL));
        }
        return s;
    }

    private RichText output(RichText t, DeconOutput o, Deconstruction d) {
        if (o.probability().isPresent()) t.word(Format.chance(o.probability().get()) + " ", Style.ACCENT);
        else t.word(o.count() + "× ", Style.NORMAL);
        t.link(o.item());
        if (o.minCondition() != null) t.text(tr(" если состояние от ") + Format.percent(o.minCondition()), Style.WARN);
        if (o.isChance() && o.probability().isEmpty()) t.text(tr(" не всегда"), Style.MUTED);
        if (o.needsExtraItem()) t.text(tr(" нужен доп. предмет"), Style.WARN);
        return t;
    }

    // ---------- оружие ----------
    /** Характеристики оружия, боеприпаса или взрывчатки; null — если их нет. */
    public Stack weaponStats(Item item) {
        if (item.weapon().isEmpty() && item.ammoFor().isEmpty()) return null;
        Stack s = new Stack(2);
        item.weapon().ifPresent(w -> {
            w.fireInterval().ifPresent(t -> s.with(text().text(tr("Стрельба: "), Style.MUTED).word(rate(t), Style.BOLD)
                    .text(w.isAutomatic() ? tr(" · автоматический огонь") : tr(" · одиночные выстрелы"), Style.MUTED)));
            if (w.spread().isPresent() || w.unskilledSpread().isPresent())
                s.with(text().text(tr("Разброс: "), Style.MUTED).word(w.spread().map(v -> Format.num(v) + "°").orElse("—"), Style.BOLD)
                        .text(tr(" с навыком · "), Style.MUTED).word(w.unskilledSpread().map(v -> Format.num(v) + "°").orElse("—"), Style.WARN)
                        .text(tr(" без навыка "), Style.MUTED).skill(Skill.WEAPONS, 0, tr("Разброс зависит от навыка «Оружие»: чем он ниже, тем ближе разброс к значению «без навыка».")));
            w.meleeInterval().ifPresent(t -> s.with(text().text(tr("Ближний бой: "), Style.MUTED).word(rate(t), Style.BOLD)
                    .text(w.meleeRange().map(r -> tr(" · дальность ≈ ") + Format.num(r / 100) + tr(" м")).orElse(""), Style.MUTED)));
            w.sprayRange().ifPresent(r -> s.with(text().text(tr("Дальность струи ≈ "), Style.MUTED).word(Format.num(r / 100) + tr(" м"), Style.BOLD)));
            w.rounds().ifPresent(n -> s.with(text().text(tr("Выстрелов в одном: "), Style.MUTED).word(String.valueOf(n), Style.BOLD)));
            for (DamageBlock b : w.ownDamage()) s.with(damage(b, null));
            for (DamageBlock b : w.roundDamage()) s.with(damage(b, tr("Урон одного выстрела")));
            if (!w.ammo().isEmpty()) {
                s.with(links(text().text(tr("Заряжается: "), Style.MUTED), w.ammo()));
                Item first = w.ammo().get(0);
                first.weapon().ifPresent(aw -> {
                    List<DamageBlock> bl = aw.roundDamage().isEmpty() ? aw.ownDamage() : aw.roundDamage();
                    for (DamageBlock b : bl) s.with(damage(b, tr("Урон за выстрел (") + first.displayName() + ")"));
                });
            }
        });
        if (!item.ammoFor().isEmpty()) s.with(links(text().text(tr("Подходит к: "), Style.MUTED), item.ammoFor()));
        return s;
    }

    private static String rate(double interval) {
        if (interval <= 0) return tr("непрерывно");
        double perMin = 60 / interval;
        return tr("раз в ") + Format.num(interval) + tr(" с (≈ ") + Math.round(perMin) + tr(" в минуту)");
    }

    private RichText damage(DamageBlock b, String title) {
        RichText t = text();
        String head = title != null ? title : b.kind() == DamageKind.EXPLOSION ? tr("Взрыв") : tr("Попадание");
        if (b.kind() == DamageKind.EXPLOSION && b.radius().isPresent()) head += tr(" (радиус ≈ ") + Format.num(b.radius().get() / 100) + tr(" м)");
        t.text(head + ": ", Style.MUTED).join(b.afflictions(), Style.MUTED, a -> {
            t.text(a.name() + " ", Style.NORMAL).word(a.isStun() ? Format.num(a.strength()) + tr(" с") : Format.num(a.strength()), Style.BAD);
            a.probability().ifPresent(p -> t.text(tr(" (шанс ") + Format.chance(p) + ")", Style.MUTED));
        });
        b.structureDamage().ifPresent(sd -> { if (sd > 0) t.text((b.afflictions().isEmpty() ? "" : " · ") + tr("стенам "), Style.MUTED).word(Format.num(sd), Style.WARN); });
        return t;
    }

    private static RichText links(RichText t, List<Item> items) { return t.join(items, Style.MUTED, t::link); }

    // ---------- ссылки ----------
    public RichText links(List<Item> items, int max) { return limited(text(), items, max); }

    /** Не больше max ссылок, дальше — «и ещё N». */
    private static RichText limited(RichText t, List<Item> items, int max) {
        links(t, items.subList(0, Math.min(max, items.size())));
        return items.size() > max ? t.text(tr(" и ещё ") + (items.size() - max), Style.MUTED) : t;
    }

    // ---------- эффекты ----------
    public Stack effects(List<Effect> list, Predicate<Effect> hit, boolean compact) {
        Stack s = new Stack(1);
        if (list.isEmpty()) { s.with(text().word("—", Style.MUTED)); return s; }
        for (Effect e : list) s.with(effect(e, hit.test(e), compact));
        return s;
    }

    public RichText effect(Effect e, boolean hit, boolean compact) {
        RichText t = text();
        if (hit) t.highlight(Theme.HIT);
        if (e.value().isEmpty()) return t.word("•  ", Style.ACCENT).text(e.name(), Style.NORMAL);
        double v = e.value().get();
        Style vs = e.kind() == EffectKind.BUFF ? Style.ACCENT : e.kind() == EffectKind.GOOD ? Style.GOOD : e.kind() == EffectKind.BAD ? Style.BAD : Style.MUTED;
        t.word(Format.signed(v) + "  ", vs);
        Optional<Affliction> af = ctx == null ? Optional.empty() : ctx.catalog().afflictions().of(e);
        if (af.isPresent() && navigator != null) t.lore(e.name(), af.get(), tr("Недуг: чем лечить, что вызывает, что защищает")); else t.text(e.name(), Style.NORMAL);
        StringBuilder m = new StringBuilder();
        e.duration().ifPresent(d -> m.append(tr(" · за ")).append(Format.seconds(d)));
        e.delay().ifPresent(d -> m.append(tr(" · через ")).append(Format.seconds(d)));
        e.chance().ifPresent(c -> m.append(tr(" · шанс ")).append(Format.percent(c)));
        if (e.isLimbOnly()) m.append(tr(" · на часть тела"));
        for (String c : e.conditions()) m.append(" · ").append(c);
        e.buffSeconds().ifPresent(b -> m.append(tr(" · бафф ")).append(Format.buffTime(b)));
        if (m.length() > 0) t.text(m.toString(), Style.MUTED);
        if (!compact && v > 0) BuffDescriptions.of(e.key()).ifPresent(d -> t.newLine().text("    " + d, Style.MUTED));
        return t;
    }
}
