package baro.data.gamefiles;


import static baro.Tr.tr;
import baro.data.CatalogBuilder;
import baro.model.*;
import org.w3c.dom.Element;

import java.util.*;

/** Предметы, цены по магазинам, рецепты и разбор из Content/Items (и модов) — в CatalogBuilder. */
final class ItemsReader {
    private final GameContent content;
    private final GameTexts texts;
    /** Что попадёт в справочник: у предмета есть цена, рецепт или разбор, либо он в них участвует. */
    final Set<String> included = new LinkedHashSet<>();

    ItemsReader(GameContent content, GameTexts texts) {
        this.content = content; this.texts = texts;
        for (Map.Entry<String, Element> e : content.items.entrySet()) {
            Element it = e.getValue();
            String cat = GameXml.attr(it, "category", "");
            if (cat.toLowerCase(Locale.ROOT).contains("legacy") || GameXml.bool(it, "hideinmenus", false) && GameXml.child(it, "price") == null) continue;
            if (GameXml.child(it, "price") != null || GameXml.child(it, "fabricate") != null || GameXml.child(it, "deconstruct") != null) included.add(e.getKey());
        }
        // ингредиенты и то, что выпадает при разборе, тоже должны быть в справочнике
        for (String id : new ArrayList<>(included)) {
            Element it = content.items.get(id);
            for (Element f : GameXml.children(it, "fabricate"))
                for (Element r : GameXml.children(f)) {
                    for (String i : GameXml.list(GameXml.attr(r, "identifier"))) if (content.items.containsKey(i)) included.add(i);
                }
            for (Element d : GameXml.children(it, "deconstruct"))
                for (Element o : GameXml.children(d, "item")) {
                    String i = GameXml.attr(o, "identifier");
                    if (i != null && content.items.containsKey(i.trim())) included.add(i.trim());
                }
        }
    }

    String name(String id) {
        Element it = content.items.get(id);
        String key = it == null ? id : GameXml.attr(it, "nameidentifier", id);
        String n = texts.get("entityname." + key);
        if (n == null && it != null) { String raw = GameXml.attr(it, "name"); if (raw != null && !raw.isBlank()) n = raw; }
        if (n == null) return id;
        if (n.contains("[type]")) {
            // «Генетический материал ([type])»: вид существа — по окончанию идентификатора
            String species = id.startsWith("geneticmaterial") ? id.substring("geneticmaterial".length()) : "";
            String t = species.isEmpty() ? null : texts.get("character." + species);
            n = t != null ? n.replace("[type]", t) : n.replace(" ([type])", "").replace("[type]", "").trim();
        }
        return n;
    }

    void register(CatalogBuilder b) { register(b, Map.of()); }

    /** @param english английские названия предметов (для поиска и вики) */
    void register(CatalogBuilder b, Map<String, String> english) {
        Map<String, Integer> sameName = new HashMap<>();
        for (String id : included) sameName.merge(name(id), 1, Integer::sum);
        for (String id : included) {
            Element it = content.items.get(id);
            String name = name(id);
            String descKey = GameXml.attr(it, "descriptionidentifier", GameXml.attr(it, "nameidentifier", id));
            String desc = texts.get("entitydescription." + descKey);
            if (desc == null) desc = GameXml.attr(it, "description", "");
            ItemCategory category = b.itemCategory(id);
            if (category == null) category = category(id, it);
            String variant = b.itemVariant(id);
            if (variant == null && sameName.get(name) > 1) variant = id;
            CatalogBuilder.ItemSpec s = b.item(id, name, category, desc, variant);
            s.english(english.get(id));
            price(s, it);
            for (Element f : GameXml.children(it, "fabricate")) recipe(s, f);
            b.removeRandomDecon(id);
            Element d = GameXml.child(it, "deconstruct");
            if (d != null) decon(b, s, id, d);
        }
    }

    private ItemCategory category(String id, Element it) {
        Set<String> tags = content.tagsOf(id);
        String file = String.valueOf(content.itemFile.get(id)).replace('\\', '/').toLowerCase(Locale.ROOT);
        if (file.contains("/genetic/") || tags.contains("geneticmaterial") || tags.contains("geneticdevice")) return ItemCategory.GENETIC;
        for (String c : GameXml.list(GameXml.attr(it, "category", ""))) {
            ItemCategory k = CATEGORIES.get(c.toLowerCase(Locale.ROOT));
            if (k != null) return k;
        }
        return ItemCategory.MISC;
    }

