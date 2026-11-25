package baro.search;

import baro.model.Effect;
import baro.model.MedCategory;
import baro.model.Medicine;

import java.util.*;
import java.util.stream.Collectors;

/** Поиск препаратов по названию и/или по эффекту. */
public final class MedicineSearch {
    private final Collection<Medicine> medicines;
    private final Set<String> effectNames = new HashSet<>();

    public MedicineSearch(Collection<Medicine> medicines) {
        this.medicines = medicines;
        for (Medicine m : medicines) for (Effect e : m.onSuccess()) effectNames.add(e.name());
    }

    /** @param category null — все разделы */
    public List<MedicineMatch> find(String nameQuery, String effectQuery, EffectMode mode, MedCategory category) {
        String q = ItemSearch.normalize(nameQuery);
        EffectMatcher em = EffectMatcher.of(effectQuery, mode, effectNames);
        List<MedicineMatch> res = new ArrayList<>();
        for (Medicine m : medicines) {
            if (category != null && m.category() != category) continue;
            if (!q.isEmpty() && !m.item().nameContains(q) && !m.item().id().contains(q)) continue;
            Effect best = null;
            if (!em.isEmpty()) {
                for (Effect e : m.onSuccess())
                    if (em.matches(e) && (best == null || strength(e) > strength(best))) best = e;
                if (best == null) continue;
            }
            res.add(new MedicineMatch(m, best));
        }
        Comparator<MedicineMatch> byName = Comparator.comparing(x -> x.medicine().item().displayName(), String.CASE_INSENSITIVE_ORDER);
        Comparator<MedicineMatch> order = em.isEmpty()
                ? Comparator.comparing((MedicineMatch x) -> !x.medicine().isBest()).thenComparing(byName)
                : Comparator.comparingDouble((MedicineMatch x) -> -strength(x.effect().get())).thenComparing(byName);
        return res.stream().sorted(order).collect(Collectors.toList());
    }

    private static double strength(Effect e) { return Math.abs(e.value().orElse(0.0)); }
}
