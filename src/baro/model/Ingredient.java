package baro.model;


import static baro.Tr.tr;
/** Ингредиент рецепта. Конкретный предмет или «любой из группы». */
public abstract class Ingredient {
    private final int count;
    private final Double minCondition;
    private final Double maxCondition;
    private final boolean partialUse;

    protected Ingredient(int count, Double minCondition, Double maxCondition, boolean partialUse) {
        this.count = count;
        this.minCondition = minCondition;
        this.maxCondition = maxCondition;
        this.partialUse = partialUse;
    }

    public int count() { return count; }
    public Double minCondition() { return minCondition; }
    public Double maxCondition() { return maxCondition; }
    public boolean isPartialUse() { return partialUse; }

    /** Название для показа. */
    public abstract String displayName();

    /** Проверить, подходит ли предмет. */
    public abstract boolean accepts(Item item);

    /** Пояснение про состояние предмета или пустая строка. */
    public String conditionNote() {
        StringBuilder b = new StringBuilder();
        if (minCondition != null) b.append(tr("состояние от ")).append(Math.round(minCondition * 100)).append("%");
        if (maxCondition != null) { if (b.length() > 0) b.append(", "); b.append(maxCondition <= 0.1 ? tr("пустой") : tr("состояние до ") + Math.round(maxCondition * 100) + "%"); }
        if (partialUse) { if (b.length() > 0) b.append(", "); b.append(tr("тратится частично")); }
        return b.toString();
    }
}
