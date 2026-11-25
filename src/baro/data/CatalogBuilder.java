package baro.data;

import baro.model.*;

import static baro.Tr.tr;

import java.util.*;

/**
 * Fluent-сборщик каталога. Сгенерированные классы данных описывают предметы через него,
 * а build() создаёт объекты и связывает их ссылками друг на друга.
 */
public final class CatalogBuilder {
    private final Map<String, ItemSpec> itemSpecs = new LinkedHashMap<>();
    private final List<MedicineSpec> medicineSpecs = new ArrayList<>();
    private final List<MissionSpec> missionSpecs = new ArrayList<>();
    private final List<WeaponSpec> weaponSpecs = new ArrayList<>();
    private final Map<String, RandomDeconSpec> randomDecons = new HashMap<>();
    private final Map<String, String> themeTitles = new LinkedHashMap<>();
    private final Map<String, TalentSpec> talentSpecs = new LinkedHashMap<>();
    private final List<JobSpec> jobSpecs = new ArrayList<>();
    private final List<Affliction> afflictions = new ArrayList<>();

    /** Недуг из файлов игры (встроенных данных о недугах нет — тогда они собираются из эффектов препаратов). */
    public void affliction(Affliction a) { afflictions.add(a); }

    /** Предмет; повторный вызов с тем же id заменяет прежнее описание целиком. */
    public ItemSpec item(String id, String name, ItemCategory category, String description, String variant) {
        ItemSpec s = new ItemSpec(id, name, category, description, variant);
        itemSpecs.put(id, s);
        return s;
    }

    /** Категория уже описанного предмета или null. */
    public ItemCategory itemCategory(String id) { ItemSpec s = itemSpecs.get(id); return s == null ? null : s.category; }
    /** Пояснение к одноимённым предметам у уже описанного предмета или null. */
    public String itemVariant(String id) { ItemSpec s = itemSpecs.get(id); return s == null ? null : s.variant; }

    /** Описание оружия; повторный вызов для того же предмета (данные из файлов игры) заменяет прежнее. */
    public WeaponSpec weapon(String itemId) { removeWeapon(itemId); WeaponSpec w = new WeaponSpec(itemId); weaponSpecs.add(w); return w; }
    public void removeWeapon(String itemId) { weaponSpecs.removeIf(w -> w.itemId.equals(itemId)); }

    public RandomDeconSpec randomDecon(String itemId, int picks) { RandomDeconSpec r = new RandomDeconSpec(picks); randomDecons.put(itemId, r); return r; }
    public void removeRandomDecon(String itemId) { randomDecons.remove(itemId); }

    /** Медикамент; повторный вызов для того же предмета заменяет прежнее описание. */
    public MedicineSpec medicine(String itemId, MedCategory category, Integer useSkill) {
        medicineSpecs.removeIf(m -> m.itemId.equals(itemId));
        MedicineSpec s = new MedicineSpec(itemId, category, useSkill);
        medicineSpecs.add(s);
        return s;
    }

    /** Раздел уже описанного медикамента или null. */
    public MedCategory medicineCategory(String itemId) {
        for (MedicineSpec m : medicineSpecs) if (m.itemId.equals(itemId)) return m.category;
        return null;
    }

    public MissionSpec mission(String id, String name, String crate, int minCrates, int maxCrates, int rewardPerCrate) {
        MissionSpec s = new MissionSpec(id, name, crate, minCrates, maxCrates, rewardPerCrate);
        missionSpecs.add(s);
        return s;
    }

    public void talentTheme(String id, String title) { themeTitles.put(id, tr(title)); }
    public TalentSpec talent(String id, String name, String description) { TalentSpec t = new TalentSpec(name, description); talentSpecs.put(id, t); return t; }
    public JobSpec job(String id, String name) { JobSpec j = new JobSpec(id, name); jobSpecs.add(j); return j; }
    /** Забыть все таланты и профессии (перед загрузкой их из файлов игры). */
    public void resetTalents() { themeTitles.clear(); talentSpecs.clear(); jobSpecs.clear(); }

