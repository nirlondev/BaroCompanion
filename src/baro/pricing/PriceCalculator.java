package baro.pricing;

import baro.model.*;

import java.util.Optional;

/**
 * Цены как в игре (Location.GetAdjustedItemBuyPrice / GetAdjustedItemSellPrice).
 * Вычисления во float с отбрасыванием дробной части — чтобы совпадать с игрой до 1 mk.
 */
public final class PriceCalculator {
    private static final float SELL_SHARE = 0.3f;              // locationTypes.xml: sellpricemodifier
    static final float DAILY_SPECIAL = 0.5f;                   // locationTypes.xml: dailyspecialpricemodifier
    static final float REQUESTED_GOOD = 2f;                    // locationTypes.xml: requestgoodpricemodifier
    /** Награды миссий во встроенных данных посчитаны для «Бездны» (MissionRewardMultiplier 0,8). */
    static final float DATA_REWARD_MULTIPLIER = 0.8f;
    private final PriceSettings settings;

    public PriceCalculator(PriceSettings settings) { this.settings = settings; }

    public PriceSettings settings() { return settings; }

    /** Цена магазина до наценок: (int)(множитель × база). */
    static int storePrice(PriceInfo info, Store store) {
        return (int) ((float) info.forStore(store).multiplier() * info.basePrice());
    }

    static Optional<Integer> buyPrice(PriceInfo info, Store store, int markup, Reputation rep, float multiplier) {
        return buyPrice(info, store, markup, rep, multiplier, false);
    }

    static Optional<Integer> buyPrice(PriceInfo info, Store store, int markup, Reputation rep, float multiplier, boolean dailySpecial) {
        if (!info.forStore(store).isSold()) return Optional.empty();
        float price = 1f * storePrice(info, store);
        if (dailySpecial) price *= DAILY_SPECIAL;
        price = (100 + markup) / 100.0f * price;
        price *= rep.buyModifier();
        price *= multiplier;
        return Optional.of(Math.max(1, (int) price));
    }

    static int sellPrice(PriceInfo info, Store store, int markup, Reputation rep) { return sellPrice(info, store, markup, rep, false); }

    static int sellPrice(PriceInfo info, Store store, int markup, Reputation rep, boolean requested) {
        float price = SELL_SHARE * storePrice(info, store);
        if (requested) price *= REQUESTED_GOOD;
        price = (100 - markup) / 100.0f * price;
        price *= rep.sellModifier();
        return Math.max(1, (int) price);
    }

    public Optional<Integer> buyPrice(Item item, Store store) {
        return item.price().flatMap(p -> buyPrice(p, store, settings.markup(store), settings.reputation(), settings.buyMultiplier(),
                settings.isDailySpecial(store, item.id())));
    }

    public Optional<Integer> sellPrice(Item item, Store store) {
        return item.price().map(p -> sellPrice(p, store, settings.markup(store), settings.reputation(), settings.isRequested(store, item.id())));
    }

    public Optional<Integer> buyPrice(Item item) { return buyPrice(item, settings.store()); }
    public Optional<Integer> sellPrice(Item item) { return sellPrice(item, settings.store()); }

    /** Станция требует предмет — в выбранном магазине он продаётся вдвое дороже. */
    public boolean isRequested(Item item) { return settings.isRequested(settings.store(), item.id()); }
    /** Скидка дня в выбранном магазине. */
    public boolean isDailySpecial(Item item) { return settings.isDailySpecial(settings.store(), item.id()); }

    /** Самый дешёвый магазин, где предмет продаётся. */
    public Optional<StoreOffer> cheapestBuy(Item item) {
        StoreOffer best = null;
        for (Store s : Store.values()) {
            Optional<Integer> p = buyPrice(item, s);
            if (p.isPresent() && (best == null || p.get() < best.price())) best = new StoreOffer(s, p.get());
        }
        return Optional.ofNullable(best);
    }

    /** Сколько дадут за всё, что выпадает при разборе (в выбранном магазине). */
    public int sellValue(Deconstruction d) {
        double sum = 0;
        for (DeconOutput o : d.outputs()) sum += sellPrice(o.item()).orElse(0) * d.expectedCount(o);
        return (int) Math.round(sum);
    }

    /** Награда за ящик с учётом сложности кампании. */
    public int missionReward(CargoMission m) {
        return Math.round(m.rewardPerCrate() / DATA_REWARD_MULTIPLIER * settings.rewardMultiplier());
    }
}