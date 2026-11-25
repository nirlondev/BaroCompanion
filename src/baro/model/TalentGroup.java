package baro.model;

import java.util.Collections;
import java.util.List;

/** Ряд талантов в ветке: сколько нужно взять, чтобы открылся следующий ряд, и сколько можно взять максимум. */
public final class TalentGroup {
    private final int required, max;
    private final List<Talent> talents;

    public TalentGroup(int required, int max, List<Talent> talents) {
        this.required = required; this.max = max; this.talents = Collections.unmodifiableList(talents);
    }

    public int required() { return required; }
    public int max() { return max; }
    public List<Talent> talents() { return talents; }
}
