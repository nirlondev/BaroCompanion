package baro.search;


import static baro.Tr.tr;
/** Порядок списка предметов. */
public enum SortMode {
    NAME(tr("По названию")), SELL(tr("Дороже продать")), BUY(tr("Дешевле купить")), BASE(tr("По базовой цене"));

    private final String title;

    SortMode(String title) { this.title = title; }

    @Override public String toString() { return title; }
}
