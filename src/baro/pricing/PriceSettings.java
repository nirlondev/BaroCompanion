package baro.pricing;

import baro.model.Store;
import baro.save.SaveSnapshot;
import baro.settings.AppSettings;

import java.util.*;

/**
 * Параметры расчёта цен: магазин, репутация, наценка каждого магазина станции, сложность кампании,
 * товары, которые требует станция (продажа ×2), и скидки дня (покупка ×0,5). Значения вводятся вручную
 * или берутся из сейва игры. Сообщает подписчикам об изменениях.
 */
public final class PriceSettings {
    public static final float ABYSS_MULTIPLIER = 2.0f;
    private final AppSettings storage;
    private final List<Runnable> listeners = new ArrayList<>();
    private Store store;
    private Reputation reputation;
    private final Map<Store, Integer> markups = new EnumMap<>(Store.class);
    private final Map<Store, Set<String>> requested = new EnumMap<>(Store.class), specials = new EnumMap<>(Store.class);
    private float buyMultiplier, rewardMultiplier;
    private Difficulty difficulty;
    private String stationInfo;
    private int batch;
    private boolean dirty;

    public PriceSettings(AppSettings storage) {
        this.storage = storage;
        this.store = Store.byGameId(storage.getString("price.store", Store.CITY.gameId()));
        this.reputation = new Reputation(storage.getInt("price.reputation", 0));
        int legacy = clampMarkup(storage.getInt("price.markup", 0)); // раньше наценка была одна на все магазины
        for (Store s : Store.values()) {
            int m = clampMarkup(storage.getInt("price.markup." + s.gameId(), s == store ? legacy : 0));
            if (m != 0) markups.put(s, m);
            Set<String> r = csv(storage.getString("price.requested." + s.gameId(), "")), d = csv(storage.getString("price.specials." + s.gameId(), ""));
            if (!r.isEmpty()) requested.put(s, r);
            if (!d.isEmpty()) specials.put(s, d);
        }
        this.buyMultiplier = positive(storage.getDouble("price.multiplier", ABYSS_MULTIPLIER), ABYSS_MULTIPLIER);
        this.rewardMultiplier = positive(storage.getDouble("price.reward", Difficulty.ABYSSAL.missionReward()), Difficulty.ABYSSAL.missionReward());
        Difficulty d;
        try { d = Difficulty.valueOf(storage.getString("price.difficulty", "")); } catch (IllegalArgumentException e) { d = Difficulty.match(buyMultiplier, rewardMultiplier); }
        this.difficulty = d;
        String info = storage.getString("price.station", "");
        this.stationInfo = info.isEmpty() ? null : info;
    }

    private static float positive(double v, float def) { return v > 0 && !Double.isInfinite(v) ? (float) v : def; }

    public Store store() { return store; }
    public Reputation reputation() { return reputation; }
    /** Случайная наценка выбранного магазина в процентах (−5…+5). */
    public int stationMarkup() { return markup(store); }
    /** Наценка конкретного магазина станции (у каждого своя). */
    public int markup(Store s) { return markups.getOrDefault(s, 0); }
    /** Множитель цен покупки (сложность кампании × скидки), на «Бездне» 2,0. */
    public float buyMultiplier() { return buyMultiplier; }
    /** Множитель наград за миссии (сложность кампании), на «Бездне» 0,8. */
    public float rewardMultiplier() { return rewardMultiplier; }
    public Difficulty difficulty() { return difficulty; }
    /** Станция требует этот предмет — продаётся вдвое дороже. */
    public boolean isRequested(Store s, String itemId) { return requested.getOrDefault(s, Set.of()).contains(itemId); }
    /** Скидка дня — покупается вдвое дешевле. */
    public boolean isDailySpecial(Store s, String itemId) { return specials.getOrDefault(s, Set.of()).contains(itemId); }
    public Set<String> requested(Store s) { return Collections.unmodifiableSet(requested.getOrDefault(s, Set.of())); }
    public Set<String> dailySpecials(Store s) { return Collections.unmodifiableSet(specials.getOrDefault(s, Set.of())); }
    /** Откуда взяты данные станции («Аванпост … — сейв …») или пусто, если всё введено вручную. */
    public Optional<String> stationInfo() { return Optional.ofNullable(stationInfo); }

