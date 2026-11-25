package baro.data.gamefiles;


import static baro.Tr.tr;
import baro.data.CatalogBuilder;

/**
 * Данные прямо из установленной игры (и включённых модов) поверх встроенных: предметы, цены, рецепты, разбор,
 * оружие, медикаменты и таланты. Чего нет в файлах игры (миссии, подборки лучших), остаётся встроенным.
 */
public final class GameFilesReader {
    private GameFilesReader() {}

    /**
     * Что прочитано: описание источника для показа пользователю и где лежат картинки предметов.
     */
    public record Result(String source, java.util.Map<String, baro.data.ItemIcons.Spec> icons) { }

    /** @param language язык текстов как в игре («Russian», «English»…) */
    public static Result register(CatalogBuilder b, GameInstall game, String language) {
        long t0 = System.currentTimeMillis();
        GameTexts texts = new GameTexts(game, language);
        GameContent content = new GameContent(game);
        if (content.items.isEmpty()) throw new IllegalStateException(tr("в файлах игры не найдено ни одного предмета"));
        ItemsReader items = new ItemsReader(content, texts);
        TalentsReader talents = new TalentsReader(game, content, texts, items);
        // английские названия — для поиска по-английски и ссылок на вики (только если язык не английский)
        java.util.Map<String, String> english = new java.util.HashMap<>();
        if (!language.equalsIgnoreCase("English")) {
            ItemsReader en = new ItemsReader(content, new GameTexts(game, "English"));
            for (String id : items.included) english.put(id, en.name(id));
        }
        java.util.Map<String, baro.data.ItemIcons.Spec> icons = items.icons(game.dir());
        // всё прочитано — теперь заменяем встроенные данные
        items.register(b, english);
        for (GameContent.Affliction a : content.afflictions.values()) b.affliction(affliction(a, texts));
        int weapons = new WeaponsReader(content, texts).register(b, items.included);
        int meds = new MedicinesReader(content, texts).register(b, items.included);
        int tal = talents.register(b);
        String mods = game.mods().isEmpty() ? "" : tr(", моды: ") + String.join(", ", game.mods());
        String s = tr("Файлы игры: ") + game.dir() + mods + tr(" — предметов ") + items.included.size() + tr(", оружия ") + weapons
                + tr(", медикаментов ") + meds + (tal >= 0 ? tr(", талантов ") + tal : tr(", таланты встроенные"))
                + (texts.isEmpty() ? tr(", тексты не найдены") : "") + " (" + (System.currentTimeMillis() - t0) + tr(" мс)");
        System.err.println(s);
        return new Result(s, icons);
    }

    /** Недуг: название и описание (с учётом translationoverride), сила, от чего защищает (для баффов-защит). */
    private static baro.model.Affliction affliction(GameContent.Affliction a, GameTexts texts) {
        String key = GameXml.attr(a.element, "translationoverride", a.id);
        String name = texts.get("afflictionname." + key);
        if (name == null) name = texts.get("afflictionname." + a.id, GameXml.attr(a.element, "name", a.id));
        String desc = texts.get("afflictiondescription." + key);
        if (desc == null) desc = texts.get("afflictiondescription." + a.id, GameXml.attr(a.element, "description", ""));
        java.util.Map<String, Double> res = new java.util.LinkedHashMap<>();
        for (org.w3c.dom.Element e : GameXml.children(a.element, "effect")) {
            double v = Math.max(GameXml.num(e, "minresistance", 0), GameXml.num(e, "maxresistance", 0));
            if (v <= 0) continue;
            for (String r : GameXml.list(GameXml.attr(e, "resistancefor"))) res.merge(r.toLowerCase(java.util.Locale.ROOT), v, Math::max);
        }
        return new baro.model.Affliction(a.id, name, desc, a.type, a.buff, a.limbSpecific, GameXml.num(a.element, "maxstrength", 0), res);
    }
}