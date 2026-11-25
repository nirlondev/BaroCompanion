package baro.talents;


import static baro.Tr.tr;
import baro.model.*;

import java.util.*;

/**
 * Выбранные таланты одной профессии и правила дерева:
 * ряд открывается, когда в предыдущем ряду взято нужное число талантов; в ряду нельзя взять больше максимума;
 * специализация открывается после «Основы»; пока одна специализация начата, но не пройдена до конца,
 * остальные закрыты — после полной прокачки специализации можно начать следующую (как в коде игры,
 * TalentTree.TalentTreeMeetsRequirements: blocking tree HasAnyTalent && !HasMaxTalents).
 */
public final class TalentBuild {
    /** Состояние таланта в дереве. */
    public enum State { CHOSEN, AVAILABLE, LOCKED, BLOCKED, FULL }

    private final Job job;
    private final Set<Talent> chosen = new LinkedHashSet<>();

    public TalentBuild(Job job) { this.job = job; }

    public Job job() { return job; }
    public Set<Talent> chosen() { return Collections.unmodifiableSet(chosen); }
    public boolean isChosen(Talent t) { return chosen.contains(t); }

    public State state(Talent t) {
        if (chosen.contains(t)) return State.CHOSEN;
        TalentSubtree s = t.subtree();
        if (isBlocked(s)) return State.BLOCKED;
        if (!subtreeOpen(s) || !groupOpen(s, t.group())) return State.LOCKED;
        if (count(t.group()) >= t.group().max()) return State.FULL;
        return State.AVAILABLE;
    }

    /** Почему талант нельзя взять (для подсказки). */
    public String reason(Talent t) {
        switch (state(t)) {
            case BLOCKED: return tr("Закрыто: сначала до конца пройдите специализацию «") + blockerName(t.subtree())
                    + tr("». После полной прокачки специализации можно начать следующую.");
            case LOCKED:
                if (!subtreeOpen(t.subtree())) return tr("Сначала пройдите ветку «") + nameOf(t.subtree().requires()) + "».";
                return tr("Сначала возьмите таланты в предыдущем ряду этой ветки.");
            case FULL: return tr("В этом ряду уже выбрано максимальное число талантов — уберите другой, чтобы взять этот.");
            default: return null;
        }
    }

    public boolean toggle(Talent t) {
        if (chosen.contains(t)) {
            chosen.remove(t);
            // специализация снова не пройдена — начатые после неё несовместимые ветки становятся невозможны
            TalentSubtree x = t.subtree();
            if (hasAny(x) && !full(x))
                for (TalentSubtree y : job.subtrees())
                    if (y != x && conflicts(x, y) && hasAny(y) && !full(y)) chosen.removeIf(c -> c.subtree() == y);
            prune();
            return true;
        }
        if (state(t) != State.AVAILABLE) return false;
        chosen.add(t);
        return true;
    }

    public void clear() { chosen.clear(); }

    public void setAll(Collection<Talent> list) {
        chosen.clear();
        // добавляем по порядку дерева, чтобы правила соблюдались
        boolean added = true;
        while (added) {
            added = false;
            for (Talent t : list) if (!chosen.contains(t) && state(t) == State.AVAILABLE) { chosen.add(t); added = true; }
        }
    }

    // ---------- правила ----------

    private int count(TalentGroup g) { int n = 0; for (Talent t : g.talents()) if (chosen.contains(t)) n++; return n; }

    private boolean groupOpen(TalentSubtree s, TalentGroup g) {
        for (TalentGroup prev : s.groups()) {
            if (prev == g) return true;
            if (count(prev) < Math.min(prev.required(), prev.talents().size())) return false;
        }
        return true;
    }

    /** Ветка пройдена: в каждом ряду взято нужное число талантов. */
    public boolean complete(TalentSubtree s) {
        for (TalentGroup g : s.groups()) if (count(g) < Math.min(g.required(), g.talents().size())) return false;
        return true;
    }

    private boolean subtreeOpen(TalentSubtree s) {
        if (s.requires() == null || s.requires().isEmpty()) return true;
        TalentSubtree req = job.subtree(s.requires());
        return req == null || complete(req);
    }

    public boolean hasAny(TalentSubtree s) {
        for (TalentGroup g : s.groups()) if (count(g) > 0) return true;
        return false;
    }

    /** В каждом ряду ветки взят максимум талантов — ветка прокачана полностью. */
    public boolean full(TalentSubtree s) {
        for (TalentGroup g : s.groups()) if (count(g) < Math.min(g.max(), g.talents().size())) return false;
        return true;
    }

    private static boolean conflicts(TalentSubtree a, TalentSubtree b) { return a.blocks().contains(b.id()) || b.blocks().contains(a.id()); }

    private TalentSubtree blocker(TalentSubtree s) {
        for (TalentSubtree other : job.subtrees())
            if (other != s && conflicts(other, s) && hasAny(other) && !full(other)) return other;
        return null;
    }

    /** Ветка закрыта, пока другая несовместимая специализация начата, но не пройдена до конца. */
    public boolean isBlocked(TalentSubtree s) { return blocker(s) != null; }

    private String blockerName(TalentSubtree s) { TalentSubtree b = blocker(s); return b == null ? "" : b.name(); }

    private String nameOf(String subtreeId) { TalentSubtree s = job.subtree(subtreeId); return s == null ? subtreeId : s.name(); }

    /** После снятия таланта убрать те, что стали недоступны. */
    private void prune() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Talent t : new ArrayList<>(chosen)) {
                chosen.remove(t);
                boolean ok = state(t) == State.AVAILABLE;
                if (ok) chosen.add(t); else changed = true;
            }
        }
    }
}
