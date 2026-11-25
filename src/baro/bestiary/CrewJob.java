package baro.bestiary;

import java.io.Serializable;
import java.util.List;

/**
 * Профессия экипажа — то, чем будут заняты боты-союзники: навыки при найме, стартовое снаряжение,
 * чем занимаются сами без приказа.
 *
 * @param autonomous приказы, которые бот выполняет сам (с множителем приоритета)
 * @param orders приказы, для которых эта профессия считается подходящей
 * @param crew профессия экипажа (false — служебная профессия NPC: охрана станции, управляющий, бандит…)
 */
public record CrewJob(String id, String name, String description, List<SkillRange> skills, List<Npc.GearSet> gear,
                      List<String> autonomous, List<String> orders, double priceMultiplier, double vitalityModifier,
                      int maxNumber, String idleBehavior, boolean crew) implements Serializable {

    public record SkillRange(String id, String name, int min, int max, boolean primary) implements Serializable { }

    @Override public String toString() { return name; }
}
