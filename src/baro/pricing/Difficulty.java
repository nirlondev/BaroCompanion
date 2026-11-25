package baro.pricing;


import static baro.Tr.tr;
/**
 * Уровень сложности кампании (Data/campaignsettings.xml): от него зависят цены покупки (ShopPriceMultiplier)
 * и награды за миссии (MissionRewardMultiplier). CUSTOM — свои значения (настройки кампании вручную).
 */
public enum Difficulty {
    EASY(tr("Низкий"), 0.9f, 1.0f),
    NORMAL(tr("Обычный"), 1.0f, 1.0f),
    HARD(tr("Высокий"), 1.5f, 1.0f),
    ABYSSAL(tr("Ужасающий (Бездна)"), 2.0f, 0.8f),
    CUSTOM(tr("Свои настройки"), 0f, 0f);

    private final String title;
    private final float shopPrice, missionReward;

    Difficulty(String title, float shopPrice, float missionReward) { this.title = title; this.shopPrice = shopPrice; this.missionReward = missionReward; }

    public String title() { return title; }
    /** Множитель цен покупки (для CUSTOM — 0, значение задаётся отдельно). */
    public float shopPrice() { return shopPrice; }
    /** Множитель наград за миссии (для CUSTOM — 0). */
    public float missionReward() { return missionReward; }

    /** Пресет с такими множителями или CUSTOM. */
    public static Difficulty match(float shopPrice, float missionReward) {
        for (Difficulty d : values())
            if (d != CUSTOM && Math.abs(d.shopPrice - shopPrice) < 1e-4 && Math.abs(d.missionReward - missionReward) < 1e-4) return d;
        return CUSTOM;
    }

    /** По имени пресета из сейва («Easy», «Normal», «Hard», «Abyssal»); null — не пресет. */
    public static Difficulty byPresetName(String name) {
        if (name == null) return null;
        switch (name.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "easy": return EASY;
            case "normal": return NORMAL;
            case "hard": return HARD;
            case "abyssal": return ABYSSAL;
            default: return null;
        }
    }

    @Override public String toString() { return this == CUSTOM ? title : title + tr(" — цены ×") + Format.num(shopPrice); }
}