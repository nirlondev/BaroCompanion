package baro.planner;


import static baro.Tr.tr;
import baro.model.*;

import java.util.*;

/**
 * Пошаговая проверка плана: начинаем с груза и покупок и выполняем разборы и крафты в любом возможном порядке.
 * Если какой-то шаг невозможен (не хватает ингредиента — как было с кислородитом) или продаётся то, чего нет,
 * план считается ошибочным.
 */
public final class PlanChecker {
    private final Map<Item, Integer> cargo;

    public PlanChecker(Map<Item, Integer> cargo) { this.cargo = cargo; }

    /** @return null — план выполним; иначе описание проблемы */
    public String check(PlanResult r) {
        CargoPlan p = r.plan();
        Map<Item, Integer> inv = new HashMap<>(cargo), produced = new HashMap<>();
        p.buy().forEach((i, n) -> { inv.merge(i, n, Integer::sum); produced.merge(i, n, Integer::sum); });
        Map<Item, Integer> dec = new LinkedHashMap<>(p.deconstruct());
        p.deconstructCrafted().forEach((i, n) -> dec.merge(i, n, Integer::sum));
        Map<Recipe, Integer> crafts = new LinkedHashMap<>(r.crafts());
        // выбранные варианты для ингредиентов-«тегов»: рецепт → (ингредиент → (предмет → на все крафты))
        Map<Recipe, Map<Ingredient, Map<Item, Integer>>> opts = new HashMap<>();
        r.options().forEach((o, n) -> opts.computeIfAbsent(o.recipe(), k -> new HashMap<>())
                .computeIfAbsent(o.ingredient(), k -> new HashMap<>()).merge(o.item(), n, Integer::sum));

        boolean progress = true;
        while (progress) {
            progress = false;
            for (Map.Entry<Item, Integer> e : dec.entrySet()) {
                while (e.getValue() > 0 && inv.getOrDefault(e.getKey(), 0) > 0) {
                    inv.merge(e.getKey(), -1, Integer::sum);
                    for (DeconOutput o : e.getKey().deconstruction().get().outputs())
                        if (CargoPlanner.deterministic(o)) { inv.merge(o.item(), o.count(), Integer::sum); produced.merge(o.item(), o.count(), Integer::sum); }
                    e.setValue(e.getValue() - 1);
                    progress = true;
                }
            }
            for (Map.Entry<Recipe, Integer> e : crafts.entrySet()) {
                Recipe rc = e.getKey();
                Map<Ingredient, Map<Item, Integer>> opt = opts.get(rc);
                while (e.getValue() > 0 && enough(needOnce(rc, opt), inv)) {
                    craftOnce(rc, inv, opt);
                    inv.merge(rc.output(), rc.outputCount(), Integer::sum);
                    produced.merge(rc.output(), rc.outputCount(), Integer::sum);
                    e.setValue(e.getValue() - 1);
                    progress = true;
                }
            }
        }
        for (Map.Entry<Recipe, Integer> e : crafts.entrySet())
            if (e.getValue() > 0) return tr("не хватает ингредиентов для «") + e.getKey().output().displayName() + "»: " + missing(e.getKey(), inv, opts.get(e.getKey()));
        for (Map.Entry<Item, Integer> e : dec.entrySet())
            if (e.getValue() > 0) return tr("не хватает «") + e.getKey().displayName() + tr("» для разбора");
        for (Map.Entry<Item, Integer> e : p.sell().entrySet()) {
            if (inv.getOrDefault(e.getKey(), 0) < e.getValue()) return tr("продаётся больше «") + e.getKey().displayName() + tr("», чем есть");
            if (produced.getOrDefault(e.getKey(), 0) < e.getValue()) return tr("продаётся груз миссии «") + e.getKey().displayName() + tr("» — так нельзя");
        }
        return null;
    }

    private static boolean enough(Map<Item, Integer> need, Map<Item, Integer> inv) {
        if (need == null) return false;
        for (Map.Entry<Item, Integer> e : need.entrySet()) if (inv.getOrDefault(e.getKey(), 0) < e.getValue()) return false;
        return true;
    }

    private static void craftOnce(Recipe r, Map<Item, Integer> inv, Map<Ingredient, Map<Item, Integer>> opt) {
        needOnce(r, opt).forEach((i, n) -> inv.merge(i, -n, Integer::sum));
        if (opt != null) for (Ingredient g : r.ingredients()) {
            Map<Item, Integer> m = opt.get(g);
            if (m == null) continue;
            int left = g.count();
            for (Map.Entry<Item, Integer> e : m.entrySet()) {
                int take = Math.min(left, e.getValue());
                e.setValue(e.getValue() - take);
                left -= take;
            }
        }
    }

    /** Что расходует один крафт (null — не выбран вариант тега). Для тегов — из выбранных вариантов, по порядку. */
    private static Map<Item, Integer> needOnce(Recipe r, Map<Ingredient, Map<Item, Integer>> opt) {
        Map<Item, Integer> need = new HashMap<>();
        for (Ingredient g : r.ingredients()) {
            if (g instanceof ItemIngredient ii) { need.merge(ii.item(), g.count(), Integer::sum); continue; }
            Map<Item, Integer> m = opt == null ? null : opt.get(g);
            if (m == null) return null;
            int left = g.count();
            for (Map.Entry<Item, Integer> e : m.entrySet()) {
                int take = Math.min(left, e.getValue());
                if (take > 0) need.merge(e.getKey(), take, Integer::sum);
                left -= take;
            }
            if (left > 0) return null;
        }
        return need;
    }

    private static String missing(Recipe r, Map<Item, Integer> inv, Map<Ingredient, Map<Item, Integer>> opt) {
        Map<Item, Integer> need = needOnce(r, opt);
        if (need == null) return tr("не выбран вариант ингредиента");
        StringJoiner b = new StringJoiner(", ");
        need.forEach((i, n) -> { if (inv.getOrDefault(i, 0) < n) b.add(i.displayName()); });
        return b.toString();
    }
}
