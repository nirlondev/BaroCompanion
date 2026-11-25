package baro.pricing;


import static baro.Tr.tr;
/** Уровень отношения фракции (как в игре). */
public enum ReputationTier {
    HOSTILE(tr("Вражда")), UNFRIENDLY(tr("Неприязнь")), NEUTRAL(tr("Нейтралитет")), FRIENDLY(tr("Дружба")), REVERED(tr("Почтение"));

    private final String title;

    ReputationTier(String title) { this.title = title; }

    public String title() { return title; }
}
