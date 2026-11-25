package baro.data.gamefiles;


import static baro.Tr.tr;
import baro.data.CatalogBuilder;
import org.w3c.dom.Element;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/** Деревья и описания талантов из Content/Talents (те же правила, что в gen_talents.py). */
final class TalentsReader {
    /** Тема таланта: подходит, если ключ характеристики/способности содержит один из keys или в описании есть одно из words. */
    private record Theme(String id, String title, List<String> keys, List<String> words) {}

    private static final List<Theme> THEMES = List.of(
            new Theme("combat", tr("Бой и оружие"), List.of("WeaponsSkillBonus", "RangedSpreadReduction", "MeleeAttack", "Explosion", "DualWielding", "MaximumHealth", "ModifyAttackData", "WeaponsSkillGain"),
                    List.of(tr("урон"), tr("чудовищ"), tr("оружи"), tr("пистолет"), tr("винтов"), tr("стрел"), tr("взрыв"), tr("монстр"), tr("атак"), tr("ближнем бою"))),
            new Theme("medical", tr("Лечение и препараты"), List.of("Medical", "BuffItem", "BuffDuration", "Poison", "CPRBoost"),
                    List.of(tr("лечен"), tr("препарат"), tr("усилени"), tr("здоровь"), tr("медицин"), tr("яд"), tr("отравл"), tr("шприц"), tr("реанимац"))),
            new Theme("repair", tr("Ремонт и механика"), List.of("Mechanical", "RepairSpeed", "RepairTool", "PumpSpeed", "MaxRepairCondition", "Detoriation"),
                    List.of(tr("ремонт"), tr("чини"), tr("насос"), tr("механ"), tr("сварк"), tr("пробоин"))),
            new Theme("electric", tr("Электрика и реактор"), List.of("Electrical", "Reactor", "Battery"), List.of(tr("реактор"), tr("электр"), tr("аккумул"), tr("проводк"), tr("ток"))),
            new Theme("helm", tr("Управление подлодкой"), List.of("Helm", "Engine", "BallastFlora"), List.of(tr("двигател"), tr("подлодк"), tr("скорость подлодки"), tr("сонар"), tr("штурв"))),
            new Theme("trade", tr("Торговля и деньги"), List.of("StoreBuy", "StoreSell", "Shipyard", "Reputation", "ExtraMission", "ExtraSpecialSales", "HireCost", "GiveMoney", "GiveReputation"),
                    List.of(tr("марок"), tr("кредит"), tr("магазин"), tr("цен"), tr("продаж"), tr("покуп"), tr("репутац"), tr("миссий"), tr("награ"), tr("денег"), tr("деньг"))),
            new Theme("craft", tr("Крафт и разбор"), List.of("Fabrication", "IncreaseFabricationQuality", "Deconstructor", "RemoveRandomIngredient", "ExtraStackSize", "InventoryExtraStackSize"),
                    List.of(tr("изготов"), tr("рецепт"), tr("разбор"), tr("качеств"), tr("фабрикатор"))),
            new Theme("genetic", tr("Генетика"), List.of("GeneticMaterial"), List.of(tr("ген"), tr("генетич"))),
            new Theme("diving", tr("Плавание и снаружи"), List.of("SwimmingSpeed", "HoldBreath", "MovementSpeed"), List.of(tr("плава"), tr("вод"), tr("дыхан"), tr("кислород"), tr("скафандр"), tr("бега"), tr("передвиж"))),
            new Theme("xp", tr("Опыт и навыки"), List.of("SkillGain", "Apprenticeship", "GiveExperience", "SkillBonus", "GiveTalentPoints"), List.of(tr("опыт"), tr("навык"), tr("уровн"), tr("талант"))),
            new Theme("team", tr("Помощь команде"), List.of("ToAllies", "GiveTalentPointsToAllies", "Apprenticeship"), List.of(tr("команд"), tr("союзник"), tr("член экипаж"), tr("рядом с вами"))));
    private static final Pattern SKILLSTAT = Pattern.compile("^(Weapons|Medical|Electrical|Mechanical|Helm)?Skill(Bonus|GainSpeed)$");
    private static final Pattern NUMBER = Pattern.compile("^-?[0-9.,]+$");
    private static final Set<String> JOBIDS = Set.of("captain", "securityofficer", "medicaldoctor", "mechanic", "engineer", "assistant");

    private record Stat(String type, double value) {}
    private record Ally(String stat, double value, String jobs, String condition) {}

    private final GameInstall game;
    private final GameTexts texts;
    private final ItemsReader items;
    /** Недуг → какие навыки он повышает. */
    private final Map<String, List<Stat>> skillAfflictions = new HashMap<>();
    /** Недуг → недуги, которые он накладывает. */
    private final Map<String, List<String>> nestedAfflictions = new HashMap<>();

