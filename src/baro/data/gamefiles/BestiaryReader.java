package baro.data.gamefiles;


import static baro.Tr.tr;
import baro.bestiary.*;
import baro.bestiary.Creature.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Бестиарий и миссии из файлов игры и модов: персонажи (Character, с вариантами), их рэгдоллы (атаки, броня) и анимации
 * (скорость), случайные события (где появляются монстры), наборы NPC, профессии, приказы и все миссии.
 */
public final class BestiaryReader {
    private final GameInstall game;
    private final GameTexts texts;
    private final Map<String, String[]> afflictions = new HashMap<>(); // id → {тип, название}
    private final Map<String, String> itemNames = new HashMap<>();
    private final Map<String, String> orderNames = new HashMap<>();
    private final Map<String, List<String>> ordersByJob = new HashMap<>();

    private BestiaryReader(GameInstall game, String language) {
        this.game = game;
        this.texts = new GameTexts(game, language);
    }

    /** Прочитать всё; бросает исключение, если в файлах игры нет ни одного существа или миссии. */
    public static Bestiary read(GameInstall game, String language) {
        long t0 = System.currentTimeMillis();
        BestiaryReader r = new BestiaryReader(game, language);
        r.readAfflictions();
        r.readOrders();
        Map<String, double[]> human = new HashMap<>();
        List<Creature> creatures = r.readCreatures(human);
        List<MissionInfo> missions = r.attachBriefings(r.readMissions());
        List<Npc> npcs = r.readNpcs(missions);
        List<CrewJob> jobs = r.readJobs();
        if (creatures.isEmpty() || missions.isEmpty()) throw new IllegalStateException(tr("в файлах игры не найдены существа или миссии"));
        Map<String, Double> hs = new HashMap<>();
        human.forEach((k, v) -> hs.put(k, v[0]));
        String mods = game.mods().isEmpty() ? "" : tr(", моды: ") + String.join(", ", game.mods());
        String src = tr("Файлы игры: ") + game.dir() + mods + tr(" — существ ") + creatures.size() + ", NPC " + npcs.size()
                + tr(", профессий ") + jobs.size() + tr(", миссий ") + missions.size() + " (" + (System.currentTimeMillis() - t0) + tr(" мс)");
        System.err.println(src);
        return new Bestiary(creatures, npcs, jobs, missions, r.itemNames, hs, src);
    }

    // ---------- общие ----------

    private String text(String key) { return clean(texts.get(key)); }

    /**
     * Фразы с подстановками игры ([location1], [winner]…) → обычные общие формулировки. В справочнике нет конкретной
     * кампании, поэтому «между точками [location1] и [location2]» становится «на этом участке пути» и т. п.
     * Порядок важен: сначала узкие фразы, потом общие формы по падежам, в конце — одиночные подстановки.
     */
    private static final String[][] STORY = {
            // кавычки вокруг мест
            {"[\"«“]\\[(location\\w*)\\][\"»”]", "[$1]"},
            // маршруты и участки пути
            {tr("путь между (?:точками|локациями) \\[location1\\] и \\[location2\\]"), tr("путь на этом участке")},
            {tr("между (?:точками|локациями) \\[location1\\] и \\[location2\\]"), tr("на этом участке пути")},
            {tr("на маршруте \\[location1\\]\\s*[-–—]\\s*\\[location2\\]"), tr("на этом маршруте")},
            {tr("по маршруту \\[location1\\]\\s*[-–—]\\s*\\[location2\\]"), tr("по этому маршруту")},
            {tr("маршрут\\w* \\[location1\\]\\s*[-–—]\\s*\\[location2\\]"), tr("этот маршрут")},
            // станция, где выдают миссию, и место задания
            {tr("В пещерах, неподал[её]ку от которых находится \\[location\\w*\\],"), tr("В пещерах неподалёку от станции")},
            {tr("неподал[её]ку от \\[location\\w*\\]"), tr("неподалёку")},
            {tr("покинутый \\[location\\w*\\]"), tr("покинутый аванпост")},
            {tr("аванпостом \\[location\\w*\\]"), tr("одним из аванпостов")},
            {tr("за аванпост \\[location\\w*\\]"), tr("за аванпост")},
            {tr("захватить: \\[location\\w*\\]\\."), tr("захватить аванпост.")},
            {tr("^\\[location\\w*\\] вызывает"), tr("Станция вызывает")},
            {"^\\[location\\w*\\]:\\s*", ""},
            {tr("посещали точку \\[location\\w*\\]"), tr("заходили на станцию")},
            {tr("покинете точку \\[location\\w*\\]"), tr("покинете станцию")},
            {tr("покинувшему точку \\[location\\w*\\]"), tr("покинувшему станцию")},
            {tr("(?:из|с) точки \\[location1\\]"), tr("с этой станции")},
            {tr("^В точке \\[location1\\]"), tr("На этой станции")},
            {tr("^В (?:точку|локацию) \\[location2?\\]"), tr("В пункт назначения")},
            {tr("руин\\w* в точке \\[location\\w*\\]"), tr("руин неподалёку")},
            {tr("руинах в точке \\[location\\w*\\]"), tr("руинах неподалёку")},
            {tr("сюда: \\[location\\w*\\]"), tr("сюда")},
            {tr("Пункт назначения: \\[location\\w*\\]\\.?\\s*"), ""},
            {tr("Награда: \\[reward\\] кред\\.\\s*"), ""},
            // пункт назначения ([location2], [location]) по падежам
            {tr("(?:в|на) (?:точку|локацию|пункт) \\[location2?\\]"), tr("в пункт назначения")},
            {tr("в (?:точке|локации|пункте) \\[location2?\\]"), tr("в пункте назначения")},
            {tr("в районе (?:точки|локации|пункта) \\[location2?\\]"), tr("в районе пункта назначения")},
            {tr("(до|из|от) (?:точки|локации|пункта) \\[location2?\\]"), tr("$1 пункта назначения")},
            {tr("к (?:точке|локации|пункту) \\[location2?\\]"), tr("к пункту назначения")},
            {tr("в \\[location2?\\]"), tr("в пункт назначения")},
            // место задания ([location1]) по падежам
            {tr("(?:в|на) (?:точку|локацию) \\[location1\\]"), tr("в указанное место")},
            {tr("в (?:точке|локации) \\[location1\\]"), tr("на этой станции")},
            {tr("до (?:точки|локации) \\[location1\\]"), tr("до места")},
            {tr("(?:точку|локацию) \\[location1\\]"), tr("указанное место")},
            // команды в PvP
            {tr("Судно \\[loser\\] было побеждено командой судна \\[winner\\]"), tr("Одна из команд одолела судно соперников")},
            {"\\[winner\\]", tr("команда-победитель")},
            {tr("первой убьет \\[winscore\\]"), tr("первой наберёт нужное число побед в стычках")},
            {tr("в течение \\[winscore\\] секунд"), tr("нужное время")},
            // всё, что осталось
            {"\\[location2?\\]|\\[locationname\\]", tr("пункт назначения")},
            {"\\[location1\\]", tr("указанное место")},
            {"\\[(?:name|speakername)\\]", tr("заказчик")},
            {"\\[playername\\]", tr("вы")},
            {"\\[currencyname\\]", tr("марок")}};
    private static final java.util.regex.Pattern[] STORY_PATTERNS = new java.util.regex.Pattern[STORY.length];
    static { for (int i = 0; i < STORY.length; i++) STORY_PATTERNS[i] = java.util.regex.Pattern.compile(STORY[i][0]); }

    /**
     * Убрать разметку цвета ‖color:…‖ и подстановки игры. [reward] остаётся меткой ‹награда› — сумму подставляет карточка
     * (с учётом сложности кампании); ‹monster›, ‹resourcequantity1›… подставляет сама миссия, остальное убирается.
     */
    static String clean(String s) {
        if (s == null) return null;
        s = s.replaceAll("‖[^‖]*‖", "");
        for (int i = 0; i < STORY.length; i++) s = STORY_PATTERNS[i].matcher(s).replaceAll(STORY[i][1]);
        s = s.replace("[reward]", tr("‹награда›"));
        s = s.replaceAll("\\[([a-z_0-9]+)\\]", "‹$1›");
        s = s.replaceAll("  +", " ").replaceAll(" ([,.!?])", "$1");
        return s.trim().isEmpty() ? null : s.trim();
    }

    private String itemName(String id) {
        String n = itemNames.get(id);
        if (n == null) {
            n = text("entityname." + id);
            if (n == null) n = id;
            itemNames.put(id, n);
        }
        return n;
    }

    /** Элементы верхнего уровня файла: сам корень с identifier или дети корня, с раскрытием &lt;Override&gt;. */
    private static List<Element> entries(Element root) {
        List<Element> l = new ArrayList<>();
        if (GameXml.is(root, "override")) { for (Element c : GameXml.children(root)) l.addAll(entries(c)); return l; }
        for (Element c : GameXml.children(root)) {
            if (GameXml.is(c, "override")) l.addAll(GameXml.children(c)); else l.add(c);
        }
        return l;
    }

    private static String a(Element e, String name) { String v = GameXml.attr(e, name); return v == null ? null : v.trim(); }

    private static String lower(String s) { return s == null ? null : s.trim().toLowerCase(Locale.ROOT); }

