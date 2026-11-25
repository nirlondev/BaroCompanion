package baro.model;


import static baro.Tr.tr;
/** Раздел медикаментов. */
public enum MedCategory {
    HEAL(tr("Лечение"), tr("Снимают травмы, кровотечение, ожоги, кровопотерю и удушье.")),
    ANTIDOTE(tr("Антидоты"), tr("Лечат отравления, хаск, психоз, радиацию и передозировку.")),
    BUFF(tr("Баффы и стимуляторы"), tr("Временно усиливают: скорость, защита, сила, особые способности.")),
    TOXIN(tr("Яды"), tr("Яды и кислоты — для шприцемёта, против монстров и врагов.")),
    OTHER(tr("Прочее"), tr("Ингредиенты и всё остальное."));

    private final String title;
    private final String description;

    MedCategory(String title, String description) { this.title = title; this.description = description; }

    public String title() { return title; }
    public String description() { return description; }
}
