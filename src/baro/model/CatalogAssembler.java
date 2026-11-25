package baro.model;

import java.util.Collection;
import java.util.List;

/**
 * Связывает предметы при сборке каталога: прикрепляет рецепты и разбор,
 * вычисляет обратные связи («используется в», «получают разбором»).
 * Единственная точка, через которую меняются связи Item.
 */
public final class CatalogAssembler {
    private CatalogAssembler() {}

    public static void attach(Item item, List<Recipe> recipes, Deconstruction deconstruction) {
        item.attach(recipes, deconstruction);
        for (Recipe r : recipes) r.setOutput(item);
    }

    public static void computeRelations(Collection<Item> items) {
        for (Item product : items) {
            for (Recipe r : product.recipes()) {
                for (Ingredient ing : r.ingredients()) {
                    if (ing instanceof ItemIngredient) ((ItemIngredient) ing).item().addUsedIn(product);
                    else for (Item opt : ((TagIngredient) ing).options()) opt.addUsedIn(product);
                }
            }
            product.deconstruction().ifPresent(d -> {
                for (DeconOutput o : d.outputs()) o.item().addObtainedFrom(product);
            });
        }
    }

    /** Отметить предмет как один из лучших в категории. */
    public static void markBest(Item item, String reason) { item.markBest(reason); }

    public static void attachWeapon(Item item, WeaponStats stats) { item.attachWeapon(stats); }

    public static void linkTalent(Talent t) { for (Item i : t.recipes()) i.addUnlockedBy(t); }
}
