package baro.data.gamefiles;


import static baro.Tr.tr;
import baro.data.CatalogBuilder;
import baro.model.EffectKind;
import baro.model.MedCategory;
import org.w3c.dom.Element;

import java.util.*;

/**
 * Эффекты медикаментов из StatusEffect type="OnSuccess"/"OnFailure" (или "OnUse", если успеха/провала нет).
 * Итог за всё время действия: сила × длительность (как во встроенных данных); снятие недуга — со знаком минус.
 */
final class MedicinesReader {
    private static final Map<String, String> TYPE_NAMES = Map.of(
            "bleeding", tr("Кровотечение (все)"), "burn", tr("Ожоги (все)"), "damage", tr("Травмы (весь урон)"),
            "infection", tr("Зараженная рана (все)"), "poison", tr("Отравления (все)"), "paralysis", tr("Паралич (все)"));

    private final GameContent content;
    private final GameTexts texts;

    MedicinesReader(GameContent content, GameTexts texts) { this.content = content; this.texts = texts; }

    /** Сумма одного эффекта по всем StatusEffect. */
    private static final class Acc {
        final String key, name; final EffectKind kind; final boolean limb; final List<String> conditions;
        double value; Double duration, delay, chance;
        double start = Double.MAX_VALUE, end; int parts;
        Acc(String key, String name, EffectKind kind, boolean limb, List<String> conditions) {
            this.key = key; this.name = name; this.kind = kind; this.limb = limb; this.conditions = conditions;
        }
        double rounded() { return Math.round(value * 1000) / 1000.0; }
    }

    int register(CatalogBuilder b, Set<String> included) {
        int n = 0;
        for (String id : included) {
            Element it = content.items.get(id);
            MedCategory known = b.medicineCategory(id);
            boolean medical = GameXml.attr(it, "category", "").toLowerCase(Locale.ROOT).contains("medical");
            if (known == null && !medical) continue;
            List<Element> success = new ArrayList<>(), failure = new ArrayList<>(), use = new ArrayList<>();
            Integer skill = null;
            for (Element comp : GameXml.children(it)) {
                if (GameXml.is(comp, "fabricate") || GameXml.is(comp, "deconstruct") || GameXml.is(comp, "price")) continue;
                for (Element e : GameXml.descendants(comp)) {
                    if (GameXml.is(e, "requiredskill") && "medical".equalsIgnoreCase(GameXml.attr(e, "identifier", "")) && skill == null)
                        skill = (int) GameXml.num(e, "level", 0);
                    if (!GameXml.is(e, "statuseffect")) continue;
                    String type = GameXml.attr(e, "type", "").trim().toLowerCase(Locale.ROOT);
                    if (type.equals("onsuccess")) success.add(e);
                    else if (type.equals("onfailure")) failure.add(e);
                    else if (type.equals("onuse") && targetsCharacter(e)) use.add(e);
                }
            }
            if (success.isEmpty()) success = use;
            List<Acc> ok = effects(success), bad = effects(failure);
            if (ok.isEmpty()) continue; // нет эффектов — оставляем встроенное описание, если оно было
            MedCategory cat = known != null ? known : category(id, ok);
            CatalogBuilder.MedicineSpec ms = b.medicine(id, cat, skill);
            for (Acc a : ok) ms.success(a.key, a.name, a.kind, a.rounded(), a.duration, a.delay, a.chance, null, a.limb, a.conditions.toArray(new String[0]));
            for (Acc a : bad) ms.failure(a.key, a.name, a.kind, a.rounded(), a.duration, a.delay, a.chance, null, a.limb, a.conditions.toArray(new String[0]));
            n++;
        }
        return n;
    }

    private static boolean targetsCharacter(Element se) {
        String t = GameXml.attr(se, "target", "").toLowerCase(Locale.ROOT);
        return t.contains("usetarget") || t.contains("character") || t.contains("limb");
    }

    /** Условия StatusEffect по-русски: «если заражение меньше 75%», «если цель — хаск». */
    private List<String> conditions(Element se) {
        List<String> l = new ArrayList<>();
        for (Element c : GameXml.children(se, "conditional")) {
            org.w3c.dom.NamedNodeMap attrs = c.getAttributes();
            for (int i = 0; i < attrs.getLength(); i++) {
                String k = attrs.item(i).getNodeName(), v = attrs.item(i).getNodeValue().trim();
                if (k.equalsIgnoreCase("ishusk")) { l.add("true".equalsIgnoreCase(v) ? tr("если цель — хаск") : tr("если цель не хаск")); continue; }
                if (k.equalsIgnoreCase("entitytype") || k.equalsIgnoreCase("targetitemcomponent") || k.equalsIgnoreCase("comparison")) continue;
                String[] p = v.split("\\s+", 2);
                String op = p.length == 2 ? p[0].toLowerCase(Locale.ROOT) : "eq", val = p.length == 2 ? p[1] : v;
                String opText = switch (op) {
                    case "lt" -> tr("меньше");
                    case "lte" -> tr("не больше");
                    case "gt" -> tr("больше");
                    case "gte" -> tr("не меньше");
                    case "neq" -> tr("не равно");
                    default -> tr("равно");
                };
                String name = texts.get("afflictionname." + k);
                if (name == null) continue; // условия не про недуги в описании не показываем
                String num = val.replaceAll("\\.0+$", "");
                l.add(tr("если ") + name.toLowerCase(Locale.ROOT) + " " + opText + " " + num + (content.afflictions.containsKey(k) ? "%" : ""));
            }
        }
        return l;
    }