    GameCatalog build() {
        Map<String, Item> items = new LinkedHashMap<>();
        for (ItemSpec s : itemSpecs.values()) items.put(s.id, s.createItem());
        for (ItemSpec s : itemSpecs.values()) s.link(items, randomDecons.get(s.id));
        for (WeaponSpec w : weaponSpecs) { Item it = items.get(w.itemId); if (it != null) CatalogAssembler.attachWeapon(it, w.create(items)); }
        CatalogAssembler.computeRelations(items.values());
        Map<Item, Medicine> meds = new LinkedHashMap<>();
        for (MedicineSpec m : medicineSpecs) {
            Item it = items.get(m.itemId);
            if (it != null) meds.put(it, m.create(it, BestPicks.reasonFor(m.itemId)));
        }
        // лучшие предметы: медицина из BestPicks, остальное из BestItems
        for (Medicine m : meds.values()) m.bestReason().ifPresent(r -> CatalogAssembler.markBest(m.item(), r));
        BestItems.all().forEach((id, reason) -> { Item it = items.get(id); if (it != null) CatalogAssembler.markBest(it, reason); });
        List<CargoMission> missions = new ArrayList<>();
        for (MissionSpec m : missionSpecs) missions.add(m.create(items));
        Map<String, Talent> talents = new LinkedHashMap<>();
        return new GameCatalog(items, meds, missions, buildJobs(items, talents), talents, themeTitles, afflictions);
    }

    private List<Job> buildJobs(Map<String, Item> items, Map<String, Talent> talents) {
        talentSpecs.forEach((id, s) -> {
            Talent t = new Talent(id, tr(s.name), tr(s.description), resolve(s.recipes, items), new LinkedHashSet<>(s.themes), s.stats, s.ally);
            talents.put(id, t);
            CatalogAssembler.linkTalent(t);
        });
        List<Job> jobs = new ArrayList<>();
        for (JobSpec j : jobSpecs) {
            List<TalentSubtree> subs = new ArrayList<>();
            for (SubtreeSpec st : j.subtrees) {
                List<TalentGroup> groups = new ArrayList<>();
                for (GroupSpec g : st.groups) groups.add(new TalentGroup(g.required, g.max, resolve(g.talentIds, talents)));
                subs.add(new TalentSubtree(st.id, tr(st.name), st.primary, st.requires, st.blocks, groups));
            }
            jobs.add(new Job(j.id, tr(j.name), subs));
        }
        return jobs;
    }

    /** id → объекты; неизвестные id (предмет из выключенного мода и т. п.) пропускаются. */
    private static <T> List<T> resolve(List<String> ids, Map<String, T> all) {
        List<T> r = new ArrayList<>();
        for (String id : ids) { T t = all.get(id); if (t != null) r.add(t); }
        return r;
    }

    private static Map<Item, Integer> resolve(Map<String, Integer> ids, Map<String, Item> all) {
        Map<Item, Integer> m = new LinkedHashMap<>();
        ids.forEach((id, n) -> { Item it = all.get(id); if (it != null) m.put(it, n); });
        return m;
    }

    // ================= предмет =================
    public static final class ItemSpec {
        private final String id, name, description, variant;
        private final ItemCategory category;
        private Integer basePrice;
        private boolean soldElsewhere;
        private final Map<Store, StorePrice> stores = new EnumMap<>(Store.class);
        private final List<RecipeSpec> recipes = new ArrayList<>();
        private DeconSpec decon;
        private String englishName;

        private ItemSpec(String id, String name, ItemCategory category, String description, String variant) {
            this.id = id; this.name = name; this.category = category; this.description = description; this.variant = variant;
        }

        /** Название в английской версии игры (для поиска по-английски и ссылки на вики). */
        public ItemSpec english(String name) { this.englishName = name; return this; }
        public ItemSpec price(int base, boolean soldElsewhere) { this.basePrice = base; this.soldElsewhere = soldElsewhere; return this; }
        public ItemSpec store(Store store, double multiplier, boolean sold) { stores.put(store, new StorePrice(multiplier, sold)); return this; }
        public RecipeSpec recipe(Fabricator f, double seconds, int out, boolean needsUnlock, int vendingPrice) {
            RecipeSpec r = new RecipeSpec(this, f, seconds, out, needsUnlock, vendingPrice); recipes.add(r); return r;
        }
        public DeconSpec decon(double seconds, boolean randomSingle, boolean geneticOnly) {
            decon = new DeconSpec(this, seconds, randomSingle, geneticOnly); return decon;
        }

