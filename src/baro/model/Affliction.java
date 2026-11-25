package baro.model;


import static baro.Tr.tr;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Недуг или состояние персонажа (кровотечение, отравление, хаск, баффы…) из Afflictions.xml.
 * Связи — чем лечить, что вызывает, что защищает — считает {@link baro.data.AfflictionIndex}.
 */
public final class Affliction {
    private final String id;
    private final String name;
    private final String description;
    private final String type;
    private final boolean buff;
    private final boolean limbSpecific;
    private final double maxStrength;
    private final Map<String, Double> resistances;

    /**
     * @param type тип из файлов игры: damage, bleeding, burn, poison, paralysis, alieninfection… (пусто — неизвестен)
     * @param resistances для баффов-защит: от чего (идентификатор или тип недуга) и насколько (0…1)
     */
    public Affliction(String id, String name, String description, String type, boolean buff, boolean limbSpecific,
                      double maxStrength, Map<String, Double> resistances) {
        this.id = id; this.name = name; this.description = description == null ? "" : description;
        this.type = type == null ? "" : type; this.buff = buff; this.limbSpecific = limbSpecific; this.maxStrength = maxStrength;
        this.resistances = Collections.unmodifiableMap(resistances);
    }

    public String id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    public String type() { return type; }
    public boolean isBuff() { return buff; }
    /** Действует на отдельную часть тела (кровотечение, ожог), а не на весь организм. */
    public boolean isLimbSpecific() { return limbSpecific; }
    /** Наибольшая сила (обычно 100); 0 — неизвестна. */
    public double maxStrength() { return maxStrength; }
    /** От чего защищает этот бафф (идентификатор или тип недуга → доля защиты 0…1). */
    public Map<String, Double> resistances() { return resistances; }
    public Optional<String> typeIfKnown() { return type.isEmpty() ? Optional.empty() : Optional.of(type); }

    /** Подходит ли эффект препарата к этому недугу: по идентификатору или по типу («Кровотечение (все)»). */
    public boolean matches(Effect e) { return e.key().equalsIgnoreCase(id) || !type.isEmpty() && e.key().equalsIgnoreCase("TYPE:" + type); }

    public boolean nameContains(String normalizedQuery) {
        return (name + " " + id).toLowerCase(java.util.Locale.ROOT).replace('ё', 'е').contains(normalizedQuery);
    }

    @Override public String toString() { return name; }

    /** Слова, по которым тип недуга понятен человеку. */
    public static String typeTitle(String type) {
        switch (type == null ? "" : type.toLowerCase(java.util.Locale.ROOT)) {
            case "damage": return tr("травма");
            case "bleeding": return tr("кровотечение");
            case "burn": return tr("ожог");
            case "poison": return tr("отравление");
            case "paralysis": return tr("паралич");
            case "stun": return tr("оглушение");
            case "alieninfection": return tr("чужеродная инфекция");
            case "bloodloss": return tr("кровопотеря");
            case "oxygenlow": return tr("удушье");
            case "infection": return tr("инфекция");
            case "psychosis": return tr("психоз");
            case "resistance": return tr("защита");
            case "addiction": return tr("зависимость");
            case "withdrawal": return tr("ломка");
            case "radiationsickness": return tr("радиация");
            case "geneticmaterialbuff": case "geneticmaterialdebuff": return tr("генетика");
            case "talentbuff": return tr("талант");
            case "buff": return tr("бафф");
            case "debuff": return tr("дебафф");
            default: return type == null ? "" : type;
        }
    }

    /** Список «защищает от» по-русски для показа. */
    public List<String> resistanceKeys() { return List.copyOf(resistances.keySet()); }
}
