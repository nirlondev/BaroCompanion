package baro.data.gamefiles;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.nio.file.Path;
import java.util.*;

/**
 * Сырые описания предметов и недугов из всех пакетов. Ваниль: первое описание идентификатора побеждает
 * (как и раньше в генераторе); моды и блоки &lt;Override&gt; заменяют прежние. Варианты (variantof)
 * собираются из родителя: атрибуты и дочерние блоки варианта перекрывают родительские.
 */
final class GameContent {
    /** Недуг: тип, бафф ли он, действует ли на отдельную часть тела. */
    static final class Affliction {
        final String id, type; final boolean buff, limbSpecific; final Element element;
        Affliction(String id, String type, boolean buff, boolean limbSpecific, Element element) {
            this.id = id; this.type = type; this.buff = buff; this.limbSpecific = limbSpecific; this.element = element;
        }
    }

    final Map<String, Element> items = new LinkedHashMap<>();
    /** Файл, откуда взят предмет (для категорий вроде «Генетика»). */
    final Map<String, Path> itemFile = new HashMap<>();
    final Map<String, Affliction> afflictions = new LinkedHashMap<>();
    private final Map<String, Set<String>> tags = new LinkedHashMap<>();

    GameContent(GameInstall game) {
        Set<Path> vanilla = new HashSet<>(game.vanillaFiles("Item"));
        Map<String, Element> raw = new LinkedHashMap<>();
        for (Path f : game.files("Item")) {
            if (f.toString().replace('\\', '/').contains("/Legacy/")) continue;
            Element root = GameXml.root(f);
            if (root == null) continue;
            boolean isVanilla = vanilla.contains(f);
            List<Element[]> cands = new ArrayList<>(); // {элемент, признак override}
            if (GameXml.attr(root, "identifier") != null) cands.add(new Element[]{root, null});
            else for (Element c : GameXml.children(root)) {
                if (GameXml.is(c, "override")) for (Element o : GameXml.children(c)) cands.add(new Element[]{o, o});
                else cands.add(new Element[]{c, null});
            }
            for (Element[] c : cands) {
                String id = GameXml.attr(c[0], "identifier");
                if (id == null || id.isBlank()) continue;
                id = id.trim();
                if (isVanilla && c[1] == null && raw.containsKey(id)) continue;
                raw.put(id, c[0]);
                itemFile.put(id, f);
            }
        }
        for (String id : raw.keySet()) {
            Element e = resolve(id, raw, 0);
            if (e != null) items.put(id, e);
        }
        for (Map.Entry<String, Element> e : items.entrySet()) {
            Set<String> t = new HashSet<>();
            for (String s : GameXml.list(GameXml.attr(e.getValue(), "tags"))) t.add(s.toLowerCase(Locale.ROOT));
            t.add(e.getKey().toLowerCase(Locale.ROOT));
            tags.put(e.getKey(), t);
        }
        for (Path f : game.files("Afflictions")) {
            Element root = GameXml.root(f);
            if (root == null) continue;
            List<Element> cands = new ArrayList<>();
            for (Element c : GameXml.children(root)) {
                if (GameXml.is(c, "override")) cands.addAll(GameXml.children(c)); else cands.add(c);
            }
            for (Element a : cands) {
                String id = GameXml.attr(a, "identifier");
                if (id == null || id.isBlank()) continue;
                afflictions.put(id.trim(), new Affliction(id.trim(), GameXml.attr(a, "type", ""),
                        GameXml.bool(a, "isbuff", false), GameXml.bool(a, "limbspecific", false), a));
            }
        }
    }

    /** Предметы, у которых есть тег (или сам идентификатор равен тегу). */
    List<String> withTag(String tag) {
        String t = tag.trim().toLowerCase(Locale.ROOT);
        List<String> l = new ArrayList<>();
        for (Map.Entry<String, Set<String>> e : tags.entrySet()) if (e.getValue().contains(t)) l.add(e.getKey());
        return l;
    }

    Set<String> tagsOf(String id) { return tags.getOrDefault(id, Collections.emptySet()); }

    private static Element resolve(String id, Map<String, Element> raw, int depth) {
        Element e = raw.get(id);
        if (e == null) return null;
        String parent = GameXml.attr(e, "variantof");
        if (parent == null || parent.isBlank() || depth > 8 || parent.trim().equals(id)) return e;
        Element p = resolve(parent.trim(), raw, depth + 1);
        if (p == null) return e;
        return merge(p, e);
    }

    /** Вариант = копия родителя, где атрибуты и одноимённые дочерние блоки заменены вариантом. */
    private static Element merge(Element parent, Element variant) {
        Element m = (Element) parent.cloneNode(true);
        NamedNodeMap attrs = variant.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr a = (Attr) attrs.item(i);
            // атрибут в другом регистре у родителя — убрать, чтобы не было двух значений
            NamedNodeMap pa = m.getAttributes();
            for (int k = pa.getLength() - 1; k >= 0; k--) {
                Node x = pa.item(k);
                if (x.getNodeName().equalsIgnoreCase(a.getName()) && !x.getNodeName().equals(a.getName())) m.removeAttribute(x.getNodeName());
            }
            m.setAttribute(a.getName(), a.getValue());
        }
        m.removeAttribute("variantof");
        Set<String> replaced = new HashSet<>();
        for (Element c : GameXml.children(variant)) {
            String tag = c.getTagName().toLowerCase(Locale.ROOT);
            if (replaced.add(tag)) for (Element old : GameXml.children(m)) if (old.getTagName().equalsIgnoreCase(tag)) m.removeChild(old);
            m.appendChild(m.getOwnerDocument().importNode(c, true));
        }
        return m;
    }
}