        private Item createItem() {
            return new Item(id, tr(name), category, tr(description), variant == null ? null : tr(variant), basePrice == null ? null : new PriceInfo(basePrice, soldElsewhere, stores), englishName);
        }

        private void link(Map<String, Item> all, RandomDeconSpec random) {
            List<Recipe> rs = new ArrayList<>();
            for (RecipeSpec r : recipes) rs.add(r.create(all));
            CatalogAssembler.attach(all.get(id), rs, decon == null ? null : decon.create(all, random));
        }
    }

    // ================= рецепт =================
    public static final class RecipeSpec {
        private final ItemSpec owner;
        private final Fabricator fabricator;
        private final double seconds;
        private final int out, vendingPrice;
        private final boolean needsUnlock;
        private final List<SkillRequirement> skills = new ArrayList<>();
        private final List<IngredientSpec> ingredients = new ArrayList<>();

        private RecipeSpec(ItemSpec owner, Fabricator f, double seconds, int out, boolean needsUnlock, int vendingPrice) {
            this.owner = owner; this.fabricator = f; this.seconds = seconds; this.out = out; this.needsUnlock = needsUnlock; this.vendingPrice = vendingPrice;
        }

        public RecipeSpec skill(Skill skill, int level) { skills.add(new SkillRequirement(skill, level)); return this; }
        public RecipeSpec item(String id, int count) { return item(id, count, null, null, false); }
        public RecipeSpec item(String id, int count, Double minCondition, Double maxCondition, boolean partialUse) {
            ingredients.add(new IngredientSpec(id, null, null, count, minCondition, maxCondition, partialUse)); return this;
        }
        public RecipeSpec tag(String label, int count, String... optionIds) {
            ingredients.add(new IngredientSpec(null, label, Arrays.asList(optionIds), count, null, null, false)); return this;
        }
        public ItemSpec done() { return owner; }

        private Recipe create(Map<String, Item> all) {
            List<Ingredient> list = new ArrayList<>();
            for (IngredientSpec s : ingredients) {
                if (s.label != null) list.add(new TagIngredient(s.label, resolve(s.options, all), s.count, s.min, s.max, s.partial));
                else if (all.containsKey(s.id)) list.add(new ItemIngredient(all.get(s.id), s.count, s.min, s.max, s.partial));
            }
            return new Recipe(fabricator, seconds, out, needsUnlock, vendingPrice, skills, list);
        }
    }

    /** Ингредиент: конкретный предмет (id) или «любой из группы» (label + options). */
    private record IngredientSpec(String id, String label, List<String> options, int count, Double min, Double max, boolean partial) {}

    // ================= разбор =================
    public static final class DeconSpec {
        private final ItemSpec owner;
        private final double seconds;
        private final boolean randomSingle, geneticOnly;
        private final List<OutSpec> outs = new ArrayList<>();

        private DeconSpec(ItemSpec owner, double seconds, boolean randomSingle, boolean geneticOnly) {
            this.owner = owner; this.seconds = seconds; this.randomSingle = randomSingle; this.geneticOnly = geneticOnly;
        }

        public DeconSpec out(String id, int count, Double minCondition, boolean chance, boolean needsExtra) {
            outs.add(new OutSpec(id, count, minCondition, chance, needsExtra)); return this;
        }
        public ItemSpec done() { return owner; }

        private Deconstruction create(Map<String, Item> all, RandomDeconSpec random) {
            List<DeconOutput> list = new ArrayList<>();
            double total = random == null ? 0 : random.weights.values().stream().mapToDouble(Double::doubleValue).sum();
            for (OutSpec o : outs) {
                Item it = all.get(o.id);
                if (it == null) continue;
                if (random != null && random.weights.containsKey(o.id) && total > 0)
                    list.add(new DeconOutput(it, 1, o.minCondition, true, o.needsExtra, random.weights.get(o.id) / total));
                else list.add(new DeconOutput(it, o.count, o.minCondition, o.chance, o.needsExtra));
            }
            return new Deconstruction(seconds, randomSingle, geneticOnly, list, random == null ? 1 : random.picks);
        }
    }

