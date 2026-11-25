package baro.model;


import static baro.Tr.tr;
import java.util.*;

/** Грузовая миссия (доставка): ящики, их содержимое и варианты переработки груза. */
public final class CargoMission {
    private final String id, name, crateName;
    private final int minCrates, maxCrates, rewardPerCrate;
    private final Map<Item, Integer> contents;
    private final List<CargoPlan> plans;
    private final String searchKey;

    public CargoMission(String id, String name, String crateName, int minCrates, int maxCrates, int rewardPerCrate,
                        Map<Item, Integer> contents, List<CargoPlan> plans) {
        this.id = id; this.name = name; this.crateName = crateName;
        this.minCrates = minCrates; this.maxCrates = maxCrates; this.rewardPerCrate = rewardPerCrate;
        this.contents = Collections.unmodifiableMap(contents);
        this.plans = Collections.unmodifiableList(plans);
        this.searchKey = (name + tr(" доставка груз миссия ") + crateName).toLowerCase(Locale.ROOT).replace('ё', 'е');
    }

    public String id() { return id; }
    public String name() { return name; }
    public String crateName() { return crateName; }
    public int minCrates() { return minCrates; }
    public int maxCrates() { return maxCrates; }
    /** Награда за доставленный ящик на «Бездне» (ориентир); с учётом сложности — PriceCalculator.missionReward. */
    public int rewardPerCrate() { return rewardPerCrate; }
    /** Содержимое одного ящика. */
    public Map<Item, Integer> contents() { return contents; }

    /** Предметов в одном ящике вместе с самим ящиком. */
    public int itemsPerCrate() { int n = 1; for (int v : contents.values()) n += v; return n; }

    /** Сколько предметов можно не довезти (миссия засчитывается при 98%), при максимуме ящиков. */
    public int allowedLoss() { return (int) Math.floor(itemsPerCrate() * maxCrates * 0.02 + 1e-9); }

    public boolean contains(Item item) { return contents.containsKey(item); }
    /** Все слова запроса встречаются в названии, типе ящика или словах «доставка/груз/миссия». */
    public boolean nameContains(String lowerQuery) {
        for (String w : lowerQuery.trim().split("\\s+")) if (!w.isEmpty() && !searchKey.contains(w)) return false;
        return true;
    }

    @Override public String toString() { return name; }
}
