package baro.ui.widgets;

import baro.ui.Theme;

import java.awt.Color;

/** Оформление фрагмента текста. */
public enum Style {
    NORMAL(Theme.FG, false), BOLD(Theme.FG, true), MUTED(Theme.MUTED, false), TITLE(Theme.WHITE, true),
    ACCENT(Theme.ACCENT, true), BUY(Theme.BUY, true), SELL(Theme.SELL, true), WARN(Theme.WARN, false),
    GOOD(Theme.SELL, true), BAD(Theme.BUY, true), FAVORITE(Theme.FAVORITE, true);

    private final Color color;
    private final boolean bold;

    Style(Color color, boolean bold) { this.color = color; this.bold = bold; }

    public Color color() { return color; }
    public boolean bold() { return bold; }
}
