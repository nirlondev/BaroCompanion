package baro.bestiary;


import static baro.Tr.tr;
/** Раздел бестиария для существ. */
public enum CreatureKind {
    SMALL(tr("Мелкие хищники"), tr("Мелкие")),
    MEDIUM(tr("Средние хищники"), tr("Средние")),
    LARGE(tr("Крупные хищники"), tr("Крупные")),
    LEVIATHAN(tr("Левиафаны и боссы"), tr("Левиафаны")),
    HUSK(tr("Хаски и заражённые"), tr("Хаски")),
    GUARDIAN(tr("Стражи руин и машины"), tr("Стражи")),
    THALAMUS(tr("Таламус: обитатели заражённых обломков"), tr("Таламус")),
    PET(tr("Питомцы и мирные существа"), tr("Питомцы")),
    LEGACY(tr("Устаревшие версии (Legacy)"), "Legacy");

    private final String title, shortTitle;

    CreatureKind(String title, String shortTitle) { this.title = title; this.shortTitle = shortTitle; }

    public String title() { return title; }
    public String shortTitle() { return shortTitle; }
}
