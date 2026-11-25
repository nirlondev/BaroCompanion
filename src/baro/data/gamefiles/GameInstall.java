package baro.data.gamefiles;


import static baro.Tr.tr;
import baro.settings.AppSettings;
import org.w3c.dom.Element;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Установленная игра и включённые пакеты контента (ваниль + моды из config_player.xml, в порядке загрузки).
 * Настройки: game.dir — папка игры (если не задана, ищется в библиотеках Steam), game.mods — читать ли моды.
 */
public final class GameInstall {
    /** Пакет контента: ваниль или мод. Файлы по типам (Item, Text, Talents, TalentTrees, Afflictions…). */
    static final class ContentPackage {
        final String name;
        final Map<String, List<Path>> files = new LinkedHashMap<>();
        ContentPackage(String name) { this.name = name; }
    }

    private final Path dir;
    private final List<ContentPackage> packages;

    private GameInstall(Path dir, List<ContentPackage> packages) { this.dir = dir; this.packages = packages; }

    public Path dir() { return dir; }

    /** Названия модов (без ванили). */
    public List<String> mods() {
        List<String> l = new ArrayList<>();
        for (int i = 1; i < packages.size(); i++) l.add(packages.get(i).name);
        return l;
    }

    /** Файлы данного типа из всех пакетов, в порядке загрузки (поздние перекрывают ранние). */
    List<Path> files(String type) {
        List<Path> l = new ArrayList<>();
        for (ContentPackage p : packages)
            for (Map.Entry<String, List<Path>> e : p.files.entrySet()) if (e.getKey().equalsIgnoreCase(type)) l.addAll(e.getValue());
        return l;
    }

    /** Название мода, из которого файл, или null — файл из ванили. */
    String modOf(Path file) {
        for (int i = 1; i < packages.size(); i++)
            for (List<Path> l : packages.get(i).files.values()) if (l.contains(file)) return packages.get(i).name;
        return null;
    }

    /** Файлы данного типа только из ванили. */
    List<Path> vanillaFiles(String type) {
        List<Path> l = new ArrayList<>();
        for (Map.Entry<String, List<Path>> e : packages.get(0).files.entrySet()) if (e.getKey().equalsIgnoreCase(type)) l.addAll(e.getValue());
        return l;
    }

    /** Найти игру; null — не найдена. */
    public static GameInstall locate(AppSettings settings) {
        Path dir = findDir(settings.getString("game.dir", ""));
        if (dir == null) return null;
        boolean mods = settings.getBoolean("game.mods", true);
        List<ContentPackage> packages = new ArrayList<>();
        Path core = dir.resolve("Content/ContentPackages/Vanilla.xml");
        List<Path> regular = new ArrayList<>();
        Element cfg = Files.exists(dir.resolve("config_player.xml")) ? GameXml.root(dir.resolve("config_player.xml")) : null;
        if (cfg != null) {
            for (Element e : GameXml.descendants(cfg)) {
                String p = GameXml.attr(e, "path");
                if (p == null || p.isEmpty()) continue;
                if (GameXml.is(e, "corepackage")) core = resolve(dir, p);
                else if (GameXml.is(e, "package") && mods) regular.add(resolve(dir, p));
            }
        }
        ContentPackage vanilla = readPackage(dir, core);
        if (vanilla == null && !core.equals(dir.resolve("Content/ContentPackages/Vanilla.xml")))
            vanilla = readPackage(dir, dir.resolve("Content/ContentPackages/Vanilla.xml"));
        if (vanilla == null) return null;
        packages.add(vanilla);
        for (Path p : regular) { ContentPackage cp = readPackage(dir, p); if (cp != null) packages.add(cp); }
        return new GameInstall(dir, packages);
    }

    public static boolean isGameDir(Path p) { return p != null && Files.isRegularFile(p.resolve("Content/ContentPackages/Vanilla.xml")); }

    private static Path findDir(String configured) {
        if (configured != null && !configured.isBlank()) {
            Path p = Paths.get(configured.trim());
            if (isGameDir(p)) return p;
        }
        for (Path lib : steamLibraries()) {
            Path p = lib.resolve("steamapps/common/Barotrauma");
            if (isGameDir(p)) return p;
        }
        return null;
    }

    /** Папки библиотек Steam: стандартные места и libraryfolders.vdf. */
    static List<Path> steamLibraries() {
        Set<Path> libs = new LinkedHashSet<>();
        List<Path> steams = new ArrayList<>();
        for (String env : new String[]{"ProgramFiles(x86)", "ProgramFiles"}) {
            String v = System.getenv(env);
            if (v != null) steams.add(Paths.get(v, "Steam"));
        }
        steams.add(Paths.get("C:/Program Files (x86)/Steam"));
        steams.add(Paths.get(System.getProperty("user.home"), ".steam/steam"));
        steams.add(Paths.get(System.getProperty("user.home"), ".local/share/Steam"));
        steams.add(Paths.get(System.getProperty("user.home"), "Library/Application Support/Steam"));
        Pattern path = Pattern.compile("\"path\"\\s+\"([^\"]+)\"");
        for (Path s : steams) {
            libs.add(s);
            Path vdf = s.resolve("steamapps/libraryfolders.vdf");
            if (!Files.isRegularFile(vdf)) continue;
            try {
                Matcher m = path.matcher(new String(Files.readAllBytes(vdf), StandardCharsets.UTF_8));
                while (m.find()) libs.add(Paths.get(m.group(1).replace("\\\\", "\\")));
            } catch (Exception e) { System.err.println(tr("Библиотеки Steam не прочитаны: ") + vdf + " — " + e); }
        }
        return new ArrayList<>(libs);
    }

    private static Path resolve(Path gameDir, String p) {
        Path path = Paths.get(p.replace('\\', '/'));
        return path.isAbsolute() ? path : gameDir.resolve(path);
    }

    private static ContentPackage readPackage(Path gameDir, Path filelist) {
        if (!Files.isRegularFile(filelist)) return null;
        Element root = GameXml.root(filelist);
        if (root == null) return null;
        ContentPackage cp = new ContentPackage(GameXml.attr(root, "name", filelist.getParent().getFileName().toString()));
        String modDir = filelist.getParent().toString().replace('\\', '/');
        for (Element e : GameXml.children(root)) {
            String f = GameXml.attr(e, "file");
            if (f == null || f.isEmpty()) continue;
            f = f.replaceAll("(?i)%ModDir(:[^%]*)?%", Matcher.quoteReplacement(modDir));
            Path p = resolve(gameDir, f);
            if (Files.isRegularFile(p)) cp.files.computeIfAbsent(e.getTagName(), k -> new ArrayList<>()).add(p);
        }
        return cp;
    }
}