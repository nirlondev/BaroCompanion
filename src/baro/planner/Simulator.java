package baro.planner;

import baro.model.*;

import java.util.*;

/**
 * Выполняет план по шагам, как в игре: разбирает и крафтит, пока хватает предметов.
 * Возвращает, что реально удалось сделать и что осталось на руках.
 */
final class Simulator {
    final Map<Item, Integer> inventory = new HashMap<>();
    /** Сколько каждого предмета получено (не из груза) — только это можно продать. */
    final Map<Item, Integer> produced = new HashMap<>();
    final Map<Item, Integer> doneDecon = new LinkedHashMap<>();
    final Map<Recipe, Integer> doneCraft = new LinkedHashMap<>();
    final Map<TagOption, Integer> doneOptions = new LinkedHashMap<>();

    Simulator(Map<Item, Integer> cargo, Map<Item, Integer> buy) {
        inventory.putAll(cargo);
        buy.forEach((i, n) -> { inventory.merge(i, n, Integer::sum); produced.merge(i, n, Integer::sum); });
    }

    /** Выполнить столько шагов из заданных, сколько возможно. */
    void run(Map<Item, Integer> decon, Map<Recipe, Integer> crafts, Map<TagOption, Integer> options) {
        Map<Item, Integer> decLeft = new LinkedHashMap<>(decon);
        Map<Recipe, Integer> craftLeft = new LinkedHashMap<>(crafts);
        Map<TagOption, Integer> optLeft = new LinkedHashMap<>(options); // варианты берутся по порядку
        boolean progress = true;
        while (progress) {
            progress = false;
            for (Map.Entry<Item, Integer> e : decLeft.entrySet()) {
                while (e.getValue() > 0 && inventory.getOrDefault(e.getKey(), 0) > 0) {
                    inventory.merge(e.getKey(), -1, Integer::sum);
                    for (DeconOutput o : e.getKey().deconstruction().get().outputs())
                        if (CargoPlanner.deterministic(o)) { inventory.merge(o.item(), o.count(), Integer::sum); produced.merge(o.item(), o.count(), Integer::sum); }
                    e.setValue(e.getValue() - 1);
                    doneDecon.merge(e.getKey(), 1, Integer::sum);
                    progress = true;
                }
            }
            for (Map.Entry<Recipe, Integer> e : craftLeft.entrySet()) {
                Recipe r = e.getKey();
                while (e.getValue() > 0) {
                    Map<Item, Integer> need = new HashMap<>();
                    Map<TagOption, Integer> takes = new HashMap<>();
                    if (!collect(r, optLeft, need, takes)) break;
                    need.forEach((i, n) -> inventory.merge(i, -n, Integer::sum));
                    takes.forEach((o, n) -> { optLeft.merge(o, -n, Integer::sum); doneOptions.merge(o, n, Integer::sum); });
                    inventory.merge(r.output(), r.outputCount(), Integer::sum);
                    produced.merge(r.output(), r.outputCount(), Integer::sum);
                    e.setValue(e.getValue() - 1);
                    doneCraft.merge(r, 1, Integer::sum);
                    progress = true;
                }
            }
        }
    }

    /** Что расходует один крафт (need) и сколько каких вариантов тегов (takes); false — не хватает предметов. */
    private boolean collect(Recipe r, Map<TagOption, Integer> optLeft, Map<Item, Integer> need, Map<TagOption, Integer> takes) {
        for (Ingredient g : r.ingredients()) {
            if (g instanceof ItemIngredient ii) { need.merge(ii.item(), g.count(), Integer::sum); continue; }
            int left = g.count();
            for (Map.Entry<TagOption, Integer> o : optLeft.entrySet()) {
                if (left == 0) break;
                if (o.getKey().recipe() != r || o.getKey().ingredient() != g) continue;
                Item it = o.getKey().item();
                int take = Math.min(left, Math.min(o.getValue(), inventory.getOrDefault(it, 0) - need.getOrDefault(it, 0)));
                if (take > 0) { need.merge(it, take, Integer::sum); takes.put(o.getKey(), take); left -= take; }
            }
            if (left > 0) return false;
        }
        for (Map.Entry<Item, Integer> n : need.entrySet()) if (inventory.getOrDefault(n.getKey(), 0) < n.getValue()) return false;
        return true;
    }

    /** Сколько можно продать: остаток на руках, но не больше полученного (груз продавать нельзя). */
    int sellable(Item i) { return Math.max(0, Math.min(inventory.getOrDefault(i, 0), produced.getOrDefault(i, 0))); }
}
