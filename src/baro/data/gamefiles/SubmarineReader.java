package baro.data.gamefiles;


import static baro.Tr.tr;
import baro.model.Submarine;
import org.w3c.dom.Element;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Подлодки игрока (.sub — gzip с XML) и улучшения подлодок (UpgradeModules). Файлы подлодок большие,
 * поэтому они читаются потоково (StAX): нужны только атрибуты корня и счёт предметов по идентификаторам.
 */
public final class SubmarineReader {
    /** Подлодки и улучшения. */
    public record Result(List<Submarine> submarines, List<Submarine.Upgrade> upgrades, Map<String, String> categoryNames,
                         List<Submarine.Fuel> fuels, List<Submarine.Reactor> reactors, List<baro.model.Device> wiring) { }

    /** Компоненты, по которым видно, что за оборудование: тег компонента в XML предмета → подпись. */
    private static final Map<String, String> EQUIPMENT = new LinkedHashMap<>();
    static {
        EQUIPMENT.put("reactor", tr("Реактор")); EQUIPMENT.put("engine", tr("Двигатель")); EQUIPMENT.put("oxygengenerator", tr("Генератор кислорода"));
        EQUIPMENT.put("fabricator", tr("Фабрикатор")); EQUIPMENT.put("deconstructor", tr("Деконструктор")); EQUIPMENT.put("pump", tr("Насос"));
        EQUIPMENT.put("powercontainer", tr("Батарея / конденсатор")); EQUIPMENT.put("sonar", tr("Сонар")); EQUIPMENT.put("searchlight", tr("Прожектор"));
    }

    private SubmarineReader() {}

    public static Result read(GameInstall game, String language) {
        long t0 = System.currentTimeMillis();
        GameTexts texts = new GameTexts(game, language);
        GameContent content = new GameContent(game);
        Map<String, Submarine.Reactor> reactors = new LinkedHashMap<>();
        Map<String, Double> heat = new LinkedHashMap<>();
        reactors(content, texts, reactors, heat);
        List<Submarine.Fuel> fuels = new ArrayList<>();
        for (Map.Entry<String, Element> e : content.items.entrySet()) {
            Set<String> tags = content.tagsOf(e.getKey());
            Double h = heat.get(e.getKey());
            if (h == null) for (String t : tags) if (heat.containsKey(t) && !t.equals(e.getKey())) { h = heat.get(t); break; }
            if (h == null || h <= 0 || !tags.contains("reactorfuel")) continue;
            fuels.add(new Submarine.Fuel(e.getKey(), texts.get("entityname." + e.getKey(), e.getKey()), h, GameXml.num(e.getValue(), "health", 100)));
        }
        List<Submarine> subs = new ArrayList<>();
        for (Path f : game.files("Submarine")) {
            try { Submarine s = submarine(f, game.modOf(f), content, texts, reactors); if (s != null) subs.add(s); }
            catch (Exception e) { System.err.println(tr("Подлодка не прочитана: ") + f + " — " + e); }
        }
        subs.sort(Comparator.comparing((Submarine s) -> s.tier() == 0 ? 9 : s.tier()).thenComparingInt(Submarine::price));
        Map<String, String> cats = new LinkedHashMap<>();
        List<Submarine.Upgrade> ups = upgrades(game, texts, cats);
        System.err.println(tr("Подлодки: ") + subs.size() + tr(", улучшений ") + ups.size() + " (" + (System.currentTimeMillis() - t0) + tr(" мс)"));
        List<baro.model.Device> wiring = WiringReader.read(content, texts, game);
        return new Result(subs, ups, cats, fuels, new ArrayList<>(reactors.values()), wiring);
    }

    /** Реакторы (компонент Reactor) и тепло каждого топлива по их ItemContainer → Containable → StatusEffect AvailableFuel. */
    private static void reactors(GameContent content, GameTexts texts, Map<String, Submarine.Reactor> out, Map<String, Double> heat) {
        for (Map.Entry<String, Element> e : content.items.entrySet()) {
            Element r = GameXml.child(e.getValue(), "reactor");
            if (r == null) continue;
            out.put(e.getKey(), new Submarine.Reactor(e.getKey(), texts.get("entityname." + e.getKey(), e.getKey()),
                    GameXml.num(r, "maxpoweroutput", 0), GameXml.num(r, "fuelconsumptionrate", 0.2)));
            Element cont = GameXml.child(e.getValue(), "itemcontainer");
            if (cont == null) continue;
            for (Element c : GameXml.children(cont, "containable"))
                for (Element se : GameXml.children(c, "statuseffect")) {
                    Double v = GameXml.num(se, "availablefuel");
                    if (v == null) continue;
                    for (String id : GameXml.list(GameXml.attr(c, "items"))) heat.putIfAbsent(id.toLowerCase(Locale.ROOT), v);
                }
        }
    }

