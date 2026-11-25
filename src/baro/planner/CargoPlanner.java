package baro.planner;


import static baro.Tr.tr;
import baro.model.*;
import baro.pricing.PriceCalculator;
import baro.pricing.StoreOffer;

import java.util.*;

/**
 * Автоматический поиск лучшего способа переработать груз: что разобрать, что скрафтить, что докупить и что продать.
 * Строит целочисленную задачу по ТЕКУЩИМ рецептам и ценам и решает её. Каждый найденный план затем проверяется
 * пошаговым прогоном (PlanChecker) — план, который нельзя выполнить, не показывается.
 */
public final class CargoPlanner {
    private final PriceCalculator prices;

    public CargoPlanner(PriceCalculator prices) { this.prices = prices; }

    /** @param budgetMs сколько времени можно искать; если не хватило — лучший найденный план с пометкой «примерно» */
    public PlanResult plan(Map<Item, Integer> cargo, PlanKind kind, long budgetMs) {
        long deadline = System.currentTimeMillis() + budgetMs;
        Model m = new Model(cargo, kind);
        // Решение задачи не учитывает порядок шагов, поэтому каждый план «проигрывается» по шагам, как в игре:
        // выполняется только то, на что реально хватает предметов, продаётся только реально полученное.
        // Если какой-то шаг не удался целиком, он ограничивается и задача решается снова; берётся лучший выполнимый план.
        PlanResult best = null;
        int bestValue = Integer.MIN_VALUE;
        boolean optimal = true;
        for (int round = 0; round < 30; round++) {
            long left = deadline - System.currentTimeMillis();
            if (round > 0 && left <= 0) { optimal = false; break; }
            LinearProgram.Result r = m.lp.solveInteger(4000, Math.max(200, left));
            if (r == null) break;
            optimal &= r.optimal();
            PlanResult res = m.realize(r.x(), kind, r.optimal());
            if (new PlanChecker(cargo).check(res) == null && value(res.plan()) > bestValue) { bestValue = value(res.plan()); best = res; }
            // что не выполнилось целиком — ограничить
            boolean cut = false;
            for (Map.Entry<Recipe, Integer> e : m.craftVar.entrySet()) {
                int want = (int) Math.floor(r.x()[e.getValue()] + 1e-6), done = res.crafts().getOrDefault(e.getKey(), 0);
                if (done < want) { m.limit(e.getValue(), done); cut = true; }
            }
            if (!cut) break;
        }
        if (best == null || best.plan().isEmpty()) return PlanResult.empty(kind, tr("Ничего ценного не получить"));
        return optimal ? best : best.approximate();
    }

    /** Чистая выручка плана по текущим ценам (продажа − покупки + средние случайные выпадения). */
    public int value(CargoPlan p) {
        int v = p.chanceValue();
        for (Map.Entry<Item, Integer> e : p.sell().entrySet()) v += sellPrice(e.getKey()) * e.getValue();
        for (Map.Entry<Item, Integer> e : p.buy().entrySet()) v -= cost(e.getKey()) * e.getValue();
        return v;
    }

    /** Лучший из планов. */
    public Optional<PlanResult> best(List<PlanResult> list) {
        return list.stream().filter(r -> !r.plan().isEmpty())
                .max(Comparator.comparingInt(r -> value(r.plan())));
    }

    /**
     * Все три варианта. Каждый следующий режим даёт больше возможностей, поэтому если в нём не нашлось плана лучше,
     * показывается план предыдущего режима.
     */
    public synchronized List<PlanResult> all(Map<Item, Integer> cargo, long budgetPerMode) {
        List<PlanResult> list = new ArrayList<>();
        PlanResult prev = null;
        for (PlanKind k : PlanKind.values()) {
            PlanResult r = plan(cargo, k, budgetPerMode);
            if (prev != null && !prev.plan().isEmpty() && value(prev.plan()) >= value(r.plan()))
                r = prev.relabel(k, tr("В этом режиме выгоднее не нашлось — тот же план, что выше."));
            list.add(r);
            prev = r;
        }
        return list;
    }

    // ===================================================================

