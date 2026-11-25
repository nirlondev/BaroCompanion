package baro.bestiary;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Существо (монстр, хаск, питомец, страж руин…) из файлов персонажей игры: здоровье, уязвимые места и броня,
 * скорость, все атаки, поведение ИИ, добыча с тела и где оно встречается.
 *
 * @param parent вид, от которого унаследован вариант («Hammerhead» у «Hammerhead_m»), или null
 * @param speeds скорости, м/с: walk, run, swimslow, swimfast
 * @param abilities особые свойства уже на русском: ауры, что выпускает при ранении или гибели
 */
public record Creature(String id, String name, CreatureKind kind, String parent, String group, String mod,
                       Health health, Ai ai, Map<String, Double> speeds, List<Attack> attacks, List<LimbArmor> armor,
                       List<Drop> drops, List<Spawn> spawns, List<String> abilities, double scale, boolean canEnterSub,
                       boolean needsAir, boolean humanoid, boolean boss) implements Serializable {

    /**
     * @param weakSpots множители урона по частям тела (голова ×1,5 — уязвимое место)
     * @param crushDepth глубина раздавливания, м (бесконечность — не давит)
     */
    public record Health(double vitality, boolean bleeds, double bleedingReduction, double burnReduction, double regen,
                         double regenWhenEating, double poisonVulnerability, double crushDepth, List<LimbMultiplier> weakSpots)
            implements Serializable { }

    public record LimbMultiplier(String limb, double multiplier, String types) implements Serializable { }

    /**
     * @param latch к чему цепляется: «стены», «корпус», «стены и корпус» или пусто
     * @param pet питомец (ест, растёт, привязывается к хозяину)
     */
    public record Ai(double combatStrength, double sight, double hearing, double fleeHealth, boolean aggressiveBoarding,
                     boolean avoidGunfire, String latch, boolean swarm, boolean pet, List<AiTarget> targets) implements Serializable { }

    /** Строка «на кого как реагирует»: тег цели, состояние (Attack, Avoid, Eat…), приоритет. */
    public record AiTarget(String tag, String state, double priority, boolean onlyOutside, boolean onlyInside) implements Serializable { }

    /**
     * Атака одной части тела.
     * @param context где применяется: Water, Ground, Any
     * @param target по чему бьёт: Character, Structure, Any
     * @param projectiles предметы-снаряды, которые выпускает атака (шипы, кислота…)
     */
    public record Attack(String limb, String context, String target, double cooldown, double range, double damageRange,
                         double structureDamage, double itemDamage, double levelWallDamage, double stun, double penetration,
                         double severChance, boolean ranged, List<Hit> hits, List<String> projectiles) implements Serializable {
        /** Прямой урон здоровью: недуги типа «урон», «ожог», «кровотечение» (с учётом вероятности). */
        public double healthDamage() {
            double d = 0;
            for (Hit h : hits) if (h.isDamage()) d += h.strength() * h.probability();
            return d;
        }
    }

    /**
     * Недуг, который накладывает атака.
     * @param type тип недуга из файлов игры: damage, bleeding, burn, stun, poison, paralysis…
     * @param viaEffect накладывается доп. эффектом атаки (яд, психоз…), а не самим ударом
     */
    public record Hit(String afflictionId, String name, String type, double strength, double probability, boolean viaEffect)
            implements Serializable {
        public boolean isDamage() {
            String t = type == null ? "" : type.toLowerCase(Locale.ROOT);
            return t.equals("damage") || t.equals("burn") || t.equals("bleeding");
        }
    }

    /** Броня части тела: что и насколько ослабляет, с какой стороны. */
    public record LimbArmor(String limb, List<ArmorMod> mods) implements Serializable { }

    /** @param what что ослабляется (названия типов урона/недугов); sector — сектор брони в градусах или пусто, если со всех сторон */
    public record ArmorMod(String what, double multiplier, String sector, boolean deflects) implements Serializable { }

    /**
     * Добыча с тела.
     * @param chance вероятность (0–1)
     * @param note откуда: «набор 1 из 2», «доп. шанс»…
     */
    public record Drop(String itemId, int count, double chance, String note) implements Serializable { }

    /**
     * Где встречается: по данным случайных событий игры.
     * @param biome идентификатор биома или пусто (любой/переходный уровень)
     * @param places тип места: mainpath, sidepath, cave, ruin, wreck, beaconstation, abyss…
     */
    public record Spawn(String biome, String biomeName, int minDifficulty, int maxDifficulty, List<String> places,
                        int minAmount, int maxAmount, int events) implements Serializable { }

    public double speed(String key) { Double v = speeds.get(key); return v == null ? 0 : v; }

    /** Самая высокая скорость (в воде или на суше). */
    public double topSpeed() {
        double m = 0;
        for (double v : speeds.values()) m = Math.max(m, v);
        return m;
    }

    /** Самый сильный удар по персонажу (урон здоровью за удар). */
    public double maxHit() {
        double m = 0;
        for (Attack a : attacks) if (!"Structure".equalsIgnoreCase(a.target())) m = Math.max(m, a.healthDamage());
        return m;
    }

    /** Самый сильный удар по корпусу. */
    public double maxStructureDamage() {
        double m = 0;
        for (Attack a : attacks) m = Math.max(m, a.structureDamage());
        return m;
    }

    public boolean nameContains(String normalizedQuery) {
        String k = (name + " " + id).toLowerCase(Locale.ROOT).replace('ё', 'е');
        return k.contains(normalizedQuery);
    }

    @Override public String toString() { return name; }
}
