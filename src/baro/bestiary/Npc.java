package baro.bestiary;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

/**
 * Человек-NPC из наборов игры (NPCSets): враги боевых миссий, бандиты, торговцы и охрана станций, наёмники,
 * пассажиры, заложники, персонажи событий. Снаряжение — возможные наборы с их относительной частотой.
 *
 * @param set набор, где он описан (combatmissionseparatistnpcs, outpostnpcs1…)
 * @param behavior поведение ИИ: Patrol, Passive, StayInHull…
 * @param interaction что с ним можно сделать на станции: Store, Hire, Upgrade… (или пусто)
 * @param salary базовая плата за найм (0 — не нанимается)
 */
public record Npc(String id, String name, NpcKind kind, String set, String job, String jobName, String faction, String factionName,
                  String behavior, double healthMultiplier, double aimAccuracy, double aimSpeed, double skillMultiplier,
                  int salary, int experience, String interaction, List<String> tags, List<GearSet> gear, String mod)
        implements Serializable {

    /** Набор снаряжения; commonness — относительная частота среди наборов этого NPC. */
    public record GearSet(double commonness, List<Gear> items) implements Serializable { }

    /**
     * Предмет снаряжения.
     * @param infinite боеприпасы не кончаются (у NPC)
     * @param inside что лежит внутри (патроны в оружии, баллон в маске)
     */
    public record Gear(String itemId, int amount, boolean equipped, boolean infinite, List<Gear> inside) implements Serializable { }

    public boolean nameContains(String normalizedQuery) {
        String k = (name + " " + id + " " + jobName).toLowerCase(Locale.ROOT).replace('ё', 'е');
        return k.contains(normalizedQuery);
    }

    @Override public String toString() { return name; }
}