    /** Переменные и ограничения задачи для одного груза и режима. */
    private final class Model {
        final Map<Item, Integer> cargo;
        final LinearProgram lp;
        final Map<Item, Integer> decVar = new LinkedHashMap<>(), buyVar = new LinkedHashMap<>(), sellVar = new LinkedHashMap<>();
        /** Рецепт → переменная «сколько раз скрафтить». */
        final Map<Recipe, Integer> craftVar = new LinkedHashMap<>();
        final List<TagOption> options = new ArrayList<>();
        final Set<Item> items = new LinkedHashSet<>();

        Model(Map<Item, Integer> cargo, PlanKind kind) {
            this.cargo = cargo;
            boolean buy = kind == PlanKind.WITH_PURCHASE;
            Set<Item> reach = new LinkedHashSet<>(cargo.keySet());
            Set<Recipe> recipes = reachable(reach, kind != PlanKind.DECONSTRUCT, buy);
            // ингредиенты, которые придётся докупать
            Set<Item> buyable = new LinkedHashSet<>();
            if (buy) for (Recipe r : recipes) for (Ingredient g : r.ingredients())
                for (Item opt : options(g)) if (prices.cheapestBuy(opt).isPresent()) buyable.add(opt);
            items.addAll(reach);
            items.addAll(buyable);
            lp = new LinearProgram(variables(reach, recipes, buyable));
            // цель: продажа − покупка + средняя цена случайных выпадений − чуть-чуть за время
            sellVar.forEach((i, v) -> lp.objective(v, sellPrice(i)));
            buyVar.forEach((i, v) -> lp.objective(v, -cost(i)));
            decVar.forEach((i, v) -> lp.objective(v, chanceValue(usableDecon(i)) - usableDecon(i).seconds() * 0.002));
            craftVar.forEach((r, v) -> lp.objective(v, -r.seconds() * 0.002));
            balance();
            tagChoices();
        }

        /** Какие предметы (дописываются в reach) и рецепты вообще достижимы из груза. */
        private Set<Recipe> reachable(Set<Item> reach, boolean craft, boolean buy) {
            Set<Recipe> recipes = new LinkedHashSet<>();
            boolean grew = true;
            while (grew) {
                grew = false;
                for (Item i : new ArrayList<>(reach)) {
                    Deconstruction d = usableDecon(i);
                    if (d != null) for (DeconOutput o : d.outputs()) if (deterministic(o) && reach.add(o.item())) grew = true;
                }
                if (!craft) continue;
                Set<Item> outputs = new LinkedHashSet<>();
                for (Item i : reach) outputs.addAll(i.usedIn());
                for (Item out : outputs)
                    for (Recipe r : out.recipes())
                        if (!recipes.contains(r) && usable(r) && satisfiable(r, reach, buy)) {
                            recipes.add(r);
                            if (reach.add(r.output())) grew = true;
                        }
            }
            return recipes;
        }

        /** Номера переменных задачи; возвращает их число. */
        private int variables(Set<Item> reach, Set<Recipe> recipes, Set<Item> buyable) {
            int n = 0;
            for (Item i : reach) if (usableDecon(i) != null) decVar.put(i, n++);
            for (Recipe r : recipes) craftVar.put(r, n++);
            for (Recipe r : recipes) for (Ingredient g : r.ingredients())
                if (g instanceof TagIngredient) for (Item opt : options(g)) if (items.contains(opt)) options.add(new TagOption(r, g, opt, n++));
            for (Item i : buyable) buyVar.put(i, n++);
            for (Item i : items) if (sellPrice(i) > 0) sellVar.put(i, n++);
            return n;
        }

