package baro.model;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

/**
 * Подлодка игрока из файлов .sub: цена, класс, уровень, экипаж, размеры, вооружение и оборудование.
 *
 * @param subClass класс подлодки (идентификатор игры: scout, attack, transport, deepdiver) или пусто
 * @param widthM   ширина и высота в метрах
 * @param turrets  орудия: название → количество
 * @param equipment основное оборудование (реактор, двигатели, фабрикаторы…): название → количество
 * @param preview  уменьшенная картинка из файла подлодки или null
 */
public record Submarine(String id, String name, String description, String subClass, String className, int tier, int price,
                        int crewMin, int crewMax, String crewExperience, double widthM, double heightM, int cargoCapacity,
                        Map<String, Integer> turrets, Map<String, Integer> equipment, String mod, BufferedImage preview,
                        Reactor reactor, Map<String, Double> powerUse, Map<String, Double> peakUse) {

    /** Реактор: наибольшая мощность (кВт) и расход топлива — из файлов игры. */
    public record Reactor(String id, String name, double maxPower, double fuelConsumption) { }

    /**
     * Топливо реактора.
     * @param heat тепло стержня в реакторе (AvailableFuel: уран 80, торий 100, фульгурий 150)
     * @param health запас стержня (максимальное состояние)
     */
    public record Fuel(String id, String name, double heat, double health) { }

    /** Постоянная нагрузка: двигатели, насосы, генератор кислорода, фабрикаторы… на полной мощности, кВт. */
    public double maxLoad() { return powerUse.values().stream().mapToDouble(Double::doubleValue).sum(); }

    public boolean nameContains(String normalizedQuery) {
        return (name + " " + id + " " + className).toLowerCase(java.util.Locale.ROOT).replace('ё', 'е').contains(normalizedQuery);
    }

    @Override public String toString() { return name; }

    /**
     * Расчёт реактора по формулам игры (Reactor.cs): тепло = скорость деления × (топливо / 100) × 2;
     * мощность = выработка турбины × наибольшая мощность при температуре от 50 %; температура стремится к «тепло − турбина».
     * Стержень теряет состояние со скоростью «деление / 100 × расход топлива» в секунду.
     *
     * @param load нужная мощность, кВт
     * @param fuel сумма тепла стержней в реакторе
     */
    public record ReactorPlan(double turbine, double fission, double maxFission, boolean enough, double secondsPerRod) {
        public static ReactorPlan of(Reactor r, double load, double fuel, double rodHealth, int rods) {
            double turbine = r.maxPower() <= 0 ? 0 : Math.min(100, load / r.maxPower() * 100);
            // нужно тепло ≈ турбина + 50, чтобы температура держалась в середине зелёной зоны
            double fission = fuel <= 0 ? Double.POSITIVE_INFINITY : (turbine + 50) * 50 / fuel;
            double maxFission = Math.min(fuel, 100);
            boolean enough = fission <= maxFission && load <= r.maxPower();
            double f = Math.min(fission, maxFission);
            double perSecond = f / 100 * r.fuelConsumption();
            double seconds = perSecond <= 0 ? Double.POSITIVE_INFINITY : rodHealth / perSecond;
            return new ReactorPlan(turbine, fission, maxFission, enough, seconds);
        }
    }

    /**
     * Улучшение подлодки (UpgradeModules.xml).
     * @param maxLevels для уровня подлодки (1, 2, 3) — наибольший уровень улучшения; classBonus — прибавка для класса
     * @param resources ресурсы по уровням: «steel ×5 (уровни 1–2)»
     */
    public record Upgrade(String id, String name, String description, String category, int maxLevel, int basePrice,
                          double increaseLow, double increaseHigh, Map<Integer, Integer> maxLevels, Map<String, Integer> classBonus,
                          List<String> resources) {
        /**
         * Цена покупки уровня level (1…maxLevel) до множителя сложности и наценки станции — как UpgradePrice.GetBuyPrice
         * в игре, которой передаётся уже купленный уровень (первая покупка — по базовой цене).
         */
        public int price(int level) {
            int have = Math.max(0, level - 1);
            float lerp = maxLevel == 0 ? have : have / (float) maxLevel;
            float mult = (float) (increaseLow + (increaseHigh - increaseLow) * lerp);
            return (int) (basePrice + basePrice * (mult / 100f));
        }

        /** Наибольший уровень на подлодке данного уровня и класса. */
        public int maxFor(int tier, String subClass) {
            int m = maxLevels.getOrDefault(tier, maxLevel);
            if (subClass != null) m += classBonus.getOrDefault(subClass.toLowerCase(java.util.Locale.ROOT), 0);
            return Math.max(0, Math.min(m, maxLevel + classBonus.values().stream().mapToInt(Integer::intValue).max().orElse(0)));
        }
    }
}