    private List<Acc> effects(List<Element> statusEffects) {
        Map<String, Acc> m = new LinkedHashMap<>();
        for (Element se : statusEffects) {
            Double duration = GameXml.num(se, "duration");
            if (duration != null && duration <= 0) duration = null;
            Double delay = GameXml.num(se, "delay");
            Double prob = GameXml.num(se, "probability");
            boolean limbTarget = GameXml.attr(se, "target", "").toLowerCase(Locale.ROOT).contains("limb");
            List<String> conds = conditions(se);
            StringBuilder condKey = new StringBuilder();
            for (Element c : GameXml.children(se, "conditional")) {
                org.w3c.dom.NamedNodeMap at = c.getAttributes();
                for (int i = 0; i < at.getLength(); i++) condKey.append(at.item(i).getNodeName().toLowerCase(Locale.ROOT)).append('=').append(at.item(i).getNodeValue()).append(';');
            }
            for (Element a : GameXml.children(se)) {
                boolean add = GameXml.is(a, "affliction"), reduce = GameXml.is(a, "reduceaffliction");
                if (!add && !reduce) continue;
                Double amount = GameXml.num(a, add ? "strength" : "amount");
                if (amount == null) amount = GameXml.num(a, add ? "amount" : "strength");
                if (amount == null || amount == 0) continue;
                String id = GameXml.attr(a, "identifier"), type = GameXml.attr(a, "type");
                String key; String name; boolean buff;
                if (id != null && !id.isBlank()) {
                    id = id.trim();
                    GameContent.Affliction af = content.afflictions.get(id);
                    key = id; name = texts.get("afflictionname." + id, id);
                    buff = af != null && af.buff;
                } else if (type != null && !type.isBlank()) {
                    type = type.trim().toLowerCase(Locale.ROOT);
                    key = "TYPE:" + type; name = typeName(type);
                    buff = false;
                    for (GameContent.Affliction af : content.afflictions.values()) if (af.type.equalsIgnoreCase(type)) { buff = af.buff; break; }
                } else continue;
                Double p = GameXml.num(a, "probability");
                if (p == null) p = prob;
                if (p != null && p >= 1) p = null;
                EffectKind kind = reduce ? (buff ? EffectKind.BAD : EffectKind.GOOD) : buff ? EffectKind.BUFF : EffectKind.BAD;
                // эффекты при разных условиях не складываются
                Acc acc = m.computeIfAbsent(key + "|" + kind + "|" + condKey,k -> new Acc(key, name, kind, limbTarget, conds));
                double total = amount * (duration != null ? duration : 1);
                acc.value += reduce ? -total : total;
                double start = delay != null && delay > 0 ? delay : 0;
                if (duration != null) {
                    acc.start = Math.min(acc.start, start);
                    acc.end = Math.max(acc.end, start + duration);
                }
                acc.parts++;
                if (delay != null && delay > 0 && acc.parts == 1) acc.delay = delay;
                if (p != null) acc.chance = p;
            }
        }
        List<Acc> l = new ArrayList<>();
        for (Acc a : m.values()) {
            if (Math.abs(a.value) <= 1e-9) continue;
            if (a.end > 0) {
                // несколько эффектов подряд: длительность — от первого начала до последнего конца
                a.duration = a.parts > 1 ? a.end - a.start : a.end - (a.delay == null ? 0 : a.delay);
                if (a.parts > 1) a.delay = a.start > 0 ? a.start : null;
            }
            l.add(a);
        }
        return l;
    }

    private String typeName(String type) {
        String n = TYPE_NAMES.get(type);
        if (n != null) return n;
        for (GameContent.Affliction af : content.afflictions.values())
            if (af.type.equalsIgnoreCase(type)) { String t = texts.get("afflictionname." + af.id); if (t != null) return t + tr(" (все)"); }
        return type;
    }

    private MedCategory category(String id, List<Acc> effects) {
        String file = String.valueOf(content.itemFile.get(id)).replace('\\', '/').toLowerCase(Locale.ROOT);
        if (file.endsWith("/poisons.xml") || content.tagsOf(id).contains("poison")) return MedCategory.TOXIN;
        boolean heals = false, buffs = false;
        for (Acc a : effects) { if (a.kind == EffectKind.GOOD) heals = true; if (a.kind == EffectKind.BUFF) buffs = true; }
        if (file.endsWith("/buffs.xml") || buffs && !heals) return MedCategory.BUFF;
        return heals ? MedCategory.HEAL : MedCategory.OTHER;
    }
}