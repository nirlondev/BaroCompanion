package baro.search;

import java.util.*;

/**
 * Сленг и сокращения игроков → идентификаторы предметов: «плазма» находит плазменный резак, «шотган» — дробовик.
 * Неизвестные идентификаторы (предмет из выключенного мода и т. п.) просто ничего не добавляют.
 */
final class Synonyms {
    private static final Map<String, List<String>> MAP = new HashMap<>();

    static {
        add("plasmacutter", "плазма", "плазморез", "резак");
        add("weldingtool", "сварка", "сварочник", "варилка");
        add("wrench", "ключ", "гаечник");
        add("screwdriver", "отвертка", "отвёртка");
        add("antibleeding1", "бинты", "перевязка");
        add("antibloodloss1", "солевой", "физра");
        add("oxygentank", "кислород", "баллон", "оксиген");
        add("fuelrod", "уран", "стержень", "топливо");
        add("thoriumfuelrod", "торий");
        add("divingsuit", "скафандр", "скаф", "водолазка");
        add("combatdivingsuit", "боевой скаф", "бронескаф");
        add("divingmask", "маска", "противогаз");
        add("shotgun", "шотган", "дробаш", "помпа");
        add("autoshotgun", "автошотган", "автодробовик");
        add("smg", "пп", "смг", "пистолет пулемет", "узи");
        add("assaultrifle", "автомат", "калаш", "штурмовая");
        add("harpoongun", "гарпун", "гарпунное");
        add("c4block", "с4", "c4", "пластид", "взрывчатка");
        add("uex", "юекс", "взрывчатка");
        add("handcannon", "ручная пушка");
        add("stungun", "тазер", "шокер");
        add("stunbaton", "дубинка");
        add("crowbar", "монтировка", "фомка");
        add("flashlight", "фонарь");
        add("railgunshell", "рельса", "снаряд рельсы");
        add("nuclearshell", "ядерка", "ядерный снаряд");
        add("coilgunammobox", "патроны пушки", "боезапас пушки");
        add("toolbelt", "пояс");
        add("meth", "мет");
        add("steroids", "стероиды", "анаболики");
    }

    private Synonyms() {}

    private static void add(String id, String... words) {
        for (String w : words) MAP.computeIfAbsent(ItemSearch.normalize(w), k -> new ArrayList<>()).add(id);
    }

    /** Идентификаторы предметов, которые игроки так называют. */
    static List<String> of(String normalizedQuery) { return MAP.getOrDefault(normalizedQuery, List.of()); }
}
