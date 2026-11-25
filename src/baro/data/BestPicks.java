package baro.data;


import static baro.Tr.tr;
import baro.model.MedCategory;

import java.util.*;

/** Подборка лучших препаратов по разделам с причинами. */
public final class BestPicks {
    private static final Map<MedCategory, List<String[]>> BY_CATEGORY = new EnumMap<>(MedCategory.class);
    private static final Map<String, String> REASON = new HashMap<>();

    static {
        add(MedCategory.HEAL,
                "deusizine", tr("Лучший универсальный: за 20 с снимает 60 травм, 50 кровопотери, кровотечение, внутренние травмы и заражение, плюс Решительность 200"),
                "antidama2", tr("Самое быстрое снятие урона: −75 травм за 5 с (для уверенного укола нужна Медицина 72)"),
                "antibloodloss2", tr("−120 кровопотери без побочных эффектов"),
                "antibleeding3", tr("−60 кровотечения за 2 с на часть тела и лечит заражение раны"),
                "antibleeding2", tr("Лучшее от ожогов: −50 за 10 с на часть тела"),
                "liquidoxygenite", tr("Спасает от удушья: −275 нехватки кислорода"));
        add(MedCategory.ANTIDOTE,
                "calyxanide", tr("Единственное лекарство от хаска — снимает заражение, пока оно меньше 100%"),
                "antibiotics", tr("Профилактика хаска (устойчивость 600) и −75 к заражению ран"),
                "antinarc", tr("Снимает передозировку, зависимость и ломку от опиатов (−60)"),
                "antipsychosis", tr("Мгновенно −100 психоза и галлюцинаций"),
                "antirad", tr("Мгновенно −100 лучевой болезни"),
                "europabrew", tr("Запасной вариант: −30 ко всем отравлениям сразу"));
        add(MedCategory.BUFF,
                "hyperzine", tr("Лучший боевой: Гиперактивность 400 + Решительность 400, ≈6–7 мин"),
                "combatstimulantsyringe", tr("Лечит в бою и даёт 45–50% защиты от оглушения; нужен талант «Суперсолдаты»"),
                "pressurestabilizer", tr("≈15 мин на глубине без водолазного костюма; нужен талант"),
                "steroids", tr("Решительность 420 (≈7 мин) — защита и сила без скорости"),
                "meth", tr("Гиперактивность 420 (≈7 мин) — самая дешёвая скорость"),
                "endocrinebooster", tr("Навсегда даёт талант другой профессии"));
        add(MedCategory.TOXIN,
                "paralyzant", tr("Обездвиживает цель — лучший против людей и для захвата"),
                "chloralhydrate", tr("Усыпляет: нарастающее оглушение 125"),
                "morbusine", tr("Смертельный яд, по монстрам почти вдвое сильнее (20 против 11)"),
                "sulphuricacidsyringe", tr("Против крупных монстров: кислотный ожог до 155 и сильное замедление"),
                "raptorbaneextract", tr("Убивает грязевых рапторов: 200 повреждения органов"));
        add(MedCategory.OTHER,
                "stabilozine", tr("Основа всех антидотов"),
                "elastin", tr("Нужен для эластичного бинта и антибиотического клея"));
    }

    private static void add(MedCategory c, String... pairs) {
        List<String[]> l = new ArrayList<>();
        for (int i = 0; i < pairs.length; i += 2) { l.add(new String[]{pairs[i], pairs[i + 1]}); REASON.put(pairs[i], pairs[i + 1]); }
        BY_CATEGORY.put(c, l);
    }

    static String reasonFor(String itemId) { return REASON.get(itemId); }

    /** Идентификаторы лучших препаратов раздела по порядку. */
    public static List<String> idsFor(MedCategory c) {
        List<String> r = new ArrayList<>();
        for (String[] p : BY_CATEGORY.getOrDefault(c, Collections.emptyList())) r.add(p[0]);
        return r;
    }

    private BestPicks() {}
}
