package baro.model;


import static baro.Tr.tr;
import java.util.Locale;

/**
 * Бонус к навыку, который талант даёт другим членам экипажа (данные игры: эффекты, накладываемые на союзников).
 * {@code stat}: WeaponsSkillBonus, MedicalSkillGainSpeed, SkillGainSpeed (все навыки), SkillShare (общий рост навыков).
 */
public final class AllyBuff {
    /** Навыки игры: тип характеристики → название. */
    public enum Skill {
        WEAPONS("Weapons", tr("Оружие")), MEDICAL("Medical", tr("Медицина")), ELECTRICAL("Electrical", tr("Электрика")),
        MECHANICAL("Mechanical", tr("Механика")), HELM("Helm", tr("Управление"));
        final String key; final String title;
        Skill(String key, String title) { this.key = key; this.title = title; }
        public String title() { return title; }
    }

    private final String stat, jobs, condition;
    private final double value;

    public AllyBuff(String stat, double value, String jobs, String condition) {
        this.stat = stat; this.value = value; this.jobs = jobs == null ? "" : jobs; this.condition = condition == null ? "" : condition;
    }

    public String stat() { return stat; }
    public double value() { return value; }
    /** Кому действует: пусто — всем; иначе id профессии (assistant, captain…). */
    public String jobs() { return jobs; }
    public String condition() { return condition; }

    public boolean appliesTo(Job job) {
        if (jobs.isEmpty()) return true;
        for (String j : jobs.split(",")) if (j.trim().equalsIgnoreCase(job.id())) return true;
        return false;
    }

    /** Навык, к которому относится бонус (null — ко всем навыкам). */
    public Skill skill() {
        for (Skill s : Skill.values()) if (stat.startsWith(s.key)) return s;
        return null;
    }

    public boolean isBonus() { return stat.endsWith("SkillBonus"); }
    public boolean isGainSpeed() { return stat.endsWith("SkillGainSpeed"); }
    public boolean isShare() { return stat.equals("SkillShare"); }

    /** «+10 к навыку Оружие», «навык Медицина растёт на 50% быстрее», «все навыки растут на 25% быстрее». */
    public String describe() {
        Skill s = skill();
        if (isShare()) return tr("получают очко навыка вместе с вами");
        if (isBonus()) return "+" + num(value) + tr(" к навыку «") + (s == null ? tr("все") : s.title()) + "»";
        String pct = num(value * 100) + "%";
        return s == null ? tr("все навыки растут на ") + pct + tr(" быстрее") : tr("навык «") + s.title() + tr("» растёт на ") + pct + tr(" быстрее");
    }

    private static String num(double v) {
        return Math.abs(v - Math.rint(v)) < 1e-9 ? String.valueOf((long) Math.rint(v)) : String.format(Locale.ROOT, "%.1f", v);
    }
}
