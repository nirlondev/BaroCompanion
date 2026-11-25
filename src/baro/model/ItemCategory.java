package baro.model;


import static baro.Tr.tr;
/** Категория предмета (как вкладки справочника). */
public enum ItemCategory {
    WEAPON(tr("Оружие")), MEDICAL(tr("Медицина")), MATERIAL(tr("Материалы")), ELECTRICAL(tr("Электрика")),
    EQUIPMENT(tr("Снаряжение")), DIVING(tr("Дайвинг")), FUEL(tr("Топливо")), GENETIC(tr("Генетика")),
    ALIEN(tr("Инопланетное")), MISC(tr("Разное"));

    private final String title;

    ItemCategory(String title) { this.title = title; }

    public String title() { return title; }
}
