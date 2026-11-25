package baro.model;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Одно попадание или взрыв с его уроном. */
public final class DamageBlock {
    private final DamageKind kind;
    private final Double radius, structureDamage;
    private final List<AfflictionHit> afflictions;

    public DamageBlock(DamageKind kind, Double radius, Double structureDamage, List<AfflictionHit> afflictions) {
        this.kind = kind; this.radius = radius; this.structureDamage = structureDamage; this.afflictions = Collections.unmodifiableList(afflictions);
    }

    public DamageKind kind() { return kind; }
    /** Радиус взрыва в единицах игры (100 ≈ 1 метр). */
    public Optional<Double> radius() { return Optional.ofNullable(radius); }
    public Optional<Double> structureDamage() { return Optional.ofNullable(structureDamage); }
    public List<AfflictionHit> afflictions() { return afflictions; }
}
