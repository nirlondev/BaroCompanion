package baro.talents;


import static baro.Tr.tr;
import baro.model.AllyBuff;
import baro.model.AllyBuff.Skill;
import baro.model.Job;
import baro.model.Talent;

import java.util.*;

/**
 * Кто из других профессий сильнее всего поднимет навыки выбранной профессии своими талантами.
 * Бонусы берутся из файлов игры (эффекты, которые таланты накладывают на союзников). Ценность бонуса
 * взвешивается тем, насколько навык важен для профессии: основной навык — 1, второй — 0,5, остальные — 0,15.
 */
public final class CrewSynergy {
    /** Один талант другой профессии и что он даст. */
    public static final class Offer {
        private final Talent talent;
        private final List<AllyBuff> buffs;
        private final double score;
        Offer(Talent talent, List<AllyBuff> buffs, double score) { this.talent = talent; this.buffs = buffs; this.score = score; }
        public Talent talent() { return talent; }
        public List<AllyBuff> buffs() { return buffs; }
        public double score() { return score; }
    }

    /** Профессия-помощник и все её подходящие таланты. */
    public static final class Provider {
        private final Job job;
        private final List<Offer> offers;
        private final double score;
        Provider(Job job, List<Offer> offers) {
            this.job = job; this.offers = offers;
            double s = 0; for (Offer o : offers) s += o.score; this.score = s;
        }
        public Job job() { return job; }
        public List<Offer> offers() { return offers; }
        public double score() { return score; }
    }

    private static final Map<String, Map<Skill, Double>> WEIGHT = new HashMap<>();
    static {
        w("captain", Skill.HELM, Skill.WEAPONS);
        w("securityofficer", Skill.WEAPONS, Skill.MEDICAL);
        w("medicaldoctor", Skill.MEDICAL, null);
        w("mechanic", Skill.MECHANICAL, Skill.ELECTRICAL);
        w("engineer", Skill.ELECTRICAL, Skill.MECHANICAL);
    }

    private static void w(String job, Skill main, Skill second) {
        Map<Skill, Double> m = new EnumMap<>(Skill.class);
        for (Skill s : Skill.values()) m.put(s, 0.15);
        m.put(main, 1.0);
        if (second != null) m.put(second, 0.5);
        WEIGHT.put(job, m);
    }

    private CrewSynergy() {}

    /** Основные навыки профессии (для подписи). */
    public static List<Skill> mainSkills(Job job) {
        Map<Skill, Double> m = weights(job);
        List<Skill> l = new ArrayList<>();
        for (Map.Entry<Skill, Double> e : m.entrySet()) if (e.getValue() >= 0.5) l.add(e.getKey());
        l.sort(Comparator.comparing((Skill s) -> -m.get(s)));
        return l;
    }

    private static Map<Skill, Double> weights(Job job) {
        Map<Skill, Double> m = WEIGHT.get(job.id());
        if (m != null) return m;
        // помощник: все навыки одинаково полезны
        Map<Skill, Double> all = new EnumMap<>(Skill.class);
        for (Skill s : Skill.values()) all.put(s, 0.5);
        return all;
    }

    /** Насколько бонус полезен профессии. */
    public static double value(AllyBuff b, Job job) {
        Map<Skill, Double> w = weights(job);
        double weight = b.skill() != null ? w.get(b.skill()) : w.values().stream().mapToDouble(Double::doubleValue).sum();
        double v;
        if (b.isShare()) v = 4;                         // общий рост навыков
        else if (b.isGainSpeed()) v = b.value() * 20;   // +50% к скорости роста ≈ +10 навыка за кампанию
        else v = b.value();
        if (!b.isShare()) v *= weight;
        if (b.condition().contains(tr("на время"))) v *= 0.5;
        return v;
    }

    /** Другие профессии, отсортированные по пользе для {@code target}. */
    public static List<Provider> forJob(Job target, List<Job> jobs) {
        List<Provider> list = new ArrayList<>();
        for (Job j : jobs) {
            if (j == target) continue;
            List<Offer> offers = new ArrayList<>();
            for (Talent t : j.talents()) {
                List<AllyBuff> got = new ArrayList<>();
                double s = 0;
                for (AllyBuff b : t.allyBuffs()) if (b.appliesTo(target)) { got.add(b); s += value(b, target); }
                if (!got.isEmpty() && s > 0) offers.add(new Offer(t, got, s));
            }
            offers.sort(Comparator.comparing((Offer o) -> -o.score));
            list.add(new Provider(j, offers));
        }
        list.sort(Comparator.comparing((Provider p) -> -p.score));
        return list;
    }
}
