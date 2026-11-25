package baro.ui.views;


import static baro.Tr.tr;
import baro.data.AfflictionIndex;
import baro.data.GameCatalog;
import baro.model.Affliction;
import baro.model.Effect;
import baro.model.Skill;
import baro.pricing.Format;
import baro.ui.widgets.RichText;
import baro.ui.widgets.Stack;
import baro.ui.widgets.Style;

import java.util.List;
import java.util.Map;

/** Карточка недуга «от проблемы»: что это, чем лечить, что защищает, что вызывает. */
public final class AfflictionView extends Stack {
    public AfflictionView(Affliction a, GameCatalog catalog, Blocks b) {
        super(4);
        padding(10, 12, 16, 12);
        AfflictionIndex idx = catalog.afflictions();
        RichText title = b.text().text(a.name(), Style.TITLE).newLine();
        String kind = a.isBuff() ? tr("Бафф / состояние") : tr("Недуг");
        String type = Affliction.typeTitle(a.type());
        title.word((kind + (type.isEmpty() || type.equals(tr("бафф")) ? "" : " · " + type) + (a.isLimbSpecific() ? tr(" · на часть тела") : tr(" · на весь организм"))).toUpperCase(), Style.MUTED);
        with(title);
        if (!a.description().isEmpty()) with(b.text().text(a.description(), Style.MUTED));
        if (a.maxStrength() > 0 && !a.isBuff()) with(b.text().text(tr("Наибольшая сила: ") + Format.num(a.maxStrength()), Style.MUTED));

        if (!a.isBuff()) {
            List<AfflictionIndex.Use> cure = idx.treatments(a);
            Stack s = new Stack(2);
            if (cure.isEmpty()) s.with(b.text().text(tr("Препаратов, которые снимают этот недуг, не найдено — проходит сам или лечится иначе (покой, кислород, хирургия)."), Style.MUTED));
            for (AfflictionIndex.Use u : cure) s.with(use(b, u, true));
            with(b.section(tr("Чем лечить") + (cure.isEmpty() ? "" : " (" + cure.size() + ")"), s));

            List<AfflictionIndex.Use> prevent = idx.preventions(a);
            Map<Affliction, Double> prot = idx.protections(a);
            if (!prevent.isEmpty() || !prot.isEmpty()) {
                Stack p = new Stack(2);
                for (Map.Entry<Affliction, Double> e : prot.entrySet())
                    p.with(b.text().text(tr("Защита "), Style.MUTED).lore(e.getKey().name(), e.getKey(), null)
                            .text(tr(" — снижает на ") + Math.round(e.getValue() * 100) + "%", Style.GOOD));
                for (AfflictionIndex.Use u : prevent) p.with(use(b, u, false));
                with(b.section(tr("Что защищает (иммунитет, устойчивость)"), p));
            }
        } else {
            List<Affliction> from = idx.protectsFrom(a);
            if (!from.isEmpty()) {
                RichText r = b.text().text(tr("Защищает от: "), Style.MUTED);
                with(b.section(tr("Действие"), r.join(from, Style.MUTED, x -> r.lore(x.name(), x, null))));
            }
        }

        List<AfflictionIndex.Use> meds = idx.causedByMedicine(a);
        if (!meds.isEmpty()) {
            Stack s = new Stack(2);
            for (AfflictionIndex.Use u : meds) s.with(use(b, u, false));
            with(b.section(a.isBuff() ? tr("Какие препараты дают") : tr("Вызывают препараты (побочный эффект, яды)"), s));
        }
        List<AfflictionIndex.Hit> hits = idx.causedByWeapons(a);
        if (!hits.isEmpty()) {
            RichText r = b.text();
            int max = Math.min(hits.size(), 24);
            r.join(hits.subList(0, max), Style.MUTED, h -> r.link(h.item()).text(" " + (h.hit().isStun() ? Format.num(h.hit().strength()) + tr(" с") : Format.num(h.hit().strength())), Style.BAD));
            if (hits.size() > max) r.text(tr(" и ещё ") + (hits.size() - max), Style.MUTED);
            with(b.section(tr("Наносят оружие и боеприпасы (") + hits.size() + ")", r));
        }
        with(b.text().text(tr("Нажмите на препарат, чтобы открыть его карточку. Сила лечения — итог за всё время действия."), Style.MUTED));
    }

    private static RichText use(Blocks b, AfflictionIndex.Use u, boolean showSkill) {
        Effect e = u.effect();
        RichText r = b.text();
        double v = e.value().orElse(0.0);
        r.word(Format.signed(v) + "  ", v < 0 ? Style.GOOD : e.kind() == baro.model.EffectKind.BUFF ? Style.ACCENT : Style.BAD).link(u.medicine().item());
        StringBuilder m = new StringBuilder();
        e.duration().ifPresent(d -> m.append(tr(" · за ")).append(Format.seconds(d)));
        if (e.key().startsWith("TYPE:")) m.append(tr(" · весь тип недугов"));
        for (String c : e.conditions()) m.append(" · ").append(c);
        if (m.length() > 0) r.text(m.toString(), Style.MUTED);
        if (showSkill) u.medicine().useSkill().ifPresent(s -> r.text(" · ", Style.MUTED).skill(Skill.MEDICAL, s, Skill.useNote(s)));
        if (u.medicine().isBest()) r.text("  ", Style.MUTED).badge();
        return r;
    }
}
