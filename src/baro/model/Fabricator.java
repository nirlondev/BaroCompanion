package baro.model;


import static baro.Tr.tr;
/** Где делается рецепт. */
public enum Fabricator {
    FABRICATOR(tr("Фабрикатор")), MEDICAL(tr("Мед. фабрикатор")), ANY(tr("Фабрикатор или мед.")), VENDING(tr("Торговый автомат"));

    private final String title;

    Fabricator(String title) { this.title = title; }

    public String title() { return title; }
}
