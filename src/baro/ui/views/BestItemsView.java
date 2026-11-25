package baro.ui.views;


import static baro.Tr.tr;
import baro.data.BestItems;
import baro.data.BestPicks;
import baro.data.GameCatalog;
import baro.model.Item;
import baro.model.ItemCategory;
import baro.model.MedCategory;
import baro.ui.widgets.Stack;
import baro.ui.widgets.Style;

import java.util.ArrayList;
import java.util.List;

/** Лучшие предметы по категориям (значок — звезда в зелёной рамке). */
public final class BestItemsView extends Stack {
    public BestItemsView(GameCatalog catalog, Blocks b, ItemCategory only) {
        super(4);
        padding(10, 12, 16, 12);
        with(b.text().badge().text(tr("Лучшие предметы"), Style.TITLE).newLine()
                .text(tr("Такой значок стоит у лучших предметов каждой категории. Выберите предмет слева или нажмите на название."), Style.MUTED));
        for (ItemCategory c : ItemCategory.values()) {
            if (only != null && c != only) continue;
            List<Item> list = new ArrayList<>();
            if (c == ItemCategory.MEDICAL)
                for (MedCategory mc : MedCategory.values()) for (String id : BestPicks.idsFor(mc)) catalog.item(id).ifPresent(list::add);
            for (String id : BestItems.idsFor(c)) catalog.item(id).ifPresent(list::add);
            if (list.isEmpty()) continue;
            Stack s = new Stack(3);
            for (Item i : list) s.with(b.text().badge().link(i).text(" — " + i.bestReason().orElse(""), Style.MUTED));
            with(b.section(c.title(), s));
        }
    }
}
