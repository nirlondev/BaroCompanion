package baro.data.gamefiles;


import static baro.Tr.tr;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Чтение XML игры (файлы и сейвы): имена тегов и атрибутов в файлах Barotrauma пишутся в любом регистре. */
public final class GameXml {
    private GameXml() {}

    /** Корневой элемент файла или null, если файл не читается. */
    static Element root(Path file) {
        try { return builder().parse(file.toFile()).getDocumentElement(); }
        catch (Exception e) {
            System.err.println(tr("Файл игры не прочитан: ") + file + " — " + e.getMessage());
            return null;
        }
    }

    /** Корневой элемент XML из строки. */
    public static Element parse(String xml) throws IOException {
        try { return builder().parse(new InputSource(new StringReader(xml))).getDocumentElement(); }
        catch (Exception e) { throw new IOException(e.getMessage(), e); }
    }

    private static DocumentBuilder builder() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(false);
        f.setExpandEntityReferences(false);
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        try { f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); } catch (Exception ignored) { }
        DocumentBuilder b = f.newDocumentBuilder();
        b.setErrorHandler(null);
        return b;
    }

    static String attr(Element e, String name) { return attr(e, name, null); }

    public static String attr(Element e, String name, String def) {
        NamedNodeMap m = e.getAttributes();
        for (int i = 0; i < m.getLength(); i++) {
            Node a = m.item(i);
            if (a.getNodeName().equalsIgnoreCase(name)) return a.getNodeValue();
        }
        return def;
    }

    static boolean bool(Element e, String name, boolean def) {
        String v = attr(e, name);
        return v == null ? def : v.trim().equalsIgnoreCase("true");
    }

    static Double num(Element e, String name) {
        String v = attr(e, name);
        if (v == null) return null;
        try { return Double.parseDouble(v.trim().replace(',', '.')); } catch (NumberFormatException ex) { return null; }
    }

    public static double num(Element e, String name, double def) { Double v = num(e, name); return v == null ? def : v; }

    public static boolean is(Element e, String tag) { return e.getTagName().equalsIgnoreCase(tag); }

    /** Прямые дочерние элементы. */
    public static List<Element> children(Element e) {
        List<Element> l = new ArrayList<>();
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) if (n instanceof Element c) l.add(c);
        return l;
    }

    static List<Element> children(Element e, String tag) {
        List<Element> l = new ArrayList<>();
        for (Element c : children(e)) if (is(c, tag)) l.add(c);
        return l;
    }

    public static Element child(Element e, String tag) {
        for (Element c : children(e)) if (is(c, tag)) return c;
        return null;
    }

    /** Все вложенные элементы (без самого e), в порядке документа. */
    public static List<Element> descendants(Element e) {
        List<Element> l = new ArrayList<>();
        collect(e, l);
        return l;
    }

    private static void collect(Element e, List<Element> out) {
        for (Element c : children(e)) { out.add(c); collect(c, out); }
    }

    static List<String> list(String csv) {
        List<String> l = new ArrayList<>();
        if (csv == null) return l;
        for (String s : csv.split(",")) { s = s.trim(); if (!s.isEmpty()) l.add(s); }
        return l;
    }
}
