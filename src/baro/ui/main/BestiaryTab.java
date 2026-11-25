package baro.ui.main;


import static baro.Tr.tr;
import baro.bestiary.*;
import baro.data.GameCatalog;
import baro.pricing.Format;
import baro.ui.views.Blocks;
import baro.ui.views.LoreViews;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/** Вкладка «Бестиарий»: монстры, люди-враги, союзники и боты экипажа, мирные NPC. */
final class BestiaryTab extends LoreTab<BestiaryTab.Section> {
    enum Section { MONSTERS, ENEMIES, ALLIES, PEOPLE, ALL }

    BestiaryTab(GameCatalog catalog, BestiarySource source, Blocks blocks, baro.ui.Navigator external) {
        super(catalog, source, blocks, external, tr("Существо, NPC, профессия…"), 3);
        sections.option(tr("Монстры"), Section.MONSTERS).option(tr("Враги-люди"), Section.ENEMIES).option(tr("Союзники"), Section.ALLIES)
                .option(tr("Мирные NPC"), Section.PEOPLE).option(tr("Все"), Section.ALL);
        sections.setToolTipText(tr("Союзники — боты экипажа (профессии), наёмники и питомцы"));
        sectionsReady();
    }

    @Override protected List<Object> entries(Section section, String q) {
        List<Object> out = new ArrayList<>();
        boolean all = section == Section.ALL || section == null;
        if (all || section == Section.MONSTERS)
            for (CreatureKind k : CreatureKind.values()) {
                if (k == CreatureKind.PET && !all) continue;
                group(out, k.title(), data.creatures().stream().filter(c -> c.kind() == k && c.nameContains(q)).map(c -> (Object) c).toList());
            }
        if (all || section == Section.ENEMIES) group(out, NpcKind.ENEMY.title(), npcs(NpcKind.ENEMY, q));
        if (all || section == Section.ALLIES) {
            group(out, tr("Боты экипажа: профессии"), data.jobs().stream().filter(j -> j.crew() && contains(j.name() + " " + j.id(), q)).map(j -> (Object) j).toList());
            group(out, NpcKind.HIREABLE.title(), npcs(NpcKind.HIREABLE, q));
            if (!all) group(out, tr("Питомцы и прирученные существа"), data.creatures().stream()
                    .filter(c -> (c.ai().pet() || c.kind() == CreatureKind.PET) && c.nameContains(q)).map(c -> (Object) c).toList());
        }
        if (all || section == Section.PEOPLE) {
            for (NpcKind k : new NpcKind[]{NpcKind.STATION, NpcKind.CIVIL, NpcKind.SPECIAL, NpcKind.TUTORIAL}) group(out, k.title(), npcs(k, q));
            group(out, tr("Служебные профессии NPC"), data.jobs().stream().filter(j -> !j.crew() && contains(j.name() + " " + j.id(), q)).map(j -> (Object) j).toList());
        }
        return out;
    }

    private List<Object> npcs(NpcKind k, String q) {
        return data.npcs().stream().filter(n -> n.kind() == k && n.nameContains(q)).map(n -> (Object) n).toList();
    }

    private static boolean contains(String s, String q) { return q.isEmpty() || s.toLowerCase(java.util.Locale.ROOT).replace('ё', 'е').contains(q); }

    private static void group(List<Object> out, String title, List<Object> items) {
        if (items.isEmpty()) return;
        out.add(new Header(title + " · " + items.size()));
        out.addAll(items);
    }

    @Override protected String[] right(Object e) {
        if (e instanceof Creature c) {
            double hit = c.maxHit();
            return new String[]{"♥ " + Format.num(c.health().vitality()) + (hit > 0 ? tr(" · удар ") + Format.num(hit) : ""), hit >= 60 ? "warn" : ""};
        }
        if (e instanceof Npc n) return new String[]{n.jobName() != null ? n.jobName() : ""};
        if (e instanceof CrewJob j) {
            for (CrewJob.SkillRange r : j.skills()) if (r.primary()) return new String[]{r.name() + " " + r.min() + "–" + r.max()};
            return new String[]{""};
        }
        return new String[]{""};
    }

    @Override protected Component overview(LoreViews v) { return v.bestiaryOverview(); }

    @Override protected Section sectionOf(Object e) {
        if (e instanceof Creature c) return c.kind() == CreatureKind.PET ? Section.ALLIES : Section.MONSTERS;
        if (e instanceof Npc n) return switch (n.kind()) {
            case ENEMY -> Section.ENEMIES;
            case HIREABLE -> Section.ALLIES;
            default -> Section.PEOPLE;
        };
        if (e instanceof CrewJob j) return j.crew() ? Section.ALLIES : Section.PEOPLE;
        return Section.ALL;
    }
}
