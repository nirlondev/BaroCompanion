package baro.data;

import baro.model.*;

import java.util.*;

/**
 * Справочник болезней «от недуга»: чем лечить, что вызывает, что защищает.
 * Если файлы игры не прочитаны, список недугов собирается из эффектов встроенных препаратов (без описаний).
 */
public final class AfflictionIndex {
    /** Препарат и его эффект на этот недуг. */
    public record Use(Medicine medicine, Effect effect) { }

    /** Оружие или боеприпас, попадание которого накладывает недуг. */
    public record Hit(Item item, AfflictionHit hit) { }

    private final List<Affliction> all;
    private final Map<String, Affliction> byId = new HashMap<>();
    private final Collection<Medicine> medicines;
    private final Collection<Item> items;
    private final boolean localized;

    /** Типы известных недугов на случай, когда файлов игры нет (во встроенных данных типов нет). */
    private static final Map<String, String> KNOWN_TYPES = Map.ofEntries(
            Map.entry("blunttrauma", "damage"), Map.entry("lacerations", "damage"), Map.entry("gunshotwound", "damage"),
            Map.entry("bitewounds", "damage"), Map.entry("explosiondamage", "damage"), Map.entry("internaldamage", "damage"),
            Map.entry("organdamage", "damage"), Map.entry("burn", "burn"), Map.entry("acidburn", "burn"), Map.entry("bleeding", "bleeding"),
            Map.entry("cyanidepoisoning", "poison"), Map.entry("sufforinpoisoning", "poison"), Map.entry("morbusinepoisoning", "poison"),
            Map.entry("deliriuminepoisoning", "poison"), Map.entry("paralysis", "paralysis"), Map.entry("huskinfection", "alieninfection"),
            Map.entry("stun", "stun"), Map.entry("bloodloss", "bloodloss"), Map.entry("oxygenlow", "oxygenlow"));

    AfflictionIndex(List<Affliction> fromGame, Collection<Medicine> medicines, Collection<Item> items) {
        this.medicines = medicines;
        this.items = items;
        List<Affliction> list = new ArrayList<>(fromGame);
        if (list.isEmpty()) list.addAll(fromEffects(medicines));
        for (Affliction a : list) byId.put(a.id().toLowerCase(Locale.ROOT), a);
        list.sort(Comparator.comparing(Affliction::name, String.CASE_INSENSITIVE_ORDER));
        this.all = Collections.unmodifiableList(list);
        long translated = list.stream().filter(a -> hasNonLatin(a.name())).count();
        this.localized = translated > list.size() / 2;
    }

    private static List<Affliction> fromEffects(Collection<Medicine> meds) {
        Map<String, Affliction> m = new LinkedHashMap<>();
        for (Medicine med : meds)
            for (List<Effect> l : List.of(med.onSuccess(), med.onFailure()))
                for (Effect e : l) {
                    if (e.key().startsWith("TYPE:") || e.value().isEmpty() || m.containsKey(e.key())) continue;
                    if (e.key().equals("talent") || e.key().equals("refund")) continue;
                    String type = KNOWN_TYPES.getOrDefault(e.key(), e.kind() == EffectKind.BUFF ? "buff" : "");
                    Map<String, Double> res = new HashMap<>();
                    if (e.key().endsWith("resistance")) res.put(e.key().substring(0, e.key().length() - "resistance".length()), 1.0);
                    m.put(e.key(), new Affliction(e.key(), e.name(), "", type, e.kind() == EffectKind.BUFF, e.isLimbOnly(), 0, res));
                }
        return new ArrayList<>(m.values());
    }

    /** Недуг (а не бафф, защита или генетический материал) — для деления списка. */
    public boolean isAilment(Affliction a) {
        String t = a.type().toLowerCase(Locale.ROOT);
        return !a.isBuff() && !t.startsWith("geneticmaterial") && !t.equals("resistance") && !t.equals("talentbuff");
    }

    /**
     * Показывать ли в списке: служебные недуги без перевода («ProgressiveStun») прячутся, если почти все
     * остальные переведены (по ссылкам из карточек они всё равно открываются).
     */
    public boolean isListed(Affliction a) { return !localized || hasNonLatin(a.name()); }