    /** Категории игры, у которых есть свой раздел; остальное — «Разное». */
    private static final Map<String, ItemCategory> CATEGORIES = Map.of("weapon", ItemCategory.WEAPON, "medical", ItemCategory.MEDICAL,
            "material", ItemCategory.MATERIAL, "electrical", ItemCategory.ELECTRICAL, "equipment", ItemCategory.EQUIPMENT,
            "diving", ItemCategory.DIVING, "fuel", ItemCategory.FUEL, "alien", ItemCategory.ALIEN);
    private static final Map<String, Skill> SKILLS = Map.of("weapons", Skill.WEAPONS, "mechanical", Skill.MECHANICAL,
            "electrical", Skill.ELECTRICAL, "medical", Skill.MEDICAL, "helm", Skill.HELM);

    /** &lt;Price baseprice sold&gt; с вложенными &lt;Price storeidentifier multiplier sold&gt;; sold магазина по умолчанию — как у родителя. */
    private static void price(CatalogBuilder.ItemSpec s, Element it) {
        Element p = GameXml.child(it, "price");
        if (p == null) return;
        Double base = GameXml.num(p, "baseprice");
        if (base == null) return;
        boolean soldDefault = GameXml.bool(p, "sold", true);
        s.price((int) Math.round(base), false);
        for (Element st : GameXml.children(p, "price")) {
            Store store = store(GameXml.attr(st, "storeidentifier"));
            if (store == null) continue;
            s.store(store, GameXml.num(st, "multiplier", 1.0), GameXml.bool(st, "sold", soldDefault));
        }
    }

    private static Store store(String gameId) {
        if (gameId == null) return null;
        for (Store s : Store.values()) if (s.gameId().equalsIgnoreCase(gameId.trim())) return s;
        return null;
    }

    private void recipe(CatalogBuilder.ItemSpec s, Element f) {
        if (GameXml.bool(f, "hidefornontraitors", false)) return;
        List<String> fabs = new ArrayList<>();
        for (String x : GameXml.list(GameXml.attr(f, "suitablefabricators", "fabricator"))) fabs.add(x.toLowerCase(Locale.ROOT));
        boolean hasIngredients = false;
        for (Element c : GameXml.children(f)) if (GameXml.is(c, "requireditem") || GameXml.is(c, "item")) hasIngredients = true;
        if (!hasIngredients && !fabs.contains("vendingmachine")) return; // пустой рецепт-заглушка (у автомата ингредиентов нет)
        Fabricator fab = fabs.contains("vendingmachine") ? Fabricator.VENDING
                : fabs.contains("medicalfabricator") && fabs.contains("fabricator") ? Fabricator.ANY
                : fabs.contains("medicalfabricator") ? Fabricator.MEDICAL : Fabricator.FABRICATOR;
        CatalogBuilder.RecipeSpec r = s.recipe(fab, GameXml.num(f, "requiredtime", 1.0), (int) GameXml.num(f, "amount", 1),
                GameXml.bool(f, "requiresrecipe", false), (int) GameXml.num(f, "requiredmoney", 0));
        for (Element c : GameXml.children(f)) {
            if (GameXml.is(c, "requiredskill")) {
                Skill sk = SKILLS.get(GameXml.attr(c, "identifier", "").trim().toLowerCase(Locale.ROOT));
                if (sk != null) r.skill(sk, (int) GameXml.num(c, "level", 0));
                continue;
            }
            if (!GameXml.is(c, "requireditem") && !GameXml.is(c, "item")) continue;
            int n = (int) GameXml.num(c, "amount", GameXml.num(c, "count", 1));
            Double min = positive(GameXml.num(c, "mincondition")), max = GameXml.num(c, "maxcondition");
            if (max != null && max >= 1) max = null;
            boolean partial = GameXml.bool(c, "usecondition", false);
            List<String> ids = GameXml.list(GameXml.attr(c, "identifier"));
            if (ids.size() == 1) { r.item(ids.get(0), n, min, max, partial); continue; }
            List<String> options = new ArrayList<>();
            if (ids.size() > 1) options.addAll(ids);
            else for (String t : GameXml.list(GameXml.attr(c, "tag"))) for (String o : content.withTag(t)) if (included.contains(o) && !options.contains(o)) options.add(o);
            if (options.isEmpty()) continue;
            r.tag(tagLabel(c, options), n, options.toArray(new String[0]));
        }
        r.done();
    }

    private String tagLabel(Element c, List<String> options) {
        String d = GameXml.attr(c, "description");
        if (d != null && !d.isBlank()) return texts.get(d, d);
        StringBuilder sb = new StringBuilder(tr("Любой из: "));
        for (int i = 0; i < options.size() && i < 4; i++) { if (i > 0) sb.append(", "); sb.append(name(options.get(i))); }
        if (options.size() > 4) sb.append("…");
        return sb.toString();
    }

