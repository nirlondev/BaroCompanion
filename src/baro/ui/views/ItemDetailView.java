package baro.ui.views;


import static baro.Tr.tr;
import baro.model.Item;
import baro.model.Medicine;
import baro.ui.widgets.RichText;
import baro.ui.widgets.Stack;
import baro.ui.widgets.Style;

import java.util.Optional;

/** Полная карточка предмета. */
public final class ItemDetailView extends Stack {
    public ItemDetailView(Item item, Optional<Medicine> medicine, Blocks b) { this(item, medicine, b, java.util.Collections.emptyList()); }

    public ItemDetailView(Item item, Optional<Medicine> medicine, Blocks b, java.util.List<baro.model.CargoMission> missions) {
        super(4);
        padding(10, 12, 16, 12);
        with(b.title(item, item.category().title()));
        with(b.actions(item));
        item.bestReason().ifPresent(r -> with(b.text().badge().word(tr("Лучший выбор: "), Style.ACCENT).text(r, Style.ACCENT)));
        RichText d = b.description(item);
        if (d != null) with(d);
        Stack ws = b.weaponStats(item);
        if (ws != null) with(b.section(tr("Характеристики"), ws));
        with(b.section(tr("Цена — ") + b.prices().settings().store().title(), b.priceBlock(item)));
        with(b.section(tr("Крафт — из чего"), b.recipes(item, false)));
        if (baro.planner.CraftTree.recipe(item) != null) with(b.section(tr("Из чего с нуля — до сырья"), new CraftTreeSection(item, b)));
        with(b.section(tr("Разбор — во что"), b.decon(item, false)));
        medicine.ifPresent(m -> with(b.section(tr("Эффект препарата"), b.effects(m.onSuccess(), e -> false, false))));
        if (!item.usedIn().isEmpty())
            with(b.section(tr("Используется в крафте (") + item.usedIn().size() + ")", b.links(item.usedIn(), 80)));
        if (!missions.isEmpty()) {
            RichText t = b.text();
            with(b.section(tr("Груз миссий доставки"), t.join(missions, Style.MUTED, t::link)));
        }
        if (!item.obtainedFrom().isEmpty())
            with(b.section(tr("Получают разбором (") + item.obtainedFrom().size() + ")", b.links(item.obtainedFrom(), 80)));
    }
}
