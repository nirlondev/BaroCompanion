package baro.model;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Эффект препарата: итог за всё время действия. value < 0 — снимает недуг, > 0 — добавляет. */
public final class Effect {
    private final String key;
    private final String name;
    private final EffectKind kind;
    private final Double value;
    private final Double duration;
    private final Double delay;
    private final Double chance;
    private final Double buffSeconds;
    private final boolean limbOnly;
    private final List<String> conditions;

    public Effect(String key, String name, EffectKind kind, Double value, Double duration, Double delay,
                  Double chance, Double buffSeconds, boolean limbOnly, List<String> conditions) {
        this.key = key; this.name = name; this.kind = kind; this.value = value; this.duration = duration;
        this.delay = delay; this.chance = chance; this.buffSeconds = buffSeconds; this.limbOnly = limbOnly;
        this.conditions = Collections.unmodifiableList(conditions);
    }

    public String key() { return key; }
    public String name() { return name; }
    public EffectKind kind() { return kind; }
    /** Пусто для особых эффектов без числа (например, «случайный талант»). */
    public Optional<Double> value() { return Optional.ofNullable(value); }
    public Optional<Double> duration() { return Optional.ofNullable(duration); }
    public Optional<Double> delay() { return Optional.ofNullable(delay); }
    public Optional<Double> chance() { return Optional.ofNullable(chance); }
    public Optional<Double> buffSeconds() { return Optional.ofNullable(buffSeconds); }
    public boolean isLimbOnly() { return limbOnly; }
    public List<String> conditions() { return conditions; }
}
