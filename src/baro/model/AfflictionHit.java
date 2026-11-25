package baro.model;


import static baro.Tr.tr;
import java.util.Optional;

/** Что наносит попадание: травма и её сила (для оглушения — секунды). */
public final class AfflictionHit {
    private final String name;
    private final double strength;
    private final Double probability;

    public AfflictionHit(String name, double strength, Double probability) { this.name = name; this.strength = strength; this.probability = probability; }

    public String name() { return name; }
    public double strength() { return strength; }
    public Optional<Double> probability() { return Optional.ofNullable(probability); }
    public boolean isStun() { return name.startsWith(tr("Оглушение")); }
}