    private static int intAttr(Element e, String name, int def) { return (int) Math.round(GameXml.num(e, name, def)); }

    private static double numOr(Element e, String name, double def) {
        String v = a(e, name);
        if (v == null) return def;
        if (v.equalsIgnoreCase("infinity")) return Double.POSITIVE_INFINITY;
        try { return Double.parseDouble(v.replace(',', '.')); } catch (NumberFormatException ex) { return def; }
    }

    private void readAfflictions() {
        for (Path f : game.files("Afflictions")) {
            Element root = GameXml.root(f);
            if (root == null) continue;
            for (Element e : entries(root)) {
                String id = a(e, "identifier");
                if (id == null || id.isEmpty()) continue;
                String name = text("afflictionname." + id);
                if (name == null) name = GameXml.attr(e, "name", id);
                afflictions.put(lower(id), new String[]{GameXml.attr(e, "type", ""), name});
            }
        }
    }

    private void readOrders() {
        for (Path f : game.files("Orders")) {
            Element root = GameXml.root(f);
            if (root == null) continue;
            for (Element e : entries(root)) {
                String id = a(e, "identifier");
                if (id == null) continue;
                String name = text("ordername." + id);
                orderNames.put(lower(id), name != null ? name : id);
                for (String job : GameXml.list(a(e, "appropriatejobs"))) ordersByJob.computeIfAbsent(lower(job), k -> new ArrayList<>()).add(name != null ? name : id);
            }
        }
    }

    // ---------- существа ----------

    /** Сырой персонаж: элемент, файл, от кого унаследован. */
    private record RawChar(String id, Element root, Path file, String inherit, String mod) { }

    /** Изменяемое описание существа: база заполняется целиком, вариант — поверх копии родителя. */
    private static final class CB implements Cloneable {
        String id, translation, group, parent, mod; Path dir;
        boolean humanoid, husk, needsAir = true, boss, canEnterSub = true, bleeds = true, stunImmune;
        double vitality = 100, bleedRed, burnRed, regen, regenEat, poison = 1, crush = Double.POSITIVE_INFINITY;
        List<Element> healthLimbs = new ArrayList<>();
        double combat, sight = 1, hearing = 1, flee; boolean boarding, avoidGun, swarm, pet; String latch = "";
        LinkedHashMap<String, AiTarget> targets = new LinkedHashMap<>();
        Path ragdoll, animations; double scale = 1, damageMul = 1, rangeMul = 1;
        List<Element> inventories = new ArrayList<>(); List<Element> extraLoot = new ArrayList<>(); List<Element> effects = new ArrayList<>();

        CB copy() {
            try {
                CB c = (CB) super.clone();
                c.healthLimbs = new ArrayList<>(healthLimbs); c.targets = new LinkedHashMap<>(targets);
                c.inventories = new ArrayList<>(inventories); c.extraLoot = new ArrayList<>(extraLoot); c.effects = new ArrayList<>(effects);
                return c;
            } catch (CloneNotSupportedException e) { throw new AssertionError(e); }
        }
    }

    private List<Creature> readCreatures(Map<String, double[]> humanSpeeds) {
        Map<String, RawChar> raw = new LinkedHashMap<>();
        for (Path f : game.files("Character")) {
            Element root = GameXml.root(f);
            if (root == null) continue;
            List<Element> cands = new ArrayList<>();
            if (GameXml.is(root, "override")) cands.addAll(GameXml.children(root)); else cands.add(root);
            for (Element e : cands) {
                if (!GameXml.is(e, "character") && !GameXml.is(e, "charactervariant")) continue;
                String id = a(e, "speciesname");
                if (id == null || id.isEmpty()) continue;
                String inherit = a(e, "inherit");
                if (inherit == null) inherit = a(e, "variantof");
                raw.put(lower(id), new RawChar(id, e, f, inherit == null || inherit.isEmpty() ? null : inherit, game.modOf(f)));
            }
        }
        speciesKeys.addAll(raw.keySet());
        Map<String, CB> resolved = new HashMap<>();
        Map<String, List<Spawn>> spawns = readSpawns();
        List<Creature> out = new ArrayList<>();
        for (RawChar rc : raw.values()) {
            CB cb = resolve(lower(rc.id), raw, resolved, 0);
            if (cb == null) continue;
            if (cb.id.equalsIgnoreCase("human")) {
                for (Map.Entry<String, Double> e : readSpeeds(cb).entrySet()) humanSpeeds.put(e.getKey(), new double[]{e.getValue()});
                continue;
            }
            try { out.add(build(cb, spawns.getOrDefault(lower(cb.id), List.of()))); }
            catch (Exception ex) { System.err.println(tr("Существо ") + cb.id + tr(" не прочитано: ") + ex); }
        }
        // одинаковые имена (Fractalguardian и Fractalguardian2 — оба «Фрактальный страж») различаем
        Map<String, Integer> seen = new HashMap<>();
        for (int i = 0; i < out.size(); i++) {
            Creature c = out.get(i);
            if (seen.merge(c.name(), 1, Integer::sum) == 1) continue;
            out.set(i, new Creature(c.id(), c.name() + " (" + c.id() + ")", c.kind(), c.parent(), c.group(), c.mod(), c.health(), c.ai(),
                    c.speeds(), c.attacks(), c.armor(), c.drops(), c.spawns(), c.abilities(), c.scale(), c.canEnterSub(), c.needsAir(),
                    c.humanoid(), c.boss()));
        }
        out.sort(Comparator.comparing((Creature c) -> c.kind().ordinal()).thenComparing(c -> -c.health().vitality()).thenComparing(Creature::name));
        return out;
    }

    private CB resolve(String key, Map<String, RawChar> raw, Map<String, CB> done, int depth) {
        if (done.containsKey(key)) return done.get(key);
        RawChar rc = raw.get(key);
        if (rc == null || depth > 8) return null;
        CB cb;
        if (rc.inherit != null && !lower(rc.inherit).equals(key)) {
            CB p = resolve(lower(rc.inherit), raw, done, depth + 1);
            if (p == null) return null;
            cb = p.copy();
            cb.parent = p.id;
        } else {
            cb = new CB();
            cb.dir = rc.file.getParent();
        }
        cb.id = rc.id;
        cb.mod = rc.mod;
        apply(cb, rc.root, rc.file, rc.inherit != null);
        done.put(key, cb);
        return cb;
    }

    /** Применить атрибуты и блоки персонажа (или варианта) поверх того, что уже есть. */
    private void apply(CB cb, Element root, Path file, boolean variant) {
        if (!variant) cb.translation = null;
        String tr = a(root, "speciestranslationoverride");
        if (tr != null && !tr.isEmpty()) cb.translation = tr;
        if (a(root, "group") != null) cb.group = a(root, "group");
        if (a(root, "humanoid") != null) cb.humanoid = GameXml.bool(root, "humanoid", false);
        if (a(root, "husk") != null) cb.husk = GameXml.bool(root, "husk", false);
        if (a(root, "needsair") != null) cb.needsAir = GameXml.bool(root, "needsair", true);
        if (a(root, "usebosshealthbar") != null) cb.boss = GameXml.bool(root, "usebosshealthbar", false);
        for (Element c : GameXml.children(root)) {
            if (GameXml.is(c, "health")) {
                cb.vitality = numOr(c, "vitality", cb.vitality);
                if (a(c, "doesbleed") != null) cb.bleeds = GameXml.bool(c, "doesbleed", true);
                cb.bleedRed = numOr(c, "bleedingreduction", cb.bleedRed);
                cb.burnRed = numOr(c, "burnreduction", cb.burnRed);
                cb.regen = numOr(c, "constanthealthregeneration", cb.regen);
                cb.regenEat = numOr(c, "healthregenerationwheneating", cb.regenEat);
                cb.poison = numOr(c, "poisonvulnerability", cb.poison);
                cb.crush = numOr(c, "crushdepth", cb.crush);
                if (a(c, "stunimmunity") != null) cb.stunImmune = GameXml.bool(c, "stunimmunity", false);
                List<Element> limbs = GameXml.children(c, "limb");
                if (!limbs.isEmpty()) cb.healthLimbs = limbs;
            } else if (GameXml.is(c, "ai")) {
                cb.combat = numOr(c, "combatstrength", cb.combat);
                cb.sight = numOr(c, "sight", cb.sight);
                cb.hearing = numOr(c, "hearing", cb.hearing);
                cb.flee = numOr(c, "fleehealththreshold", cb.flee);
                if (a(c, "aggressiveboarding") != null) cb.boarding = GameXml.bool(c, "aggressiveboarding", false);
                if (a(c, "avoidgunfire") != null) cb.avoidGun = GameXml.bool(c, "avoidgunfire", false);
                for (Element t : GameXml.descendants(c)) {
                    if (GameXml.is(t, "target")) {
                        String tag = a(t, "tag");
                        if (tag == null) continue;
                        cb.targets.put(lower(tag), new AiTarget(lower(tag), GameXml.attr(t, "state", "Attack"), numOr(t, "priority", 0),
                                GameXml.bool(t, "ignoreinside", false), GameXml.bool(t, "ignoreoutside", false)));
                    } else if (GameXml.is(t, "clear") && t.getParentNode() instanceof Element p && GameXml.is(p, "chooserandom")) cb.latch = "";
                    else if (GameXml.is(t, "swarmbehavior")) cb.swarm = true;
                    else if (GameXml.is(t, "petbehavior")) cb.pet = true;
                }
                String latch = latchOf(c);
                if (latch != null) cb.latch = latch;
            } else if (GameXml.is(c, "ragdolls")) {
                Path p = folder(c, cb.dir, "Ragdolls");
                if (p != null) cb.ragdoll = p;
            } else if (GameXml.is(c, "animations")) {
                Path p = folder(c, cb.dir, "Animations");
                if (p != null) cb.animations = p;
            } else if (GameXml.is(c, "ragdoll")) {
                cb.scale *= numOr(c, "scalemultiplier", 1);
                if (a(c, "canentersubmarine") != null) cb.canEnterSub = GameXml.bool(c, "canentersubmarine", true);
            } else if (GameXml.is(c, "attack")) {
                cb.damageMul *= numOr(c, "damagemultiplier", 1);
                cb.rangeMul *= numOr(c, "rangemultiplier", 1);
            }
        }
        List<Element> inv = GameXml.children(root, "inventory");
        if (!inv.isEmpty()) cb.inventories = inv;
        List<Element> extra = new ArrayList<>();
        for (Element se : GameXml.children(root, "statuseffect")) {
            String type = lower(GameXml.attr(se, "type", ""));
            if (type.equals("ondeconstructed") || type.equals("ondeath"))
                for (Element s : GameXml.descendants(se)) if (GameXml.is(s, "spawnitem")) { s.setAttribute("_when", type); extra.add(s); }
        }
        if (!extra.isEmpty() || !variant) cb.extraLoot = extra;
        List<Element> effects = new ArrayList<>();
        for (Element se : GameXml.children(root, "statuseffect")) if (!lower(GameXml.attr(se, "type", "")).equals("ondeconstructed")) effects.add(se);
        if (!effects.isEmpty() || !variant) cb.effects = effects;
        if (cb.ragdoll == null) cb.ragdoll = folder(null, cb.dir, "Ragdolls");
        if (cb.animations == null) cb.animations = folder(null, cb.dir, "Animations");
    }

