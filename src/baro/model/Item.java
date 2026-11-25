package baro.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Предмет игры. Рецепты и разбор ссылаются на другие объекты Item. */
public final class Item {
    private final String id;
    private final String name;
    private final ItemCategory category;
    private final String description;
    private final String variantLabel;
    private final PriceInfo price;
    private final String searchKey;
    private final String englishName;
    private List<Recipe> recipes = Collections.emptyList();
    private Deconstruction deconstruction;
    private final List<Item> usedIn = new ArrayList<>();
    private final List<Item> obtainedFrom = new ArrayList<>();
    private String bestReason;
    private WeaponStats weapon;
    private final List<Item> ammoFor = new ArrayList<>();
    private final List<Talent> unlockedBy = new ArrayList<>();

    public Item(String id, String name, ItemCategory category, String description, String variantLabel, PriceInfo price) {
        this(id, name, category, description, variantLabel, price, null);
    }

    /** @param englishName название в английской версии игры (для поиска и ссылки на вики) или null */
    public Item(String id, String name, ItemCategory category, String description, String variantLabel, PriceInfo price, String englishName) {
        this.id = id;
        this.englishName = englishName == null || englishName.equals(name) ? null : englishName;
        this.name = name;
        this.category = category;
        this.description = description == null ? "" : description;
        this.variantLabel = variantLabel;
        this.price = price;
        this.searchKey = ((variantLabel == null ? name : name + " " + variantLabel) + (this.englishName == null ? "" : " | " + this.englishName))
                .toLowerCase(Locale.ROOT).replace('ё', 'е');
    }

    public String id() { return id; }
    public String name() { return name; }
    public ItemCategory category() { return category; }
    public String description() { return description; }
    /** Пояснение, если в игре несколько предметов с одинаковым названием. */
    public Optional<String> variantLabel() { return Optional.ofNullable(variantLabel); }
    /** Название в английской версии игры, если оно отличается от показанного. */
    public Optional<String> englishName() { return Optional.ofNullable(englishName); }
    /** Строка поиска: название, пояснение и английское название (в нижнем регистре, ё → е). */
    public String searchKey() { return searchKey; }
    public Optional<PriceInfo> price() { return Optional.ofNullable(price); }
    public List<Recipe> recipes() { return recipes; }
    public Optional<Deconstruction> deconstruction() { return Optional.ofNullable(deconstruction); }
    /** Предметы, в рецептах которых этот предмет — ингредиент. */
    public List<Item> usedIn() { return Collections.unmodifiableList(usedIn); }
    /** Предметы, при разборе которых выпадает этот. */
    public List<Item> obtainedFrom() { return Collections.unmodifiableList(obtainedFrom); }

    /** Почему предмет — один из лучших в своей категории (если это так). */
    public Optional<String> bestReason() { return Optional.ofNullable(bestReason); }
    public boolean isBest() { return bestReason != null; }
    /** Характеристики оружия/боеприпаса, если есть. */
    public Optional<WeaponStats> weapon() { return Optional.ofNullable(weapon); }
    /** Таланты, которые открывают рецепт этого предмета. */
    public List<Talent> unlockedBy() { return Collections.unmodifiableList(unlockedBy); }
    /** Оружие, которое заряжается этим предметом. */
    public List<Item> ammoFor() { return Collections.unmodifiableList(ammoFor); }

    public boolean nameContains(String lowerQuery) { return searchKey.contains(lowerQuery); }
    public boolean nameStartsWith(String lowerQuery) { return searchKey.startsWith(lowerQuery); }
    public String displayName() { return variantLabel == null ? name : name + " (" + variantLabel + ")"; }

    // --- связывание выполняет только каталог при сборке ---
    void attach(List<Recipe> recipes, Deconstruction deconstruction) {
        this.recipes = Collections.unmodifiableList(recipes);
        this.deconstruction = deconstruction;
    }
    void addUsedIn(Item item) { if (!usedIn.contains(item)) usedIn.add(item); }
    void markBest(String reason) { this.bestReason = reason; }
    void addUnlockedBy(Talent t) { if (!unlockedBy.contains(t)) unlockedBy.add(t); }
    void attachWeapon(WeaponStats w) { this.weapon = w; for (Item a : w.ammo()) if (!a.ammoFor.contains(this)) a.ammoFor.add(this); }
    void addObtainedFrom(Item item) { if (!obtainedFrom.contains(item)) obtainedFrom.add(item); }

    @Override public String toString() { return displayName(); }
}
