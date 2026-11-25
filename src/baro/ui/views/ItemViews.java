package baro.ui.views;

import baro.data.GameCatalog;
import baro.model.Item;
import baro.model.Medicine;

import java.awt.Component;

/** Какую карточку показать для предмета: препарат — карточка препарата, иначе обычная. */
public final class ItemViews {
    private ItemViews() {}

    public static Component forItem(Item item, GameCatalog catalog, Blocks b) {
        Medicine m = catalog.medicineOf(item).orElse(null);
        if (m != null) return new MedicineDetailView(m, b, e -> false, catalog.missionsWith(item));
        return new ItemDetailView(item, java.util.Optional.empty(), b, catalog.missionsWith(item));
    }
}
