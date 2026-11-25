package baro.model;

/** Ингредиент — конкретный предмет. */
public final class ItemIngredient extends Ingredient {
    private final Item item;

    public ItemIngredient(Item item, int count, Double minCondition, Double maxCondition, boolean partialUse) {
        super(count, minCondition, maxCondition, partialUse);
        this.item = item;
    }

    public Item item() { return item; }

    @Override public String displayName() { return item.name(); }
    @Override public boolean accepts(Item other) { return item == other; }
}
