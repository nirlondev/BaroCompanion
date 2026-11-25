package baro.bestiary;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

/**
 * Миссия любого типа из Missions.xml (и модов): тексты, награда, репутация, где выдаётся,
 * какие монстры, люди и предметы в ней участвуют, особые условия.
 *
 * @param type тип миссии в игре: Monster, Salvage, Cargo, Escort, Pirate…
 * @param commonness частота выпадения (0 — только по событию/сюжету)
 * @param reward базовая награда (для доставки — за все ящики по максимуму)
 * @param facts особые условия уже на русском: «нужны руины», «только одиночная игра»…
 * @param briefings диалоги, в которых миссию выдают (события станций): реплики по порядку, ответы игрока начинаются с «— »
 */
public record MissionInfo(String id, String type, MissionKind kind, String name, String description, String success, String failure,
                          List<Message> messages, int reward, String rewardNote, int difficulty, int commonness, List<String> tags,
                          List<Reputation> reputation, List<String> locations, List<Count> monsters, List<Count> npcs,
                          List<Count> items, List<String> facts, String mod, List<List<String>> briefings) implements Serializable {

    /** Та же миссия с диалогами выдачи. */
    public MissionInfo withBriefings(List<List<String>> b) {
        return new MissionInfo(id, type, kind, name, description, success, failure, messages, reward, rewardNote, difficulty, commonness,
                tags, reputation, locations, monsters, npcs, items, facts, mod, b);
    }

    public record Message(String header, String text) implements Serializable { }

    /** Изменение репутации при успехе; opposing — у противоборствующей фракции. */
    public record Reputation(String faction, String factionName, double amount, double opposing) implements Serializable { }

    /** Участник миссии: вид существа, NPC или предмет, сколько и пояснение. */
    public record Count(String id, int min, int max, String note) implements Serializable { }

    public boolean nameContains(String normalizedQuery) {
        String k = (name + " " + id + " " + kind.title()).toLowerCase(Locale.ROOT).replace('ё', 'е');
        return k.contains(normalizedQuery);
    }

    @Override public String toString() { return name; }
}
