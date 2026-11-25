package baro.search;

import baro.model.Item;
import baro.pricing.StoreOffer;

import java.util.Optional;

/** Предмет, из которого при разборе выпадает искомый материал. */
public final class DeconSource {
    private final Item source;
    private final double expected;
    private final String yieldText;
    private final boolean chance;
    private final StoreOffer offer;
    private final int otherValue;
    private final double seconds;

    DeconSource(Item source, double expected, String yieldText, boolean chance, StoreOffer offer, int otherValue, double seconds) {
        this.source = source; this.expected = expected; this.yieldText = yieldText; this.chance = chance;
        this.offer = offer; this.otherValue = otherValue; this.seconds = seconds;
    }

    public Item source() { return source; }
    /** Сколько материала в среднем с одного разбора. */
    public double expectedCount() { return expected; }
    /** «3», «шанс 22%», «2 × 22%». */
    public String yieldText() { return yieldText; }
    public boolean isChance() { return chance; }
    public Optional<StoreOffer> cheapestOffer() { return Optional.ofNullable(offer); }
    /** Цена за единицу материала (в среднем) при покупке исходного предмета. */
    public Optional<Double> unitPrice() { return offer == null || expected <= 0 ? Optional.empty() : Optional.of(offer.price() / expected); }
    public int otherValue() { return otherValue; }
    public double seconds() { return seconds; }
}
