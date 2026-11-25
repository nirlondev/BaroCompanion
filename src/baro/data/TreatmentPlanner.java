package baro.data;

import baro.model.*;

import java.util.*;
import java.util.function.ToIntFunction;

/**
 * План лечения сразу от нескольких недугов: жадно набирается наименьший набор препаратов, который снимает все отмеченные.
 * Выше ценятся сильное лечение, наличие на борту, нехватка навыка и побочные эффекты — штраф.
 */
public final class TreatmentPlanner {
    /** Препарат в плане: что он снимает из отмеченного и насколько, его побочные эффекты. */
    public record Pick(Medicine medicine, Map<Affliction, Double> cures, List<Effect> sideEffects, boolean skillTooLow, int onBoard) { }

    /** План: препараты по порядку и недуги, от которых лечения не нашлось. */
    public record Plan(List<Pick> picks, List<Affliction> untreated) { }

    private TreatmentPlanner() {}

    /**
     * @param skill    навык «Медицина» того, кто лечит (ниже нужного — возможен провал)
     * @param onBoard  сколько препарата на борту (0 — нет или сейва нет)
     * @param obtainable можно ли его достать: купить, скрафтить или есть на борту
     */
    public static Plan plan(AfflictionIndex idx, List<Affliction> selected, int skill, ToIntFunction<Item> onBoard,
                            java.util.function.Predicate<Item> obtainable) {
        Map<Medicine, Map<Affliction, Double>> cures = new LinkedHashMap<>();
        for (Affliction a : selected)
            for (AfflictionIndex.Use u : idx.treatments(a))
                cures.computeIfAbsent(u.medicine(), k -> new LinkedHashMap<>()).merge(a, Math.abs(u.effect().value().orElse(0.0)), Math::max);
        Set<Affliction> left = new LinkedHashSet<>(selected);
        List<Pick> picks = new ArrayList<>();
        while (!left.isEmpty()) {
            Medicine best = null;
            double bestScore = 0;
            for (Map.Entry<Medicine, Map<Affliction, Double>> e : cures.entrySet()) {
                double gain = 0;
                for (Map.Entry<Affliction, Double> c : e.getValue().entrySet()) if (left.contains(c.getKey())) gain += 0.5 + Math.min(c.getValue(), 100) / 200;
                if (gain <= 0) continue;
                Medicine m = e.getKey();
                double score = gain - 0.12 * sideEffects(m, selected).size();
                if (m.useSkill().orElse(0) > skill) score -= 0.3;
                if (onBoard.applyAsInt(m.item()) > 0) score += 0.25;
                else if (!obtainable.test(m.item())) score -= 0.4;
                if (m.isBest()) score += 0.05;
                if (best == null || score > bestScore) { best = m; bestScore = score; }
            }
            if (best == null) break;
            Map<Affliction, Double> mine = new LinkedHashMap<>();
            for (Map.Entry<Affliction, Double> c : cures.get(best).entrySet()) if (left.contains(c.getKey())) mine.put(c.getKey(), c.getValue());
            left.removeAll(mine.keySet());
            picks.add(new Pick(best, mine, sideEffects(best, selected), best.useSkill().orElse(0) > skill, onBoard.applyAsInt(best.item())));
            cures.remove(best);
        }
        return new Plan(picks, new ArrayList<>(left));
    }

    /** Побочные эффекты: что препарат добавляет плохого (кроме того, что и так лечим). */
    private static List<Effect> sideEffects(Medicine m, List<Affliction> selected) {
        List<Effect> l = new ArrayList<>();
        for (Effect e : m.onSuccess()) if (e.kind() == EffectKind.BAD && e.value().orElse(0.0) > 0) l.add(e);
        return l;
    }
}
