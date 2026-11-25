package baro.model;


import static baro.Tr.tr;
/** Вариант переработки груза. */
public enum PlanKind {
    DECONSTRUCT(tr("А. Разобрать и продать"), tr("Всё, что разбирается, разобрать и продать полученное.")),
    CARGO_ONLY(tr("Б. Разобрать и скрафтить"), tr("Лучший крафт из того, что дал груз, без покупок.")),
    WITH_PURCHASE(tr("В. Крафт с докупкой"), tr("Как Б, но можно докупить недостающие ингредиенты по ценам «Бездны»."));

    private final String title, description;

    PlanKind(String title, String description) { this.title = title; this.description = description; }

    public String title() { return title; }
    public String description() { return description; }
}
