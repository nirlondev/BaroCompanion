package baro.ui.views;


import static baro.Tr.tr;
import baro.model.CargoPlan;
import baro.model.Item;
import baro.planner.CargoPlanner;
import baro.planner.PlanResult;
import baro.pricing.Format;
import baro.ui.widgets.RichText;
import baro.ui.widgets.Stack;
import baro.ui.widgets.Style;

import java.util.List;
import java.util.Map;

/** Отображение плана переработки: шаги со ссылками, выручка, время. */
public final class PlanView extends Stack {
    public PlanView(PlanResult r, boolean best, Blocks b, CargoPlanner planner) {
        super(1);
        CargoPlan p = r.plan();
        RichText head = b.text();
        if (best) head.badge();
        head.word(p.kind().title(), Style.BOLD);
        if (p.isEmpty()) {
            with(head.text("  — " + r.note().orElse(tr("ничего ценного не получить")), Style.MUTED));
            return;
        }
        int value = planner.value(p);
        head.text("   ", Style.NORMAL).word((value >= 0 ? "+" : "−") + Format.money(Math.abs(value)), value > 0 ? Style.GOOD : Style.BAD)
                .text(" · ≈ " + (p.seconds() >= 90 ? Math.round(p.seconds() / 60) + tr(" мин") : Format.seconds(p.seconds())), Style.MUTED);
        if (!r.isOptimal()) head.text(tr(" · найден за отведённое время, может быть не самым лучшим"), Style.MUTED);
        with(head);
        with(b.text().text(p.kind().description(), Style.MUTED));
        r.note().ifPresent(n -> with(b.text().text(n, Style.MUTED)));
        Stack steps = new Stack(1).padding(0, 14, 0, 0);
        step(steps, b, tr("Разобрать груз: "), p.deconstruct());
        if (!p.buy().isEmpty()) {
            int cost = 0;
            for (Map.Entry<Item, Integer> e : p.buy().entrySet()) cost += b.prices().cheapestBuy(e.getKey()).map(o -> o.price()).orElse(0) * e.getValue();
            steps.with(list(b.text().text(tr("Докупить: "), Style.MUTED), p.buy()).text("  (−" + Format.money(cost) + ")", Style.BUY));
        }
        step(steps, b, tr("Груз сразу в крафт: "), p.useDirectly());
        step(steps, b, tr("Скрафтить (раз): "), p.craft());
        step(steps, b, tr("Разобрать скрафченное и полученное: "), p.deconstructCrafted());
        if (!p.sell().isEmpty()) {
            int rev = 0;
            for (Map.Entry<Item, Integer> e : p.sell().entrySet()) rev += b.prices().sellPrice(e.getKey()).orElse(0) * e.getValue();
            steps.with(list(b.text().text(tr("Продать: "), Style.MUTED), p.sell()).text("  (+" + Format.money(rev) + ")", Style.SELL));
        }
        if (p.chanceValue() > 0)
            steps.with(b.text().text(tr("+ случайные выпадения при разборе в среднем ≈ "), Style.MUTED).word(Format.money(p.chanceValue()), Style.SELL));
        if (!p.lockedRecipes().isEmpty()) steps.with(links(b.text().text(tr("Нужны открытые рецепты: "), Style.WARN), p.lockedRecipes()));
        with(steps);
    }

    private static void step(Stack steps, Blocks b, String label, Map<Item, Integer> m) {
        if (!m.isEmpty()) steps.with(list(b.text().text(label, Style.MUTED), m));
    }

    /** «2× предмет, 5× предмет». */
    public static RichText list(RichText t, Map<Item, Integer> m) {
        return t.join(m.entrySet(), Style.MUTED, e -> t.word(e.getValue() + "× ", Style.NORMAL).link(e.getKey()));
    }

    private static RichText links(RichText t, List<Item> items) { return t.join(items, Style.MUTED, t::link); }

    /** Набор из трёх планов с отметкой лучшего. */
    public static Stack all(List<PlanResult> plans, Blocks b, CargoPlanner planner) {
        Stack s = new Stack(8);
        PlanResult best = planner.best(plans).orElse(null);
        for (PlanResult r : plans) s.with(new PlanView(r, r == best, b, planner));
        return s;
    }
}
