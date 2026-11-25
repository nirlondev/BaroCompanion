package baro.search;

import baro.model.Effect;

import java.util.*;

/**
 * Сопоставление запроса с названием эффекта. Если запрос не совпадает ни с одним названием напрямую,
 * используются бытовые синонимы («скорость» → «гиперактивность», «кровь» → «кровотечение» и т. п.).
 */
public final class EffectMatcher {
    private static final String[][] ALIASES = {
            {"скорост", "гиперактивность"}, {"быстр", "гиперактивность"},
            {"сил", "решительность"}, {"защит", "решительность"},
            {"кров", "кров"}, {"кислород", "кислород"}, {"удуш", "кислород"},
            {"хаск", "паразит"}, {"паразит", "паразит"},
            {"яд", "отравлен"}, {"отрав", "отравлен"},
            {"радиац", "лучев"}, {"оглуш", "оглуш"},
            {"боль", "травм"}, {"урон", "травм"}, {"лечени", "травм"},
            {"ожог", "ожог"}, {"давлен", "давлен"}, {"паралич", "паралич"},
            {"психоз", "психоз"}, {"зависим", "зависим"}, {"ломк", "ломк"},
            {"передоз", "передоз"}, {"инфекц", "заражен"},
    };

    private final List<String> needles;
    private final EffectMode mode;

    private EffectMatcher(List<String> needles, EffectMode mode) { this.needles = needles; this.mode = mode; }

    /** @param allEffectNames названия всех эффектов (для проверки прямого совпадения) */
    public static EffectMatcher of(String query, EffectMode mode, Collection<String> allEffectNames) {
        String q = ItemSearch.normalize(query);
        if (q.isEmpty()) return new EffectMatcher(Collections.emptyList(), mode);
        boolean direct = allEffectNames.stream().anyMatch(n -> ItemSearch.normalize(n).contains(q));
        List<String> needles = new ArrayList<>();
        needles.add(q);
        if (!direct) for (String[] a : ALIASES) if (q.startsWith(a[0]) || a[0].startsWith(q) && q.length() >= 3) needles.add(a[1]);
        return new EffectMatcher(needles, mode);
    }

    public boolean isEmpty() { return needles.isEmpty(); }

    public boolean matches(Effect e) {
        if (needles.isEmpty()) return false;
        String n = ItemSearch.normalize(e.name());
        boolean nameOk = needles.stream().anyMatch(n::contains);
        if (!nameOk) return false;
        double v = e.value().orElse(0.0);
        switch (mode) {
            case REMOVES: return v < 0;
            case GIVES: return v > 0;
            default: return true;
        }
    }
}
