package baro.model;

import java.util.Collections;
import java.util.List;

/** Ингредиент — любой предмет из группы (например, «любой провод»). */
public final class TagIngredient extends Ingredient {
    private final String label;
    private final List<Item> options;

    public TagIngredient(String label, List<Item> options, int count, Double minCondition, Double maxCondition, boolean partialUse) {
        super(count, minCondition, maxCondition, partialUse);
        this.label = label;
        this.options = Collections.unmodifiableList(options);
    }

    public List<Item> options() { return options; }

    @Override public String displayName() { return label; }
    @Override public boolean accepts(Item item) { return options.contains(item); }
}
