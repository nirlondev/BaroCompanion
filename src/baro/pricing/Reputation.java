package baro.pricing;

/** Репутация у фракции от −100 до +100 и её влияние на цены (Location.GetReputationModifier). */
public final class Reputation {
    public static final int MIN = -100, MAX = 100;
    private static final float MAX_MODIFIER = 0.1f;   // locationTypes.xml: maxreputationmodifier
    private static final float MIN_MODIFIER = 1.0f;   // locationTypes.xml: minreputationmodifier
    private final int value;

    public Reputation(int value) { this.value = Math.max(MIN, Math.min(MAX, value)); }

    public int value() { return value; }

    public float buyModifier() {
        return value >= 0 ? lerp(1f, 1f - MAX_MODIFIER, value / (float) MAX) : lerp(1f, 1f + MIN_MODIFIER, value / (float) MIN);
    }

    public float sellModifier() {
        return value >= 0 ? lerp(1f, 1f + MAX_MODIFIER, value / (float) MAX) : lerp(1f, 1f - MIN_MODIFIER, value / (float) MIN);
    }

    public ReputationTier tier() {
        float n = (value - MIN) / (float) (MAX - MIN);
        if (n < 0.2f) return ReputationTier.HOSTILE;
        if (n < 0.4f) return ReputationTier.UNFRIENDLY;
        if (n < 0.6f) return ReputationTier.NEUTRAL;
        if (n < 0.8f) return ReputationTier.FRIENDLY;
        return ReputationTier.REVERED;
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
