package baro.pricing;

import baro.model.*;

import java.util.*;

/** Оценка груза миссий доставки по текущим ценам: что выпадает при разборе и сколько дают планы. */
public final class CargoValuation {
    /** Итог разбора всего содержимого ящика. */
    public static final class DeconResult {
        private final Map<Item, Integer> outputs = new LinkedHashMap<>();
        private final Set<Item> chance = new HashSet<>();
        private final List<Item> notDeconstructible = new ArrayList<>();
        private double seconds;
        private int sellValue;

        public Map<Item, Integer> outputs() { return outputs; }
        /** Выпадает не всегда (шанс или одно случайное). */
        public boolean isChance(Item item) { return chance.contains(item); }
        public List<Item> notDeconstructible() { return notDeconstructible; }
        /** Время разбора всего ящика на одном разборщике. */
        public double seconds() { return seconds; }
        public int sellValue() { return sellValue; }
    }

    private final PriceCalculator prices;

    public CargoValuation(PriceCalculator prices) { this.prices = prices; }

    public PriceCalculator prices() { return prices; }

    /** Награда за доставку одного ящика при текущей сложности. */
    public int reward(CargoMission m) { return prices.missionReward(m); }

    public DeconResult deconstruct(CargoMission mission) {
        DeconResult r = new DeconResult();
        for (Map.Entry<Item, Integer> e : mission.contents().entrySet()) {
            Optional<Deconstruction> d = e.getKey().deconstruction();
            if (!d.isPresent() || d.get().outputs().isEmpty()) { r.notDeconstructible.add(e.getKey()); continue; }
            r.seconds += d.get().seconds() * e.getValue();
            for (DeconOutput o : d.get().outputs()) {
                r.outputs.merge(o.item(), o.count() * e.getValue(), Integer::sum);
                if (o.isChance() || d.get().isRandomSingle()) r.chance.add(o.item());
            }
        }
        for (Map.Entry<Item, Integer> e : r.outputs.entrySet())
            if (!r.chance.contains(e.getKey())) r.sellValue += prices.sellPrice(e.getKey()).orElse(0) * e.getValue();
        return r;
    }

    public int revenue(CargoPlan p) { return sum(p.sell(), i -> prices.sellPrice(i).orElse(0)); }

    public int purchaseCost(CargoPlan p) {
        return sum(p.buy(), i -> prices.cheapestBuy(i).map(StoreOffer::price).orElse(0));
    }

    /** Чистая выручка плана: продажа − покупки + средние случайные выпадения. */
    public int value(CargoPlan p) { return revenue(p) - purchaseCost(p) + p.chanceValue(); }

    private static int sum(Map<Item, Integer> m, java.util.function.ToIntFunction<Item> unit) {
        int s = 0;
        for (Map.Entry<Item, Integer> e : m.entrySet()) s += unit.applyAsInt(e.getKey()) * e.getValue();
        return s;
    }
}
