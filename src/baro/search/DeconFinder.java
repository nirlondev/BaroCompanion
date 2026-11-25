package baro.search;


import static baro.Tr.tr;
import baro.model.DeconOutput;
import baro.model.Deconstruction;
import baro.model.Item;
import baro.pricing.Format;
import baro.pricing.PriceCalculator;
import baro.pricing.StoreOffer;

import java.util.*;

/** «Где добыть разбором»: из чего выпадает материал, отсортировано от самого дешёвого за единицу. */
public final class DeconFinder {
    private final PriceCalculator prices;

    public DeconFinder(PriceCalculator prices) { this.prices = prices; }

    public List<DeconSource> sourcesOf(Item material) {
        List<DeconSource> res = new ArrayList<>();
        for (Item src : material.obtainedFrom()) {
            Optional<Deconstruction> od = src.deconstruction();
            if (!od.isPresent()) continue;
            Deconstruction d = od.get();
            double expected = 0, other = 0;
            String text = null;
            boolean chance = false;
            for (DeconOutput o : d.outputs()) {
                double e = d.expectedCount(o);
                if (o.item() == material) {
                    expected += e;
                    if (o.probability().isPresent()) {
                        chance = true;
                        String p = Format.chance(o.probability().get());
                        text = d.picks() > 1 ? d.picks() + " × " + p : tr("шанс ") + p;
                    } else {
                        chance |= o.isChance();
                        text = o.count() + (o.isChance() ? tr(" (не всегда)") : "");
                    }
                } else other += prices.sellPrice(o.item()).orElse(0) * e;
            }
            if (expected <= 0) continue;
            StoreOffer offer = prices.cheapestBuy(src).orElse(null);
            res.add(new DeconSource(src, expected, text, chance, offer, (int) Math.round(other), d.seconds()));
        }
        res.sort(Comparator.comparingDouble((DeconSource s) -> s.unitPrice().orElse(Double.MAX_VALUE))
                .thenComparing(s -> s.source().displayName()));
        return res;
    }
}
