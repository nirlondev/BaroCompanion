package baro.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Ценовые данные предмета из файлов игры. */
public final class PriceInfo {
    private final int basePrice;
    private final boolean soldElsewhere;
    private final Map<Store, StorePrice> stores;

    public PriceInfo(int basePrice, boolean soldElsewhere, Map<Store, StorePrice> stores) {
        this.basePrice = basePrice;
        this.soldElsewhere = soldElsewhere;
        this.stores = Collections.unmodifiableMap(new EnumMap<>(stores));
    }

    public int basePrice() { return basePrice; }

    /** Цена в магазине до наценок (как PriceInfo.Price в игре) и продаётся ли предмет там. */
    public StorePrice forStore(Store store) {
        StorePrice p = stores.get(store);
        return p != null ? p : new StorePrice(1.0, soldElsewhere);
    }
}