    TalentsReader(GameInstall game, GameContent content, GameTexts texts, ItemsReader items) {
        this.game = game; this.texts = texts; this.items = items;
        for (GameContent.Affliction a : content.afflictions.values()) {
            List<Stat> vals = new ArrayList<>();
            for (Element e : GameXml.children(a.element, "effect"))
                for (Element sv : GameXml.children(e, "statvalue")) {
                    String st = GameXml.attr(sv, "stattype", "");
                    Double v = GameXml.num(sv, "value");
                    if (v == null) v = GameXml.num(sv, "maxvalue");
                    if (SKILLSTAT.matcher(st).matches() && v != null) vals.add(new Stat(st, v));
                }
            if (!vals.isEmpty()) skillAfflictions.put(a.id, vals);
            List<String> refs = new ArrayList<>();
            for (Element x : GameXml.descendants(a.element)) if (GameXml.is(x, "affliction") && GameXml.attr(x, "identifier") != null) refs.add(GameXml.attr(x, "identifier"));
            if (!refs.isEmpty()) nestedAfflictions.put(a.id, refs);
        }
    }

    /** @return число талантов или −1, если деревьев в файлах нет (тогда встроенные таланты остаются) */
    int register(CatalogBuilder b) {
        Map<String, Element> talents = new LinkedHashMap<>();
        for (Path f : game.files("Talents")) {
            Element root = GameXml.root(f);
            if (root == null) continue;
            for (Element t : GameXml.children(root))
                for (Element c : GameXml.is(t, "override") ? GameXml.children(t) : List.of(t))
                    if (GameXml.is(c, "talent") && GameXml.attr(c, "identifier") != null) talents.put(GameXml.attr(c, "identifier"), c);
        }
        Map<String, Element> trees = new LinkedHashMap<>();
        for (Path f : game.files("TalentTrees")) {
            Element root = GameXml.root(f);
            if (root == null) continue;
            for (Element t : GameXml.descendants(root))
                if (GameXml.is(t, "talenttree") && GameXml.attr(t, "jobidentifier") != null) trees.put(GameXml.attr(t, "jobidentifier"), t);
        }
        if (trees.isEmpty() || talents.isEmpty()) return -1;

        b.resetTalents();
        for (Theme th : THEMES) b.talentTheme(th.id, th.title);
        Set<String> used = new LinkedHashSet<>();
        trees.forEach((job, tree) -> {
            CatalogBuilder.JobSpec js = b.job(job, texts.get("jobname." + job, job));
            for (Element st : GameXml.children(tree, "subtree")) {
                String sid = GameXml.attr(st, "identifier", "");
                boolean primary = "primary".equalsIgnoreCase(GameXml.attr(st, "type", ""));
                String name = primary ? tr("Основа") : texts.get(GameXml.attr(st, "nameidentifier", "talenttree." + sid), sid);
                js.subtree(sid, name, primary, GameXml.attr(st, "requires", ""), GameXml.list(GameXml.attr(st, "blocks")).toArray(new String[0]));
                for (Element g : GameXml.children(st, "talentoptions")) {
                    List<String> opts = new ArrayList<>();
                    for (Element o : GameXml.children(g, "talentoption")) if (GameXml.attr(o, "identifier") != null) opts.add(GameXml.attr(o, "identifier"));
                    js.group((int) GameXml.num(g, "requiredtalents", 1), (int) GameXml.num(g, "maxchosentalents", 1), opts.toArray(new String[0]));
                    used.addAll(opts);
                }
            }
            js.done();
        });
        int n = 0;
        for (String tid : used) if (talents.containsKey(tid)) { talent(b, tid, talents.get(tid)); n++; }
        return n;
    }

    private void talent(CatalogBuilder b, String tid, Element t) {
        List<String> desc = new ArrayList<>();
        for (Element d : GameXml.children(t, "description")) {
            String text = texts.get(GameXml.attr(d, "tag"), "");
            for (Element r : GameXml.children(d, "replace")) text = text.replace(GameXml.attr(r, "tag", "\u0000"), replacement(GameXml.attr(r, "value", "")));
            text = text.replace("\\n", "\n").replaceAll("‖[^‖]*‖", "").trim();
            if (!text.isEmpty()) desc.add(text);
        }
        List<String> recipes = new ArrayList<>();
        for (Element r : GameXml.children(t, "addedrecipe")) {
            String id = GameXml.attr(r, "itemidentifier");
            if (id != null && items.included.contains(id)) recipes.add(id);
        }
        List<Stat> stats = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (Element e : GameXml.descendants(t)) {
            String tg = e.getTagName();
            if (tg.startsWith("CharacterAbility")) keys.add(tg.substring("CharacterAbility".length()));
            String st = GameXml.attr(e, "stattype");
            if (st == null) continue;
            keys.add(st);
            String val = GameXml.attr(e, "value");
            if ((tg.equals("CharacterAbilityGiveStat") || tg.equals("CharacterAbilityGivePermanentStat")) && val != null && val.matches("^-?[0-9.]+$"))
                stats.add(new Stat(st, Double.parseDouble(val)));
        }
        String lower = String.join(" ", desc).toLowerCase(Locale.ROOT);
        Set<String> themes = new TreeSet<>();
        for (Theme th : THEMES) {
            boolean hit = th.words.stream().anyMatch(lower::contains);
            for (String sk : th.keys) for (String k : keys) if (k.toLowerCase(Locale.ROOT).contains(sk.toLowerCase(Locale.ROOT))) hit = true;
            if (hit) themes.add(th.id);
        }
        if (!recipes.isEmpty()) themes.add("craft");
        for (String r : recipes) {
            baro.model.ItemCategory c = b.itemCategory(r);
            String theme = c == null ? null : switch (c) {
                case WEAPON -> "combat";
                case MEDICAL -> "medical";
                case ELECTRICAL, FUEL -> "electric";
                case DIVING -> "diving";
                case GENETIC -> "genetic";
                default -> null;
            };
            if (theme != null) themes.add(theme);
        }
        CatalogBuilder.TalentSpec s = b.talent(tid, texts.get("talentname." + tid, tid), String.join("\n", desc));
        if (!recipes.isEmpty()) s.recipes(recipes.toArray(new String[0]));
        if (!themes.isEmpty()) s.themes(themes.toArray(new String[0]));
        for (Stat st : stats) s.stat(st.type, texts.get("stattypenames." + st.type.toLowerCase(Locale.ROOT), ""), st.value);
        for (Ally a : allyBuffs(t)) s.ally(a.stat, a.value, a.jobs, a.condition);
    }

