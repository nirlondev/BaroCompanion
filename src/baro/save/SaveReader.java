package baro.save;


import static baro.Tr.tr;
import baro.data.gamefiles.GameXml;
import baro.model.Store;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

import static baro.data.gamefiles.GameXml.*;

/**
 * Чтение сейвов Barotrauma (*.save — gzip-архив, внутри gamesession.xml и файлы подлодки).
 * Сейвы ищутся в %LOCALAPPDATA%\Daedalic Entertainment GmbH\Barotrauma (одиночная игра) и в его папке Multiplayer.
 */
public final class SaveReader {
    private static final int MAX_BYTES = 64 << 20;

    private SaveReader() {}

    /** Папки, где игра хранит сейвы. @param custom папка из настроек (save.dir) или пусто */
    public static List<Path> saveDirs(String custom) {
        List<Path> dirs = new ArrayList<>();
        if (custom != null && !custom.isBlank()) dirs.add(Paths.get(custom.trim()));
        List<Path> roots = new ArrayList<>();
        String local = System.getenv("LOCALAPPDATA");
        if (local != null) roots.add(Paths.get(local, "Daedalic Entertainment GmbH", "Barotrauma"));
        roots.add(Paths.get(System.getProperty("user.home"), ".local/share/Daedalic Entertainment GmbH/Barotrauma"));
        roots.add(Paths.get(System.getProperty("user.home"), "Library/Application Support/Daedalic Entertainment GmbH/Barotrauma"));
        for (Path r : roots) { dirs.add(r); dirs.add(r.resolve("Multiplayer")); }
        return dirs;
    }

