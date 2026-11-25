package baro.ui.views;


import static baro.Tr.tr;
import baro.bestiary.Bestiary;
import baro.bestiary.Creature;
import baro.bestiary.MissionInfo;
import baro.pricing.Format;
import baro.save.SaveSnapshot;
import baro.ui.widgets.RichText;
import baro.ui.widgets.Stack;
import baro.ui.widgets.Style;

import java.util.*;

/** Что известно из сейва о текущем месте: взятые и предлагаемые миссии, монстры биома. */
public final class SaveViews {
    private SaveViews() {}

    /** Миссии из сейва: взятые — с участниками (монстры, NPC), затем предлагаемые станцией. */
    public static Stack missions(SaveSnapshot s, Bestiary best, Blocks b, boolean compact) {
        Stack st = new Stack(compact ? 1 : 3);
        if (s.missions().isEmpty()) {
            st.with(b.text().text(tr("В сейве нет взятых миссий, и станция миссий не предлагает."), Style.MUTED));
            return st;
        }
        float mul = b.prices().settings().rewardMultiplier();
        for (SaveSnapshot.Mission m : s.missions()) {
            MissionInfo mi = best == null ? null : find(best, m.prefabId());
            RichText r = b.text();
            r.word(m.selected() ? tr("Взята: ") : "• ", m.selected() ? Style.GOOD : Style.MUTED);
            if (mi != null) r.lore(mi.name(), mi, mi.kind().title()); else r.text(m.prefabId(), Style.NORMAL);
            if (!m.destination().isEmpty()) r.text(" → " + m.destination(), Style.MUTED);
            if (mi != null && mi.reward() > 0) r.text(" · ", Style.MUTED).word(Format.money(Math.round(mi.reward() * mul)), Style.SELL);
            if (!m.selected()) r.text(tr(" (предлагает станция)"), Style.MUTED);
            st.with(r);
            if (mi != null && m.selected() && !mi.monsters().isEmpty()) {
                RichText who = b.text().text(tr("    кто там: "), Style.MUTED);
                who.join(mi.monsters(), Style.MUTED, c -> {
                    Optional<Creature> cr = best.creature(c.id());
                    if (cr.isPresent()) who.lore(cr.get().name(), cr.get(), "♥ " + Format.num(cr.get().health().vitality()) + tr(" · удар ") + Format.num(cr.get().maxHit()));
                    else who.text(c.id(), Style.NORMAL);
                    if (c.max() > 1) who.text(" ×" + (c.min() == c.max() ? c.max() : c.min() + "–" + c.max()), Style.MUTED);
                });
                st.with(who);
            }
            if (mi != null && m.selected() && !mi.items().isEmpty() && b.context() != null) {
                RichText what = b.text().text(tr("    предметы: "), Style.MUTED);
                what.join(mi.items(), Style.MUTED, c -> {
                    Optional<baro.model.Item> it = b.context().catalog().item(c.id());
                    if (it.isPresent()) what.link(it.get()); else what.text(best.itemName(c.id()), Style.NORMAL);
                    if (c.max() > 1) what.text(" ×" + (c.min() == c.max() ? c.max() : c.min() + "–" + c.max()), Style.MUTED);
                });
                st.with(what);
            }
        }
        return st;
    }

    /** Монстры, которые встречаются в биоме текущей станции при её сложности, — самые опасные первыми. */
    public static Stack biomeMonsters(SaveSnapshot s, Bestiary best, Blocks b, int limit) {
        Stack st = new Stack(1);
        List<Creature> l = biome(s, best);
        if (l.isEmpty()) { st.with(b.text().text(tr("Нет данных о монстрах этого биома."), Style.MUTED)); return st; }
        for (Creature c : l.subList(0, Math.min(limit, l.size()))) {
            RichText r = b.text().lore(c.name(), c, c.kind().title())
                    .text("  ♥ " + Format.num(c.health().vitality()), Style.MUTED);
            if (c.maxHit() > 0) r.text(tr(" · удар "), Style.MUTED).word(Format.num(c.maxHit()), c.maxHit() >= 50 ? Style.BAD : Style.NORMAL);
            if (c.canEnterSub()) r.text(tr(" · заходит в лодку"), Style.WARN);
            st.with(r);
        }
        if (l.size() > limit) st.with(b.text().text(tr("и ещё ") + (l.size() - limit), Style.MUTED));
        return st;
    }

    /** Название биома для показа (как в бестиарии) или идентификатор. */
    public static String biomeName(SaveSnapshot s, Bestiary best) {
        if (best != null) for (Creature c : best.creatures()) for (Creature.Spawn sp : c.spawns())
            if (sp.biome().equalsIgnoreCase(s.biome()) && sp.biomeName() != null && !sp.biomeName().isEmpty()) return sp.biomeName();
        return s.biome();
    }

    static List<Creature> biome(SaveSnapshot s, Bestiary best) {
        if (best == null || s.biome().isEmpty()) return List.of();
        double d = s.levelDifficulty();
        List<Creature> l = new ArrayList<>();
        for (Creature c : best.creatures()) {
            if (c.ai().pet()) continue;
            for (Creature.Spawn sp : c.spawns()) {
                boolean biomeOk = sp.biome().isEmpty() || sp.biome().equalsIgnoreCase(s.biome());
                int max = sp.maxDifficulty() <= 0 ? 100 : sp.maxDifficulty();
                boolean diffOk = d <= 0 || d >= sp.minDifficulty() && d <= max;
                if (biomeOk && !sp.biome().isEmpty() && diffOk) { l.add(c); break; }
            }
        }
        l.sort(Comparator.comparingDouble((Creature c) -> -c.maxHit() * Math.max(1, c.health().vitality() / 100)));
        return l;
    }

    private static MissionInfo find(Bestiary best, String id) {
        for (MissionInfo m : best.missions()) if (m.id().equalsIgnoreCase(id)) return m;
        return null;
    }
}
