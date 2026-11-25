package baro.app;

import baro.data.GameCatalog;
import baro.model.Item;
import baro.settings.AppSettings;

import java.util.*;

/**
 * Личные списки игрока: избранное, недавно открытые, список покупок (предмет → количество) и набор для сравнения.
 * Хранятся в настройках (fav.items, recent.items, shop.list, compare.items); подписчики узнают об изменениях.
 */
public final class UserLists {
    private static final int RECENT_MAX = 30, COMPARE_MAX = 4;
    private final GameCatalog catalog;
    private final AppSettings settings;
    private final LinkedHashSet<String> favorites, recent, compare;
    private final LinkedHashMap<String, Integer> shopping = new LinkedHashMap<>();
    private final List<Runnable> listeners = new ArrayList<>();

    public UserLists(GameCatalog catalog, AppSettings settings) {
        this.catalog = catalog;
        this.settings = settings;
        favorites = new LinkedHashSet<>(settings.getList("fav.items"));
        recent = new LinkedHashSet<>(settings.getList("recent.items"));
        compare = new LinkedHashSet<>(settings.getList("compare.items"));
        for (String e : settings.getList("shop.list")) {
            int i = e.lastIndexOf(':');
            try { if (i > 0) shopping.put(e.substring(0, i), Math.max(1, Integer.parseInt(e.substring(i + 1)))); }
            catch (NumberFormatException ignored) { }
        }
    }

    public void addListener(Runnable r) { listeners.add(r); }

    // ---------- избранное ----------

    public boolean isFavorite(Item item) { return favorites.contains(item.id()); }

    public void toggleFavorite(Item item) {
        if (!favorites.remove(item.id())) favorites.add(item.id());
        settings.putList("fav.items", favorites);
        changed();
    }

    public List<Item> favorites() { return items(favorites); }

    // ---------- недавние ----------

    /** Отметить, что карточку предмета открыли (самые свежие — первыми). */
    public void opened(Item item) {
        if (!recent.isEmpty() && recent.iterator().next().equals(item.id())) return;
        LinkedHashSet<String> next = new LinkedHashSet<>();
        next.add(item.id());
        for (String id : recent) { if (next.size() >= RECENT_MAX) break; next.add(id); }
        recent.clear();
        recent.addAll(next);
        settings.putList("recent.items", recent);
        settings.save();
        // список недавних не перерисовываем на каждый щелчок — только по требованию вкладки
    }

    public List<Item> recent() { return items(recent); }

    // ---------- список покупок ----------

    public Map<Item, Integer> shopping() {
        Map<Item, Integer> m = new LinkedHashMap<>();
        shopping.forEach((id, n) -> catalog.item(id).ifPresent(it -> m.put(it, n)));
        return m;
    }

    public int shoppingCount(Item item) { return shopping.getOrDefault(item.id(), 0); }

    /** Добавить к количеству в списке (n может быть отрицательным; 0 и меньше — убрать). */
    public void addToShopping(Item item, int n) { setShopping(item, shoppingCount(item) + n); }

    public void setShopping(Item item, int n) {
        if (n <= 0) shopping.remove(item.id()); else shopping.put(item.id(), Math.min(n, 999));
        saveShopping();
        changed();
    }

    public void clearShopping() { shopping.clear(); saveShopping(); changed(); }

    private void saveShopping() {
        List<String> l = new ArrayList<>();
        shopping.forEach((id, n) -> l.add(id + ":" + n));
        settings.putList("shop.list", l);
    }

    // ---------- сравнение ----------

    public boolean inCompare(Item item) { return compare.contains(item.id()); }

    /** Добавить к сравнению или убрать; больше COMPARE_MAX — самый старый уходит. */
    public void toggleCompare(Item item) {
        if (!compare.remove(item.id())) {
            compare.add(item.id());
            while (compare.size() > COMPARE_MAX) compare.remove(compare.iterator().next());
        }
        settings.putList("compare.items", compare);
        changed();
    }

    public void clearCompare() { compare.clear(); settings.putList("compare.items", compare); changed(); }

    public List<Item> compare() { return items(compare); }

    // ----------

    private List<Item> items(Collection<String> ids) {
        List<Item> l = new ArrayList<>();
        for (String id : ids) catalog.item(id).ifPresent(l::add);
        return l;
    }

    private void changed() {
        settings.save();
        for (Runnable r : new ArrayList<>(listeners)) r.run();
    }
}
