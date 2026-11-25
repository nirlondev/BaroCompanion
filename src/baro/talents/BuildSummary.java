package baro.talents;


import static baro.Tr.tr;
import baro.model.*;

import java.util.*;

/** Что даёт выбранный набор талантов: описания, бонусы к навыкам, рецепты, сработавшие сочетания. */
public final class BuildSummary {
    private static final Map<String, String> SKILL_BONUS = Map.of(
            "WeaponsSkillBonus", tr("Оружие"), "MedicalSkillBonus", tr("Медицина"), "ElectricalSkillBonus", tr("Электрика"),
            "MechanicalSkillBonus", tr("Механика"), "HelmSkillBonus", tr("Управление"));

    private final TalentBuild build;
    private final SynergyFinder synergies;

    public BuildSummary(TalentBuild build, SynergyFinder synergies) { this.build = build; this.synergies = synergies; }

    public int points() { return build.chosen().size(); }

    /** Прибавка к навыкам (складывается из всех талантов). */
    public Map<String, Double> skillBonuses() {
        Map<String, Double> m = new LinkedHashMap<>();
        for (Talent t : build.chosen())
            for (TalentStat s : t.stats()) {
                String skill = SKILL_BONUS.get(s.type());
                if (skill != null) m.merge(skill, s.value(), Double::sum);
            }
        return m;
    }

    public List<Item> recipes() {
        List<Item> l = new ArrayList<>();
        for (Talent t : build.chosen()) for (Item i : t.recipes()) if (!l.contains(i)) l.add(i);
        return l;
    }

    /** Сочетания, где взяты оба таланта. */
    public List<Synergy> activeSynergies() {
        List<Synergy> l = new ArrayList<>();
        for (Synergy s : synergies.forJob(build.job())) if (build.isChosen(s.a()) && build.isChosen(s.b())) l.add(s);
        return l;
    }

    /** Лучшие сочетания, которые ещё можно добрать к текущему выбору. */
    public List<Synergy> suggestions(int limit) {
        List<Synergy> l = new ArrayList<>();
        for (Synergy s : synergies.forJob(build.job())) {
            boolean ca = build.isChosen(s.a()), cb = build.isChosen(s.b());
            if (ca == cb) continue;
            Talent missing = ca ? s.b() : s.a();
            if (build.state(missing) == TalentBuild.State.BLOCKED) continue;
            l.add(s);
            if (l.size() >= limit) break;
        }
        return l;
    }
}
