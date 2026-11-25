package baro.model;


import static baro.Tr.tr;
/** Магазины станций (идентификаторы как в файлах игры). */
public enum Store {
    CITY("merchantcity", tr("Колония")),
    OUTPOST("merchantoutpost", tr("Жилой аванпост")),
    RESEARCH("merchantresearch", tr("Исследовательский аванпост")),
    MILITARY("merchantmilitary", tr("Военный аванпост")),
    MINE("merchantmine", tr("Шахтёрский аванпост")),
    ARMORY("merchantarmory", tr("Арсенал")),
    MEDICAL("merchantmedical", tr("Медицинские товары")),
    ENGINEERING("merchantengineering", tr("Инженерные товары")),
    HUSK("merchanthusk", tr("Торговец Церкви Хаска")),
    CLOWN("merchantclown", tr("Торговец клоунов")),
    NIGHTCLUB("merchantnightclub", tr("Ночной клуб"));

    private final String gameId;
    private final String title;

    Store(String gameId, String title) { this.gameId = gameId; this.title = title; }

    public String gameId() { return gameId; }
    public String title() { return title; }

    public static Store byGameId(String id) {
        for (Store s : values()) if (s.gameId.equals(id)) return s;
        return CITY;
    }

    @Override public String toString() { return title; }
}
