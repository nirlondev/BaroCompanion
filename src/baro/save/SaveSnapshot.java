package baro.save;


import static baro.Tr.tr;
import baro.model.Store;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

/** Что известно о кампании из сейва: настройки сложности, текущая станция, её магазины, репутация. */
public final class SaveSnapshot {
    /** Магазин станции: наценка, что станция требует (продажа ×2), скидки дня (покупка ×0,5), наличие. */
    public static final class StoreState {
        private final int priceModifier;
        private final Set<String> requested, dailySpecials;
        private final Map<String, Integer> stock;
        StoreState(int priceModifier, Set<String> requested, Set<String> dailySpecials, Map<String, Integer> stock) {
            this.priceModifier = priceModifier;
            this.requested = Collections.unmodifiableSet(requested);
            this.dailySpecials = Collections.unmodifiableSet(dailySpecials);
            this.stock = Collections.unmodifiableMap(stock);
        }
        public int priceModifier() { return priceModifier; }
        public Set<String> requested() { return requested; }
        public Set<String> dailySpecials() { return dailySpecials; }
        public Map<String, Integer> stock() { return stock; }
    }

    /**
     * Миссия из сейва.
     * @param selected взята (отмечена в терминале), иначе только предлагается на станции
     * @param destination куда вести (название станции) или пусто
     */
    public record Mission(String prefabId, boolean selected, String destination) { }

    private final Path file;
    private final long saveTime;
    private final String presetName;
    private final float shopPriceMultiplier, missionRewardMultiplier;
    private final String locationName, locationType, faction;
    private final Map<Store, StoreState> stores;
    private final Map<String, Double> reputations;
    private String biome = "";
    private double levelDifficulty;
    private List<Mission> missions = List.of();
    private String submarine = "";
    private Map<String, Integer> inventory = Map.of();

    SaveSnapshot(Path file, long saveTime, String presetName, float shopPriceMultiplier, float missionRewardMultiplier,
                 String locationName, String locationType, String faction, Map<Store, StoreState> stores, Map<String, Double> reputations) {
        this.file = file; this.saveTime = saveTime; this.presetName = presetName;
        this.shopPriceMultiplier = shopPriceMultiplier; this.missionRewardMultiplier = missionRewardMultiplier;
        this.locationName = locationName; this.locationType = locationType; this.faction = faction;
        this.stores = Collections.unmodifiableMap(stores);
        this.reputations = Collections.unmodifiableMap(reputations);
    }

    public Path file() { return file; }
    /** Время сохранения (секунды Unix) из самого сейва; 0 — неизвестно. */
    public long saveTime() { return saveTime; }
    /** Имя пресета сложности («Easy»…) или пусто, если настройки кампании свои. */
    public String presetName() { return presetName; }
    /** 0 — не указан. */
    public float shopPriceMultiplier() { return shopPriceMultiplier; }
    /** 0 — не указан. */
    public float missionRewardMultiplier() { return missionRewardMultiplier; }
    public String locationName() { return locationName; }
    /** Тип станции как в игре: Outpost, City, Research, Military, Mine… */
    public String locationType() { return locationType; }
    /** Фракция станции (coalition, separatists…) или пусто. */
    public String faction() { return faction; }
    /** Магазины текущей станции. */
    public Map<Store, StoreState> stores() { return stores; }
    /** Репутация у фракций (−100…+100). */
    public Map<String, Double> reputations() { return reputations; }

    /** Биом текущей станции (идентификатор, как в файлах игры: coldcaverns…) или пусто. */
    public String biome() { return biome; }
    /** Сложность уровня текущей станции (0–100), 0 — неизвестна. */
    public double levelDifficulty() { return levelDifficulty; }
    /** Взятые миссии и миссии, которые предлагает текущая станция. */
    public List<Mission> missions() { return missions; }

    /** Подлодка из сейва (имя файла .sub) или пусто. */
    public String submarine() { return submarine; }
    /** Запасы на борту: предметы в контейнерах подлодки и у экипажа (идентификатор → штук). */
    public Map<String, Integer> inventory() { return inventory; }
    /** Сколько штук предмета на борту. */
    public int onBoard(String itemId) { return inventory.getOrDefault(itemId, 0); }

    void setInventory(String submarine, Map<String, Integer> inventory) {
        this.submarine = submarine == null ? "" : submarine;
        this.inventory = Collections.unmodifiableMap(inventory);
    }

    void setWorld(String biome, double difficulty, List<Mission> missions) {
        this.biome = biome == null ? "" : biome;
        this.levelDifficulty = difficulty;
        this.missions = Collections.unmodifiableList(missions);
    }

    /** Репутация у фракции текущей станции. */
    public Optional<Double> stationReputation() { return faction == null || faction.isEmpty() ? Optional.empty() : Optional.ofNullable(reputations.get(faction)); }

    /** Основной магазин станции по её типу (аванпост → «Жилой аванпост» и т. д.). */
    public Optional<Store> mainStore() {
        if (locationType == null) return Optional.empty();
        Store s;
        switch (locationType.toLowerCase(Locale.ROOT)) {
            case "outpost": s = Store.OUTPOST; break;
            case "city": s = Store.CITY; break;
            case "research": s = Store.RESEARCH; break;
            case "military": s = Store.MILITARY; break;
            case "mine": s = Store.MINE; break;
            default: s = null;
        }
        return s != null && stores.containsKey(s) ? Optional.of(s) : Optional.empty();
    }

    /** «Аванпост Katreus Linea · сейв «1 10» от 15.10 14:35» */
    public String describe() {
        String name = file.getFileName().toString().replaceFirst("\\.save$", "");
        String when = saveTime > 0 ? tr(" от ") + new SimpleDateFormat("dd.MM HH:mm").format(new Date(saveTime * 1000)) : "";
        return (locationName == null || locationName.isEmpty() ? tr("станция неизвестна") : locationName) + tr(" · сейв «") + name + "»" + when;
    }
}