        /** Баланс по каждому предмету: потрачено + продано − получено − куплено ≤ было в грузе. */
        private void balance() {
            // produced — только полученное разбором, крафтом или покупкой: груз миссии продать нельзя
            Map<Item, double[]> row = new HashMap<>(), produced = new HashMap<>();
            for (Item i : items) { row.put(i, lp.newRow()); produced.put(i, lp.newRow()); }
            decVar.forEach((i, v) -> {
                row.get(i)[v] += 1;
                for (DeconOutput o : usableDecon(i).outputs()) if (deterministic(o)) gain(row, produced, o.item(), v, o.count());
            });
            craftVar.forEach((r, v) -> {
                for (Ingredient g : r.ingredients())
                    if (g instanceof ItemIngredient ii && row.containsKey(ii.item())) row.get(ii.item())[v] += g.count();
                gain(row, produced, r.output(), v, r.outputCount());
            });
            for (TagOption o : options) row.get(o.item())[o.var()] += 1;
            buyVar.forEach((i, v) -> gain(row, produced, i, v, 1));
            sellVar.forEach((i, v) -> { row.get(i)[v] += 1; produced.get(i)[v] += 1; });
            int units = 0;
            for (int v : cargo.values()) units += v;
            for (Item i : items) {
                if (sellVar.containsKey(i)) lp.constraint(produced.get(i), LinearProgram.Sense.LE, 0);
                lp.constraint(row.get(i), LinearProgram.Sense.LE, cargo.getOrDefault(i, 0));
                // ограничение покупок, чтобы задача была конечной
                Integer bv = buyVar.get(i);
                if (bv != null) limit(bv, 20 * Math.max(1, units));
            }
        }

        private void gain(Map<Item, double[]> row, Map<Item, double[]> produced, Item i, int var, int count) {
            if (!row.containsKey(i)) return;
            row.get(i)[var] -= count;
            produced.get(i)[var] -= count;
        }

        /** Ингредиент-«тег»: сумма выбранных вариантов = нужное количество. */
        private void tagChoices() {
            craftVar.forEach((r, v) -> {
                for (Ingredient g : r.ingredients()) {
                    if (!(g instanceof TagIngredient)) continue;
                    double[] row = lp.newRow();
                    row[v] = -g.count();
                    for (TagOption o : options) if (o.recipe() == r && o.ingredient() == g) row[o.var()] += 1;
                    lp.constraint(row, LinearProgram.Sense.EQ, 0);
                }
            });
        }

        /** Ограничить переменную сверху (отсечение невыполнимого шага, предел покупок). */
        void limit(int var, int max) {
            double[] row = lp.newRow();
            row[var] = 1;
            lp.constraint(row, LinearProgram.Sense.LE, Math.max(0, max));
        }

        /** Превратить решение задачи в выполнимый план: округлить вниз, «проиграть» по шагам, продать полученное. */
        PlanResult realize(double[] x, PlanKind kind, boolean optimal) {
            Map<Item, Integer> dec = new LinkedHashMap<>(), buy = new LinkedHashMap<>();
            Map<Recipe, Integer> crafts = new LinkedHashMap<>();
            Map<TagOption, Integer> opts = new LinkedHashMap<>();
            decVar.forEach((i, v) -> putPositive(dec, i, (int) Math.floor(x[v] + 1e-6)));
            craftVar.forEach((r, v) -> putPositive(crafts, r, (int) Math.floor(x[v] + 1e-6)));
            for (TagOption o : options) putPositive(opts, o, (int) Math.ceil(x[o.var()] - 1e-6));
            buyVar.forEach((i, v) -> putPositive(buy, i, (int) Math.ceil(x[v] - 1e-6)));
            Simulator sim = new Simulator(cargo, buy);
            sim.run(dec, crafts, opts);
            // купленное, но не понадобившееся — не покупаем
            Map<Item, Integer> needBuy = new LinkedHashMap<>();
            for (Map.Entry<Item, Integer> e : buy.entrySet())
                putPositive(needBuy, e.getKey(), e.getValue() - Math.min(e.getValue(), sim.inventory.getOrDefault(e.getKey(), 0)));
            if (!needBuy.equals(buy)) {
                sim = new Simulator(cargo, needBuy);
                sim.run(dec, crafts, opts);
            }
            Map<Item, Integer> sell = new LinkedHashMap<>();
            for (Item i : items) if (sellPrice(i) > 0) putPositive(sell, i, sim.sellable(i));
            return build(kind, sim, needBuy, sell, optimal);
        }

