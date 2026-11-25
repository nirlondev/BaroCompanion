package baro.model;

/** Что выпадает при разборе. */
public final class DeconOutput {
    private final Item item;
    private final int count;
    private final Double minCondition;
    private final boolean chance;
    private final boolean needsExtraItem;
    private final Double probability;

    public DeconOutput(Item item, int count, Double minCondition, boolean chance, boolean needsExtraItem) {
        this(item, count, minCondition, chance, needsExtraItem, null);
    }

    /** @param probability вероятность выпасть за одно случайное выпадение (0…1) или null */
    public DeconOutput(Item item, int count, Double minCondition, boolean chance, boolean needsExtraItem, Double probability) {
        this.probability = probability;
        this.item = item;
        this.count = count;
        this.minCondition = minCondition;
        this.chance = chance;
        this.needsExtraItem = needsExtraItem;
    }

    public Item item() { return item; }
    public int count() { return count; }
    public Double minCondition() { return minCondition; }
    public boolean isChance() { return chance; }
    public boolean needsExtraItem() { return needsExtraItem; }
    /** Вероятность за одно выпадение, если выход случайный. */
    public java.util.Optional<Double> probability() { return java.util.Optional.ofNullable(probability); }
}