    private static String latchOf(Element ai) {
        boolean walls = false, sub = false, any = false;
        for (Element l : GameXml.descendants(ai)) {
            if (!GameXml.is(l, "latchonto")) continue;
            any = true;
            if (GameXml.bool(l, "attachtowalls", false)) walls = true;
            if (GameXml.bool(l, "attachtosub", false)) sub = true;
        }
        if (!any) return null;
        return walls && sub ? tr("к стенам пещер и корпусу подлодки") : sub ? tr("к корпусу подлодки") : walls ? tr("к стенам пещер") : "";
    }

    /** Папка рэгдоллов/анимаций: folder="default" — рядом с файлом вида, иначе путь от папки игры. */
    private Path folder(Element e, Path speciesDir, String sub) {
        String f = e == null ? null : a(e, "folder");
        if (f != null && !f.isEmpty() && !f.equalsIgnoreCase("default") && !f.contains("%")) {
            Path p = game.dir().resolve(f.replace('\\', '/'));
            if (Files.isDirectory(p)) return p;
        }
        if (speciesDir == null) return null;
        Path p = speciesDir.resolve(sub);
        return Files.isDirectory(p) ? p : speciesDir;
    }

    private static List<Path> xmlFiles(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) return List.of();
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xml")).sorted().toList();
        } catch (Exception e) { return List.of(); }
    }

    /** Файл рэгдолла: *DefaultRagdoll.xml, иначе любой *ragdoll*.xml, иначе первый xml в папке Ragdolls. */
    private Element ragdollOf(CB cb) {
        List<Path> files = xmlFiles(cb.ragdoll);
        Path best = null;
        for (Path p : files) {
            String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
            if (n.contains("ragdoll") && n.contains("default")) { best = p; break; }
        }
        if (best == null) for (Path p : files) if (p.getFileName().toString().toLowerCase(Locale.ROOT).contains("ragdoll")) { best = p; break; }
        if (best == null && !files.isEmpty() && !Objects.equals(cb.ragdoll, cb.dir)) best = files.get(0);
        if (best == null) return null;
        Element r = GameXml.root(best);
        return r != null && GameXml.is(r, "ragdoll") ? r : null;
    }

    private static final String[][] ANIMS = {{"walk", "walk"}, {"run", "run"}, {"swimslow", "swimslow"}, {"swimfast", "swimfast"}};

    private Map<String, Double> readSpeeds(CB cb) {
        Map<String, Double> m = new LinkedHashMap<>();
        if (Objects.equals(cb.animations, cb.dir)) return m;
        List<Path> files = xmlFiles(cb.animations);
        String sp = lower(cb.id);
        for (String[] k : ANIMS) {
            Path pick = null;
            for (Path p : files) {
                String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                n = n.substring(0, n.length() - 4);
                if (!n.endsWith(k[0]) || (k[0].equals("run") && n.endsWith("swimfast"))) continue;
                boolean other = false;
                for (String x : new String[]{"drunken", "divingsuit", "exosuit", "crouch"}) if (n.contains(x) && !sp.contains(x)) other = true;
                if (other) continue;
                if (pick == null || n.startsWith(sp)) pick = p;
            }
            if (pick == null) continue;
            Element r = GameXml.root(pick);
            if (r == null) continue;
            double v = numOr(r, "movementspeed", 0);
            if (v > 0) m.put(k[1], v);
        }
        return m;
    }

    private Creature build(CB cb, List<Spawn> spawns) {
        Element rag = ragdollOf(cb);
        List<Attack> attacks = new ArrayList<>();
        List<LimbArmor> armor = new ArrayList<>();
        Map<Integer, String> limbByHealthIndex = new HashMap<>();
        double ragScale = 1;
        boolean canEnter = cb.canEnterSub;
        if (rag != null) {
            ragScale = numOr(rag, "limbscale", 1);
            if (a(rag, "canentersubmarine") != null) canEnter = GameXml.bool(rag, "canentersubmarine", true) && cb.canEnterSub;
            for (Element limb : GameXml.children(rag, "limb")) {
                String lname = limbName(limb);
                int hi = intAttr(limb, "healthindex", -1);
                if (hi >= 0) limbByHealthIndex.merge(hi, Glossary.limb(a(limb, "type")), (x, y) -> x.equals(y) ? x : x);
                for (Element at : GameXml.children(limb, "attack")) attacks.add(attack(at, lname, cb));
                List<ArmorMod> mods = new ArrayList<>();
                for (Element dm : GameXml.children(limb, "damagemodifier")) {
                    ArmorMod m = armorMod(dm);
                    if (m != null) mods.add(m);
                }
                if (!mods.isEmpty()) armor.add(new LimbArmor(lname, mods));
            }
        }
        // уязвимые места: Limb в <health> по порядку соответствуют healthindex рэгдолла;
        // одинаковые множители у разных частей собираем в одну строку («голова, туловище: …», «всё тело: …»)
        Map<String, List<String>> byMod = new LinkedHashMap<>();
        Map<String, Double> modMul = new HashMap<>();
        Map<String, String> modTypes = new HashMap<>();
        for (int i = 0; i < cb.healthLimbs.size(); i++) {
            Element l = cb.healthLimbs.get(i);
            String n = a(l, "name");
            n = n != null && !n.isEmpty() ? Glossary.limb(n.replace(" ", "")) : limbByHealthIndex.getOrDefault(i, tr("часть ") + (i + 1));
            for (Element vm : GameXml.children(l, "vitalitymultiplier")) {
                double mul = numOr(vm, "multiplier", 1);
                if (Math.abs(mul - 1) < 1e-6) continue;
                String types = firstNonNull(a(vm, "type"), a(vm, "types"));
                String what = types != null ? typesRu(types) : afflictionNames(firstNonNull(a(vm, "identifier"), a(vm, "identifiers")));
                String key = what + "|" + mul;
                List<String> limbs = byMod.computeIfAbsent(key, k -> new ArrayList<>());
                if (!limbs.contains(n)) limbs.add(n);
                modMul.put(key, mul);
                modTypes.put(key, what);
            }
        }
        List<LimbMultiplier> weak = new ArrayList<>();
        int limbCount = cb.healthLimbs.size();
        for (Map.Entry<String, List<String>> e : byMod.entrySet()) {
            String limbs = e.getValue().size() >= limbCount && limbCount > 1 ? tr("всё тело") : String.join(", ", e.getValue());
            weak.add(new LimbMultiplier(limbs, modMul.get(e.getKey()), modTypes.get(e.getKey())));
        }
        List<Drop> drops = drops(cb);
        String name = creatureName(cb);
        creatureNames.put(cb.id, name);
        Ai ai = new Ai(cb.combat, cb.sight, cb.hearing, cb.flee, cb.boarding, cb.avoidGun, cb.latch, cb.swarm, cb.pet,
                new ArrayList<>(cb.targets.values()));
        Health h = new Health(cb.vitality, cb.bleeds, cb.bleedRed, cb.burnRed, cb.regen, cb.regenEat, cb.poison, cb.crush, weak);
        CreatureKind kind = kind(cb, attacks);
        return new Creature(cb.id, name, kind, cb.parent, cb.group, cb.mod, h, ai, readSpeeds(cb), attacks, armor, drops,
                spawns, abilities(cb), ragScale * cb.scale, canEnter, cb.needsAir, cb.humanoid, cb.boss);
    }

    /** Особые эффекты персонажа (ауры, кислота при ранении, что появляется при гибели) — строками на русском. */
    private List<String> abilities(CB cb) {
        Set<String> out = new LinkedHashSet<>();
        if (cb.stunImmune) out.add(tr("Невосприимчив к оглушению"));
        for (Element se : cb.effects) {
            String type = lower(GameXml.attr(se, "type", ""));
            String target = lower(GameXml.attr(se, "target", ""));
            String when = switch (type) {
                case "onactive", "always" -> tr("Постоянно");
                case "ondamaged" -> tr("При получении урона");
                case "ondeath" -> tr("При гибели");
                case "onspawn" -> tr("При появлении");
                default -> null;
            };
            if (when == null) continue;
            boolean perSecond = type.equals("onactive") || type.equals("always");
            double range = numOr(se, "range", 0);
            String who = a(se, "targets");
            String scope = target.contains("nearby")
                    ? tr(" вокруг себя (") + (range > 0 ? tr("до ") + Math.round(range / 100) + tr(" м") : tr("рядом")) + (who != null ? tr(", цели: ") + tagsRu(who) : "") + ")"
                    : target.equals("this") || target.isEmpty() ? "" : tr(" на цель");
            List<String> parts = new ArrayList<>();
            for (Element c : GameXml.children(se)) {
                if (GameXml.is(c, "affliction")) {
                    Hit h = hit(c, 1, true);
                    if (h.strength() > 0) parts.add((scope.isEmpty() ? tr("на себя ") : "") + "«" + h.name() + "» " + fmt(h.strength()) + (perSecond ? tr("/с") : ""));
                } else if (GameXml.is(c, "reduceaffliction")) {
                    String id = firstNonNull(a(c, "identifier"), a(c, "type"));
                    String[] af = id == null ? null : afflictions.get(lower(id));
                    double amount = numOr(c, "amount", numOr(c, "strength", 0));
                    parts.add(tr("снимает «") + (af != null ? af[1] : Glossary.afflictionType(id)) + "»" + (amount > 0 ? " " + fmt(amount) + (perSecond ? tr("/с") : "") : ""));
                } else if (GameXml.is(c, "spawnitem")) {
                    for (String id : GameXml.list(firstNonNull(a(c, "identifiers"), a(c, "identifier"))))
                        parts.add(tr("выпускает «") + itemName(id) + "»" + (intAttr(c, "count", 1) > 1 ? " ×" + intAttr(c, "count", 1) : ""));
                } else if (GameXml.is(c, "spawncharacter")) {
                    String sp = firstNonNull(a(c, "speciesname"), a(c, "species"));
                    if (sp != null) { String n = text("character." + sp); parts.add(tr("порождает: ") + (n != null ? n : sp) + (intAttr(c, "count", 1) > 1 ? " ×" + intAttr(c, "count", 1) : "")); }
                } else if (GameXml.is(c, "explosion")) parts.add(tr("взрывается") + (a(c, "range") != null ? tr(" (радиус ") + Math.round(numOr(c, "range", 0) / 100) + tr(" м)") : ""));
            }
            if (!parts.isEmpty()) out.add(when + scope + ": " + String.join(", ", parts));
        }
        // «выпускает … ×3», «×2», «» (по порогам здоровья) — одной строкой «×1–3»
        Map<String, int[]> merged = new LinkedHashMap<>();
        for (String line : out) {
            java.util.regex.Matcher mt = java.util.regex.Pattern.compile(" ×(\\d+)$").matcher(line);
            int n = mt.find() ? Integer.parseInt(mt.group(1)) : 1;
            String key = mt.reset().find() ? line.substring(0, mt.start()) : line;
            int[] r = merged.computeIfAbsent(key, k -> new int[]{n, n, 0});
            r[0] = Math.min(r[0], n); r[1] = Math.max(r[1], n); r[2]++;
        }
        List<String> l = new ArrayList<>();
        for (Map.Entry<String, int[]> e : merged.entrySet()) {
            int[] r = e.getValue();
            l.add(e.getKey() + (r[1] > 1 ? " ×" + (r[0] == r[1] ? r[1] : r[0] + "–" + r[1]) : "") + (r[2] > 1 ? tr(" (в разных условиях)") : ""));
        }
        return l;
    }

    private String tagsRu(String csv) {
        List<String> l = new ArrayList<>();
        for (String t : GameXml.list(csv)) { String r = Glossary.tag(t); l.add(r != null ? r : t); }
        return String.join(", ", l);
    }

    private static String limbName(Element limb) {
        String type = a(limb, "type"), name = a(limb, "name");
        String t = type == null || type.equalsIgnoreCase("none") ? null : Glossary.limb(type);
        if (name == null || name.isEmpty() || name.matches("(?i)limb ?\\d+")) return t != null ? t : tr("часть тела");
        return t != null && !t.equalsIgnoreCase(name) ? name + " (" + t + ")" : name;
    }

    private String afflictionNames(String csv) {
        List<String> l = new ArrayList<>();
        for (String s : GameXml.list(csv)) { String[] af = afflictions.get(lower(s)); l.add(af != null ? af[1] : s); }
        return String.join(", ", l);
    }

    private static String typesRu(String csv) {
        if (csv == null) return "";
        List<String> l = new ArrayList<>();
        for (String s : GameXml.list(csv)) l.add(Glossary.afflictionType(s));
        return String.join(", ", l);
    }

    private ArmorMod armorMod(Element dm) {
        double mul = numOr(dm, "damagemultiplier", 1);
        List<String> what = new ArrayList<>();
        for (String s : GameXml.list(a(dm, "afflictiontypes"))) what.add(Glossary.afflictionType(s));
        for (String s : GameXml.list(a(dm, "afflictionidentifiers"))) { String[] af = afflictions.get(lower(s)); what.add(af != null ? af[1] : s); }
        if (what.isEmpty() || Math.abs(mul - 1) < 1e-6) return null;
        String sector = a(dm, "armorsector");
        if (sector != null && sector.replace(" ", "").equals("0,360")) sector = "";
        return new ArmorMod(String.join(", ", what), mul, sector == null || sector.isEmpty() ? "" : sector.replace(" ", "").replace(",", "–") + "°",
                GameXml.bool(dm, "deflectprojectiles", false));
    }

    private Attack attack(Element at, String limb, CB cb) {
        List<Hit> hits = new ArrayList<>();
        List<String> projectiles = new ArrayList<>();
        double mul = cb.damageMul;
        for (Element af : GameXml.children(at, "affliction")) hits.add(hit(af, mul, false));
        for (Element se : GameXml.children(at, "statuseffect")) {
            for (Element s : GameXml.descendants(se))
                if (GameXml.is(s, "spawnitem")) for (String id : GameXml.list(firstNonNull(a(s, "identifiers"), a(s, "identifier"))))
                    if (!projectiles.contains(id)) { itemName(id); projectiles.add(id); }
            String target = lower(GameXml.attr(se, "target", ""));
            if (target.equals("this") || target.isEmpty()) continue;
            for (Element af : GameXml.descendants(se)) if (GameXml.is(af, "affliction")) hits.add(hit(af, 1, true));
        }
        return new Attack(limb, GameXml.attr(at, "context", "Any"), GameXml.attr(at, "targettype", "Any"), numOr(at, "cooldown", 0),
                numOr(at, "range", 0) * cb.rangeMul, numOr(at, "damagerange", 0) * cb.rangeMul, numOr(at, "structuredamage", 0) * mul,
                numOr(at, "itemdamage", 0) * mul, numOr(at, "levelwalldamage", 0) * mul, numOr(at, "stun", 0), numOr(at, "penetration", 0),
                numOr(at, "severlimbsprobability", 0), GameXml.bool(at, "ranged", false), hits, projectiles);
    }

    private Hit hit(Element af, double mul, boolean viaEffect) {
        String id = a(af, "identifier");
        if (id == null) id = a(af, "type");
        String[] info = id == null ? null : afflictions.get(lower(id));
        String name = info != null ? info[1] : id;
        return new Hit(id, name, info != null ? info[0] : "", numOr(af, "strength", 0) * mul, numOr(af, "probability", 1), viaEffect);
    }

    private List<Drop> drops(CB cb) {
        List<Drop> l = new ArrayList<>();
        double total = 0;
        for (Element inv : cb.inventories) total += numOr(inv, "commonness", 1);
        int n = 0;
        for (Element inv : cb.inventories) {
            n++;
            double chance = total > 0 ? numOr(inv, "commonness", 1) / total : 1;
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (Element it : GameXml.descendants(inv)) {
                if (!GameXml.is(it, "item")) continue;
                String id = a(it, "identifier");
                if (id != null) counts.merge(id, Math.max(1, intAttr(it, "amount", 1)), Integer::sum);
            }
            String note = cb.inventories.size() > 1 ? tr("набор ") + n + tr(" из ") + cb.inventories.size() : tr("всегда при себе");
            for (Map.Entry<String, Integer> e : counts.entrySet()) { itemName(e.getKey()); l.add(new Drop(e.getKey(), e.getValue(), chance, note)); }
        }
        for (Element s : cb.extraLoot) {
            String when = "ondeath".equals(GameXml.attr(s, "_when", "")) ? tr("при гибели") : tr("при разборе тела");
            String ids = a(s, "identifiers");
            if (ids == null) ids = a(s, "identifier");
            List<String> list = GameXml.list(ids);
            for (String id : list) {
                itemName(id);
                l.add(new Drop(id, Math.max(1, intAttr(s, "count", 1)), numOr(s, "probability", 1) / (list.size() > 1 && GameXml.bool(s, "spawnrandom", false) ? list.size() : 1), when));
            }
        }
        return l;
    }

    private final Set<String> speciesKeys = new HashSet<>();
    private final Map<String, String> creatureNames = new HashMap<>();

    /**
     * Название как в игре (на выбранном языке, иначе английское). Если своего названия нет — название вида, от которого
     * унаследован вариант, с идентификатором в скобках; если нет и его — сам идентификатор. Своих переводов не придумываем.
     */
    private String creatureName(CB cb) {
        String own = text("character." + cb.id);
        if (own != null) return own;
        String id = lower(cb.id);
        String base = cb.translation != null ? text("character." + cb.translation) : null;
        if (base == null && id.startsWith("legacy")) base = text("character." + id.substring(6));
        if (base == null && cb.parent != null) base = text("character." + cb.parent);
        return base == null ? cb.id : base + " (" + cb.id + ")";
    }

    private static CreatureKind kind(CB cb, List<Attack> attacks) {
        String id = lower(cb.id), g = lower(cb.group == null ? "" : cb.group);
        if (id.startsWith("legacy") || id.equals("charybdisold")) return CreatureKind.LEGACY;
        if (cb.husk || g.contains("husk") || id.contains("husk")) return CreatureKind.HUSK;
        if (id.equals("leucocyte") || id.equals("terminalcell") || g.contains("thalamus")) return CreatureKind.THALAMUS;
        if (id.contains("guardian") || id.contains("defensebot") || id.contains("repairbot")) return CreatureKind.GUARDIAN;
        if (id.equals("ancient") || id.equals("jove")) return CreatureKind.LEVIATHAN;
        boolean attacksPeople = false;
        for (AiTarget t : cb.targets.values()) if (t.tag().equals("human") && t.state().toLowerCase(Locale.ROOT).contains("attack") || t.tag().equals("human") && t.state().equalsIgnoreCase("aggressive")) attacksPeople = true;
        if (cb.pet || cb.combat <= 10 && !attacksPeople) return CreatureKind.PET;
        double size = Math.max(cb.combat, cb.vitality);
        if (size >= 2000) return CreatureKind.LEVIATHAN;
        if (size >= 900) return CreatureKind.LARGE;
        if (size > 150) return CreatureKind.MEDIUM;
        return CreatureKind.SMALL;
    }

    // ---------- где встречаются (случайные события) ----------

    private Map<String, List<Spawn>> readSpawns() {
        // вид → биом → {minDiff, maxDiff, minAmount, maxAmount, events}, места
        Map<String, Map<String, int[]>> agg = new HashMap<>();
        Map<String, Map<String, Set<String>>> places = new HashMap<>();
        for (Path f : game.files("RandomEvents")) {
            Element root = GameXml.root(f);
            if (root == null) continue;
            for (Element e : GameXml.descendants(root)) {
                if (!GameXml.is(e, "monsterevent")) continue;
                String sp = a(e, "speciesname");
                if (sp == null) sp = a(e, "characterfile");
                if (sp == null || sp.isEmpty()) continue;
                sp = sp.replace('\\', '/');
                if (sp.contains("/")) sp = sp.substring(sp.lastIndexOf('/') + 1);
                if (sp.toLowerCase(Locale.ROOT).endsWith(".xml")) sp = sp.substring(0, sp.length() - 4);
                String biome = "";
                int minD = 0, maxD = 100;
                for (Node n = e.getParentNode(); n instanceof Element p; n = p.getParentNode()) {
                    if (biome.isEmpty() && a(p, "biome") != null) biome = lower(a(p, "biome"));
                    minD = Math.max(minD, intAttr(p, "minleveldifficulty", 0));
                    maxD = Math.min(maxD, intAttr(p, "maxleveldifficulty", 100));
                }
                int amount = intAttr(e, "amount", 1);
                int lo = intAttr(e, "minamount", amount), hi = Math.max(lo, intAttr(e, "maxamount", amount));
                int[] v = agg.computeIfAbsent(lower(sp), k -> new LinkedHashMap<>()).computeIfAbsent(biome, k -> new int[]{100, 0, Integer.MAX_VALUE, 0, 0});
                v[0] = Math.min(v[0], minD); v[1] = Math.max(v[1], maxD); v[2] = Math.min(v[2], lo); v[3] = Math.max(v[3], hi); v[4]++;
                Set<String> pl = places.computeIfAbsent(lower(sp), k -> new HashMap<>()).computeIfAbsent(biome, k -> new LinkedHashSet<>());
                for (String s : GameXml.list(GameXml.attr(e, "spawntype", "mainpath"))) pl.add(lower(s));
            }
        }
        Map<String, List<Spawn>> out = new HashMap<>();
        for (Map.Entry<String, Map<String, int[]>> e : agg.entrySet()) {
            List<Spawn> l = new ArrayList<>();
            for (Map.Entry<String, int[]> b : e.getValue().entrySet()) {
                int[] v = b.getValue();
                String bn = b.getKey().isEmpty() ? tr("переходные уровни и любые биомы") : biomeName(b.getKey());
                l.add(new Spawn(b.getKey(), bn, v[0], v[1], new ArrayList<>(places.get(e.getKey()).get(b.getKey())), v[2], v[3], v[4]));
            }
            l.sort(Comparator.comparingInt(Spawn::minDifficulty));
            out.put(e.getKey(), l);
        }
        return out;
    }

    private String biomeName(String id) {
        String n = text("biomename." + id);
        if (n == null) n = text("biomename.the" + id);
        return n != null ? n : id;
    }

    // ---------- NPC ----------

    /** Название как в игре (на выбранном языке, иначе английское), иначе идентификатор. */
    private String npcName(String id) {
        String n = text("npctitle." + id);
        if (n == null) n = text("charactername." + id);
        if (n == null) n = text("jobname." + id);
        return n != null ? n : id;
    }

    private static NpcKind npcKind(String set, String id, Element e) {
        String s = lower(set), i = lower(id);
        if (s.contains("tutorial")) return NpcKind.TUTORIAL;
        if (i.startsWith("hireable")) return NpcKind.HIREABLE;
        if (i.contains("hostage") || i.contains("prisoner")) return NpcKind.CIVIL;
        if (s.startsWith("combatmission")) return NpcKind.ENEMY;
        if (s.contains("abandoned")) return NpcKind.ENEMY;
        if (s.contains("escort")) return NpcKind.CIVIL;
        if (s.contains("custom") || s.contains("special")) return NpcKind.SPECIAL;
        return NpcKind.STATION;
    }

    private List<Npc> readNpcs(List<MissionInfo> missions) {
        List<Npc> out = new ArrayList<>();
        Map<String, Integer> index = new HashMap<>();
        for (Path f : game.files("NPCSets")) {
            Element root = GameXml.root(f);
            if (root == null) continue;
            for (Element setEl : entries(root)) {
                String set = GameXml.attr(setEl, "identifier", "");
                for (Element e : GameXml.children(setEl, "npc")) {
                    String id = a(e, "identifier");
                    if (id == null) continue;
                    String job = a(e, "job");
                    String jobName = job == null ? null : text("jobname." + job);
                    if (jobName == null) jobName = job;
                    String faction = a(e, "faction");
                    String factionName = faction == null ? null : text("faction." + faction);
                    List<String> tags = GameXml.list(a(e, "tags"));
                    List<Npc.GearSet> gear = new ArrayList<>();
                    for (Element is : GameXml.children(e, "itemset")) gear.add(new Npc.GearSet(numOr(is, "commonness", 1), gear(is)));
                    Npc n = new Npc(id, npcName(id), npcKind(set, id, e), set, job, jobName, faction, factionName != null ? factionName : faction,
                            GameXml.attr(e, "behavior", ""), numOr(e, "healthmultiplier", 1), numOr(e, "aimaccuracy", -1), numOr(e, "aimspeed", -1),
                            numOr(e, "skillmultiplier", 1), intAttr(e, "basesalary", 0), intAttr(e, "experiencepoints", 0),
                            GameXml.attr(e, "campaigninteractiontype", ""), tags, gear, game.modOf(f));
                    String key = lower(set) + "/" + lower(id);
                    Integer at = index.get(key);
                    if (at != null) out.set(at, n); else { index.put(key, out.size()); out.add(n); }
                }
            }
        }
        Map<String, Integer> names = new HashMap<>();
        for (Npc n : out) names.merge(n.kind() + "|" + n.name(), 1, Integer::sum);
        out.replaceAll(n -> names.get(n.kind() + "|" + n.name()) < 2 ? n : new Npc(n.id(), n.name() + " (" + n.id() + ")", n.kind(), n.set(),
                n.job(), n.jobName(), n.faction(), n.factionName(), n.behavior(), n.healthMultiplier(), n.aimAccuracy(), n.aimSpeed(),
                n.skillMultiplier(), n.salary(), n.experience(), n.interaction(), n.tags(), n.gear(), n.mod()));
        out.sort(Comparator.comparing((Npc n) -> n.kind().ordinal()).thenComparing(Npc::name));
        return out;
    }

    private List<Npc.Gear> gear(Element parent) {
        List<Npc.Gear> l = new ArrayList<>();
        for (Element it : GameXml.children(parent, "item")) {
            String id = a(it, "identifier");
            if (id == null || "false".equalsIgnoreCase(a(it, "showpreview")) || "pvp".equalsIgnoreCase(a(it, "gamemode"))) continue;
            itemName(id);
            l.add(new Npc.Gear(id, Math.max(1, intAttr(it, "amount", 1)), GameXml.bool(it, "equip", false), GameXml.bool(it, "infinite", false), gear(it)));
        }
        // одинаковые патроны подряд (6× revolverround) — одной строкой
        List<Npc.Gear> merged = new ArrayList<>();
        for (Npc.Gear g : l) {
            Npc.Gear last = merged.isEmpty() ? null : merged.get(merged.size() - 1);
            if (last != null && last.itemId().equals(g.itemId()) && last.inside().isEmpty() && g.inside().isEmpty() && last.equipped() == g.equipped())
                merged.set(merged.size() - 1, new Npc.Gear(g.itemId(), last.amount() + g.amount(), g.equipped(), g.infinite() || last.infinite(), List.of()));
            else merged.add(g);
        }
        return merged;
    }

    // ---------- профессии ----------

    private List<CrewJob> readJobs() {
        List<CrewJob> out = new ArrayList<>();
        for (Path f : game.files("Jobs")) {
            Element root = GameXml.root(f);
            if (root == null) continue;
            for (Element e : entries(root)) {
                if (!GameXml.is(e, "job")) continue;
                String id = a(e, "identifier");
                if (id == null || GameXml.bool(e, "hidden", false)) continue;
                List<CrewJob.SkillRange> skills = new ArrayList<>();
                Element sk = GameXml.child(e, "skills");
                if (sk != null) for (Element s : GameXml.children(sk, "skill")) {
                    String sid = a(s, "identifier");
                    List<String> lv = GameXml.list(GameXml.attr(s, "level", "0"));
                    int lo = (int) parse(lv.isEmpty() ? "0" : lv.get(0)), hi = lv.size() > 1 ? (int) parse(lv.get(1)) : lo;
                    String sn = text("skillname." + sid);
                    skills.add(new CrewJob.SkillRange(sid, sn != null ? sn : sid, lo, hi, GameXml.bool(s, "primary", false)));
                }
                List<Npc.GearSet> gear = new ArrayList<>();
                for (Element is : GameXml.children(e, "itemset")) gear.add(new Npc.GearSet(1, gear(is)));
                List<String> auto = new ArrayList<>();
                Element ao = GameXml.child(e, "autonomousobjectives");
                if (ao != null) for (Element o : GameXml.children(ao, "order")) {
                    String oid = lower(a(o, "identifier"));
                    String name = orderNames.getOrDefault(oid, oid);
                    String opt = a(o, "option");
                    if (opt != null && !opt.isEmpty()) name += " (" + ORDER_OPTIONS.getOrDefault(lower(opt), opt) + ")";
                    auto.add(name);
                }
                List<String> orders = new ArrayList<>(ordersByJob.getOrDefault(lower(id), List.of()));
                Element ap = GameXml.child(e, "appropriateorders");
                if (ap != null) for (Element o : GameXml.children(ap)) {
                    String oid = lower(GameXml.attr(o, "identifier", ""));
                    String name = orderNames.getOrDefault(oid, oid);
                    if (!oid.isEmpty() && !orders.contains(name)) orders.add(name);
                }
                if (skills.isEmpty()) continue;
                String name = text("jobname." + id);
                out.add(new CrewJob(id, name != null ? name : id, text("jobdescription." + id), skills, gear, auto, orders,
                        numOr(e, "pricemultiplier", 1), numOr(e, "vitalitymodifier", 0), intAttr(e, "maxnumber", 0), GameXml.attr(e, "idlebehavior", ""),
                        !GameXml.bool(e, "hiddenjob", false)));
            }
        }
        return out;
    }

    private static final Map<String, String> ORDER_OPTIONS = Map.of("pumpout", tr("откачивать воду"), "pumpin", tr("набирать воду"),
            "powerup", tr("запустить"), "shutdown", tr("заглушить"), "maintainposition", tr("держать позицию"), "navigatetodestination", tr("вести к цели"),
            "navigateback", tr("вести назад"), "fireatwill", tr("огонь по готовности"), "holdfire", tr("не стрелять"));

    private static double parse(String s) { try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return 0; } }

    // ---------- миссии ----------

    private List<MissionInfo> readMissions() {
        Map<String, MissionInfo> out = new LinkedHashMap<>();
        for (Path f : game.files("Missions")) {
            Element root = GameXml.root(f);
            if (root == null) continue;
            boolean pvp = f.getFileName().toString().toLowerCase(Locale.ROOT).contains("pvp");
            for (Element e : entries(root)) {
                String id = a(e, "identifier");
                if (id == null || id.isEmpty()) continue;
                try { out.put(lower(id), mission(e, id, pvp, game.modOf(f))); }
                catch (Exception ex) { System.err.println(tr("Миссия ") + id + tr(" не прочитана: ") + ex); }
            }
        }
        List<MissionInfo> l = new ArrayList<>(out.values());
        l.sort(Comparator.comparing((MissionInfo m) -> m.kind().ordinal()).thenComparingInt(MissionInfo::difficulty).thenComparing(MissionInfo::name));
        return l;
    }

    private String key(Element e, String attr, String prefix, String tid) {
        String k = a(e, attr);
        if (k != null && !k.isEmpty()) {
            String v = text(k);
            if (v != null) return v;
            if (!k.contains(".")) return clean(k);
        }
        return text(prefix + "." + tid);
    }

    private MissionInfo mission(Element e, String id, boolean pvp, String mod) {
        String tid = a(e, "textidentifier");
        if (tid == null || tid.isEmpty()) tid = id;
        MissionKind kind = MissionKind.byElement(e.getTagName());
        String type = GameXml.attr(e, "type", e.getTagName());
        String name = key(e, "name", "missionname", tid);
        if (name == null && !tid.equals(id)) name = text("missionname." + id);
        if (name == null) name = id;
        String desc = key(e, "description", "missiondescription", tid);
        if (desc == null) desc = text("missiondescription." + id);
        if (desc == null) desc = text("missiondescriptionneutral." + tid);
        List<MissionInfo.Message> messages = new ArrayList<>();
        // командные миссии (PvP): у каждой стороны своё описание
        for (int team = 1; team <= 2; team++) {
            String d = text("missiondescription" + team + "." + tid);
            if (d == null || d.equals(desc)) continue;
            String side = text("missionteam" + team + "." + tid);
            if (desc == null) desc = d; else messages.add(new MissionInfo.Message(side != null ? tr("За сторону «") + side + "»" : null, d));
        }
        String success = key(e, "successmessage", "missionsuccess", tid);
        String failure = key(e, "failuremessage", "missionfailure", tid);

        List<MissionInfo.Reputation> rep = new ArrayList<>();
        List<String> locations = new ArrayList<>();
        Map<String, int[]> monsters = new LinkedHashMap<>(), npcs = new LinkedHashMap<>(), items = new LinkedHashMap<>();
        Map<String, String> monsterNotes = new HashMap<>(), npcNotes = new HashMap<>(), itemNotes = new HashMap<>();
        List<String> facts = new ArrayList<>();
        int reward = intAttr(e, "reward", 0);
        String rewardNote = null;

        Map<String, List<String>> charTypes = new HashMap<>();
        Element ct = GameXml.child(e, "charactertypes");
        if (ct != null) for (Element t : GameXml.children(ct, "charactertype")) {
            List<String> ids = new ArrayList<>();
            for (Element c : GameXml.children(t, "character")) if (a(c, "identifier") != null) ids.add(npcKey(c));
            charTypes.put(lower(GameXml.attr(t, "typeidentifier", "")), ids);
        }

        for (Element c : GameXml.children(e)) {
            String tag = lower(c.getTagName());
            switch (tag) {
                case "message" -> {
                    String h = text(a(c, "header")), t = text(a(c, "text"));
                    if (t != null || h != null) messages.add(new MissionInfo.Message(h, t));
                }
                case "reputation" -> {
                    String fid = GameXml.attr(c, "identifier", "location");
                    String fn = fid.equalsIgnoreCase("location") ? tr("фракция станции-заказчика") : text("faction." + fid);
                    rep.add(new MissionInfo.Reputation(fid, fn != null ? fn : fid, numOr(c, "amount", 0), numOr(c, "amountforopposingfaction", 0)));
                }
                case "locationtype" -> {
                    String from = a(c, "from"), to = a(c, "to"), only = a(c, "identifier");
                    if (only != null) locations.add(tr("только в: ") + Glossary.location(only));
                    else if (from != null || to != null) locations.add(Glossary.location(from == null ? "any" : from) + " → " + Glossary.location(to == null ? "any" : to));
                }
                case "locationtypechange" -> facts.add(tr("После успеха локация «") + Glossary.location(a(c, "from")) + tr("» может стать «") + Glossary.location(a(c, "to")) + "»");
                case "monster", "monsters" -> {
                    String sp = a(c, "character");
                    if (sp == null) sp = a(c, "speciesname");
                    if (sp == null) sp = a(c, "characterfile");
                    if (sp == null) sp = a(c, "identifier");
                    if (sp == null) break;
                    int amount = intAttr(c, "amount", 1);
                    add(monsters, sp, intAttr(c, "min", amount), intAttr(c, "max", intAttr(c, "min", amount)));
                    if (GameXml.bool(c, "multiplayeronly", false)) monsterNotes.put(sp, tr("часть — только в мультиплеере"));
                }
                case "characters" -> {
                    for (Element ch : GameXml.children(c, "character")) {
                        boolean mp = GameXml.bool(ch, "multiplayeronly", false);
                        String cid = a(ch, "identifier") == null ? null : npcKey(ch), typeId = a(ch, "typeidentifier");
                        if (cid != null && a(ch, "from") == null && speciesKeys.contains(lower(cid))) {
                            // в заброшенных аванпостах так записаны монстры: <Character identifier="husk" min= max=>
                            add(monsters, cid, mp ? 0 : intAttr(ch, "min", 1), intAttr(ch, "max", intAttr(ch, "min", 1)));
                            if (GameXml.bool(ch, "requirekill", false)) monsterNotes.put(cid, tr("нужно убить"));
                            if (mp) monsterNotes.put(cid, tr("часть — только в мультиплеере"));
                        } else if (cid != null) {
                            add(npcs, cid, mp ? 0 : 1, 1);
                            if (GameXml.bool(ch, "requirekill", false)) npcNotes.put(cid, tr("цель — устранить"));
                            else if (mp) npcNotes.merge(cid, tr("часть — только в мультиплеере"), (x, y) -> x);
                        } else if (typeId != null) {
                            List<String> opts = charTypes.getOrDefault(lower(typeId), List.of());
                            int lo = intAttr(ch, "minamount", 1), hi = Math.max(lo, intAttr(ch, "maxamount", lo));
                            for (String o : opts) {
                                add(npcs, o, lo, hi);
                                if (opts.size() > 1) npcNotes.put(o, tr("один из ") + opts.size() + tr(" вариантов, по сложности уровня"));
                            }
                        }
                    }
                }
                case "items" -> {
                    for (Element it : GameXml.children(c, "item")) {
                        String iid = a(it, "identifier");
                        if (iid == null) continue;
                        itemName(iid);
                        if (kind == MissionKind.CARGO) {
                            int per = intAttr(it, "reward", 0), max = intAttr(it, "maxcount", 1);
                            add(items, iid, 1, max);
                            itemNotes.put(iid, tr("ящик") + (per > 0 ? ", " + per + tr(" mk за ящик") : ""));
                            for (Element in : GameXml.children(it, "item")) {
                                String cid = a(in, "identifier");
                                if (cid == null) continue;
                                itemName(cid);
                                int am = Math.max(1, intAttr(in, "amount", 1));
                                add(items, cid, am, am);
                                itemNotes.putIfAbsent(cid, tr("в каждом ящике"));
                            }
                            if (per > 0) { reward = Math.max(reward, 0) + per * max; rewardNote = per + tr(" mk за ящик × до ") + max + tr(" ящ."); }
                        } else {
                            add(items, iid, 1, 1);
                            for (Element in : GameXml.children(it, "item")) {
                                String cid = a(in, "identifier");
                                if (cid == null) continue;
                                itemName(cid);
                                int am = Math.max(1, intAttr(in, "amount", 1));
                                add(items, cid, am, am);
                                itemNotes.putIfAbsent(cid, tr("внутри: ") + itemName(iid));
                            }
                        }
                    }
                }
                case "target" -> {
                    String iid = a(c, "itemidentifier");
                    if (iid == null) for (Element x : GameXml.descendants(c)) if (a(x, "itemidentifier") != null) { iid = a(x, "itemidentifier"); break; }
                    String itag = a(c, "itemtag");
                    if (iid != null) {
                        itemName(iid);
                        int lo = intAttr(c, "minamount", 1);
                        add(items, iid, lo, Math.max(lo, intAttr(c, "maxamount", lo)));
                        String sp = a(c, "spawntype");
                        if (sp != null) itemNotes.put(iid, tr("где: ") + placesRu(sp));
                    } else if (itag != null) facts.add(tr("Цель: предмет с тегом «") + itag + "»" + (a(c, "spawntype") != null ? " (" + placesRu(a(c, "spawntype")) + ")" : ""));
                }
                case "triggerevent" -> { if (GameXml.bool(c, "campaignonly", false)) facts.add(tr("В кампании запускает сюжетное событие")); }
                default -> { }
            }
        }
        // предметы и цели, заданные атрибутами
        String iid = a(e, "itemidentifier");
        if (iid != null) { itemName(iid); add(items, iid, 1, 1); itemNotes.putIfAbsent(iid, tr("найти и поднять")); }
        String itag = a(e, "itemtag");
        if (iid == null && itag != null && items.isEmpty()) facts.add(tr("Цель: предмет с тегом «") + itag + "»");
        for (String t : GameXml.list(firstNonNull(a(e, "targetitems"), a(e, "targetitem")))) {
            itemName(t);
            add(items, t, 1, 1);
            itemNotes.put(t, tr("цель — уничтожить"));
        }
        for (String t : GameXml.list(a(e, "targetenemies"))) { add(monsters, t, intAttr(e, "minenemycount", 1), intAttr(e, "minenemycount", 1)); monsterNotes.put(t, tr("стражи руин")); }
        if (a(e, "monsterfile") != null && monsters.isEmpty()) add(monsters, a(e, "monsterfile"), 1, 1);
        if (a(e, "bossfile") != null) { add(monsters, a(e, "bossfile"), 1, 1); monsterNotes.put(a(e, "bossfile"), tr("босс")); }
        if (a(e, "minionfile") != null) {
            int n = intAttr(e, "minioncount", 1);
            add(monsters, a(e, "minionfile"), n, n);
            monsterNotes.put(a(e, "minionfile"), tr("приспешники босса"));
        }

        // особые условия
        if (pvp) facts.add(tr("Только мультиплеер (PvP)"));
        if (GameXml.bool(e, "requireruin", false)) facts.add(tr("Нужны инопланетные руины на уровне"));
        if (GameXml.bool(e, "requirewreck", false)) facts.add(tr("Нужен затонувший корабль на уровне"));
        if (GameXml.bool(e, "requirethalamuswreck", false)) facts.add(tr("Нужен обломок, захваченный таламусом"));
        if (GameXml.bool(e, "singleplayeronly", false)) facts.add(tr("Только в одиночной игре"));
        if (GameXml.bool(e, "multiplayeronly", false)) facts.add(tr("Только в мультиплеере"));
        if (GameXml.bool(e, "campaignonly", false)) facts.add(tr("Только в кампании"));
        if (GameXml.bool(e, "allowretry", false)) facts.add(tr("При провале можно попробовать снова"));
        if (GameXml.bool(e, "requiredestinationreached", false)) facts.add(tr("Нужно добраться до цели уровня"));
        if (GameXml.bool(e, "blocklocationtypechanges", false)) facts.add(tr("Пока миссия активна, тип локации не меняется"));
        if (GameXml.bool(e, "issideobjective", false) || GameXml.bool(e, "sideobjective", false)) facts.add(tr("Побочная цель: выдаётся вместе с другой миссией"));
        int minLd = intAttr(e, "minleveldifficulty", -1), maxLd = intAttr(e, "maxleveldifficulty", -1);
        if (minLd >= 0 || maxLd >= 0) facts.add(tr("Сложность уровня: ") + (minLd >= 0 ? tr("от ") + minLd : "") + (maxLd >= 0 ? (minLd >= 0 ? " " : "") + tr("до ") + maxLd : ""));
        String sp = firstNonNull(a(e, "spawntype"), a(e, "positiontype"));
        if (sp != null) facts.add(tr("Где: ") + placesRu(sp));
        if (a(e, "faction") != null) { String fn = text("faction." + a(e, "faction")); facts.add(tr("Противник: ") + (fn != null ? fn : a(e, "faction"))); }
        if (a(e, "requiredlocationfaction") != null) { String fn = text("faction." + a(e, "requiredlocationfaction")); facts.add(tr("Выдаёт только фракция: ") + (fn != null ? fn : a(e, "requiredlocationfaction"))); }
        if (a(e, "baseescortedcharacters") != null)
            facts.add(tr("Пассажиров: ") + Math.round(numOr(e, "baseescortedcharacters", 1)) + (numOr(e, "scalingescortedcharacters", 0) > 0
                    ? " + " + fmt(numOr(e, "scalingescortedcharacters", 0)) + tr(" за каждого игрока") : ""));
        if (numOr(e, "terroristchance", 0) > 0) facts.add(tr("Шанс, что среди пассажиров террорист: ") + Math.round(numOr(e, "terroristchance", 0) * 100) + "%");
        if (a(e, "resourcehandoveramount") != null) facts.add(tr("Заказчику сдаётся: ") + Math.round(numOr(e, "resourcehandoveramount", 0) * 100) + tr("% добытого"));
        if (a(e, "requireddeliveryamount") != null) facts.add(tr("Нужно доставить: ") + Math.round(numOr(e, "requireddeliveryamount", 0) * 100) + tr("% предметов"));
        if (a(e, "nestobjectamount") != null) facts.add(tr("Объектов гнезда: ") + intAttr(e, "nestobjectamount", 0));
        if (a(e, "targets") != null) facts.add(tr("Точек сканирования: ") + intAttr(e, "targets", 0));
        if (a(e, "addedmissiondifficultyperplayer") != null) facts.add(tr("Сложность растёт на ") + intAttr(e, "addedmissiondifficultyperplayer", 0) + tr(" за каждого игрока"));
        if (a(e, "maxsonarmarkerdistance") != null) facts.add(tr("Метка на сонаре видна с ") + Math.round(numOr(e, "maxsonarmarkerdistance", 0) / 100) + tr(" м"));

        // «Добыча минералов ([resourcename1])» → название минерала
        Map<String, String> tokens = tokens(counts(monsters, monsterNotes), counts(items, itemNotes));
        if (a(e, "resourcehandoveramount") != null) tokens.put("handoverpercentage", Math.round(numOr(e, "resourcehandoveramount", 0) * 100) + tr("% добытого"));
        name = fill(name, tokens);
        desc = fill(desc, tokens);
        success = fill(success, tokens);
        failure = fill(failure, tokens);
        messages.replaceAll(m -> new MissionInfo.Message(fill(m.header(), tokens), fill(m.text(), tokens)));
        return new MissionInfo(id, type, kind, name, desc, success, failure, messages, reward, rewardNote, intAttr(e, "difficulty", 0),
                intAttr(e, "commonness", 0), GameXml.list(a(e, "tags")), rep, locations,
                counts(monsters, monsterNotes), counts(npcs, npcNotes), counts(items, itemNotes), facts, mod, List.of());
    }

    /**
     * Диалоги выдачи миссий из сценарных событий: реплики (ConversationAction) и выбранные ответы (Option) на пути от начала
     * события до MissionAction. Миссия находится по missionidentifier или по тегу; сначала — события именно этой миссии,
     * потом — по самым узким тегам; не больше трёх диалогов на миссию.
     */
    private List<MissionInfo> attachBriefings(List<MissionInfo> missions) {
        Map<String, List<List<String>>> byId = new HashMap<>(), byTag = new HashMap<>();
        for (Path f : game.files("RandomEvents")) {
            Element root = GameXml.root(f);
            if (root == null) continue;
            for (Element ma : GameXml.descendants(root)) {
                if (!GameXml.is(ma, "missionaction")) continue;
                List<String> lines = dialogTo(ma);
                if (lines.isEmpty()) continue;
                String mid = a(ma, "missionidentifier"), tag = a(ma, "missiontag");
                if (mid != null) addDialog(byId, lower(mid), lines);
                else if (tag != null) addDialog(byTag, lower(tag), lines);
            }
        }
        Map<String, Integer> tagUse = new HashMap<>();
        for (MissionInfo m : missions) for (String t : m.tags()) tagUse.merge(lower(t), 1, Integer::sum);
        List<MissionInfo> out = new ArrayList<>();
        for (MissionInfo m : missions) {
            List<List<String>> b = new ArrayList<>(byId.getOrDefault(lower(m.id()), List.of()));
            List<String> tags = new ArrayList<>();
            for (String t : m.tags()) tags.add(lower(t));
            tags.sort(Comparator.comparingInt(t -> tagUse.getOrDefault(t, 0)));
            for (String t : tags) for (List<String> d : byTag.getOrDefault(t, List.of())) if (!b.contains(d)) b.add(d);
            if (b.isEmpty()) { out.add(m); continue; }
            Map<String, String> tokens = tokens(m.monsters(), m.items());
            List<List<String>> filled = new ArrayList<>();
            for (List<String> d : b.subList(0, Math.min(3, b.size()))) {
                List<String> l = new ArrayList<>();
                for (String x : d) { String f = fill(x, tokens); if (f != null && !f.isEmpty() && !f.equals("—")) l.add(f); }
                filled.add(l);
            }
            out.add(m.withBriefings(filled));
        }
        return out;
    }

    private static void addDialog(Map<String, List<List<String>>> m, String key, List<String> lines) {
        List<List<String>> l = m.computeIfAbsent(key, k -> new ArrayList<>());
        if (!l.contains(lines)) l.add(lines);
    }

    /** Реплики, ведущие к элементу: предки-реплики и выбранные ответы, а также реплики перед ним на каждом уровне. */
    private List<String> dialogTo(Element target) {
        List<Element> path = new ArrayList<>();
        for (Node n = target; n instanceof Element e && !GameXml.is(e, "scriptedevent"); n = e.getParentNode()) path.add(0, e);
        List<String> lines = new ArrayList<>();
        for (Element e : path) {
            Node parent = e.getParentNode();
            if (parent instanceof Element p) for (Element sib : GameXml.children(p)) {
                if (sib == e) break;
                if (GameXml.is(sib, "conversationaction")) line(lines, sib, false);
            }
            if (e != target && GameXml.is(e, "conversationaction")) line(lines, e, false);
            if (GameXml.is(e, "option")) line(lines, e, true);
        }
        return lines.size() > 14 ? new ArrayList<>(lines.subList(lines.size() - 14, lines.size())) : lines;
    }

    private void line(List<String> lines, Element e, boolean answer) {
        String t = text(a(e, "text"));
        if (t == null) return;
        t = t.replace("‹speakername›", tr("заказчик")).replace("‹playername›", tr("вы"));
        String s = answer ? "— " + t : t;
        if (lines.isEmpty() || !lines.get(lines.size() - 1).equals(s)) lines.add(s);
    }

    /** Ссылка на NPC: «набор/идентификатор» (один и тот же идентификатор бывает в разных наборах). */
    private static String npcKey(Element ch) {
        String from = a(ch, "from"), id = a(ch, "identifier");
        return from == null || from.isEmpty() ? id : from + "/" + id;
    }

    /** Значения подстановок этой миссии: названия ресурсов и их количество, первый монстр. */
    private Map<String, String> tokens(List<MissionInfo.Count> monsters, List<MissionInfo.Count> items) {
        Map<String, String> t = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            t.put("resourcename" + (i + 1), itemName(items.get(i).id()).toLowerCase(Locale.ROOT));
            t.put("resourcequantity" + (i + 1), String.valueOf(items.get(i).max()));
        }
        if (!monsters.isEmpty()) {
            String sp = monsters.get(0).id(), n = null;
            for (Map.Entry<String, String> e : creatureNames.entrySet()) if (e.getKey().equalsIgnoreCase(sp)) n = e.getValue();
            t.put("monster", n != null ? n.replaceAll(" \\(.*\\)$", "").toLowerCase(Locale.ROOT) : sp);
        }
        return t;
    }

    /** Подставить значения миссии; оставшиеся метки (кроме ‹награда› — её подставляет карточка) убрать. */
    private static String fill(String s, Map<String, String> tokens) {
        if (s == null) return null;
        for (Map.Entry<String, String> e : tokens.entrySet()) s = s.replace("‹" + e.getKey() + "›", e.getValue());
        s = s.replaceAll(tr("\\s*\\(‹(?!награда)[^›]*›\\)"), "").replaceAll(tr("‹(?!награда)[^›]*›"), "")
                .replaceAll("  +", " ").replaceAll(" ([,.!?])", "$1").trim();
        if (!s.isEmpty() && Character.isLowerCase(s.charAt(0))) s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
        return s;
    }

    private static String firstNonNull(String x, String y) { return x != null ? x : y; }

    private static String fmt(double v) { return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(Math.round(v * 100) / 100.0).replace('.', baro.Tr.english() ? '.' : ','); }

    private static String placesRu(String csv) {
        List<String> l = new ArrayList<>();
        for (String s : GameXml.list(csv)) l.add(Glossary.place(s));
        return String.join(", ", l);
    }

    private static void add(Map<String, int[]> m, String id, int lo, int hi) {
        int[] v = m.computeIfAbsent(id, k -> new int[2]);
        v[0] += lo; v[1] += Math.max(lo, hi);
    }

    private static List<MissionInfo.Count> counts(Map<String, int[]> m, Map<String, String> notes) {
        List<MissionInfo.Count> l = new ArrayList<>();
        for (Map.Entry<String, int[]> e : m.entrySet()) l.add(new MissionInfo.Count(e.getKey(), e.getValue()[0], e.getValue()[1], notes.get(e.getKey())));
        return l;
    }

    /** Сборка: записать встроенную копию (только ваниль). Аргументы: путь к файлу, [язык]. */
    public static void main(String[] args) throws Exception {
        Path tmp = Files.createTempFile("baro-bestiary-", ".properties");
        Files.delete(tmp);
        tmp.toFile().deleteOnExit();
        System.setProperty("baro.settings", tmp.toString());
        baro.settings.AppSettings settings = baro.settings.AppSettings.openDefault();
        settings.put("game.mods", false);
        GameInstall game = GameInstall.locate(settings);
        if (game == null) { System.err.println(tr("Игра не найдена — встроенный бестиарий не обновлён")); System.exit(2); }
        Bestiary b = read(game, args.length > 1 ? args[1] : "Russian");
        b.write(new File(args[0]));
        System.out.println(tr("Записано: ") + args[0] + " (" + new File(args[0]).length() / 1024 + tr(" КБ)"));
        System.exit(0);
    }
}