        PlanResult build(PlanKind kind, Simulator sim, Map<Item, Integer> buy, Map<Item, Integer> sell, boolean optimal) {
            Map<Item, Integer> decCargo = new LinkedHashMap<>(), decOther = new LinkedHashMap<>(), use = new LinkedHashMap<>(), craft = new LinkedHashMap<>();
            double seconds = 0;
            int chance = 0;
            for (Map.Entry<Item, Integer> e : sim.doneDecon.entrySet()) {
                Item i = e.getKey();
                int k = e.getValue(), fromCargo = Math.min(k, cargo.getOrDefault(i, 0));
                putPositive(decCargo, i, fromCargo);
                putPositive(decOther, i, k - fromCargo);
                Deconstruction d = usableDecon(i);
                seconds += d.seconds() * k;
                chance += (int) Math.round(chanceValue(d) * k);
            }
            Set<Item> lockedUsed = new LinkedHashSet<>();
            for (Map.Entry<Recipe, Integer> e : sim.doneCraft.entrySet()) {
                craft.merge(e.getKey().output(), e.getValue(), Integer::sum);
                seconds += e.getKey().seconds() * e.getValue();
                if (e.getKey().needsUnlock()) lockedUsed.add(e.getKey().output());
            }
            // груз сразу в крафт: сколько его ушло в рецепты, не считая разобранного
            for (Map.Entry<Item, Integer> e : cargo.entrySet()) {
                Item i = e.getKey();
                int consumed = 0;
                for (Map.Entry<Recipe, Integer> c : sim.doneCraft.entrySet())
                    for (Ingredient g : c.getKey().ingredients())
                        if (g instanceof ItemIngredient ii && ii.item() == i) consumed += g.count() * c.getValue();
                for (Map.Entry<TagOption, Integer> o : sim.doneOptions.entrySet()) if (o.getKey().item() == i) consumed += o.getValue();
                putPositive(use, i, Math.min(e.getValue() - decCargo.getOrDefault(i, 0), consumed));
            }
            CargoPlan plan = new CargoPlan(kind, decCargo, use, buy, craft, decOther, sell, seconds, new ArrayList<>(lockedUsed)).withChanceValue(chance);
            return new PlanResult(plan, sim.doneCraft, sim.doneOptions, optimal, null);
        }
    }

    private static <K> void putPositive(Map<K, Integer> m, K key, int n) { if (n > 0) m.put(key, n); }

    // ===================================================================

    /** Разбор, которым можно пользоваться (без станции генетики и без доп. предмета). */
    static Deconstruction usableDecon(Item i) {
        Deconstruction d = i.deconstruction().orElse(null);
        return d == null || d.isGeneticStationOnly() || d.outputs().isEmpty() ? null : d;
    }

    /** Выход разбора, который выпадает всегда. */
    static boolean deterministic(DeconOutput o) { return !o.isChance() && o.probability().isEmpty() && !o.needsExtraItem(); }

    /** Рецепт, которым можно пользоваться в этом режиме. */
    private static boolean usable(Recipe r) {
        if (r.isVending() || r.needsUnlock()) return false;
        for (Ingredient g : r.ingredients()) if (g.maxCondition() != null && g.maxCondition() <= 0.1) return false; // нужен пустой/сломанный предмет
        return !r.ingredients().isEmpty();
    }

    private static List<Item> options(Ingredient g) {
        return g instanceof ItemIngredient ii ? List.of(ii.item()) : ((TagIngredient) g).options();
    }

    private boolean satisfiable(Recipe r, Set<Item> reach, boolean buy) {
        boolean anchored = false;
        for (Ingredient g : r.ingredients()) {
            boolean ok = false;
            for (Item opt : options(g)) {
                if (reach.contains(opt)) { ok = true; anchored = true; }
                else if (buy && prices.cheapestBuy(opt).isPresent()) ok = true;
            }
            if (!ok) return false;
        }
        return anchored; // хотя бы один ингредиент должен идти из груза
    }

    private int sellPrice(Item i) { return prices.sellPrice(i).orElse(0); }

    private int cost(Item i) { return prices.cheapestBuy(i).map(StoreOffer::price).orElse(Integer.MAX_VALUE / 4); }

    /** Средняя выручка от случайных выпадений одного разбора. */
    private double chanceValue(Deconstruction d) {
        double v = 0;
        for (DeconOutput o : d.outputs()) if (!deterministic(o) && !o.needsExtraItem()) v += sellPrice(o.item()) * d.expectedCount(o);
        return v;
    }
}
