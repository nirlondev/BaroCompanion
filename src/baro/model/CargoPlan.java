package baro.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** План переработки груза одного ящика: что разобрать, докупить, скрафтить и продать. */
public final class CargoPlan {
    private final PlanKind kind;
    private final Map<Item, Integer> deconstruct, useDirectly, buy, craft, deconstructCrafted, sell;
    private final double seconds;
    private final List<Item> lockedRecipes;
    private int chanceValue;

    public CargoPlan(PlanKind kind, Map<Item, Integer> deconstruct, Map<Item, Integer> useDirectly, Map<Item, Integer> buy,
                     Map<Item, Integer> craft, Map<Item, Integer> deconstructCrafted, Map<Item, Integer> sell,
                     double seconds, List<Item> lockedRecipes) {
        this.kind = kind;
        this.deconstruct = Collections.unmodifiableMap(deconstruct);
        this.useDirectly = Collections.unmodifiableMap(useDirectly);
        this.buy = Collections.unmodifiableMap(buy);
        this.craft = Collections.unmodifiableMap(craft);
        this.deconstructCrafted = Collections.unmodifiableMap(deconstructCrafted);
        this.sell = Collections.unmodifiableMap(sell);
        this.seconds = seconds;
        this.lockedRecipes = Collections.unmodifiableList(lockedRecipes);
    }

    public PlanKind kind() { return kind; }
    /** Груз, который нужно разобрать. */
    public Map<Item, Integer> deconstruct() { return deconstruct; }
    /** Груз, который сразу идёт в крафт. */
    public Map<Item, Integer> useDirectly() { return useDirectly; }
    public Map<Item, Integer> buy() { return buy; }
    /** Сколько раз запустить каждый рецепт. */
    public Map<Item, Integer> craft() { return craft; }
    public Map<Item, Integer> deconstructCrafted() { return deconstructCrafted; }
    public Map<Item, Integer> sell() { return sell; }
    public double seconds() { return seconds; }
    public List<Item> lockedRecipes() { return lockedRecipes; }
    public boolean isEmpty() { return sell.isEmpty() && chanceValue == 0; }

    /** Средняя выручка за случайные выпадения при разборе (уже по текущим ценам). */
    public int chanceValue() { return chanceValue; }
    public CargoPlan withChanceValue(int v) { this.chanceValue = v; return this; }
}