    /** Все сейвы, самые свежие первыми. */
    public static List<Path> saveFiles(String custom) {
        List<Path> files = new ArrayList<>();
        for (Path d : saveDirs(custom)) {
            if (!Files.isDirectory(d)) continue;
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(d, "*.save")) { for (Path p : ds) if (Files.isRegularFile(p)) files.add(p); }
            catch (IOException e) { System.err.println(tr("Папка сейвов не читается: ") + d + " — " + e); }
        }
        files.sort(Comparator.comparingLong(SaveReader::modified).reversed());
        return files;
    }

    public static long modified(Path p) {
        try { return Files.getLastModifiedTime(p).toMillis(); } catch (IOException e) { return 0; }
    }

    /** Самый свежий сейв, который удалось прочитать. */
    public static Optional<SaveSnapshot> latest(String custom) {
        for (Path p : saveFiles(custom)) {
            try { return Optional.of(read(p)); } catch (Exception e) { System.err.println(tr("Сейв не прочитан: ") + p + " — " + e.getMessage()); }
        }
        return Optional.empty();
    }

    public static SaveSnapshot read(Path file) throws IOException {
        byte[] raw;
        try (InputStream in = new GZIPInputStream(Files.newInputStream(file))) { raw = in.readNBytes(MAX_BYTES + 1); }
        if (raw.length > MAX_BYTES) throw new IOException(tr("сейв слишком большой"));
        SaveSnapshot snap = parse(file, new String(raw, StandardCharsets.UTF_8));
        try { inventory(snap, raw); } catch (Exception e) { System.err.println(tr("Запасы на борту не прочитаны: ") + file + " — " + e); }
        return snap;
    }

    /**
     * Запасы: предметы в шкафах и контейнерах подлодки (.sub внутри архива сейва) и в инвентаре экипажа.
     * Архив — подряд записи «длина имени, имя в UTF-16, длина, данные»; подлодка внутри ещё раз сжата gzip.
     * На подлодке все предметы лежат списком, а содержимое контейнера — их ID в атрибуте contained.
     */
    static void inventory(SaveSnapshot snap, byte[] raw) throws IOException {
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(raw).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        Map<String, Integer> inv = new HashMap<>();
        String subName = "";
        while (buf.remaining() >= 8) {
            int nameLen = buf.getInt();
            if (nameLen < 0 || nameLen > 1024 || buf.remaining() < nameLen * 2 + 4) break;
            byte[] nb = new byte[nameLen * 2];
            buf.get(nb);
            String name = new String(nb, StandardCharsets.UTF_16LE);
            int len = buf.getInt();
            if (len < 0 || len > buf.remaining()) break;
            byte[] data = new byte[len];
            buf.get(data);
            if (name.toLowerCase(Locale.ROOT).endsWith(".sub")) {
                if (data.length > 2 && (data[0] & 0xff) == 0x1f && (data[1] & 0xff) == 0x8b)
                    try (InputStream in = new GZIPInputStream(new java.io.ByteArrayInputStream(data))) { data = in.readNBytes(MAX_BYTES); }
                subItems(new String(data, StandardCharsets.UTF_8), inv);
                if (subName.isEmpty()) subName = name.replaceFirst("(?i)\\.sub$", "");
            } else if (name.equalsIgnoreCase("gamesession.xml")) crewItems(new String(data, StandardCharsets.UTF_8), inv);
        }
        snap.setInventory(subName, inv);
    }

    private static final java.util.regex.Pattern ITEM = java.util.regex.Pattern.compile("<Item\\s[^>]*?identifier=\"([^\"]+)\"[^>]*?\\sID=\"(\\d+)\"", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern CONTAINED = java.util.regex.Pattern.compile("\\scontained=\"([^\"]*)\"", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern ID_ATTR = java.util.regex.Pattern.compile("<Item\\s[^>]*?identifier=\"([^\"]+)\"", java.util.regex.Pattern.CASE_INSENSITIVE);

    /** Предметы подлодки, которые лежат в контейнерах (а не встроенное оборудование вроде насосов и дверей). */
    private static void subItems(String xml, Map<String, Integer> inv) {
        Map<String, String> byId = new HashMap<>();
        java.util.regex.Matcher m = ITEM.matcher(xml);
        while (m.find()) byId.put(m.group(2), m.group(1));
        m = CONTAINED.matcher(xml);
        while (m.find())
            for (String id : m.group(1).split("[,;]")) {
                String it = byId.get(id.trim());
                if (it != null) inv.merge(it, 1, Integer::sum);
            }
    }

    /** Предметы в инвентаре экипажа (раздел crew в gamesession.xml). */
    private static void crewItems(String xml, Map<String, Integer> inv) {
        int a = indexOfIgnoreCase(xml, "<crew", 0);
        if (a < 0) return;
        int b = indexOfIgnoreCase(xml, "</crew>", a);
        java.util.regex.Matcher m = ID_ATTR.matcher(b < 0 ? xml.substring(a) : xml.substring(a, b));
        while (m.find()) inv.merge(m.group(1), 1, Integer::sum);
    }

    /** Разбор содержимого архива: из него берётся только gamesession.xml. */
    static SaveSnapshot parse(Path file, String archive) throws IOException {
        int start = indexOfIgnoreCase(archive, "<gamesession", 0);
        if (start < 0) throw new IOException(tr("в сейве нет gamesession.xml"));
        int end = indexOfIgnoreCase(archive, "</gamesession>", start);
        if (end < 0) throw new IOException(tr("gamesession.xml обрезан"));
        Element gs;
        try { gs = GameXml.parse(archive.substring(start, end + "</gamesession>".length())); }
        catch (IOException e) { throw new IOException(tr("gamesession.xml не читается: ") + e.getMessage(), e); }

        Element campaign = null;
        for (Element c : children(gs)) if (c.getTagName().toLowerCase(Locale.ROOT).endsWith("campaign")) { campaign = c; break; }
        if (campaign == null) throw new IOException(tr("в сейве нет кампании"));

        Element cs = child(campaign, "campaignsettings");
        String preset = cs == null ? "" : attr(cs, "presetname", "");
        float shop = cs == null ? 0 : (float) num(cs, "shoppricemultiplier", 0);
        float reward = cs == null ? 0 : (float) num(cs, "missionrewardmultiplier", 0);

        Map<String, Double> reps = new LinkedHashMap<>();
        Element meta = child(campaign, "metadata");
        if (meta != null) for (Element d : children(meta)) {
            String key = attr(d, "key", "");
            if (key.toLowerCase(Locale.ROOT).startsWith("reputation.faction.")) {
                double v = num(d, "value", Double.NaN);
                if (!Double.isNaN(v)) reps.put(key.substring("reputation.faction.".length()), Math.max(-100, Math.min(100, v)));
            }
        }

        Element map = child(campaign, "map");
        Element loc = null;
        if (map != null) {
            String cur = attr(map, "currentlocation", "");
            for (Element l : children(map)) if (is(l, "location") && cur.equals(attr(l, "i", null))) { loc = l; break; }
        }
        Map<Store, SaveSnapshot.StoreState> stores = new EnumMap<>(Store.class);
        String locName = "", locType = "", faction = "";
        if (loc != null) {
            locName = attr(loc, "name", "");
            locType = attr(loc, "type", "");
            faction = attr(loc, "faction", "");
            for (Element s : descendants(loc)) {
                if (!is(s, "store")) continue;
                Store store = store(attr(s, "identifier", ""));
                if (store == null) continue;
                Set<String> req = new LinkedHashSet<>(), spec = new LinkedHashSet<>();
                Map<String, Integer> stock = new LinkedHashMap<>();
                for (Element c : children(s)) {
                    if (is(c, "stock")) stock.put(attr(c, "id", ""), (int) num(c, "qty", 0));
                    else if (is(c, "requestedgoods")) for (Element i : children(c)) req.add(attr(i, "id", ""));
                    else if (is(c, "dailyspecials")) for (Element i : children(c)) spec.add(attr(i, "id", ""));
                }
                req.remove(""); spec.remove(""); stock.remove("");
                stores.put(store, new SaveSnapshot.StoreState((int) Math.round(num(s, "pricemodifier", 0)), req, spec, stock));
            }
        }
        long time = (long) num(gs, "savetime", 0);
        SaveSnapshot snap = new SaveSnapshot(file, time, preset, shop, reward, locName, locType, faction, stores, reps);
        if (map != null && loc != null) world(snap, map, loc);
        return snap;
    }

    /** Биом и сложность текущей станции; взятые миссии (со всей карты) и миссии, которые она предлагает. */
    private static void world(SaveSnapshot snap, Element map, Element loc) {
        Map<String, String> names = new HashMap<>();
        for (Element l : children(map)) if (is(l, "location")) names.put(attr(l, "i", ""), attr(l, "name", ""));
        String cur = attr(loc, "i", "");
        double diff = 0;
        Element level = child(loc, "level");
        if (level != null) diff = num(level, "difficulty", 0);
        List<SaveSnapshot.Mission> ms = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Element l : children(map)) {
            if (!is(l, "location")) continue;
            boolean here = l == loc;
            for (Element m : descendants(l)) {
                if (!is(m, "mission")) continue;
                boolean selected = "true".equalsIgnoreCase(attr(m, "selected", "false"));
                if (!here && !selected) continue;
                String id = attr(m, "prefabid", "");
                String dest = names.getOrDefault(attr(m, "destinationindex", ""), "");
                if (id.isEmpty() || !seen.add(id + "|" + dest + "|" + selected)) continue;
                ms.add(new SaveSnapshot.Mission(id, selected, dest.equals(names.get(cur)) ? "" : dest));
            }
        }
        ms.sort(Comparator.comparing((SaveSnapshot.Mission m) -> !m.selected()));
        snap.setWorld(attr(loc, "biome", ""), diff, ms);
    }

    private static Store store(String gameId) {
        for (Store s : Store.values()) if (s.gameId().equalsIgnoreCase(gameId.trim())) return s;
        return null;
    }

    private static int indexOfIgnoreCase(String s, String what, int from) {
        int n = what.length();
        for (int i = from; i + n <= s.length(); i++) if (s.regionMatches(true, i, what, 0, n)) return i;
        return -1;
    }
}