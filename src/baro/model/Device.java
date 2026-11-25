package baro.model;

import java.util.List;

/**
 * Предмет с панелью проводки: сигнальный компонент, датчик или устройство подлодки.
 *
 * @param group      раздел: LOGIC, MATH, SENSOR, OUTPUT, MACHINE, OTHER
 * @param components компоненты из XML предмета (AndComponent, MotionSensor, Pump…) — по ним берётся описание
 * @param inputs     входы: имя в файлах игры и подпись как в игре
 */
public record Device(String id, String name, String description, String group, List<String> components,
                     List<Port> inputs, List<Port> outputs, String mod) {
    /** Разъём панели проводки. */
    public record Port(String name, String label) { }

    public boolean nameContains(String normalizedQuery) {
        return (name + " " + id).toLowerCase(java.util.Locale.ROOT).replace('ё', 'е').contains(normalizedQuery);
    }

    @Override public String toString() { return name; }
}
