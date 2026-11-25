package baro.ui.views;


import static baro.Tr.tr;
import baro.model.CargoMission;
import baro.model.Effect;
import baro.model.Skill;
import baro.model.Item;
import baro.model.Medicine;
import baro.ui.widgets.RichText;
import baro.ui.widgets.Stack;
import baro.ui.widgets.Style;

import java.util.function.Predicate;

/** Карточка препарата: эффекты в первую очередь. */
public final class MedicineDetailView extends Stack {
    public MedicineDetailView(Medicine m, Blocks b, Predicate<Effect> hit) { this(m, b, hit, java.util.Collections.emptyList()); }

    public MedicineDetailView(Medicine m, Blocks b, Predicate<Effect> hit, java.util.List<CargoMission> missions) {
        super(4);
        padding(10, 12, 16, 12);
        Item item = m.item();
        with(b.title(item, m.category().title()));
        with(b.actions(item));
        m.useSkill().ifPresent(s -> with(b.text().text(tr("Для применения нужен навык "), Style.MUTED).skill(Skill.MEDICAL, s, Skill.useNote(s))));
        m.bestReason().ifPresent(r -> with(b.text().badge().word(tr("Лучший выбор: "), Style.ACCENT).text(r, Style.ACCENT)));
        RichText d = b.description(item);
        if (d != null) with(d);
        with(b.section(tr("Эффект при успехе"), b.effects(m.onSuccess(), hit, false)));
        if (!m.onFailure().isEmpty()) with(b.section(tr("При провале (навык ниже нужного)"), b.effects(m.onFailure(), hit, false)));
        else with(b.section(tr("При провале (навык ниже нужного)"),
                b.text().text(m.useSkill().isPresent() ? tr("как при успехе") : tr("провала не бывает"), Style.MUTED)));
        with(b.section(tr("Крафт"), b.recipes(item, false)));
        if (baro.planner.CraftTree.recipe(item) != null) with(b.section(tr("Из чего с нуля — до сырья"), new CraftTreeSection(item, b)));
        RichText where = b.text();
        if (missions.isEmpty()) where.text(tr("Не встречается в грузе миссий доставки — только магазины, крафт и находки."), Style.MUTED);
        else {
            where.text(tr("Лежит в ящиках миссий доставки: "), Style.MUTED)
                    .join(missions, Style.MUTED, cm -> where.link(cm).text(" (" + cm.contents().get(item) + tr(" шт. в ящике)"), Style.MUTED));
        }
        with(b.section(tr("Где получить в миссиях"), where));
        with(b.section(tr("Цена — ") + b.prices().settings().store().title(), b.priceBlock(item)));
        with(b.section(tr("Разбор"), b.decon(item, false)));
        if (!item.usedIn().isEmpty()) with(b.section(tr("Используется в крафте"), b.links(item.usedIn(), 60)));
    }
}