    private record OutSpec(String id, int count, Double minCondition, boolean chance, boolean needsExtra) {}

    /** Случайный разбор: выпадает picks предметов из списка с весами. */
    public static final class RandomDeconSpec {
        private final int picks;
        private final Map<String, Double> weights = new LinkedHashMap<>();
        private RandomDeconSpec(int picks) { this.picks = picks; }
        public RandomDeconSpec weight(String itemId, double w) { weights.merge(itemId, w, Double::sum); return this; }
    }

    // ================= медикамент =================
    public static final class MedicineSpec {
        private final String itemId;
        private final MedCategory category;
        private final Integer useSkill;
        private final List<Effect> success = new ArrayList<>(), failure = new ArrayList<>();

        private MedicineSpec(String itemId, MedCategory category, Integer useSkill) { this.itemId = itemId; this.category = category; this.useSkill = useSkill; }

        public MedicineSpec success(String key, String name, EffectKind kind, Double value, Double duration, Double delay,
                                    Double chance, Double buffSeconds, boolean limbOnly, String... conditions) {
            success.add(new Effect(key, tr(name), kind, value, duration, delay, chance, buffSeconds, limbOnly, Arrays.asList(conditions))); return this;
        }
        public MedicineSpec failure(String key, String name, EffectKind kind, Double value, Double duration, Double delay,
                                    Double chance, Double buffSeconds, boolean limbOnly, String... conditions) {
            failure.add(new Effect(key, tr(name), kind, value, duration, delay, chance, buffSeconds, limbOnly, Arrays.asList(conditions))); return this;
        }

        private Medicine create(Item item, String bestReason) { return new Medicine(item, category, useSkill, success, failure, bestReason); }
    }

    // ================= грузовая миссия =================
    public static final class MissionSpec {
        private final String id, name, crate;
        private final int minCrates, maxCrates, reward;
        private final Map<String, Integer> contents = new LinkedHashMap<>();
        private final List<PlanSpec> plans = new ArrayList<>();

        private MissionSpec(String id, String name, String crate, int minCrates, int maxCrates, int reward) {
            this.id = id; this.name = name; this.crate = crate; this.minCrates = minCrates; this.maxCrates = maxCrates; this.reward = reward;
        }

        public MissionSpec content(String itemId, int count) { contents.put(itemId, count); return this; }
        public PlanSpec plan(PlanKind kind, double seconds) { PlanSpec p = new PlanSpec(this, kind, seconds); plans.add(p); return p; }
        /** Конец описания миссии (для читаемости сгенерированного кода). */
        public void done() { }

        private CargoMission create(Map<String, Item> all) {
            List<CargoPlan> ps = new ArrayList<>();
            for (PlanSpec p : plans) ps.add(p.create(all));
            return new CargoMission(id, tr(name), tr(crate), minCrates, maxCrates, reward, resolve(contents, all), ps);
        }
    }

    public static final class PlanSpec {
        private final MissionSpec owner;
        private final PlanKind kind;
        private final double seconds;
        private final Map<String, Integer> deconstruct = new LinkedHashMap<>(), use = new LinkedHashMap<>(), buy = new LinkedHashMap<>(),
                craft = new LinkedHashMap<>(), deconCrafted = new LinkedHashMap<>(), sell = new LinkedHashMap<>();
        private final List<String> locked = new ArrayList<>();

        private PlanSpec(MissionSpec owner, PlanKind kind, double seconds) { this.owner = owner; this.kind = kind; this.seconds = seconds; }

