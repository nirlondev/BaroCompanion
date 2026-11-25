package baro.model;

/** Требование навыка для крафта. */
public record SkillRequirement(Skill skill, int level) {
    @Override public String toString() { return skill.title() + " " + level; }
}
