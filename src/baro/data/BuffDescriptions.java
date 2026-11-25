package baro.data;


import static baro.Tr.tr;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Что дают баффы на пике (по данным вики игры). */
public final class BuffDescriptions {
    private static final Map<String, String> TEXT = new HashMap<>();

    static {
        TEXT.put("strengthen", tr("на пике (600): до −50% входящего урона и +35% урона в ближнем бою"));
        TEXT.put("haste", tr("на пике (600): до +35% к скорости бега и атаки в ближнем бою"));
        TEXT.put("combatstimulant", tr("45–50% защиты от оглушения; лечит урон, ожоги, кровопотерю и ломку по 1–2 в секунду"));
        TEXT.put("pressurestabilized", tr("иммунитет к давлению на глубине и 100% защита от нехватки кислорода"));
        TEXT.put("adrenalinerush", tr("защита от оглушения, не теряешь сознание при здоровье ниже нуля"));
        TEXT.put("durationincrease", tr("замедляет спад других баффов — они держатся дольше"));
        TEXT.put("paralysisresistance", tr("защита от паралича"));
        TEXT.put("huskinfectionresistance", tr("защита от заражения хаском"));
    }

    public static Optional<String> of(String effectKey) { return Optional.ofNullable(TEXT.get(effectKey)); }

    private BuffDescriptions() {}
}
