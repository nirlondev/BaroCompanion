package baro.model;

import java.util.*;

/** Талант персонажа: кому принадлежит, в какой ветке, что даёт и какие рецепты открывает. */
public final class Talent {
    private final String id, name, description;
    private final List<Item> recipes;
    private final Set<String> themes;
    private final List<TalentStat> stats;
    private final List<AllyBuff> allyBuffs;
    private Job job;
    private TalentSubtree subtree;
    private TalentGroup group;

    public Talent(String id, String name, String description, List<Item> recipes, Set<String> themes, List<TalentStat> stats, List<AllyBuff> allyBuffs) {
        this.id = id; this.name = name; this.description = description;
        this.recipes = Collections.unmodifiableList(recipes);
        this.themes = Collections.unmodifiableSet(themes);
        this.stats = Collections.unmodifiableList(stats);
        this.allyBuffs = Collections.unmodifiableList(allyBuffs);
    }

    public String id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    /** Рецепты, которые открывает талант. */
    public List<Item> recipes() { return recipes; }
    /** Темы (бой, лечение, ремонт…) — по ним ищутся сочетания. */
    public Set<String> themes() { return themes; }
    public List<TalentStat> stats() { return stats; }
    /** Бонусы к навыкам, которые талант даёт другим членам экипажа. */
    public List<AllyBuff> allyBuffs() { return allyBuffs; }
    /** Профессия, которой принадлежит талант. */
    public Job job() { return job; }
    /** Ветка (Основа или специализация). */
    public TalentSubtree subtree() { return subtree; }
    public TalentGroup group() { return group; }

    /** «Врач · Химик» */
    public String owner() { return job.name() + " · " + subtree.name(); }

    void place(Job job, TalentSubtree subtree, TalentGroup group) { this.job = job; this.subtree = subtree; this.group = group; }

    @Override public String toString() { return name; }
}
