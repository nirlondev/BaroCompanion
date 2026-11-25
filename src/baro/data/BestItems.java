package baro.data;


import static baro.Tr.tr;
import baro.model.ItemCategory;

import java.util.*;

/**
 * Лучшие предметы каждой категории (кроме медицины — там свои подборки в BestPicks) с причинами.
 * Порядок внутри категории — по полезности.
 */
public final class BestItems {
    private static final Map<String, String> REASON = new LinkedHashMap<>();
    private static final Map<ItemCategory, List<String>> BY_CATEGORY = new EnumMap<>(ItemCategory.class);

    static {
        add(ItemCategory.WEAPON,
                "hmg", tr("ТП: самый высокий урон в секунду из ручного оружия, лучший против крупных монстров"),
                "assaultrifle", tr("Универсальная автоматическая винтовка: точная, скорострельная, на любую дистанцию"),
                "autoshotgun", tr("Огромный урон вблизи — против абордажа и монстров внутри лодки"),
                "grenadelauncher", tr("Урон по площади, разрывает группы ползунов и хасков"),
                "stungun", tr("Оглушает людей на расстоянии — лучшее против пиратов и бунтовщиков"),
                "physicoriumammobox", tr("Лучшие боеприпасы для магнитной пушки"),
                "nuclearshell", tr("Самый мощный снаряд рельсотрона"),
                "flakcannonammoboxexplosive", tr("Лучшие снаряды для шрапнельного орудия против роёв"));
        add(ItemCategory.MATERIAL,
                "physicorium", tr("Нужен для физикориевых боеприпасов — лучших для турелей"),
                "fulgurium", tr("Грозовиевые аккумуляторы и топливные стержни"),
                "incendium", tr("Воспламенитовое топливо и зажигательные боеприпасы"),
                "dementonite", tr("Дементонитовые инструменты и гравитационные снаряды"),
                "thorium", tr("Ториевые топливные стержни"),
                "titaniumaluminiumalloy", tr("Основа для оружия и брони; выпадает из многих предметов"),
                "endwormshell", tr("Самый дорогой материал для продажи"),
                "molochbone", tr("Очень дорого продаётся"));
        add(ItemCategory.ELECTRICAL,
                "fulguriumbatterycell", tr("Самый ёмкий аккумулятор для инструментов и оружия"),
                "fpgacircuit", tr("Нужна почти во всех электронных рецептах"),
                "wificomponent", tr("Связь без проводов — удобнее всего для автоматизации"),
                "memorycomponent", tr("Хранит значения — основа схем с переключением режимов"),
                "relaycomponent", tr("Включает и выключает питание по сигналу"),
                "motiondetector", tr("Сигнализация о монстрах и чужих в отсеке"));
        add(ItemCategory.EQUIPMENT,
                "bodyarmor", tr("Лучшая обычная броня от пуль и укусов"),
                "ballistichelmet1", tr("Лучшая защита головы"),
                "ballistichelmet2", tr("Лучшая защита головы"),
                "ballistichelmet3", tr("Лучшая защита головы"),
                "toolbelt", tr("Много места под инструменты и боеприпасы"),
                "plasmacutter", tr("Режет стены и двери быстрее всех"),
                "thermalgoggles", tr("Видно монстров и людей сквозь стены"),
                "handheldstatusmonitor", tr("Показывает состояние лодки и отсеков в руке"));
        add(ItemCategory.DIVING,
                "abyssdivingsuit", tr("Самая большая глубина без разрыва"),
                "combatdivingsuit", tr("Скафандр с бронёй — для боя снаружи"),
                "pucs", tr("Скафандр с автоматической помощью при ранениях"),
                "oxygenitetank", tr("Кислорода хватает намного дольше обычного баллона"),
                "underwaterscooter", tr("Быстрое перемещение снаружи лодки"));
        add(ItemCategory.FUEL,
                "thoriumfuelrod", tr("Больше энергии и дольше обычного стержня"),
                "fulguriumfuelrod", tr("Ещё мощнее ториевого"),
                "fulguriumfuelrodvolatile", tr("Самый мощный, но нестабилен — держите охлаждение под контролем"),
                "incendiumfueltank", tr("Лучшее топливо для огнемёта и сварки"));
        add(ItemCategory.GENETIC,
                "geneticmaterialmoloch", tr("Ген молоха: снижает получаемый урон (10–25%)"),
                "geneticmaterialhammerheadmatriarch", tr("Ген матриарха: лечит урон со временем"),
                "geneticmaterialthresher", tr("Ген акульего тигра: меньше расход кислорода (до 100%)"),
                "geneticmaterialhusk", tr("Ген хаска: иммунитет к превращению в хаска"),
                "geneticmaterialmantis", tr("Ген богомола: быстрее ходьба"),
                "genesplicer", tr("Нужен, чтобы вживить гены"));
        add(ItemCategory.ALIEN,
                "thermalartifact", tr("Продаётся очень дорого (база 2000 mk)"),
                "faradayartifact", tr("Продаётся очень дорого (база 2000 mk)"),
                "nasonovartifact", tr("Продаётся очень дорого (база 2000 mk)"),
                "skyholderartifact", tr("Продаётся очень дорого (база 2000 mk)"),
                "psychosisartifact", tr("Продаётся очень дорого (база 2000 mk)"),
                "oxygeniteshard", tr("Источник кислородита для баллонов"));
        add(ItemCategory.MISC,
                "advancedgenesplicer", tr("Вживляет гены с меньшим риском"),
                "advancedsyringegun", tr("Лучший шприцемёт для лечения на расстоянии"),
                "defensebotspawner", tr("Боевой дрон, который защищает лодку"),
                "slipsuit", tr("Гидрокостюм: свобода движений внутри лодки и защита в воде"),
                "portablepump", tr("Откачивает воду из затопленных отсеков"),
                "skillbooksubmarinewarfare", tr("Книга навыка: повышает навык"),
                "skillbookeuropanmedicine", tr("Книга навыка: повышает навык"),
                "skillbookhandyseaman", tr("Книга навыка: повышает навык"),
                "skillbooksailorsguide", tr("Книга навыка: повышает навык"));
        add(ItemCategory.MEDICAL,
                "autoinjectorheadset", tr("Сам вкалывает препарат при тяжёлом ранении"),
                "syringegun", tr("Лечение и яды на расстоянии"));
    }

    private BestItems() {}

    private static void add(ItemCategory c, String... pairs) {
        List<String> ids = BY_CATEGORY.computeIfAbsent(c, k -> new ArrayList<>());
        for (int i = 0; i < pairs.length; i += 2) { ids.add(pairs[i]); REASON.put(pairs[i], pairs[i + 1]); }
    }

    public static Map<String, String> all() { return Collections.unmodifiableMap(REASON); }
    public static List<String> idsFor(ItemCategory c) { return BY_CATEGORY.getOrDefault(c, Collections.emptyList()); }
}