    public void setStore(Store s) { if (s != store) { store = s; changed(); } }
    public void setReputation(int v) { if (v != reputation.value()) { reputation = new Reputation(v); changed(); } }
    public void setStationMarkup(int v) { setMarkup(store, v); }
    public void setMarkup(Store s, int v) {
        v = clampMarkup(v);
        if (v != markup(s)) { if (v == 0) markups.remove(s); else markups.put(s, v); changed(); }
    }
    /** Множитель покупки вручную: сложность становится пресетом с таким множителем или «своей». */
    public void setBuyMultiplier(float v) {
        if (v > 0 && !Float.isInfinite(v) && v != buyMultiplier) { buyMultiplier = v; difficulty = Difficulty.match(buyMultiplier, rewardMultiplier); changed(); }
    }
    /** Выбрать пресет сложности: множители покупки и наград берутся из него (CUSTOM их не меняет). */
    public void setDifficulty(Difficulty d) {
        if (d == null || d == difficulty) return;
        difficulty = d;
        if (d != Difficulty.CUSTOM) { buyMultiplier = d.shopPrice(); rewardMultiplier = d.missionReward(); }
        changed();
    }

    /** Забыть требуемые товары, скидки дня и подпись станции (например, перешли на другую станцию вручную). */
    public void clearStation() {
        if (requested.isEmpty() && specials.isEmpty() && stationInfo == null) return;
        requested.clear(); specials.clear(); stationInfo = null;
        changed();
    }

    /** Подставить всё, что известно из сейва: сложность, магазин станции, наценки, репутацию, требуемые товары. */
    public void apply(SaveSnapshot s) {
        batch++;
        try {
            if (s.shopPriceMultiplier() > 0) { buyMultiplier = s.shopPriceMultiplier(); dirty = true; }
            if (s.missionRewardMultiplier() > 0) { rewardMultiplier = s.missionRewardMultiplier(); dirty = true; }
            Difficulty preset = Difficulty.byPresetName(s.presetName());
            difficulty = preset != null && preset.shopPrice() == buyMultiplier && preset.missionReward() == rewardMultiplier ? preset : Difficulty.match(buyMultiplier, rewardMultiplier);
            if (!s.stores().isEmpty()) {
                markups.clear(); requested.clear(); specials.clear();
                for (Map.Entry<Store, SaveSnapshot.StoreState> e : s.stores().entrySet()) {
                    int m = clampMarkup(e.getValue().priceModifier());
                    if (m != 0) markups.put(e.getKey(), m);
                    if (!e.getValue().requested().isEmpty()) requested.put(e.getKey(), new LinkedHashSet<>(e.getValue().requested()));
                    if (!e.getValue().dailySpecials().isEmpty()) specials.put(e.getKey(), new LinkedHashSet<>(e.getValue().dailySpecials()));
                }
                Store main = s.mainStore().orElse(null);
                if (main != null) store = main;
                else if (!s.stores().containsKey(store)) store = s.stores().keySet().iterator().next();
            }
            s.stationReputation().ifPresent(r -> reputation = new Reputation((int) Math.round(r)));
            stationInfo = s.describe();
            dirty = true;
        } finally {
            batch--;
            if (batch == 0 && dirty) changed();
        }
    }

    /** Строка, однозначно описывающая все параметры цен (для кэша планов). */
    public String signature() {
        StringBuilder b = new StringBuilder(store.gameId()).append('|').append(reputation.value()).append('|').append(buyMultiplier).append('|').append(markups);
        b.append('|').append(requested).append('|').append(specials);
        return b.toString();
    }

    public void addListener(Runnable r) { listeners.add(r); }
    public void removeListener(Runnable r) { listeners.remove(r); }

    private static int clampMarkup(int v) { return Math.max(-5, Math.min(5, v)); }

    private static Set<String> csv(String s) {
        Set<String> r = new LinkedHashSet<>();
        for (String x : s.split(",")) if (!x.isBlank()) r.add(x.trim());
        return r;
    }

    private void changed() {
        if (batch > 0) { dirty = true; return; }
        dirty = false;
        storage.put("price.store", store.gameId());
        storage.put("price.reputation", reputation.value());
        storage.put("price.markup", stationMarkup());
        for (Store s : Store.values()) {
            storage.put("price.markup." + s.gameId(), markup(s));
            storage.put("price.requested." + s.gameId(), String.join(",", requested.getOrDefault(s, Set.of())));
            storage.put("price.specials." + s.gameId(), String.join(",", specials.getOrDefault(s, Set.of())));
        }
        storage.put("price.multiplier", buyMultiplier);
        storage.put("price.reward", rewardMultiplier);
        storage.put("price.difficulty", difficulty.name());
        storage.put("price.station", stationInfo == null ? "" : stationInfo);
        storage.save();
        for (Runnable r : new ArrayList<>(listeners)) r.run();
    }
}