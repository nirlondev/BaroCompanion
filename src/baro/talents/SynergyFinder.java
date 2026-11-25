package baro.talents;


import static baro.Tr.tr;
import baro.data.GameCatalog;
import baro.model.*;

import java.util.*;

/**
 * Ищет сочетания талантов одной профессии, которые усиливают друг друга:
 * общие характеристики из файлов игры, общие темы (бой, лечение, ремонт…) и цепочки рецептов
 * (один талант открывает предмет, который нужен для рецепта другого).
 * Учитываются только пары, которые можно взять вместе по правилам дерева.
 */
public final class SynergyFinder {
    private static final Set<String> GENERIC = Set.of("xp", "team", "craft");
    private final GameCatalog catalog;
    private final Map<Job, List<Synergy>> cache = new HashMap<>();

    public SynergyFinder(GameCatalog catalog) { this.catalog = catalog; }

    /** Все сочетания профессии, от самых сильных. */
    public List<Synergy> forJob(Job job) {
        return cache.computeIfAbsent(job, j -> {
            List<Talent> ts = j.talents();
            List<Synergy> res = new ArrayList<>();
            for (int i = 0; i < ts.size(); i++)
                for (int k = i + 1; k < ts.size(); k++) {
                    Synergy s = pair(ts.get(i), ts.get(k));
                    if (s != null) res.add(s);
                }
            res.sort(Comparator.comparingInt(Synergy::score).reversed());
            return res;
        });
    }

    /** Сочетания с конкретным талантом. */
    public List<Synergy> forTalent(Talent t) {
        List<Synergy> res = new ArrayList<>();
        for (Synergy s : forJob(t.job())) if (s.involves(t)) res.add(s);
        return res;
    }

    private Synergy pair(Talent a, Talent b) {
        if (!compatible(a, b)) return null;
        int score = 0;
        List<String> reasons = new ArrayList<>();
        // одинаковые характеристики
        Map<String, String> sa = new LinkedHashMap<>();
        for (TalentStat s : a.stats()) sa.put(s.type(), s.target());
        Set<String> sb = new HashSet<>();
        for (TalentStat s : b.stats()) sb.add(s.type());
        sa.keySet().retainAll(sb);
        if (!sa.isEmpty()) {
            score += 3 * sa.size();
            StringBuilder names = new StringBuilder();
            for (String lbl : sa.values()) if (!lbl.isEmpty() && names.indexOf(lbl) < 0) { if (names.length() > 0) names.append(", "); names.append(lbl.toLowerCase()); }
            reasons.add(names.length() > 0 ? tr("оба повышают «") + names + tr("» — бонусы складываются") : tr("усиливают одну и ту же характеристику — бонусы складываются"));
        }
        // общие темы
        List<String> common = new ArrayList<>();
        for (String th : a.themes()) if (b.themes().contains(th)) common.add(th);
        int specific = 0;
        for (String th : common) if (!GENERIC.contains(th)) specific++;
        score += 2 * specific + (common.size() - specific);
        if (specific > 0) {
            StringBuilder s = new StringBuilder(tr("оба про: "));
            boolean first = true;
            for (String th : common) if (!GENERIC.contains(th)) { if (!first) s.append(", "); first = false; s.append(catalog.themeTitle(th).toLowerCase()); }
            reasons.add(s.toString());
        }
        // цепочки рецептов
        String chain = chain(a, b);
        if (chain == null) chain = chain(b, a);
        if (chain != null) { score += 4; reasons.add(chain); }
        // талант-усилитель + открываемые им предметы той же темы
        if (!a.recipes().isEmpty() && b.recipes().isEmpty() && boosts(b, a)) { score += 2; reasons.add("«" + b.name() + tr("» усиливает то, что открывает «") + a.name() + "»"); }
        if (!b.recipes().isEmpty() && a.recipes().isEmpty() && boosts(a, b)) { score += 2; reasons.add("«" + a.name() + tr("» усиливает то, что открывает «") + b.name() + "»"); }
        if (score < 4 || reasons.isEmpty()) return null;
        return new Synergy(a, b, score, reasons);
    }

    /** Предмет из рецептов «from» нужен как ингредиент в рецептах «to». */
    private static String chain(Talent from, Talent to) {
        for (Item produced : from.recipes())
            for (Item target : to.recipes())
                for (Recipe r : target.recipes())
                    if (r.uses(produced)) return "«" + from.name() + tr("» открывает «") + produced.name() + tr("» — он нужен для «") + target.name() + tr("» из «") + to.name() + "»";
        return null;
    }

    private static boolean boosts(Talent booster, Talent unlocker) {
        for (String th : booster.themes()) if (!GENERIC.contains(th) && unlocker.themes().contains(th) && !booster.stats().isEmpty()) return true;
        return false;
    }

    /** Можно ли взять оба таланта по правилам дерева. */
    static boolean compatible(Talent a, Talent b) {
        if (a.group() == b.group() && a.group().max() < 2) return false;
        TalentSubtree x = a.subtree(), y = b.subtree();
        if (x != y && (x.blocks().contains(y.id()) || y.blocks().contains(x.id()))) return false;
        return true;
    }
}
