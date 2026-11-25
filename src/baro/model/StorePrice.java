package baro.model;

/** Цена предмета в конкретном магазине: множитель к базовой цене и продаётся ли он там. */
public final class StorePrice {
    private final double multiplier;
    private final boolean sold;

    public StorePrice(double multiplier, boolean sold) { this.multiplier = multiplier; this.sold = sold; }

    public double multiplier() { return multiplier; }
    public boolean isSold() { return sold; }
}