    private static boolean hasNonLatin(String s) {
        for (int i = 0; i < s.length(); i++) { char c = s.charAt(i); if (Character.isLetter(c) && Character.UnicodeScript.of(c) != Character.UnicodeScript.LATIN) return true; }
        return false;
    }

    /** Все недуги и состояния, по алфавиту. */
    public List<Affliction> all() { return all; }

    public Optional<Affliction> get(String id) { return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id.toLowerCase(Locale.ROOT))); }

    /** Недуг по названию (так на них ссылается урон оружия). */
    public Optional<Affliction> byName(String name) {
        for (Affliction a : all) if (a.name().equalsIgnoreCase(name)) return Optional.of(a);
        return Optional.empty();
    }

    /** Чем лечить: препараты, которые снимают недуг (сильнее — выше). */
    public List<Use> treatments(Affliction a) { return uses(a, EffectKind.GOOD, true); }

    /** Препараты, после которых недуг появляется (побочный эффект или яд). */
    public List<Use> causedByMedicine(Affliction a) { return uses(a, a.isBuff() ? EffectKind.BUFF : EffectKind.BAD, false); }

    private List<Use> uses(Affliction a, EffectKind kind, boolean reduces) {
        List<Use> l = new ArrayList<>();
        for (Medicine m : medicines)
            for (Effect e : m.onSuccess())
                if (e.kind() == kind && a.matches(e) && e.value().isPresent() && (reduces ? e.value().get() < 0 : e.value().get() > 0)) { l.add(new Use(m, e)); break; }
        l.sort(Comparator.comparingDouble((Use u) -> -Math.abs(u.effect().value().orElse(0.0)))
                .thenComparing(u -> !u.effect().key().equalsIgnoreCase(a.id())));
        return l;
    }

    /** Оружие и боеприпасы, чьё попадание накладывает недуг. */
    public List<Hit> causedByWeapons(Affliction a) {
        List<Hit> l = new ArrayList<>();
        for (Item it : items) {
            if (it.weapon().isEmpty()) continue;
            WeaponStats w = it.weapon().get();
            AfflictionHit best = null;
            for (List<DamageBlock> blocks : List.of(w.ownDamage(), w.roundDamage()))
                for (DamageBlock b : blocks)
                    for (AfflictionHit h : b.afflictions())
                        if (h.name().equalsIgnoreCase(a.name()) && (best == null || h.strength() > best.strength())) best = h;
            if (best != null) l.add(new Hit(it, best));
        }
        l.sort(Comparator.comparingDouble((Hit h) -> -h.hit().strength()));
        return l;
    }

    /** Баффы, которые защищают от недуга (по идентификатору или типу), и доля защиты. */
    public Map<Affliction, Double> protections(Affliction a) {
        Map<Affliction, Double> m = new LinkedHashMap<>();
        for (Affliction b : all)
            for (Map.Entry<String, Double> r : b.resistances().entrySet())
                if (r.getKey().equalsIgnoreCase(a.id()) || !a.type().isEmpty() && r.getKey().equalsIgnoreCase(a.type())) m.merge(b, r.getValue(), Math::max);
        return m;
    }

    /** Препараты, которые дают защиту от недуга (через баффы-защиты). */
    public List<Use> preventions(Affliction a) {
        List<Use> l = new ArrayList<>();
        for (Affliction b : protections(a).keySet())
            for (Medicine m : medicines)
                for (Effect e : m.onSuccess())
                    if (e.kind() == EffectKind.BUFF && b.matches(e) && e.value().orElse(0.0) > 0 && l.stream().noneMatch(u -> u.medicine() == m)) l.add(new Use(m, e));
        return l;
    }

    /** От чего защищает бафф: недуги по ключам защиты. */
    public List<Affliction> protectsFrom(Affliction buff) {
        List<Affliction> l = new ArrayList<>();
        for (String k : buff.resistances().keySet()) {
            Optional<Affliction> one = get(k);
            if (one.isPresent()) { if (!l.contains(one.get())) l.add(one.get()); continue; }
            for (Affliction x : all) if (x.type().equalsIgnoreCase(k) && !x.isBuff() && !l.contains(x)) l.add(x);
        }
        return l;
    }

    /** Недуг, который снимает или накладывает эффект (для ссылки из карточки препарата). */
    public Optional<Affliction> of(Effect e) {
        Optional<Affliction> a = get(e.key());
        return a.isPresent() ? a : byName(e.name());
    }
}
