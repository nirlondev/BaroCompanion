package baro.ui.views;


import static baro.Tr.tr;
import baro.bestiary.*;
import baro.bestiary.Creature.*;
import baro.data.GameCatalog;
import baro.model.Item;
import baro.pricing.Format;
import baro.ui.widgets.RichText;
import baro.ui.widgets.Stack;
import baro.ui.widgets.Style;

import java.awt.Component;
import java.util.*;

/** Карточки бестиария и миссий: существо, человек-NPC, профессия экипажа, миссия; обзорные страницы. */
public final class LoreViews {
    private final Bestiary best;
    private final GameCatalog catalog;
    private final Blocks b;

    public LoreViews(Bestiary best, GameCatalog catalog, Blocks blocks) { this.best = best; this.catalog = catalog; this.b = blocks; }

    /** Блоки карточек (со ссылками этой вкладки). */
    public Blocks blocks() { return b; }

    public Bestiary bestiary() { return best; }

    public Component view(Object entry) {
        if (entry instanceof Creature c) return creature(c);
        if (entry instanceof Npc n) return npc(n);
        if (entry instanceof CrewJob j) return job(j);
        if (entry instanceof MissionInfo m) return mission(m);
        return new Stack(4);
    }

    // ---------- ссылки ----------

    private RichText item(RichText t, String id) {
        Optional<Item> it = catalog.item(id);
        return it.isPresent() ? t.link(it.get()) : t.word(best.itemName(id), Style.NORMAL);
    }

    private RichText creatureLink(RichText t, String id) {
        Optional<Creature> c = best.creature(id);
        return c.isPresent() ? t.lore(c.get().name(), c.get(), c.get().kind().title()) : t.word(id, Style.NORMAL);
    }

    private RichText missionLink(RichText t, MissionInfo m) { return t.lore(m.name(), m, m.kind().title()); }

    /** Цель ИИ по тегу: существо (ссылкой), понятное название или сам тег. */
    private void target(RichText t, String tag) {
        Optional<Creature> c = best.creature(tag);
        if (c.isPresent()) { t.lore(c.get().name(), c.get(), null); return; }
        for (Creature x : best.creatures()) if (x.group() != null && x.group().equalsIgnoreCase(tag) && x.parent() == null) { t.lore(x.name(), x, tr("и все из группы «") + tag + "»"); return; }
        String g = Glossary.tag(tag);
        t.word(g != null ? g : tag, Style.NORMAL);
    }

    private static String m(double units) { return Format.num(units / 100) + tr(" м"); }

    /** Множитель: «×1,5», «×0,75». */
    private static String times(double v) {
        double r = Math.round(v * 100) / 100.0;
        return "×" + (r == Math.rint(r) ? String.valueOf((long) r) : String.valueOf(r).replace('.', baro.Tr.english() ? '.' : ','));
    }

    private RichText row(String label, String value) { return b.text().word(label + "  ", Style.MUTED).text(value, Style.NORMAL); }

    private Stack card(String title, String chip) {
        Stack s = new Stack(4).padding(10, 12, 16, 12);
        s.with(b.text().text(title, Style.TITLE).newLine().word(chip.toUpperCase(Locale.ROOT), Style.MUTED));
        return s;
    }

    // ---------- существо ----------

    /** Название существа для вики: идентификатор вида без суффиксов вариантов («Mudraptor_unarmored» → «Mudraptor»). */
    private static String wikiName(Creature c) {
        String id = c.parent() != null ? c.parent() : c.id();
        id = id.replaceAll("(?i)_(m|hatchling|veteran|unarmored|passive|pet)$", "");
        return id.isEmpty() ? c.name() : Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }

