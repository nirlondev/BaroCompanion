package baro.bestiary;


import static baro.Tr.tr;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Русские названия игровых терминов: цели и состояния ИИ, места появления, локации, поведение NPC. */
public final class Glossary {
    private Glossary() {}

    private static final Map<String, String> TAGS = new HashMap<>(), STATES = new HashMap<>(), PLACES = new HashMap<>(),
            LOCATIONS = new HashMap<>(), BEHAVIORS = new HashMap<>(), INTERACTIONS = new HashMap<>(), LIMBS = new HashMap<>();

    static {
        put(TAGS, "human", tr("люди"), "weaker", tr("слабее его"), "stronger", tr("сильнее его"), "dead", tr("трупы"), "room", tr("отсеки подлодки (изнутри)"),
                "wall", tr("стены и корпус"), "door", tr("двери и люки"), "sonar", tr("активный сонар"), "turret", tr("турели"), "searchlight", tr("прожекторы"),
                "provocative", tr("провоцирующее (громкое, светящееся)"), "decoy", tr("приманки"), "weapon", tr("оружие в руках"), "tool", tr("инструменты в руках"),
                "monsterfood", tr("корм для монстров"), "husk", tr("хаски"), "nasonov", tr("феромон Насонова"), "engine", tr("работающие двигатели"),
                "light", tr("свет"), "owner", tr("хозяин"), "food", tr("еда"), "pet", tr("питомцы"), "player", tr("игроки"), "monster", tr("монстры"),
                "submarine", tr("подлодка"), "character", tr("персонажи"), "outpost", tr("станции"), "hull", tr("отсеки"), "leucocyte", tr("лейкоциты"),
                "thalamus", tr("таламус"), "ancient", tr("древние"), "huskcontainer", tr("контейнер хаска"), "crawler", tr("ползуны"), "mudraptor", tr("грязевые рапторы"),
                "tigerthresher", tr("тигровые молотильщики"), "hammerhead", tr("молотоглавы"), "watcher", tr("наблюдатели"), "moloch", tr("молохи"),
                "crawlerbroodmother", tr("матка ползунов"), "crawler_large", tr("крупные ползуны"), "guardian", tr("стражи"), "fractalguardian", tr("фрактальные стражи"),
                "spineling", tr("шипочеревики"), "swarmfeeder", tr("роевики"), "bonethresher", tr("костяные молотильщики"), "charybdis", tr("харибды"),
                "endworm", tr("черви-разрушители"), "latcher", tr("присоски"), "mantis", tr("богомолы"), "psilotoad", tr("псиложабы"), "peanut", tr("арахисы"),
                "orangeboy", tr("оранжевые мальчики"), "balloon", tr("шарики"), "defensebot", tr("боты защиты"), "enemy", tr("враги"), "ally", tr("союзники"),
                "sonarbeacon", tr("сонарные маяки"), "flare", tr("сигнальные огни"), "reactor", tr("реактор"), "corpse", tr("трупы"), "egg", tr("яйца"),
                "eggs", tr("яйца"), "nest", tr("гнездо"), "plant", tr("растения"), "ballastflora", tr("балластная флора"));
        put(STATES, "attack", tr("атакует"), "aggressive", tr("агрессивен к"), "passiveaggressive", tr("атакует, если рядом или его тронули"),
                "avoid", tr("избегает"), "escape", tr("убегает от"), "eat", tr("ест"), "protect", tr("защищает"), "follow", tr("следует за"),
                "observe", tr("наблюдает за"), "idle", tr("не обращает внимания на"), "freeze", tr("замирает при"), "flee", tr("убегает от"));
        put(PLACES, "mainpath", tr("основной путь"), "sidepath", tr("боковые ходы"), "cave", tr("пещеры"), "ruin", tr("инопланетные руины"),
                "wreck", tr("затонувшие корабли"), "beaconstation", tr("станции-маяки"), "abyss", tr("бездна"), "abysscave", tr("пещеры бездны"),
                "outpost", tr("станции"), "none", tr("особое событие"), "enemysubmarine", tr("вражеская подлодка"), "any", tr("любое место"));
        put(LOCATIONS, "anyoutpost", tr("любая станция"), "any", tr("любое место"), "none", tr("пустая точка"), "city", tr("колония"), "outpost", tr("жилой аванпост"),
                "military", tr("военный аванпост"), "research", tr("исследовательский аванпост"), "mine", tr("шахтёрский аванпост"), "lair", tr("нерестилище"),
                "ruins", tr("руины"), "abandoned", tr("покинутый аванпост"), "explored", tr("исследованное место"), "endlocation", tr("конец пути"),
                "explorationpoint", tr("точка исследования"), "huskhive", tr("улей хасков"), "blackmarket", tr("чёрный рынок"));
        put(BEHAVIORS, "patrol", tr("патрулирует"), "passive", tr("пассивен (защищается)"), "stayinhull", tr("остаётся в своём отсеке"),
                "staywithincamera", tr("держится в поле зрения"), "active", tr("активен"));
        put(INTERACTIONS, "store", tr("торговец"), "hire", tr("отдел кадров (найм)"), "upgrade", tr("улучшения подлодки"), "purchasesub", tr("продажа подлодок"),
                "crew", tr("экипаж"), "medicalclinic", tr("клиника (лечение)"), "talk", tr("разговор"), "map", tr("карта"), "none", "—");
        put(LIMBS, "head", tr("голова"), "torso", tr("туловище"), "tail", tr("хвост"), "leftleg", tr("левая нога"), "rightleg", tr("правая нога"),
                "leftarm", tr("левая рука"), "rightarm", tr("правая рука"), "jaw", tr("челюсть"), "lefthand", tr("левая кисть"), "righthand", tr("правая кисть"),
                "leftfoot", tr("левая стопа"), "rightfoot", tr("правая стопа"), "leftthigh", tr("левое бедро"), "rightthigh", tr("правое бедро"),
                "waist", tr("пояс"), "leftforearm", tr("левое предплечье"), "rightforearm", tr("правое предплечье"), "none", tr("особая часть"));
    }

