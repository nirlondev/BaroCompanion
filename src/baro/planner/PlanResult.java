package baro.planner;

import baro.model.*;

import java.util.*;

/** Найденный план и подробности, нужные для проверки (какой именно рецепт и какие варианты ингредиентов). */
public final class PlanResult {
    private final CargoPlan plan;
    private final Map<Recipe, Integer> crafts;
    private final Map<TagOption, Integer> options;
    private final boolean optimal;
    private final String note;

    PlanResult(CargoPlan plan, Map<Recipe, Integer> crafts, Map<TagOption, Integer> options, boolean optimal, String note) {
        this.plan = plan; this.crafts = crafts; this.options = options; this.optimal = optimal; this.note = note;
    }

    static PlanResult empty(PlanKind kind, String note) {
        CargoPlan p = new CargoPlan(kind, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), 0, List.of());
        return new PlanResult(p, Map.of(), Map.of(), true, note);
    }

    /** Тот же план под другим режимом (когда в более широком режиме лучше не нашлось). */
    PlanResult relabel(PlanKind kind, String note) {
        CargoPlan p = new CargoPlan(kind, plan.deconstruct(), plan.useDirectly(), plan.buy(), plan.craft(), plan.deconstructCrafted(), plan.sell(),
                plan.seconds(), plan.lockedRecipes()).withChanceValue(plan.chanceValue());
        return new PlanResult(p, crafts, options, optimal, note);
    }

    /** Тот же план с пометкой «поиск остановлен по времени». */
    PlanResult approximate() { return new PlanResult(plan, crafts, options, false, note); }

    public CargoPlan plan() { return plan; }
    /** Рецепт → сколько раз скрафтить. */
    public Map<Recipe, Integer> crafts() { return crafts; }
    Map<TagOption, Integer> options() { return options; }
    /** false — поиск остановлен по времени, план хороший, но может быть не самым лучшим. */
    public boolean isOptimal() { return optimal; }
    public Optional<String> note() { return Optional.ofNullable(note); }
}
