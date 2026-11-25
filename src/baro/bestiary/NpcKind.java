package baro.bestiary;


import static baro.Tr.tr;
/** Раздел бестиария для людей. */
public enum NpcKind {
    ENEMY(tr("Враги: сепаратисты, Коалиция, бандиты"), tr("Враги")),
    HIREABLE(tr("Наёмники: кого можно нанять"), tr("Наёмники")),
    STATION(tr("Персонал и жители станций"), tr("Станции")),
    CIVIL(tr("Пассажиры, пленники, заложники"), tr("Пассажиры")),
    SPECIAL(tr("Персонажи событий и сюжета"), tr("События")),
    TUTORIAL(tr("Обучение"), tr("Обучение"));

    private final String title, shortTitle;

    NpcKind(String title, String shortTitle) { this.title = title; this.shortTitle = shortTitle; }

    public String title() { return title; }
    public String shortTitle() { return shortTitle; }
}
