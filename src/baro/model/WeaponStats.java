package baro.model;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Характеристики оружия, боеприпаса или взрывчатки (из файлов игры). */
public final class WeaponStats {
    private final Double fireInterval, spread, unskilledSpread, meleeInterval, meleeRange, sprayRange;
    private final boolean automatic;
    private final Integer rounds;
    private final List<Item> ammo;
    private final List<DamageBlock> ownDamage, roundDamage;

    public WeaponStats(Double fireInterval, boolean automatic, Double spread, Double unskilledSpread, Double meleeInterval, Double meleeRange,
                       Double sprayRange, Integer rounds, List<Item> ammo, List<DamageBlock> ownDamage, List<DamageBlock> roundDamage) {
        this.fireInterval = fireInterval; this.automatic = automatic; this.spread = spread; this.unskilledSpread = unskilledSpread;
        this.meleeInterval = meleeInterval; this.meleeRange = meleeRange; this.sprayRange = sprayRange; this.rounds = rounds;
        this.ammo = Collections.unmodifiableList(ammo);
        this.ownDamage = Collections.unmodifiableList(ownDamage);
        this.roundDamage = Collections.unmodifiableList(roundDamage);
    }

    /** Время между выстрелами, с. */
    public Optional<Double> fireInterval() { return Optional.ofNullable(fireInterval); }
    public boolean isAutomatic() { return automatic; }
    public Optional<Double> spread() { return Optional.ofNullable(spread); }
    public Optional<Double> unskilledSpread() { return Optional.ofNullable(unskilledSpread); }
    /** Время между ударами, с. */
    public Optional<Double> meleeInterval() { return Optional.ofNullable(meleeInterval); }
    public Optional<Double> meleeRange() { return Optional.ofNullable(meleeRange); }
    public Optional<Double> sprayRange() { return Optional.ofNullable(sprayRange); }
    /** Выстрелов в магазине / снарядов в ящике. */
    public Optional<Integer> rounds() { return Optional.ofNullable(rounds); }
    /** Чем заряжается. */
    public List<Item> ammo() { return ammo; }
    /** Урон самого предмета (удар, снаряд, взрыв). */
    public List<DamageBlock> ownDamage() { return ownDamage; }
    /** Урон одного патрона внутри магазина. */
    public List<DamageBlock> roundDamage() { return roundDamage; }
}
