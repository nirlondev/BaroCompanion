package baro.talents;

import baro.model.Talent;

import java.util.List;

/** Пара талантов, которые хорошо работают вместе, и почему. */
public record Synergy(Talent a, Talent b, int score, List<String> reasons) {
    public boolean involves(Talent t) { return a == t || b == t; }
    public Talent other(Talent t) { return a == t ? b : a; }
}
