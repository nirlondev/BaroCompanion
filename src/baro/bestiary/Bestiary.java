package baro.bestiary;


import static baro.Tr.tr;
import baro.settings.AppSettings;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Бестиарий и все миссии: существа, люди-NPC, профессии экипажа, миссии любого типа.
 * Читается из файлов игры (с модами) при первом открытии вкладки; без игры — встроенная копия (/data/bestiary.bin,
 * по-английски — /data/bestiary.en.bin).
 */
public final class Bestiary implements Serializable {
    private static final long serialVersionUID = 1L;
    static final String RESOURCE = "/data/bestiary.bin", RESOURCE_EN = "/data/bestiary.en.bin";

    private final List<Creature> creatures;
    private final List<Npc> npcs;
    private final List<CrewJob> jobs;
    private final List<MissionInfo> missions;
    private final Map<String, String> itemNames;
    private final Map<String, Double> humanSpeeds;
    private String source;

    private transient Map<String, Creature> creatureIndex;
    private transient Map<String, Npc> npcIndex;

    public Bestiary(List<Creature> creatures, List<Npc> npcs, List<CrewJob> jobs, List<MissionInfo> missions,
                    Map<String, String> itemNames, Map<String, Double> humanSpeeds, String source) {
        this.creatures = new ArrayList<>(creatures);
        this.npcs = new ArrayList<>(npcs);
        this.jobs = new ArrayList<>(jobs);
        this.missions = new ArrayList<>(missions);
        this.itemNames = new HashMap<>(itemNames);
        this.humanSpeeds = new HashMap<>(humanSpeeds);
        this.source = source;
    }

    public List<Creature> creatures() { return Collections.unmodifiableList(creatures); }
    public List<Npc> npcs() { return Collections.unmodifiableList(npcs); }
    public List<CrewJob> jobs() { return Collections.unmodifiableList(jobs); }
    public List<MissionInfo> missions() { return Collections.unmodifiableList(missions); }
    /** Откуда данные: файлы игры или встроенные. */
    public String source() { return source; }

    /** Скорости человека (для сравнения): walk, run, swimslow, swimfast. */
    public double humanSpeed(String key) { Double v = humanSpeeds.get(key); return v == null ? 0 : v; }

    /** Название предмета из файлов игры (если предмета нет в каталоге справочника). */
    public String itemName(String id) { return itemNames.getOrDefault(id, id); }

    public Optional<Creature> creature(String id) {
        if (id == null) return Optional.empty();
        if (creatureIndex == null) {
            Map<String, Creature> m = new HashMap<>();
            for (Creature c : creatures) m.put(c.id().toLowerCase(Locale.ROOT), c);
            creatureIndex = m;
        }
        return Optional.ofNullable(creatureIndex.get(id.toLowerCase(Locale.ROOT)));
    }

    /** NPC по идентификатору или по «набор/идентификатор» (так на них ссылаются миссии). */
    public Optional<Npc> npc(String key) {
        if (key == null) return Optional.empty();
        if (npcIndex == null) {
            Map<String, Npc> m = new HashMap<>();
            for (Npc n : npcs) {
                m.putIfAbsent(n.id().toLowerCase(Locale.ROOT), n);
                m.put((n.set() + "/" + n.id()).toLowerCase(Locale.ROOT), n);
            }
            npcIndex = m;
        }
        String k = key.toLowerCase(Locale.ROOT);
        Npc n = npcIndex.get(k);
        if (n == null && k.contains("/")) n = npcIndex.get(k.substring(k.indexOf('/') + 1));
        return Optional.ofNullable(n);
    }

    /** Варианты вида (Hammerhead_m, Hammerheadgold_m… для Hammerhead). */
    public List<Creature> variantsOf(Creature c) {
        List<Creature> l = new ArrayList<>();
        for (Creature x : creatures) if (x.parent() != null && x.parent().equalsIgnoreCase(c.id())) l.add(x);
        return l;
    }

    /** Миссии, где участвует это существо. */
    public List<MissionInfo> missionsWith(Creature c) {
        List<MissionInfo> l = new ArrayList<>();
        for (MissionInfo m : missions) for (MissionInfo.Count k : m.monsters()) if (k.id().equalsIgnoreCase(c.id())) { l.add(m); break; }
        return l;
    }

    /** Миссии, где участвует этот NPC. */
    public List<MissionInfo> missionsWith(Npc n) {
        List<MissionInfo> l = new ArrayList<>();
        for (MissionInfo m : missions) for (MissionInfo.Count k : m.npcs()) if (npc(k.id()).orElse(null) == n) { l.add(m); break; }
        return l;
    }

    /** Существа, у которых в добыче есть предмет. */
    public List<Creature> droppedBy(String itemId) {
        List<Creature> l = new ArrayList<>();
        for (Creature c : creatures) for (Creature.Drop d : c.drops()) if (d.itemId().equals(itemId)) { l.add(c); break; }
        return l;
    }

    // ---------- загрузка ----------

    /** Данные из файлов игры (если включены и игра найдена), иначе встроенные; null — нет ни того, ни другого. */
    public static Bestiary load(AppSettings settings) {
        if (settings.getBoolean("game.files", true)) {
            try {
                baro.data.gamefiles.GameInstall game = baro.data.gamefiles.GameInstall.locate(settings);
                if (game != null) return baro.data.gamefiles.BestiaryReader.read(game, settings.getString("game.language", baro.Tr.gameLanguage()));
            } catch (Throwable t) {
                System.err.println(tr("Бестиарий из файлов игры не прочитан, используются встроенные данные: ") + t);
                t.printStackTrace();
            }
        }
        return embedded();
    }

