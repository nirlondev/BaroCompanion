package baro.data.gamefiles;


import static baro.Tr.tr;
import baro.data.CatalogBuilder;
import baro.model.AfflictionHit;
import baro.model.DamageKind;
import baro.model.ItemCategory;
import org.w3c.dom.Element;

import java.util.*;

/** Характеристики оружия, боеприпасов и взрывчатки (те же правила, что в gen_weapons.py). */
final class WeaponsReader {
    private static final Map<String, String> EXTRA = Map.of(
            "emp", tr("ЭМИ (выводит из строя электронику)"), "electricshock", tr("Удар током"), "blind", tr("Ослепление"), "blind_hard", tr("Сильное ослепление"),
            "sensoryoverload", tr("Сенсорная перегрузка"), "burning", tr("Горение"), "incendium", tr("Горение воспламенитом"));

    private final GameContent content;
    private final GameTexts texts;

    WeaponsReader(GameContent content, GameTexts texts) { this.content = content; this.texts = texts; }

    private record Block(DamageKind kind, Double radius, Double structure, List<AfflictionHit> afflictions) {}

    int register(CatalogBuilder b, Set<String> included) {
        int n = 0;
        for (String id : included) {
            Element it = content.items.get(id);
            ItemCategory cat = b.itemCategory(id);
            boolean weaponCat = cat == ItemCategory.WEAPON;
            Element rw = null, mw = null, rt = null;
            List<Element> containers = new ArrayList<>();
            for (Element c : GameXml.children(it)) {
                if (GameXml.is(c, "rangedweapon")) rw = c;
                else if (GameXml.is(c, "meleeweapon")) mw = c;
                else if (GameXml.is(c, "repairtool") && weaponCat) rt = c;
                else if (GameXml.is(c, "itemcontainer")) containers.add(c);
            }
            List<Block> own = blocks(it);
            Integer rounds = null;
            List<Block> round = new ArrayList<>();
            for (Element c : containers) {
                String sid = GameXml.attr(c, "spawnwithid");
                if (sid == null || !content.items.containsKey(sid.trim()) || included.contains(sid.trim())) continue;
                round = blocks(content.items.get(sid.trim()));
                Double delta = null;
                for (Element se : GameXml.descendants(c))
                    if (GameXml.is(se, "statuseffect") && GameXml.attr(se, "type", "").equalsIgnoreCase("onuse")) delta = GameXml.num(se, "condition");
                double health = GameXml.num(it, "health", 100);
                if (delta != null && delta < 0) rounds = (int) Math.round(health / -delta);
            }
            List<String> ammo = new ArrayList<>();
            boolean hasTool = rw != null || mw != null || rt != null;
            if (hasTool) {
                for (Element c : containers) {
                    Element ca = GameXml.child(c, "containable");
                    if (ca != null) for (String a : containable(GameXml.attr(ca, "items"), included)) {
                        ItemCategory ac = b.itemCategory(a);
                        if (!ammo.contains(a) && !a.equals(id) && (ac == ItemCategory.WEAPON || ac == ItemCategory.FUEL || ac == ItemCategory.MISC || ac == ItemCategory.ELECTRICAL)) ammo.add(a);
                    }
                    if (!ammo.isEmpty()) break;
                }
            }
            if ((!hasTool || !weaponCat) && own.isEmpty() && round.isEmpty()) { b.removeWeapon(id); continue; }
            CatalogBuilder.WeaponSpec w = b.weapon(id);
            if (rw != null) w.ranged(GameXml.num(rw, "reload"), GameXml.bool(rw, "holdtrigger", false), GameXml.num(rw, "spread"), GameXml.num(rw, "unskilledspread"));
            if (mw != null) w.melee(GameXml.num(mw, "reload"), GameXml.num(mw, "range"));
            if (rt != null) w.sprayer(GameXml.num(rt, "range"));
            if (rounds != null && rounds > 0) w.rounds(rounds);
            if (!ammo.isEmpty()) w.ammo(ammo.subList(0, Math.min(12, ammo.size())).toArray(new String[0]));
            emit(w, own, true);
            emit(w, round, false);
            n++;
        }
        return n;
    }

    private void emit(CatalogBuilder.WeaponSpec w, List<Block> blocks, boolean own) {
        for (Block bl : blocks) {
            w.hit(own, bl.kind, bl.radius, bl.structure);
            for (AfflictionHit a : bl.afflictions) w.affliction(a.name(), a.strength(), a.probability().orElse(null));
        }
    }

    private List<Block> blocks(Element it) {
        List<Block> res = new ArrayList<>();
        for (Element a : GameXml.descendants(it)) {
            boolean explosion = GameXml.is(a, "explosion");
            if (!explosion && !GameXml.is(a, "attack")) continue;
            Double sd = GameXml.num(a, "structuredamage");
            Block bl = new Block(explosion ? DamageKind.EXPLOSION : DamageKind.HIT, explosion ? GameXml.num(a, "range") : null, sd != null && sd > 0 ? sd : null, new ArrayList<>());
            for (Element x : GameXml.children(a, "affliction")) {
                Double s = GameXml.num(x, "strength");
                if (s == null || s == 0) continue;
                String aid = GameXml.attr(x, "identifier", "");
                bl.afflictions.add(new AfflictionHit(texts.get("afflictionname." + aid, EXTRA.getOrDefault(aid, aid)), s, GameXml.num(x, "probability")));
            }
            if (bl.afflictions.isEmpty() && bl.structure == null) continue;
            res.add(bl);
        }
        return res;
    }

    private List<String> containable(String spec, Set<String> included) {
        List<String> out = new ArrayList<>();
        for (String token : GameXml.list(spec)) {
            if (included.contains(token)) { out.add(token); continue; }
            for (String i : content.withTag(token)) if (included.contains(i) && !out.contains(i)) out.add(i);
        }
        return out;
    }
}