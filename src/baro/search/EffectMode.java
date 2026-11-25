package baro.search;


import static baro.Tr.tr;
/** Что ищем в эффектах препарата. */
public enum EffectMode {
    ANY(tr("любой")), REMOVES(tr("снимает")), GIVES(tr("даёт"));

    private final String title;

    EffectMode(String title) { this.title = title; }

    @Override public String toString() { return title; }
}