    /** Встроенная копия или null. По-английски — копия из английских текстов игры, а если её нет — русская через словарь. */
    public static Bestiary embedded() {
        if (baro.Tr.english()) {
            Bestiary en = read(RESOURCE_EN);
            if (en != null) return en;
        }
        Bestiary b = read(RESOURCE);
        if (b != null && baro.Tr.english()) b = b.translated();
        return b;
    }

    private static String sourceOf(Bestiary b) {
        return tr("Встроенные данные: существ ") + b.creatures.size() + ", NPC " + b.npcs.size() + tr(", миссий ") + b.missions.size();
    }

    private static Bestiary read(String resource) {
        try (InputStream in = Bestiary.class.getResourceAsStream(resource)) {
            if (in == null) return null;
            try (ObjectInputStream o = new ObjectInputStream(new GZIPInputStream(new BufferedInputStream(in)))) {
                o.setObjectInputFilter(info -> {
                    Class<?> c = info.serialClass();
                    if (c == null) return ObjectInputFilter.Status.UNDECIDED;
                    while (c.isArray()) c = c.getComponentType();
                    String n = c.getName();
                    return c.isPrimitive() || n.startsWith("baro.bestiary.") || n.startsWith("java.util.") || n.startsWith("java.lang.")
                            ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
                });
                Bestiary b = (Bestiary) o.readObject();
                b.source = sourceOf(b);
                return b;
            }
        } catch (Exception e) {
            System.err.println(tr("Встроенный бестиарий не прочитан: ") + e);
            return null;
        }
    }

    /**
     * Русская встроенная копия для английского интерфейса, если английской нет: названия, описания и тексты миссий переводятся словарём
     * (официальный английский текст игры). Числа, связи и всё остальное не меняются.
     */
    Bestiary translated() {
        List<Creature> cs = new ArrayList<>();
        for (Creature c : creatures) {
            List<Creature.Spawn> sp = new ArrayList<>();
            for (Creature.Spawn s : c.spawns())
                sp.add(new Creature.Spawn(s.biome(), tr(s.biomeName()), s.minDifficulty(), s.maxDifficulty(), s.places(), s.minAmount(), s.maxAmount(), s.events()));
            cs.add(new Creature(c.id(), tr(c.name()), c.kind(), c.parent(), c.group(), c.mod(), c.health(), c.ai(), c.speeds(), c.attacks(),
                    c.armor(), c.drops(), sp, c.abilities(), c.scale(), c.canEnterSub(), c.needsAir(), c.humanoid(), c.boss()));
        }
        List<Npc> ns = new ArrayList<>();
        for (Npc n : npcs)
            ns.add(new Npc(n.id(), tr(n.name()), n.kind(), n.set(), n.job(), tr(n.jobName()), n.faction(), tr(n.factionName()), n.behavior(),
                    n.healthMultiplier(), n.aimAccuracy(), n.aimSpeed(), n.skillMultiplier(), n.salary(), n.experience(), n.interaction(),
                    n.tags(), n.gear(), n.mod()));
        List<CrewJob> js = new ArrayList<>();
        for (CrewJob j : jobs) {
            List<CrewJob.SkillRange> sk = new ArrayList<>();
            for (CrewJob.SkillRange s : j.skills()) sk.add(new CrewJob.SkillRange(s.id(), tr(s.name()), s.min(), s.max(), s.primary()));
            js.add(new CrewJob(j.id(), tr(j.name()), tr(j.description()), sk, j.gear(), j.autonomous(), j.orders(), j.priceMultiplier(),
                    j.vitalityModifier(), j.maxNumber(), j.idleBehavior(), j.crew()));
        }
        List<MissionInfo> ms = new ArrayList<>();
        for (MissionInfo m : missions) {
            List<MissionInfo.Message> msg = new ArrayList<>();
            for (MissionInfo.Message x : m.messages()) msg.add(new MissionInfo.Message(tr(x.header()), tr(x.text())));
            List<MissionInfo.Reputation> rep = new ArrayList<>();
            for (MissionInfo.Reputation r : m.reputation()) rep.add(new MissionInfo.Reputation(r.faction(), tr(r.factionName()), r.amount(), r.opposing()));
            ms.add(new MissionInfo(m.id(), m.type(), m.kind(), tr(m.name()), tr(m.description()), tr(m.success()), tr(m.failure()), msg,
                    m.reward(), m.rewardNote(), m.difficulty(), m.commonness(), m.tags(), rep, m.locations(), m.monsters(), m.npcs(), m.items(),
                    m.facts(), m.mod(), m.briefings()));
        }
        return new Bestiary(cs, ns, js, ms, itemNames, humanSpeeds, source);
    }

    private static String tr(String s) { return s == null ? null : baro.Tr.tr(s); }

    /** Сохранить как встроенную копию (для сборки). */
    public void write(File file) throws IOException {
        try (ObjectOutputStream o = new ObjectOutputStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(file))))) {
            o.writeObject(this);
        }
    }
}
