package baro.search;


import static baro.Tr.tr;
import baro.data.GameCatalog;
import baro.model.CargoMission;
import baro.model.Item;
import baro.model.Medicine;
import baro.pricing.PriceCalculator;

import java.util.*;

/**
 * Один поиск на всё для маленького окна: в режиме «Всё» — по названиям предметов,
 * в режиме «Мед» — по названиям препаратов, а затем по их эффектам («кровь», «скорость»…).
 */
public final class QuickSearch {
    private final GameCatalog catalog;
    private final ItemSearch items;
    private final MedicineSearch medicines;

    public QuickSearch(GameCatalog catalog, PriceCalculator prices) {
        this.catalog = catalog;
        this.items = new ItemSearch(catalog.items(), prices);
        this.medicines = new MedicineSearch(catalog.medicines());
    }

    public List<QuickHit> find(String query, boolean medicinesOnly, int limit) {
        List<QuickHit> res = new ArrayList<>();
        if (ItemSearch.normalize(query).isEmpty()) return res;
        if (!medicinesOnly) {
            String q = ItemSearch.normalize(query);
            List<QuickHit> missions = new ArrayList<>();
            for (CargoMission m : catalog.missions()) if (q.length() >= 3 && m.nameContains(q)) missions.add(new QuickHit(m));
            boolean missionsFirst = q.startsWith(tr("дост")) || q.startsWith(tr("груз")) || q.startsWith(tr("мисс")) || q.startsWith(tr("ящик"));
            if (missionsFirst) res.addAll(missions);
            for (Item i : items.find(query, null, SortMode.NAME, limit)) res.add(new QuickHit(i, catalog.medicineOf(i).orElse(null), null));
            if (!missionsFirst) res.addAll(missions);
            // таланты всех профессий
            if (q.length() >= 3) for (baro.model.Job j : catalog.jobs()) for (baro.model.Talent t : j.talents())
                if (ItemSearch.normalize(t.name()).contains(q)) res.add(new QuickHit(t));
            return res.size() > limit ? new ArrayList<>(res.subList(0, limit)) : res;
        }
        Set<Medicine> seen = new HashSet<>();
        List<MedicineMatch> byName = medicines.find(query, "", EffectMode.ANY, null);
        byName.sort(Comparator.comparing(m -> !m.medicine().item().nameStartsWith(ItemSearch.normalize(query))));
        for (MedicineMatch m : byName) { seen.add(m.medicine()); res.add(new QuickHit(m.medicine().item(), m.medicine(), null)); }
        for (MedicineMatch m : medicines.find("", query, EffectMode.ANY, null))
            if (seen.add(m.medicine())) res.add(new QuickHit(m.medicine().item(), m.medicine(), m.effect().orElse(null)));
        return res.size() > limit ? new ArrayList<>(res.subList(0, limit)) : res;
    }
}
