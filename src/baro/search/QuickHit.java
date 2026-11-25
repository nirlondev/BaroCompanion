package baro.search;

import baro.model.CargoMission;
import baro.model.Effect;
import baro.model.Item;
import baro.model.Medicine;

import java.util.Optional;

/** Результат быстрого поиска для окна поверх игры. */
public final class QuickHit {
    private final Item item;
    private final Medicine medicine;
    private final Effect effect;
    private final CargoMission mission;
    private final baro.model.Talent talent;

    QuickHit(Item item, Medicine medicine, Effect effect) { this.item = item; this.medicine = medicine; this.effect = effect; this.mission = null; this.talent = null; }

    QuickHit(CargoMission mission) { this.item = null; this.medicine = null; this.effect = null; this.mission = mission; this.talent = null; }

    QuickHit(baro.model.Talent talent) { this.item = null; this.medicine = null; this.effect = null; this.mission = null; this.talent = talent; }

    public Optional<baro.model.Talent> talent() { return Optional.ofNullable(talent); }

    /** Предмет (null, если найдена миссия). */
    public Item item() { return item; }
    public Optional<CargoMission> mission() { return Optional.ofNullable(mission); }
    public String title() { return mission != null ? mission.name() : talent != null ? talent.name() : item.displayName(); }
    public boolean isBest() { return item != null && item.isBest(); }
    public Optional<Medicine> medicine() { return Optional.ofNullable(medicine); }
    /** Эффект, по которому нашёлся препарат (поиск по эффекту). */
    public Optional<Effect> effect() { return Optional.ofNullable(effect); }
}
