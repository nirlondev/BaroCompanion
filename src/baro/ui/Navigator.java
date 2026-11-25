package baro.ui;

import baro.model.CargoMission;
import baro.model.Item;

/** Куда переходить по ссылке на предмет или миссию. */
public interface Navigator {
    void open(Item item);

    /** Открыть миссию доставки (по умолчанию — ничего). */
    default void openMission(CargoMission mission) { }

    /** Открыть талант на вкладке талантов. */
    default void openTalent(baro.model.Talent talent) { }

    /** Открыть запись бестиария или миссию (Creature, Npc, CrewJob, MissionInfo). */
    default void openLore(Object entry) { }
}
