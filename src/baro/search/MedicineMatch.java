package baro.search;

import baro.model.Effect;
import baro.model.Medicine;

import java.util.Optional;

/** Найденный препарат и эффект, по которому он нашёлся (если искали по эффекту). */
public final class MedicineMatch {
    private final Medicine medicine;
    private final Effect effect;

    MedicineMatch(Medicine medicine, Effect effect) { this.medicine = medicine; this.effect = effect; }

    public Medicine medicine() { return medicine; }
    public Optional<Effect> effect() { return Optional.ofNullable(effect); }
}