    /** Значение для подстановки в описание: числа как есть, иначе ключи текстов («entityname.…» → название предмета). */
    private String replacement(String v) {
        if (NUMBER.matcher(v).matches() || v.equals("true") || v.equals("false")) return v;
        List<String> parts = new ArrayList<>();
        for (String part : v.split(",")) {
            part = part.trim();
            String text = texts.get(part);
            if (text == null && part.startsWith("entityname.")) text = items.name(part.substring("entityname.".length()));
            parts.add(text != null ? text : part);
        }
        return String.join(", ", parts);
    }

    /** Бонусы к навыкам, которые талант даёт другим членам экипажа (через эффекты-недуги), без повторов. */
    private Set<Ally> allyBuffs(Element t) {
        Set<Ally> out = new LinkedHashSet<>();
        Map<Element, Element> parent = new HashMap<>();
        for (Element p : GameXml.descendants(t)) for (Element c : GameXml.children(p)) parent.put(c, p);
        for (Element c : GameXml.children(t)) parent.put(c, t);
        for (Element ab : GameXml.descendants(t)) {
            String tg = ab.getTagName();
            if (tg.contains("GainSimultaneousSkill") && "true".equals(GameXml.attr(ab, "targetallies"))) {
                out.add(new Ally("SkillShare", 1.0, "", tr("когда вы повышаете навык")));
                continue;
            }
            if (!tg.startsWith("CharacterAbility") || !(tg.contains("Allies") || tg.contains("Ally") || tg.contains("Apprenticeship") || tg.contains("Crew"))) continue;
            if ("true".equals(GameXml.attr(ab, "allowself")) && !tg.contains("Allies")) continue;
            Element g = ab;
            while (parent.containsKey(g) && !g.getTagName().startsWith("AbilityGroup")) g = parent.get(g);
            String trig = GameXml.attr(g, "abilityeffecttype", "");
            List<String> cond = new ArrayList<>();
            if ("true".equals(GameXml.attr(ab, "insameroom"))) cond.add(tr("в том же отсеке"));
            else if (GameXml.attr(ab, "maxdistance") != null || GameXml.attr(ab, "range") != null) cond.add(tr("рядом"));
            if (g.getTagName().equals("AbilityGroupEffect") && !trig.isEmpty() && !trig.equals("None")) cond.add(tr("на время, после действия"));
            if (GameXml.child(g, "Conditions") != null && g.getTagName().equals("AbilityGroupInterval")) cond.add(tr("при условии"));
            if (tg.contains("Apprenticeship"))
                cond.add("true".equals(GameXml.attr(ab, "invert")) ? tr("профессиям, по которым помощник НЕ стажируется") : tr("профессии, по которой стажируется помощник"));
            String jobs = GameXml.attr(ab, "jobs", ""), c = String.join(", ", cond);
            for (Element af : GameXml.descendants(ab)) {
                String aid = GameXml.is(af, "affliction") ? GameXml.attr(af, "identifier") : null;
                if (aid == null) continue;
                List<Stat> vals = skillAfflictions.get(aid);
                if (vals != null) for (Stat v : vals) out.add(new Ally(v.type, v.value, jobs, c));
                else for (String ref : nestedAfflictions.getOrDefault(aid, List.of())) {
                    String j = ref.substring(ref.lastIndexOf('_') + 1);
                    if (JOBIDS.contains(j)) for (Stat v : skillAfflictions.getOrDefault(ref, List.of())) out.add(new Ally(v.type, v.value, j, c));
                }
            }
        }
        return out;
    }
}
