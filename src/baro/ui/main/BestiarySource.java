package baro.ui.main;

import baro.bestiary.Bestiary;
import baro.settings.AppSettings;

/** Бестиарий загружается один раз, в фоне, когда впервые понадобился (вкладки «Бестиарий», «Миссии», окно поверх игры). */
public final class BestiarySource extends baro.app.LazyData<Bestiary> {
    public BestiarySource(AppSettings settings) { super("bestiary-load", () -> Bestiary.load(settings)); }
}