    private static void put(Map<String, String> m, String... kv) { for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]); }

    private static String look(Map<String, String> m, String key, String def) {
        if (key == null) return def;
        String v = m.get(key.trim().toLowerCase(Locale.ROOT));
        return v != null ? v : def;
    }

    /** Тег цели ИИ (null — неизвестный тег). */
    public static String tag(String tag) { return look(TAGS, tag, null); }

    public static String state(String state) { return look(STATES, state, state); }

    public static String place(String place) { return look(PLACES, place, place); }

    public static String location(String type) { return look(LOCATIONS, type, type); }

    public static String behavior(String b) { return look(BEHAVIORS, b, b); }

    public static String interaction(String i) { return look(INTERACTIONS, i, i); }

    public static String limb(String l) { return look(LIMBS, l, l); }

    /** Где применяется атака. */
    public static String context(String c) {
        if (c == null) return tr("везде");
        return switch (c.toLowerCase(Locale.ROOT)) {
            case "water" -> tr("в воде");
            case "ground" -> tr("на суше (внутри подлодки)");
            default -> tr("везде");
        };
    }

    /** По чему бьёт атака. */
    public static String attackTarget(String t) {
        if (t == null) return tr("по всему");
        return switch (t.toLowerCase(Locale.ROOT)) {
            case "character" -> tr("по существам и людям");
            case "structure" -> tr("по корпусу и стенам");
            default -> tr("по всему");
        };
    }

    /** Тип недуга (для брони). */
    public static String afflictionType(String t) {
        if (t == null) return "";
        return switch (t.trim().toLowerCase(Locale.ROOT)) {
            case "damage" -> tr("урон");
            case "bleeding" -> tr("кровотечение");
            case "burn" -> tr("ожоги");
            case "stun" -> tr("оглушение");
            case "poison" -> tr("яды");
            case "paralysis" -> tr("паралич");
            case "explosiondamage" -> tr("взрывы");
            case "radiationsickness" -> tr("радиация");
            default -> t.trim();
        };
    }
}
