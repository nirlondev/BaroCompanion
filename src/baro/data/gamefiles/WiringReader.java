package baro.data.gamefiles;

import baro.model.Device;
import org.w3c.dom.Element;

import java.util.*;

/** Всё, у чего есть панель проводки (ConnectionPanel): входы, выходы и компоненты — для вкладки «Проводка». */
final class WiringReader {
    private static final Set<String> LOGIC = Set.of("andcomponent", "orcomponent", "xorcomponent", "notcomponent", "equalscomponent",
            "greatercomponent", "memorycomponent", "delaycomponent", "relaycomponent", "oscillatorcomponent", "signalcheckcomponent",
            "regexfindcomponent", "concatcomponent", "colorcomponent", "multiplexercomponent", "demultiplexercomponent", "wificomponent");
    private static final Set<String> MATH = Set.of("addercomponent", "subtractcomponent", "multiplycomponent", "dividecomponent",
            "exponentiationcomponent", "modulocomponent", "functioncomponent", "trigonometricfunctioncomponent");
    private static final Set<String> SENSOR = Set.of("motionsensor", "waterdetector", "oxygendetector", "smokedetector");
    private static final Set<String> MACHINE = Set.of("reactor", "engine", "pump", "oxygengenerator", "sonar", "steering", "powercontainer",
            "powertransfer", "turret", "door", "fabricator", "deconstructor", "dockingport", "electricaldischarger", "minimap");

    private WiringReader() {}

    static List<Device> read(GameContent content, GameTexts texts, GameInstall game) {
        List<Device> l = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (Map.Entry<String, Element> e : content.items.entrySet()) {
            Element it = e.getValue();
            Element panel = GameXml.child(it, "connectionpanel");
            if (panel == null) continue;
            String cat = GameXml.attr(it, "category", "").toLowerCase(Locale.ROOT);
            if (cat.contains("legacy") || GameXml.bool(it, "hideinmenus", false)) continue;
            String id = e.getKey();
            String name = texts.get("entityname." + GameXml.attr(it, "nameidentifier", id));
            if (name == null) { String raw = GameXml.attr(it, "name"); name = raw != null && !raw.isBlank() ? raw : id; }
            List<Device.Port> in = new ArrayList<>(), out = new ArrayList<>();
            for (Element c : GameXml.children(panel)) {
                boolean input = GameXml.is(c, "input");
                if (!input && !GameXml.is(c, "output")) continue;
                String n = GameXml.attr(c, "name", "");
                if (n.isBlank()) continue;
                (input ? in : out).add(new Device.Port(n, label(texts, GameXml.attr(c, "displayname"), n)));
            }
            if (in.isEmpty() && out.isEmpty()) continue;
            List<String> comps = new ArrayList<>();
            for (Element c : GameXml.children(it)) {
                String t = c.getTagName();
                String low = t.toLowerCase(Locale.ROOT);
                if (LOGIC.contains(low) || MATH.contains(low) || SENSOR.contains(low) || MACHINE.contains(low) || low.equals("lightcomponent")
                        || low.equals("terminal") || low.equals("buttonterminal") || low.equals("itemlabel") || low.equals("customterminal"))
                    if (!comps.contains(t)) comps.add(t);
            }
            String group = group(comps, cat);
            String desc = texts.get("entitydescription." + GameXml.attr(it, "descriptionidentifier", GameXml.attr(it, "nameidentifier", id)));
            if (desc == null) desc = GameXml.attr(it, "description", "");
            // одинаковые по названию варианты (лампы разной формы) — одной записью
            if (!names.add(name + "|" + in.size() + "|" + out.size())) continue;
            l.add(new Device(id, name, desc, group, comps, in, out, game.modOf(content.itemFile.get(id))));
        }
        l.sort(Comparator.comparing(Device::name, String.CASE_INSENSITIVE_ORDER));
        return l;
    }

    private static String group(List<String> comps, String cat) {
        for (String c : comps) { String k = c.toLowerCase(Locale.ROOT); if (LOGIC.contains(k)) return "LOGIC"; }
        for (String c : comps) { String k = c.toLowerCase(Locale.ROOT); if (MATH.contains(k)) return "MATH"; }
        for (String c : comps) { String k = c.toLowerCase(Locale.ROOT); if (SENSOR.contains(k)) return "SENSOR"; }
        for (String c : comps) { String k = c.toLowerCase(Locale.ROOT); if (MACHINE.contains(k)) return "MACHINE"; }
        if (cat.contains("machine")) return "MACHINE";
        for (String c : comps) { String k = c.toLowerCase(Locale.ROOT); if (k.equals("lightcomponent") || k.contains("terminal")) return "OUTPUT"; }
        return "OTHER";
    }

    /** «connection.signalinx~[num]=2» → текст ключа с подстановкой: «сигнал_в_2». */
    private static String label(GameTexts texts, String displayName, String fallback) {
        if (displayName == null || displayName.isBlank()) return fallback;
        String[] parts = displayName.split("~");
        String t = texts.get(parts[0].trim());
        if (t == null) return fallback;
        for (int i = 1; i < parts.length; i++) {
            int eq = parts[i].indexOf('=');
            if (eq > 0) t = t.replace(parts[i].substring(0, eq), parts[i].substring(eq + 1));
        }
        return t;
    }
}