    public Component creature(Creature c) {
        StringBuilder chip = new StringBuilder(c.kind().title());
        if (c.mod() != null) chip.append(tr(" · мод «")).append(c.mod()).append("»");
        Stack s = card(c.name(), chip.toString());
        RichText act = b.text();
        if (b.context() != null) act.action(tr("Сравнить"), tr("Добавить к сравнению существ (до 4)"), () -> b.context().actions().compare(c)).word("   ", Style.MUTED);
        act.action(tr("Вики"), tr("Страница существа в официальной вики игры"), () -> baro.ui.Links.open(act, baro.ui.Links.page(wikiName(c))));
        s.with(act);
        if (c.parent() != null) {
            RichText t = b.text().text(tr("Вариант вида "), Style.MUTED);
            creatureLink(t, c.parent());
            s.with(t.text(tr(" — всё, что не указано отдельно, как у него."), Style.MUTED));
        }
        String note = CreatureNotes.of(c, best);
        if (note != null) s.with(b.text().text(note, Style.NORMAL));

        // главное
        Stack main = new Stack(1);
        Health h = c.health();
        main.with(b.text().word(tr("Здоровье  "), Style.MUTED).word(Format.num(h.vitality()), Style.ACCENT)
                .text(tr("   ·   сила для ИИ  ") + Format.num(c.ai().combatStrength()), Style.MUTED));
        double hit = c.maxHit(), struct = c.maxStructureDamage();
        RichText dmg = b.text().word(tr("Сильнейший удар  "), Style.MUTED).word(hit > 0 ? Format.num(hit) + tr(" урона") : "—", hit >= 50 ? Style.BAD : Style.NORMAL);
        if (struct > 0) dmg.text(tr("   ·   по корпусу до ") + Format.num(struct), Style.MUTED);
        main.with(dmg);
        double top = c.topSpeed(), human = Math.max(best.humanSpeed("swimfast"), best.humanSpeed("run"));
        if (top > 0) main.with(b.text().word(tr("Скорость до  "), Style.MUTED).word(Format.num(top) + tr(" м/с"), Style.NORMAL)
                .text(human > 0 ? "   ·   " + (top >= human ? tr("в ") + Format.num(top / human) + tr(" раза быстрее") : tr("медленнее")) + tr(" человека") : "", Style.MUTED));
        main.with(row(tr("Размер"), times(c.scale()) + tr(" от модели") + (c.canEnterSub() ? tr(" · может пробраться внутрь подлодки") : tr(" · внутрь подлодки не попадает"))));
        s.with(b.section(tr("Главное"), main));

        s.with(b.section(tr("Как с ним справиться"), tactics(c)));

        // атаки
        if (!c.attacks().isEmpty()) {
            Stack at = new Stack(6);
            List<Attack> list = new ArrayList<>(c.attacks());
            list.sort(Comparator.comparingDouble((Attack a) -> -(a.healthDamage() + a.structureDamage() / 10)));
            for (Attack a : list) at.with(attack(a));
            s.with(b.section(tr("Атаки (") + list.size() + ")", at));
        }

        // уязвимости и броня
        Stack ar = new Stack(2);
        for (LimbMultiplier w : h.weakSpots()) {
            Style st = w.multiplier() > 1 ? Style.GOOD : Style.WARN;
            String what = w.multiplier() == 0 ? tr("не действует") : tr("урон ") + times(w.multiplier());
            ar.with(b.text().word(w.limb() + ": ", Style.NORMAL).word(what, st).text(w.types().isEmpty() ? "" : " — " + w.types(), Style.MUTED));
        }
        // одинаковая броня у многих частей (13 шипов Смотрителя) — одной строкой
        Map<String, List<LimbArmor>> sameArmor = new LinkedHashMap<>();
        for (LimbArmor la : c.armor()) sameArmor.computeIfAbsent(la.mods().toString().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(la);
        for (List<LimbArmor> group : sameArmor.values()) {
            List<String> names = new ArrayList<>();
            for (LimbArmor la : group) names.add(la.limb());
            String limbs = names.size() > 3 ? names.get(0) + " … " + names.get(names.size() - 1) + " (" + names.size() + tr(" частей)") : String.join(", ", names);
            RichText t = b.text().word(limbs + ": ", Style.NORMAL);
            boolean first = true, deflects = false;
            for (ArmorMod am : group.get(0).mods()) {
                if (!first) t.word("; ", Style.MUTED);
                first = false;
                t.word(am.what() + " " + times(am.multiplier()), am.multiplier() < 1 ? Style.WARN : Style.GOOD);
                if (!am.sector().isEmpty()) t.text(tr(" (сектор ") + am.sector() + ")", Style.MUTED);
                deflects |= am.deflects();
            }
            if (deflects) t.text(tr(" · отражает пули"), Style.MUTED);
            ar.with(t);
        }
        if (ar.getComponentCount() > 0) {
            ar.with(b.text().text(tr("×1,5 — эта часть получает в полтора раза больше урона; ×0,1 — броня пропускает только десятую часть. ")
                    + tr("Сектор — с какой стороны прикрывает броня (0° — спереди)."), Style.MUTED));
            s.with(b.section(tr("Уязвимые места и броня"), ar));
        }

        // здоровье подробно
        Stack hs = new Stack(1);
        hs.with(row(tr("Кровотечение"), h.bleeds() ? (h.bleedingReduction() > 0 ? tr("кровоточит, само останавливается со скоростью ") + Format.num(h.bleedingReduction()) + tr("/с") : tr("кровоточит")) : tr("не истекает кровью")));
        if (h.burnReduction() > 0) hs.with(row(tr("Ожоги"), tr("заживают сами: ") + Format.num(h.burnReduction()) + tr("/с")));
        if (h.regen() > 0) hs.with(row(tr("Регенерация"), "+" + Format.num(h.regen()) + tr(" здоровья в секунду")));
        if (h.regenWhenEating() > 0) hs.with(row(tr("Когда ест"), "+" + Format.num(h.regenWhenEating()) + tr(" здоровья в секунду")));
        hs.with(row(tr("Яды"), h.poisonVulnerability() == 0 ? tr("не действуют") : tr("восприимчивость ") + times(h.poisonVulnerability())));
        hs.with(row(tr("Давление"), Double.isInfinite(h.crushDepth()) ? tr("глубина не страшна") : tr("раздавливает глубже ") + Format.num(h.crushDepth()) + tr(" м")));
        hs.with(row(tr("Дыхание"), c.needsAir() ? tr("нужен воздух") : tr("воздух не нужен")));
        s.with(b.section(tr("Здоровье"), hs));

        if (!c.abilities().isEmpty()) {
            Stack ab = new Stack(1);
            for (String a : c.abilities()) ab.with(b.text().text("• " + a, Style.NORMAL));
            s.with(b.section(tr("Особые свойства"), ab));
        }

        if (!c.speeds().isEmpty()) {
            Stack sp = new Stack(1);
            String[][] keys = {{"walk", tr("шагом")}, {"run", tr("бегом")}, {"swimslow", tr("плывёт спокойно")}, {"swimfast", tr("плывёт быстро")}};
            for (String[] k : keys) {
                double v = c.speed(k[0]);
                if (v <= 0) continue;
                double hv = best.humanSpeed(k[0]);
                sp.with(b.text().word(k[1] + "  ", Style.MUTED).word(Format.num(v) + tr(" м/с"), Style.NORMAL)
                        .text(hv > 0 ? tr("   (человек ") + Format.num(hv) + tr(" м/с)") : "", Style.MUTED));
            }
            s.with(b.section(tr("Скорость"), sp));
        }

        s.with(b.section(tr("Поведение"), behavior(c)));

        if (!c.drops().isEmpty()) {
            Stack d = new Stack(1);
            for (Drop dr : c.drops()) {
                RichText t = b.text().word((dr.count() > 1 ? dr.count() + "× " : ""), Style.NORMAL);
                item(t, dr.itemId());
                t.text("  " + (dr.chance() >= 0.999 ? tr("всегда") : Format.chance(dr.chance())) + " · " + dr.note(), Style.MUTED);
                d.with(t);
            }
            d.with(b.text().text(tr("«Набор» — у существа при себе одно из нескольких наборов (шанс указан). Обыщите тело, чтобы забрать."), Style.MUTED));
            s.with(b.section(tr("Добыча с тела"), d));
        }
        geneticMaterial(c).ifPresent(g -> s.with(b.section(tr("Генетический материал"), b.text().text(tr("Из этого вида получают "), Style.MUTED).link(g))));

        if (!c.spawns().isEmpty()) {
            Stack sp = new Stack(2);
            for (Spawn x : c.spawns()) {
                List<String> places = new ArrayList<>();
                for (String p : x.places()) places.add(Glossary.place(p));
                sp.with(b.text().word(x.biomeName(), Style.BOLD).text(tr("  ·  сложность ") + x.minDifficulty() + "–" + x.maxDifficulty()
                        + "  ·  " + String.join(", ", places) + tr("  ·  за раз ") + (x.minAmount() == x.maxAmount() ? x.maxAmount() : x.minAmount() + "–" + x.maxAmount())
                        + tr("  ·  событий: ") + x.events(), Style.MUTED));
            }
            sp.with(b.text().text(tr("По случайным событиям игры: в каких биомах и на какой сложности уровня может появиться, в каких местах и сколько."), Style.MUTED));
            s.with(b.section(tr("Где встречается"), sp));
        } else if (c.parent() == null || best.missionsWith(c).isEmpty())
            s.with(b.section(tr("Где встречается"), b.text().text(tr("В случайных событиях не появляется — только в миссиях, особых событиях или из яиц."), Style.MUTED)));

        List<MissionInfo> ms = best.missionsWith(c);
        if (!ms.isEmpty()) {
            Stack ml = new Stack(1);
            for (MissionInfo mi : ms) ml.with(missionLink(b.text(), mi).text("  · " + mi.kind().shortTitle() + " · " + Format.money(mi.reward()), Style.MUTED));
            s.with(b.section(tr("Миссии с ним"), ml));
        }
        List<Creature> vars = best.variantsOf(c);
        if (!vars.isEmpty()) {
            RichText t = b.text();
            s.with(b.section(tr("Варианты"), t.join(vars, Style.MUTED, v -> t.lore(v.name(), v, null))));
        }
        s.with(b.text().text(tr("Идентификатор: ") + c.id() + (c.group() != null ? tr(" · группа: ") + c.group() : ""), Style.MUTED));
        return s;
    }

    private Optional<Item> geneticMaterial(Creature c) {
        for (Creature x = c; x != null; x = x.parent() == null ? null : best.creature(x.parent()).orElse(null)) {
            Optional<Item> it = catalog.item("geneticmaterial" + x.id().toLowerCase(Locale.ROOT));
            if (it.isPresent()) return it;
            if (x.parent() != null && x.parent().equalsIgnoreCase(x.id())) break;
        }
        return Optional.empty();
    }

    private Stack attack(Attack a) {
        Stack s = new Stack(1);
        RichText head = b.text();
        boolean structure = "Structure".equalsIgnoreCase(a.target());
        head.word(a.ranged() ? tr("Дальняя атака") : structure ? tr("Удар по корпусу") : tr("Атака"), Style.BOLD)
                .text(" · " + Glossary.attackTarget(a.target()) + " · " + Glossary.context(a.context()) + " · " + a.limb(), Style.MUTED);
        s.with(head);
        RichText d = b.text();
        boolean any = false;
        for (Hit h : a.hits()) {
            if (h.strength() <= 0) continue;
            if (any) d.word(", ", Style.MUTED);
            any = true;
            d.word(h.name() + " " + Format.num(h.strength()), h.isDamage() ? Style.BAD : Style.NORMAL);
            if (h.probability() < 0.999) d.text(" (" + Format.chance(h.probability()) + ")", Style.MUTED);
            if (h.viaEffect()) d.text(tr(" доп."), Style.MUTED);
        }
        if (!any && a.projectiles().isEmpty() && a.structureDamage() <= 0) d.text(tr("урона здоровью нет"), Style.MUTED);
        if (any) s.with(d);
        if (!a.projectiles().isEmpty()) {
            RichText p = b.text().text(tr("Выпускает: "), Style.MUTED);
            p.join(a.projectiles(), Style.MUTED, id -> item(p, id));
            s.with(p);
        }
        List<String> parts = new ArrayList<>();
        if (a.structureDamage() > 0) parts.add(tr("корпус ") + Format.num(a.structureDamage()));
        if (a.itemDamage() > 0) parts.add(tr("устройства ") + Format.num(a.itemDamage()));
        if (a.levelWallDamage() > 0) parts.add(tr("стены пещер ") + Format.num(a.levelWallDamage()));
        if (a.range() > 0) parts.add(tr("дальность ") + m(a.range()));
        if (a.cooldown() > 0) parts.add(tr("раз в ") + Format.seconds(a.cooldown()));
        if (a.stun() > 0) parts.add(tr("оглушает на ") + Format.seconds(a.stun()));
        if (a.penetration() > 0) parts.add(tr("пробивает броню ") + Format.percent(a.penetration()));
        if (a.severChance() > 0) parts.add(tr("шанс оторвать конечность ") + Format.chance(a.severChance()));
        if (!parts.isEmpty()) s.with(b.text().text(String.join(" · ", parts), Style.MUTED));
        return s;
    }

    private Stack tactics(Creature c) {
        Stack s = new Stack(1);
        List<String> tips = new ArrayList<>();
        Ai ai = c.ai();
        Map<String, AiTarget> t = new HashMap<>();
        for (AiTarget x : ai.targets()) t.put(x.tag().toLowerCase(Locale.ROOT), x);
        if (c.canEnterSub() && !c.ai().pet()) {
            boolean ground = c.attacks().stream().anyMatch(a -> !"Water".equalsIgnoreCase(a.context()) && !"Structure".equalsIgnoreCase(a.target()));
            if (ground) tips.add(ai.aggressiveBoarding() ? tr("Целенаправленно лезет внутрь подлодки через пробоины и шлюзы — внутри нападает на экипаж.")
                    : tr("Может пробраться внутрь через пробоины и открытые шлюзы."));
        }
        if (!ai.latch().isEmpty()) tips.add(tr("Цепляется ") + ai.latch() + ".");
        if (c.maxStructureDamage() >= 100) tips.add(tr("Сильно бьёт по корпусу (до ") + Format.num(c.maxStructureDamage()) + tr(" за удар) — держите наготове сварщиков и ремонт."));
        if (state(t, "sonar", "attack")) tips.add(tr("Идёт на сигнал активного сонара — рядом с ним переключайтесь на пассивный."));
        if (state(t, "provocative", "attack") || state(t, "decoy", "attack")) tips.add(tr("Отвлекается на шумные и светящиеся предметы и приманки."));
        if (state(t, "engine", "avoid")) tips.add(tr("Избегает работающих двигателей."));
        if (state(t, "stronger", "avoid")) tips.add(tr("Убегает от существ сильнее себя."));
        if (state(t, "weapon", "avoid")) tips.add(tr("Держится подальше от людей с оружием."));
        if (state(t, "dead", "eat") || state(t, "monsterfood", "eat")) tips.add(tr("Отвлекается на трупы и корм для монстров."));
        if (ai.fleeHealth() > 0) tips.add(tr("Отступает, когда здоровье падает ниже ") + Format.num(ai.fleeHealth()) + "%.");
        if (ai.swarm()) tips.add(tr("Держится стаей — ждите сразу нескольких."));
        if (ai.avoidGunfire()) tips.add(tr("Уходит из-под обстрела."));
        List<String> weak = new ArrayList<>();
        for (LimbMultiplier w : c.health().weakSpots()) if (w.multiplier() > 1) weak.add(w.limb() + " " + times(w.multiplier()));
        for (LimbArmor la : c.armor()) for (ArmorMod am : la.mods()) if (am.multiplier() > 1) weak.add(la.limb() + " " + times(am.multiplier()));
        if (!weak.isEmpty()) tips.add(tr("Цельтесь в: ") + String.join(", ", new LinkedHashSet<>(weak)) + ".");
        if (c.armor().stream().anyMatch(la -> la.mods().stream().anyMatch(ArmorMod::deflects)))
            tips.add(tr("Броня отражает пули — стреляйте в незащищённые части или берите взрывчатку, огонь, тяжёлые турели."));
        if (c.health().poisonVulnerability() >= 1.5) tips.add(tr("Особенно уязвим к ядам (") + times(c.health().poisonVulnerability()) + ").");
        if (c.health().poisonVulnerability() == 0) tips.add(tr("Яды не действуют."));
        if (!c.health().bleeds()) tips.add(tr("Не истекает кровью — оружие на кровотечение бесполезно."));
        if (c.health().vitality() >= 2000) tips.add(tr("Очень много здоровья: в бою нужны турели и запас боеприпасов, иногда разумнее уйти."));
        if (tips.isEmpty()) tips.add(tr("Особых приёмов не нужно."));
        for (String x : tips) s.with(b.text().text("• " + x, Style.NORMAL));
        return s;
    }

    private static boolean state(Map<String, AiTarget> t, String tag, String state) {
        AiTarget x = t.get(tag);
        return x != null && x.state().toLowerCase(Locale.ROOT).contains(state);
    }

    private Stack behavior(Creature c) {
        Stack s = new Stack(2);
        Ai ai = c.ai();
        s.with(b.text().word(tr("Зрение  "), Style.MUTED).word(times(ai.sight()), Style.NORMAL).word(tr("   Слух  "), Style.MUTED).word(times(ai.hearing()), Style.NORMAL)
                .text(ai.pet() ? tr("   ·   питомец") : "", Style.MUTED));
        Map<String, List<AiTarget>> byState = new LinkedHashMap<>();
        List<AiTarget> sorted = new ArrayList<>(ai.targets());
        sorted.sort(Comparator.comparingDouble((AiTarget x) -> -x.priority()));
        for (AiTarget x : sorted) byState.computeIfAbsent(Glossary.state(x.state()), k -> new ArrayList<>()).add(x);
        for (Map.Entry<String, List<AiTarget>> e : byState.entrySet()) {
            RichText t = b.text().word(cap(e.getKey()) + ":  ", Style.ACCENT);
            t.join(e.getValue(), Style.MUTED, x -> {
                target(t, x.tag());
                if (x.onlyOutside()) t.text(tr(" (снаружи)"), Style.MUTED);
                if (x.onlyInside()) t.text(tr(" (внутри)"), Style.MUTED);
            });
            s.with(t);
        }
        if (!byState.isEmpty()) s.with(b.text().text(tr("Порядок — от самого важного для существа к наименее важному."), Style.MUTED));
        return s;
    }

    private static String cap(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    // ---------- люди ----------

    public Component npc(Npc n) {
        Stack s = card(n.name(), n.kind().title() + (n.factionName() != null ? " · " + n.factionName() : "") + (n.mod() != null ? tr(" · мод «") + n.mod() + "»" : ""));
        s.with(b.text().text(npcSummary(n), Style.NORMAL));
        Stack st = new Stack(1);
        if (n.jobName() != null) {
            RichText t = b.text().word(tr("Профессия  "), Style.MUTED);
            Optional<CrewJob> j = best.jobs().stream().filter(x -> x.id().equalsIgnoreCase(n.job())).findFirst();
            if (j.isPresent()) t.lore(n.jobName(), j.get(), tr("Профессия: навыки и чем занимается")); else t.word(n.jobName(), Style.NORMAL);
            st.with(t);
        }
        st.with(row(tr("Здоровье"), Format.num(100 * n.healthMultiplier()) + (n.healthMultiplier() != 1 ? " (" + times(n.healthMultiplier()) + tr(" от обычного человека)") : tr(" (как у обычного человека)"))));
        if (n.aimAccuracy() >= 0) st.with(row(tr("Меткость"), Format.percent(n.aimAccuracy()) + (n.aimSpeed() >= 0 ? tr(" · скорость прицеливания ") + Format.percent(n.aimSpeed()) : "")));
        if (!n.behavior().isEmpty()) st.with(row(tr("Поведение"), Glossary.behavior(n.behavior())));
        if (!n.interaction().isEmpty()) st.with(row(tr("На станции"), Glossary.interaction(n.interaction())));
        if (n.salary() > 0) st.with(row(tr("Найм"), tr("базовая плата ") + Format.money(n.salary()) + (n.skillMultiplier() != 1 ? tr(" · навыки ") + times(n.skillMultiplier()) + tr(" от обычных") : "")));
        else if (n.skillMultiplier() != 1) st.with(row(tr("Навыки"), times(n.skillMultiplier()) + tr(" от обычных для профессии")));
        if (n.experience() > 0) st.with(row(tr("Опыт"), n.experience() + tr(" очков (уже прокачанные таланты)")));
        if (!n.tags().isEmpty()) st.with(row(tr("Теги"), String.join(", ", n.tags())));
        st.with(row(tr("Набор"), n.set()));
        s.with(b.section(tr("Характеристики"), st));
        if (!n.gear().isEmpty()) s.with(b.section(tr("Снаряжение"), gear(n.gear())));
        List<MissionInfo> ms = best.missionsWith(n);
        if (!ms.isEmpty()) {
            Stack ml = new Stack(1);
            for (MissionInfo mi : ms) ml.with(missionLink(b.text(), mi).text("  · " + mi.kind().shortTitle(), Style.MUTED));
            s.with(b.section(tr("Миссии"), ml));
        }
        s.with(b.text().text(tr("Идентификатор: ") + n.id(), Style.MUTED));
        return s;
    }

    private String npcSummary(Npc n) {
        return switch (n.kind()) {
            case ENEMY -> tr("Враг. Встречается в боевых миссиях и на захваченных аванпостах; при гибели с тела можно забрать снаряжение.");
            case HIREABLE -> tr("Можно нанять в отделе кадров станции (если выпадет среди кандидатов). Навыки и опыт — выше, чем у обычного новичка.");
            case STATION -> tr("Работает или живёт на станции. Нападение на него портит репутацию и поднимает охрану.");
            case CIVIL -> tr("Пассажир, пленник или заложник: участвует в миссиях сопровождения и спасения.");
            case SPECIAL -> tr("Персонаж событий и сюжетных цепочек.");
            case TUTORIAL -> tr("Персонаж обучения.");
        };
    }

    private Stack gear(List<Npc.GearSet> sets) {
        Stack s = new Stack(6);
        double total = 0;
        for (Npc.GearSet g : sets) total += g.commonness();
        int i = 0;
        for (Npc.GearSet g : sets) {
            i++;
            Stack one = new Stack(1);
            if (sets.size() > 1) one.with(b.text().word(tr("Вариант ") + i, Style.BOLD).text(total > 0 ? tr("  · шанс ") + Format.chance(g.commonness() / total) : "", Style.MUTED));
            for (Npc.Gear x : g.items()) gearLine(one, x, 0);
            s.with(one);
        }
        return s;
    }

    private void gearLine(Stack s, Npc.Gear g, int depth) {
        RichText t = b.text().word("    ".repeat(depth) + (g.amount() > 1 ? g.amount() + "× " : ""), Style.NORMAL);
        item(t, g.itemId());
        if (g.equipped()) t.text(tr("  надето"), Style.MUTED);
        if (g.infinite()) t.text(tr("  бесконечно"), Style.MUTED);
        s.with(t);
        for (Npc.Gear in : g.inside()) gearLine(s, in, depth + 1);
    }

    // ---------- профессии ----------

    public Component job(CrewJob j) {
        Stack s = card(j.name(), j.crew() ? tr("Профессия экипажа · бот-союзник") : tr("Служебная профессия NPC"));
        if (j.description() != null) s.with(b.text().text(j.description(), Style.NORMAL));
        Stack sk = new Stack(1);
        for (CrewJob.SkillRange r : j.skills())
            sk.with(b.text().word(r.name() + "  ", r.primary() ? Style.ACCENT : Style.MUTED).word(r.min() == r.max() ? String.valueOf(r.min()) : r.min() + "–" + r.max(), Style.NORMAL)
                    .text(r.primary() ? tr("  основной навык") : "", Style.MUTED));
        sk.with(b.text().text(tr("Навыки нового члена экипажа при найме (растут с опытом и талантами)."), Style.MUTED));
        s.with(b.section(tr("Навыки"), sk));
        if (!j.autonomous().isEmpty()) {
            Stack a = new Stack(1);
            for (String o : j.autonomous()) a.with(b.text().text("• " + o, Style.NORMAL));
            a.with(b.text().text(tr("Чем бот этой профессии занимается сам, без приказа (в порядке важности)."), Style.MUTED));
            s.with(b.section(tr("Сам, без приказа"), a));
        }
        if (!j.orders().isEmpty()) s.with(b.section(tr("Подходящие приказы"), b.text().text(String.join(", ", j.orders()), Style.NORMAL)));
        Stack info = new Stack(1);
        if (j.priceMultiplier() != 1) info.with(row(tr("Стоимость найма"), times(j.priceMultiplier()) + tr(" от базовой")));
        if (j.vitalityModifier() != 0) info.with(row(tr("Здоровье"), Format.signed(j.vitalityModifier()) + tr(" к обычному")));
        if (j.maxNumber() > 0) info.with(row(tr("В экипаже"), tr("не больше ") + j.maxNumber()));
        if (!j.idleBehavior().isEmpty()) info.with(row(tr("Без дела"), Glossary.behavior(j.idleBehavior())));
        if (info.getComponentCount() > 0) s.with(b.section(tr("Прочее"), info));
        if (!j.gear().isEmpty()) s.with(b.section(tr("Стартовое снаряжение"), gear(j.gear())));
        List<Npc> hire = new ArrayList<>();
        for (Npc n : best.npcs()) if (j.id().equalsIgnoreCase(n.job()) && (n.kind() == NpcKind.HIREABLE || !j.crew())) hire.add(n);
        if (!hire.isEmpty()) {
            Stack h = new Stack(1);
            for (Npc n : hire) h.with(b.text().lore(n.name(), n, null).text("  · " + n.kind().shortTitle(), Style.MUTED));
            s.with(b.section(j.crew() ? tr("Кого можно нанять") : tr("NPC с этой профессией"), h));
        }
        s.with(b.text().text(tr("Идентификатор: ") + j.id(), Style.MUTED));
        return s;
    }

    // ---------- миссии ----------

    /** Миссия: сюжет — тексты самой игры (описание, сообщения по ходу, развязка), в конце — сколько за неё платят. */
    public Component mission(MissionInfo mi) {
        Stack s = card(mi.name(), mi.kind().title() + tr(" · сложность ") + stars(mi.difficulty()) + (mi.mod() != null ? tr(" · мод «") + mi.mod() + "»" : ""));
        float mul = b.prices().settings().rewardMultiplier();
        String money = mi.reward() > 0 ? Format.money(Math.round(mi.reward() * mul)) : null;
        // в текстах игры вместо суммы стоит «[reward] кред.» — подставляем настоящую
        String sum = money != null ? money : tr("вознаграждение");
        java.util.function.UnaryOperator<String> fill = t -> t == null ? null
                : t.replace(tr("‹награда› кредитов"), sum).replace(tr("‹награда› кред."), sum).replace(tr("‹награда› кред"), sum).replace(tr("‹награда›"), sum);
        Stack story = new Stack(6);
        if (mi.description() != null) story.with(b.text().text(fill.apply(mi.description()), Style.NORMAL));
        for (List<String> dialog : mi.briefings()) {
            Stack d = new Stack(2).padding(2, 12, 2, 0);
            d.with(b.text().word(mi.briefings().size() > 1 ? tr("Как выдают миссию (один из вариантов):") : tr("Как выдают миссию:"), Style.ACCENT));
            for (String line : dialog)
                d.with(b.text().text(fill.apply(line), line.startsWith("— ") ? Style.MUTED : Style.NORMAL));
            story.with(d);
        }
        for (MissionInfo.Message msg : mi.messages()) {
            if (msg.text() != null && msg.text().equals(mi.description())) continue;
            RichText t = b.text();
            if (msg.header() != null) t.word(msg.header(), Style.BOLD).newLine();
            if (msg.text() != null) t.text(fill.apply(msg.text()), Style.NORMAL);
            story.with(t);
        }
        if (mi.success() != null) story.with(b.text().word(tr("Развязка: "), Style.GOOD).text(fill.apply(mi.success()), Style.NORMAL));
        if (mi.failure() != null) story.with(b.text().word(tr("Если провалить: "), Style.BAD).text(fill.apply(mi.failure()), Style.NORMAL));
        if (story.getComponentCount() == 0) story.with(b.text().text(tr("В игре у этой миссии нет сюжетного текста."), Style.MUTED));
        s.with(b.section(tr("Сюжет"), story));

        RichText pay = b.text().word(tr("Оплата за миссию:  "), Style.MUTED);
        if (mi.reward() > 0) {
            pay.word(money, Style.SELL);
            if (mi.rewardNote() != null) pay.text("  (" + mi.rewardNote() + ")", Style.MUTED);
        } else pay.text(tr("не платят"), Style.NORMAL);
        s.with(pay);
        return s;
    }

    /** «1 миссия», «3 миссии», «12 миссий». */
    static String plural(int n, String one, String few, String many) {
        int m10 = n % 10, m100 = n % 100;
        String w = m10 == 1 && m100 != 11 ? one : m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14) ? few : many;
        return n + " " + w;
    }

    /** Сложность миссии «3 из 5» (звёзды шрифт интерфейса не рисует). */
    public static String stars(int d) { return d <= 0 ? "—" : Math.min(5, d) + tr(" из 5"); }

    // ---------- обзоры ----------

    /** Обзор бестиария: что здесь есть, рейтинги самых опасных. */
    public Component bestiaryOverview() {
        Stack s = card(tr("Бестиарий"), sourceChip());
        long monsters = best.creatures().stream().filter(c -> c.kind() != CreatureKind.PET && c.kind() != CreatureKind.LEGACY).count();
        long enemies = best.npcs().stream().filter(n -> n.kind() == NpcKind.ENEMY).count();
        long crew = best.jobs().stream().filter(CrewJob::crew).count();
        s.with(b.text().text(tr("Все существа и люди Европы прямо из файлов игры: здоровье, атаки, броня и уязвимые места, поведение, добыча с тела, ")
                + tr("где встречаются и в каких миссиях участвуют. Слева — разделы: монстры (") + monsters + tr("), люди-враги (") + enemies
                + tr("), союзники и боты (") + crew + tr(" профессий экипажа, наёмники, питомцы), мирные NPC. Ссылки открывают связанные карточки."), Style.MUTED));
        Map<CreatureKind, Integer> byKind = new EnumMap<>(CreatureKind.class);
        for (Creature c : best.creatures()) byKind.merge(c.kind(), 1, Integer::sum);
        Stack kinds = new Stack(1);
        for (Map.Entry<CreatureKind, Integer> e : byKind.entrySet()) kinds.with(row(e.getKey().title(), e.getValue() + ""));
        s.with(b.section(tr("Существа по разделам"), kinds));
        List<Creature> real = best.creatures().stream().filter(c -> c.kind() != CreatureKind.LEGACY && c.kind() != CreatureKind.PET).toList();
        s.with(b.section(tr("Самый сильный удар по человеку"), top(real, Creature::maxHit, tr(" урона"))));
        s.with(b.section(tr("Самый сильный удар по корпусу"), top(real, Creature::maxStructureDamage, tr(" корпусу"))));
        s.with(b.section(tr("Больше всего здоровья"), top(real, c -> c.health().vitality(), tr(" здоровья"))));
        s.with(b.section(tr("Самые быстрые"), top(real, Creature::topSpeed, tr(" м/с"))));
        s.with(b.text().text(best.source(), Style.MUTED));
        return s;
    }

    private String sourceChip() { return best.source().startsWith(tr("Файлы игры")) ? tr("из файлов игры") + (best.source().contains(tr("моды:")) ? tr(" и модов") : "") : tr("встроенные данные"); }

    private Stack top(List<Creature> list, java.util.function.ToDoubleFunction<Creature> f, String unit) {
        List<Creature> l = new ArrayList<>(list);
        l.sort(Comparator.comparingDouble(f).reversed());
        Stack s = new Stack(1);
        int n = 0;
        Set<String> names = new HashSet<>();
        for (Creature c : l) {
            if (f.applyAsDouble(c) <= 0 || !names.add(c.name().replaceAll(" \\(.*\\)$", ""))) continue;
            s.with(b.text().word((++n) + ". ", Style.MUTED).lore(c.name(), c, null).text("  " + Format.num(f.applyAsDouble(c)) + unit, Style.MUTED));
            if (n >= 8) break;
        }
        return s;
    }

    /** Обзор миссий: типы и как они устроены, самые доходные. */
    public Component missionsOverview() {
        Stack s = card(tr("Все миссии"), plural(best.missions().size(), tr("миссия"), tr("миссии"), tr("миссий")) + " · " + sourceChip());
        s.with(b.text().text(tr("Все миссии игры любого типа: что нужно сделать, награда (с учётом сложности кампании), репутация, где выдаются, ")
                + tr("какие монстры, люди и предметы участвуют, особые условия и тексты. Выберите миссию слева или тип ниже."), Style.MUTED));
        Map<MissionKind, List<MissionInfo>> byKind = new EnumMap<>(MissionKind.class);
        for (MissionInfo m : best.missions()) byKind.computeIfAbsent(m.kind(), k -> new ArrayList<>()).add(m);
        for (Map.Entry<MissionKind, List<MissionInfo>> e : byKind.entrySet()) {
            int lo = Integer.MAX_VALUE, hi = 0;
            for (MissionInfo m : e.getValue()) if (m.reward() > 0) { lo = Math.min(lo, m.reward()); hi = Math.max(hi, m.reward()); }
            Stack k = new Stack(1);
            k.with(b.text().text(e.getKey().description(), Style.MUTED));
            k.with(b.text().text(plural(e.getValue().size(), tr("миссия"), tr("миссии"), tr("миссий")) + (hi > 0 ? tr(" · награда ") + (lo == hi ? Format.money(hi) : Format.money(lo) + " – " + Format.money(hi)) : ""), Style.NORMAL));
            s.with(b.section(e.getKey().title(), k));
        }
        List<MissionInfo> rich = new ArrayList<>(best.missions());
        rich.removeIf(m -> m.commonness() <= 0);
        rich.sort(Comparator.comparingInt(MissionInfo::reward).reversed());
        Stack r = new Stack(1);
        float mul = b.prices().settings().rewardMultiplier();
        for (int i = 0; i < Math.min(10, rich.size()); i++)
            r.with(b.text().word((i + 1) + ". ", Style.MUTED).lore(rich.get(i).name(), rich.get(i), null)
                    .text("  " + Format.money(Math.round(rich.get(i).reward() * mul)) + " · " + rich.get(i).kind().shortTitle(), Style.MUTED));
        s.with(b.section(tr("Самые доходные (из обычных)"), r));
        return s;
    }
}
