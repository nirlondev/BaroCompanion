package baro.model;

import java.util.Collections;
import java.util.List;

/** Ветка талантов: «Основа» профессии или одна из специализаций. */
public final class TalentSubtree {
    private final String id, name;
    private final boolean primary;
    private final String requires;
    private final List<String> blocks;
    private final List<TalentGroup> groups;

    public TalentSubtree(String id, String name, boolean primary, String requires, List<String> blocks, List<TalentGroup> groups) {
        this.id = id; this.name = name; this.primary = primary; this.requires = requires;
        this.blocks = Collections.unmodifiableList(blocks); this.groups = Collections.unmodifiableList(groups);
    }

    public String id() { return id; }
    public String name() { return name; }
    public boolean isPrimary() { return primary; }
    /** Какая ветка должна быть пройдена, чтобы открылась эта. */
    public String requires() { return requires; }
    /** Ветки, которые закрываются, если выбрать таланты в этой. */
    public List<String> blocks() { return blocks; }
    public List<TalentGroup> groups() { return groups; }
}