        public PlanSpec deconstruct(String id, int n) { deconstruct.put(id, n); return this; }
        public PlanSpec useDirectly(String id, int n) { use.put(id, n); return this; }
        public PlanSpec buy(String id, int n) { buy.put(id, n); return this; }
        public PlanSpec craft(String id, int n) { craft.put(id, n); return this; }
        public PlanSpec deconstructCrafted(String id, int n) { deconCrafted.put(id, n); return this; }
        public PlanSpec sell(String id, int n) { sell.put(id, n); return this; }
        public PlanSpec locked(String... ids) { locked.addAll(Arrays.asList(ids)); return this; }
        public MissionSpec done() { return owner; }

        private CargoPlan create(Map<String, Item> all) {
            return new CargoPlan(kind, resolve(deconstruct, all), resolve(use, all), resolve(buy, all), resolve(craft, all),
                    resolve(deconCrafted, all), resolve(sell, all), seconds, resolve(locked, all));
        }
    }

    // ================= таланты =================
    public static final class TalentSpec {
        final String name, description;
        final List<String> recipes = new ArrayList<>(), themes = new ArrayList<>();
        final List<TalentStat> stats = new ArrayList<>();
        final List<AllyBuff> ally = new ArrayList<>();
        TalentSpec(String name, String description) { this.name = name; this.description = description; }
        public TalentSpec recipes(String... ids) { recipes.addAll(Arrays.asList(ids)); return this; }
        public TalentSpec themes(String... ids) { themes.addAll(Arrays.asList(ids)); return this; }
        public TalentSpec stat(String type, String target, double value) { stats.add(new TalentStat(type, target, value)); return this; }
        public TalentSpec ally(String stat, double value, String jobs, String condition) { ally.add(new AllyBuff(stat, value, jobs, condition)); return this; }
    }

    public static final class JobSpec {
        final String id, name;
        final List<SubtreeSpec> subtrees = new ArrayList<>();
        JobSpec(String id, String name) { this.id = id; this.name = name; }
        public JobSpec subtree(String id, String name, boolean primary, String requires, String... blocks) {
            subtrees.add(new SubtreeSpec(id, name, primary, requires, Arrays.asList(blocks), new ArrayList<>())); return this;
        }
        /** Группа талантов последней добавленной ветки. */
        public JobSpec group(int required, int max, String... talentIds) {
            subtrees.get(subtrees.size() - 1).groups.add(new GroupSpec(required, max, Arrays.asList(talentIds))); return this;
        }
        public void done() { }
    }

    private record SubtreeSpec(String id, String name, boolean primary, String requires, List<String> blocks, List<GroupSpec> groups) {}
    private record GroupSpec(int required, int max, List<String> talentIds) {}

    // ================= оружие =================
    public static final class WeaponSpec {
        private final String itemId;
        private Double fire, spread, unskilled, melee, meleeRange, spray;
        private boolean auto;
        private Integer rounds;
        private final List<String> ammo = new ArrayList<>();
        private final List<DamageBlock> own = new ArrayList<>(), round = new ArrayList<>();
        private List<AfflictionHit> current;

        private WeaponSpec(String itemId) { this.itemId = itemId; }

        public WeaponSpec ranged(Double interval, boolean automatic, Double spread, Double unskilledSpread) {
            this.fire = interval; this.auto = automatic; this.spread = spread; this.unskilled = unskilledSpread; return this;
        }
        public WeaponSpec melee(Double interval, Double range) { this.melee = interval; this.meleeRange = range; return this; }
        public WeaponSpec sprayer(Double range) { this.spray = range; return this; }
        public WeaponSpec rounds(int n) { this.rounds = n; return this; }
        public WeaponSpec ammo(String... ids) { ammo.addAll(Arrays.asList(ids)); return this; }
        /** Новый блок урона; следующие affliction(...) добавляются в него. */
        public WeaponSpec hit(boolean ownDamage, DamageKind kind, Double radius, Double structure) {
            current = new ArrayList<>();
            (ownDamage ? own : round).add(new DamageBlock(kind, radius, structure, current));
            return this;
        }
        public WeaponSpec affliction(String name, double strength, Double probability) { current.add(new AfflictionHit(name, strength, probability)); return this; }

        private WeaponStats create(Map<String, Item> all) {
            return new WeaponStats(fire, auto, spread, unskilled, melee, meleeRange, spray, rounds, resolve(ammo, all), own, round);
        }
    }
}
