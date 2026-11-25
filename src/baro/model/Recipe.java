package baro.model;

import java.util.Collections;
import java.util.List;

/** Рецепт фабрикатора. */
public final class Recipe {
    private final Fabricator fabricator;
    private final double seconds;
    private final int outputCount;
    private final boolean needsUnlock;
    private final int vendingPrice;
    private final List<SkillRequirement> skills;
    private final List<Ingredient> ingredients;
    private Item output;

    public Recipe(Fabricator fabricator, double seconds, int outputCount, boolean needsUnlock, int vendingPrice,
                  List<SkillRequirement> skills, List<Ingredient> ingredients) {
        this.fabricator = fabricator;
        this.seconds = seconds;
        this.outputCount = outputCount;
        this.needsUnlock = needsUnlock;
        this.vendingPrice = vendingPrice;
        this.skills = Collections.unmodifiableList(skills);
        this.ingredients = Collections.unmodifiableList(ingredients);
    }

    public Fabricator fabricator() { return fabricator; }
    public double seconds() { return seconds; }
    public int outputCount() { return outputCount; }
    public boolean needsUnlock() { return needsUnlock; }
    public int vendingPrice() { return vendingPrice; }
    public List<SkillRequirement> skills() { return skills; }
    public List<Ingredient> ingredients() { return ingredients; }
    public boolean isVending() { return fabricator == Fabricator.VENDING; }
    /** Что получается по рецепту. */
    public Item output() { return output; }
    void setOutput(Item item) { this.output = item; }

    public boolean uses(Item item) {
        for (Ingredient i : ingredients) if (i.accepts(item)) return true;
        return false;
    }
}