    private static void decon(CatalogBuilder b, CatalogBuilder.ItemSpec s, String id, Element d) {
        boolean random = GameXml.bool(d, "chooserandom", false);
        String station = GameXml.attr(d, "requireddeconstructor", "");
        boolean genetic = !station.isBlank() && !station.trim().equalsIgnoreCase("deconstructor");
        // одинаковые выходы в файле игры записаны отдельными строками — складываем количество (свойства — от первой)
        Map<String, Element> first = new LinkedHashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        Map<String, Double> weights = new LinkedHashMap<>();
        for (Element o : GameXml.children(d, "item")) {
            String out = GameXml.attr(o, "identifier");
            if (out == null || out.isBlank()) continue;
            out = out.trim();
            first.putIfAbsent(out, o);
            counts.merge(out, random ? 1 : (int) GameXml.num(o, "amount", 1), random ? (a, x) -> a : Integer::sum);
            weights.merge(out, GameXml.num(o, "commonness", 1.0), Double::sum);
        }
        if (first.isEmpty()) return;
        CatalogBuilder.DeconSpec ds = s.decon(GameXml.num(d, "time", 1.0), random, genetic);
        first.forEach((out, o) -> ds.out(out, counts.get(out), positive(GameXml.num(o, "mincondition")), random, GameXml.attr(o, "requiredotheritem") != null));
        ds.done();
        if (random) {
            CatalogBuilder.RandomDeconSpec rnd = b.randomDecon(id, (int) GameXml.num(d, "amount", 1));
            weights.forEach(rnd::weight);
        }
    }

    private static Double positive(Double v) { return v == null || v <= 0 ? null : v; }

    /**
     * Где лежит картинка каждого предмета: &lt;InventoryIcon&gt;, а если его нет — &lt;Sprite&gt;.
     * Путь к текстуре — от папки игры («Content/Items/…»), от папки мода (%ModDir%) или от папки самого XML.
     */
    Map<String, baro.data.ItemIcons.Spec> icons(java.nio.file.Path gameDir) {
        Map<String, baro.data.ItemIcons.Spec> m = new LinkedHashMap<>();
        for (String id : included) {
            Element it = content.items.get(id);
            Element sprite = GameXml.child(it, "inventoryicon");
            if (sprite == null || GameXml.attr(sprite, "texture") == null) sprite = GameXml.child(it, "sprite");
            if (sprite == null) continue;
            String tex = GameXml.attr(sprite, "texture");
            java.nio.file.Path xml = content.itemFile.get(id);
            if (tex == null || tex.isBlank() || xml == null) continue;
            java.nio.file.Path file = texture(gameDir, xml, tex.trim());
            if (file == null) continue;
            int[] r = rect(sprite);
            m.put(id, new baro.data.ItemIcons.Spec(file.toString(), r));
        }
        return m;
    }

    private static java.nio.file.Path texture(java.nio.file.Path gameDir, java.nio.file.Path xml, String tex) {
        String t = tex.replace('\\', '/');
        if (t.toLowerCase(Locale.ROOT).contains("%moddir")) {
            java.nio.file.Path mod = xml.getParent();
            while (mod != null && !java.nio.file.Files.isRegularFile(mod.resolve("filelist.xml"))) mod = mod.getParent();
            if (mod == null) return null;
            t = t.replaceAll("(?i)%ModDir(:[^%]*)?%/?", java.util.regex.Matcher.quoteReplacement(mod.toString().replace('\\', '/') + "/"));
        }
        java.nio.file.Path p = java.nio.file.Paths.get(t);
        if (p.isAbsolute()) return java.nio.file.Files.isRegularFile(p) ? p : null;
        for (java.nio.file.Path base : new java.nio.file.Path[]{gameDir, xml.getParent()}) {
            if (base == null) continue;
            java.nio.file.Path f = base.resolve(t);
            if (java.nio.file.Files.isRegularFile(f)) return f;
        }
        return null;
    }

    /** Прямоугольник на текстуре {x, y, w, h} или null — вся картинка. */
    private static int[] rect(Element sprite) {
        String sr = GameXml.attr(sprite, "sourcerect");
        if (sr != null) {
            String[] p = sr.split(",");
            if (p.length == 4) try {
                int[] r = new int[4];
                for (int i = 0; i < 4; i++) r[i] = (int) Math.round(Double.parseDouble(p[i].trim()));
                return r[2] > 0 && r[3] > 0 ? r : null;
            } catch (NumberFormatException ignored) { }
        }
        String idx = GameXml.attr(sprite, "sheetindex"), size = GameXml.attr(sprite, "sheetelementsize");
        if (idx != null && size != null) {
            String[] i = idx.split(","), z = size.split(",");
            if (i.length == 2 && z.length == 2) try {
                int w = (int) Double.parseDouble(z[0].trim()), h = (int) Double.parseDouble(z[1].trim());
                return new int[]{(int) Double.parseDouble(i[0].trim()) * w, (int) Double.parseDouble(i[1].trim()) * h, w, h};
            } catch (NumberFormatException ignored) { }
        }
        return null;
    }
}