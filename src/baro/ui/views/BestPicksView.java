package baro.ui.views;


import static baro.Tr.tr;
import baro.data.BestPicks;
import baro.data.GameCatalog;
import baro.model.Item;
import baro.model.MedCategory;
import baro.model.Medicine;
import baro.ui.widgets.Stack;
import baro.ui.widgets.Style;

/** «Лучшие препараты» по разделам. */
public final class BestPicksView extends Stack {
    public BestPicksView(GameCatalog catalog, Blocks b, MedCategory only) {
        super(4);
        padding(10, 12, 16, 12);
        with(b.text().badge().text(tr("Лучшие препараты"), Style.TITLE).newLine().text(tr("Выберите препарат слева или нажмите на название ниже."), Style.MUTED));
        for (MedCategory c : MedCategory.values()) {
            if (only != null && c != only) continue;
            Stack list = new Stack(3);
            list.with(b.text().text(c.description(), Style.MUTED));
            for (String id : BestPicks.idsFor(c)) {
                Item item = catalog.item(id).orElse(null);
                if (item == null) continue;
                Medicine m = catalog.medicineOf(item).orElse(null);
                list.with(b.text().badge().link(item).text(" — " + (m == null ? "" : m.bestReason().orElse("")), Style.MUTED));
            }
            with(b.section(c.title(), list));
        }
    }
}