    private static Submarine submarine(Path file, String mod, GameContent content, GameTexts texts, Map<String, Submarine.Reactor> reactors) throws Exception {
        XMLInputFactory f = XMLInputFactory.newFactory();
        f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        Map<String, String> root = new HashMap<>();
        Map<String, Integer> ids = new HashMap<>();
        try (InputStream in = new GZIPInputStream(Files.newInputStream(file), 1 << 16)) {
            XMLStreamReader r = f.createXMLStreamReader(in);
            int depth = 0;
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    depth++;
                    if (depth == 1) for (int i = 0; i < r.getAttributeCount(); i++) root.put(r.getAttributeLocalName(i).toLowerCase(Locale.ROOT), r.getAttributeValue(i));
                    else if (depth == 2 && r.getLocalName().equalsIgnoreCase("item")) {
                        String id = null;
                        for (int i = 0; i < r.getAttributeCount(); i++) if (r.getAttributeLocalName(i).equalsIgnoreCase("identifier")) id = r.getAttributeValue(i);
                        if (id != null) ids.merge(id.trim(), 1, Integer::sum);
                    }
                } else if (ev == XMLStreamConstants.END_ELEMENT) depth--;
            }
            r.close();
        }
        String type = root.getOrDefault("type", "Player");
        String tags = root.getOrDefault("tags", "");
        if (!type.equalsIgnoreCase("player") || tags.toLowerCase(Locale.ROOT).contains("hideinmenus")) return null;
        String id = root.getOrDefault("name", file.getFileName().toString().replaceFirst("(?i)\\.sub$", ""));
        String key = id.toLowerCase(Locale.ROOT);
        String name = texts.get("submarine.name." + key, id);
        String desc = texts.get("submarine.description." + key, root.getOrDefault("description", ""));
        boolean shuttle = tags.toLowerCase(Locale.ROOT).contains("shuttle");
        String cls = root.getOrDefault("class", "").toLowerCase(Locale.ROOT);
        if (cls.equals("none") || cls.equals("undefined")) cls = "";
        String className = shuttle ? texts.get("shuttle", tr("Челнок")) : cls.isEmpty() ? "" : texts.get("submarineclass." + cls, cls);
        double[] dim = pair(root.get("dimensions"));
        Map<String, Integer> turrets = new TreeMap<>(), equipment = new LinkedHashMap<>();
        Map<String, Double> power = new LinkedHashMap<>(), peak = new LinkedHashMap<>();
        Submarine.Reactor[] reactor = new Submarine.Reactor[1];
        for (String label : EQUIPMENT.values()) equipment.put(label, 0);
        ids.forEach((itemId, n) -> {
            Element it = content.items.get(itemId);
            if (it == null) return;
            Submarine.Reactor rr = reactors.get(itemId);
            if (rr != null && (reactor[0] == null || rr.maxPower() > reactor[0].maxPower())) reactor[0] = rr;
            double use = 0;
            for (Element comp : GameXml.children(it)) use += Math.max(0, GameXml.num(comp, "powerconsumption", 0));
            String low = itemId.toLowerCase(Locale.ROOT);
            // орудия, разрядная катушка и прожекторы берут энергию только при зарядке или включении
            boolean burst = GameXml.child(it, "turret") != null || GameXml.child(it, "electricaldischarger") != null || low.contains("searchlight");
            if (use > 0) (burst ? peak : power).merge(texts.get("entityname." + GameXml.attr(it, "nameidentifier", itemId), itemId), use * n, Double::sum);
            if (low.contains("hardpoint")) return; // пустое гнездо под орудие
            if (low.contains("searchlight")) { equipment.merge(tr("Прожектор"), n, Integer::sum); return; }
            if (GameXml.child(it, "turret") != null) { turrets.merge(texts.get("entityname." + GameXml.attr(it, "nameidentifier", itemId), itemId), n, Integer::sum); return; }
            for (Map.Entry<String, String> e : EQUIPMENT.entrySet())
                if (GameXml.child(it, e.getKey()) != null) { equipment.merge(e.getValue(), n, Integer::sum); break; }
        });
        equipment.values().removeIf(v -> v == 0);
        String exp = root.getOrDefault("recommendedcrewexperience", "");
        return new Submarine(id, name, desc, cls, className, (int) num(root.get("tier"), 0), (int) num(root.get("price"), 0),
                (int) num(root.get("recommendedcrewsizemin"), 0), (int) num(root.get("recommendedcrewsizemax"), 0),
                exp.isEmpty() ? "" : texts.get(exp, exp), dim[0] / 100, dim[1] / 100, (int) num(root.get("cargocapacity"), 0),
                turrets, equipment, mod, preview(root.get("previewimage")), reactor[0], sortedDesc(power), sortedDesc(peak));
    }

    private static Map<String, Double> sortedDesc(Map<String, Double> m) {
        Map<String, Double> r = new LinkedHashMap<>();
        m.entrySet().stream().sorted((a, b) -> Double.compare(b.getValue(), a.getValue())).forEach(e -> r.put(e.getKey(), e.getValue()));
        return r;
    }

    private static BufferedImage preview(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64.trim())));
            if (img == null) return null;
            int w = Math.min(520, img.getWidth()), h = Math.max(1, img.getHeight() * w / img.getWidth());
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, w, h, null);
            g.dispose();
            return out;
        } catch (Exception e) { return null; }
    }

    private static List<Submarine.Upgrade> upgrades(GameInstall game, GameTexts texts, Map<String, String> categoryNames) {
        Map<String, Submarine.Upgrade> m = new LinkedHashMap<>();
        for (Path f : game.files("UpgradeModules")) {
            Element root = GameXml.root(f);
            if (root == null) continue;
            List<Element> all = new ArrayList<>();
            for (Element c : GameXml.children(root)) if (GameXml.is(c, "override")) all.addAll(GameXml.children(c)); else all.add(c);
            for (Element e : all) {
                String id = GameXml.attr(e, "identifier");
                if (id == null || id.isBlank()) continue;
                if (GameXml.is(e, "upgradecategory")) {
                    String key = GameXml.attr(e, "nameidentifier", "upgradecategory." + id);
                    categoryNames.put(id, texts.get(key, texts.get("upgradecategory." + id, id)));
                    continue;
                }
                if (!GameXml.is(e, "upgrademodule")) continue;
                Element price = GameXml.child(e, "price");
                Map<Integer, Integer> byTier = new HashMap<>();
                Map<String, Integer> byClass = new HashMap<>();
                for (Element ml : GameXml.children(e, "maxlevel")) {
                    String lv = GameXml.attr(ml, "level", "0").trim();
                    int v;
                    try { v = Integer.parseInt(lv.replace("+", "")); } catch (NumberFormatException ex) { continue; }
                    if (GameXml.attr(ml, "tier") != null) byTier.put((int) GameXml.num(ml, "tier", 0), v);
                    else if (GameXml.attr(ml, "class") != null) byClass.put(GameXml.attr(ml, "class").toLowerCase(Locale.ROOT), v);
                }
                List<String> res = new ArrayList<>();
                for (Element rc : GameXml.children(e, "resourcecost")) {
                    String item = GameXml.attr(rc, "item", "");
                    String name = texts.get("entityname." + item, item);
                    res.add(name + " ×" + (int) GameXml.num(rc, "amount", 1) + (GameXml.attr(rc, "levels") != null ? tr(" (уровни ") + GameXml.attr(rc, "levels").replace("-", "–") + ")" : ""));
                }
                List<String> cats = GameXml.list(GameXml.attr(e, "categories"));
                m.put(id, new Submarine.Upgrade(id, texts.get("upgradename." + id, GameXml.attr(e, "name", id)),
                        texts.get("upgradedescription." + id, GameXml.attr(e, "description", "")), cats.isEmpty() ? "" : cats.get(0),
                        (int) GameXml.num(e, "maxlevel", 1), price == null ? 0 : (int) GameXml.num(price, "baseprice", 0),
                        percent(price, "increaselow"), percent(price, "increasehigh"), byTier, byClass, res));
            }
        }
        return new ArrayList<>(m.values());
    }

    private static double percent(Element price, String attr) {
        if (price == null) return 0;
        String v = GameXml.attr(price, attr, "0").replace("%", "").replace("+", "").trim();
        try { return Double.parseDouble(v); } catch (NumberFormatException e) { return 0; }
    }

    private static double num(String v, double def) {
        if (v == null) return def;
        try { return Double.parseDouble(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    private static double[] pair(String v) {
        if (v == null) return new double[]{0, 0};
        String[] p = v.split(",");
        return p.length == 2 ? new double[]{num(p[0], 0), num(p[1], 0)} : new double[]{0, 0};
    }
}
