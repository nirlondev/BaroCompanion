package baro.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Профессия и её дерево талантов. */
public final class Job {
    private final String id, name;
    private final List<TalentSubtree> subtrees;

    public Job(String id, String name, List<TalentSubtree> subtrees) {
        this.id = id; this.name = name; this.subtrees = Collections.unmodifiableList(subtrees);
        for (TalentSubtree s : subtrees) for (TalentGroup g : s.groups()) for (Talent t : g.talents()) t.place(this, s, g);
    }

    public String id() { return id; }
    public String name() { return name; }
    public List<TalentSubtree> subtrees() { return subtrees; }

    public TalentSubtree subtree(String id) {
        for (TalentSubtree s : subtrees) if (s.id().equals(id)) return s;
        return null;
    }

    public List<Talent> talents() {
        List<Talent> l = new ArrayList<>();
        for (TalentSubtree s : subtrees) for (TalentGroup g : s.groups()) l.addAll(g.talents());
        return l;
    }

    @Override public String toString() { return name; }
}
