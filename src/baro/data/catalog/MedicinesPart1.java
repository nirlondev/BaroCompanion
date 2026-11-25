package baro.data.catalog;

import baro.data.CatalogBuilder;
import baro.model.*;

/** Сгенерировано из файлов игры: эффекты медикаментов (итог за всё время действия). Не редактировать вручную. */
final class MedicinesPart1 {
    private MedicinesPart1() {}

    static void register(CatalogBuilder b) {
        b.medicine("adrenaline", MedCategory.BUFF, null)
            .success("adrenalinerush", "Прилив адреналина", EffectKind.BUFF, 8.0, null, null, null, null, false)
            .success("nausea", "Тошнота", EffectKind.BAD, 40.0, 10.0, 8.0, null, null, false)
            .success("organdamage", "Повреждение органа", EffectKind.BAD, 20.0, 10.0, 8.0, null, null, false);
        b.medicine("antibiotics", MedCategory.ANTIDOTE, 25)
            .success("huskinfectionresistance", "Устойчивость к трупным паразитам", EffectKind.BUFF, 600.0, null, null, null, null, false)
            .success("infection", "Зараженная рана", EffectKind.GOOD, -75.0, 60.0, null, null, null, false)
            .success("huskinfection", "Заражение паразитами", EffectKind.GOOD, -30.0, 60.0, null, null, null, false, "если заражение меньше 75%")
            .success("drunkweakness", "Восприимчивость к алкоголю", EffectKind.BAD, 50.0, 5.0, null, null, null, false)
            .success("organdamage", "Повреждение органа", EffectKind.BAD, 33.0, 60.0, null, null, null, true)
            .failure("huskinfectionresistance", "Устойчивость к трупным паразитам", EffectKind.BUFF, 300.0, null, null, null, null, false)
            .failure("infection", "Зараженная рана", EffectKind.GOOD, -45.0, 60.0, null, null, null, false)
            .failure("huskinfection", "Заражение паразитами", EffectKind.GOOD, -15.0, 60.0, null, null, null, false, "если заражение меньше 75%")
            .failure("drunkweakness", "Восприимчивость к алкоголю", EffectKind.BAD, 100.0, 5.0, null, null, null, false)
            .failure("organdamage", "Повреждение органа", EffectKind.BAD, 60.0, 60.0, null, null, null, true);
        b.medicine("antibleeding1", MedCategory.HEAL, 40)
            .success("TYPE:bleeding", "Кровотечение (все)", EffectKind.GOOD, -30.0, 20.0, null, null, null, true)
            .success("TYPE:burn", "Ожоги (все)", EffectKind.GOOD, -15.0, 20.0, null, null, null, true)
            .failure("TYPE:bleeding", "Кровотечение (все)", EffectKind.GOOD, -22.5, 20.0, null, null, null, true)
            .failure("TYPE:burn", "Ожоги (все)", EffectKind.GOOD, -12.0, 20.0, null, null, null, true);
        b.medicine("antibleeding2", MedCategory.HEAL, 25)
            .success("TYPE:burn", "Ожоги (все)", EffectKind.GOOD, -50.0, 10.0, null, null, null, true)
            .success("TYPE:bleeding", "Кровотечение (все)", EffectKind.GOOD, -40.0, 10.0, null, null, null, true)
            .failure("TYPE:burn", "Ожоги (все)", EffectKind.GOOD, -37.5, 10.0, null, null, null, true)
            .failure("TYPE:bleeding", "Кровотечение (все)", EffectKind.GOOD, -30.0, 10.0, null, null, null, true);
        b.medicine("antibleeding3", MedCategory.HEAL, 55)
            .success("TYPE:bleeding", "Кровотечение (все)", EffectKind.GOOD, -60.0, 2.0, null, null, null, true)
            .success("TYPE:infection", "Зараженная рана (все)", EffectKind.GOOD, -22.5, 30.0, null, null, null, true)
            .success("TYPE:burn", "Ожоги (все)", EffectKind.GOOD, -15.0, 2.0, null, null, null, true)
            .success("organdamage", "Повреждение органа", EffectKind.BAD, 3.0, 5.0, null, null, null, true)
            .failure("TYPE:bleeding", "Кровотечение (все)", EffectKind.GOOD, -40.0, 2.0, null, null, null, true)
            .failure("TYPE:infection", "Зараженная рана (все)", EffectKind.GOOD, -15.0, 30.0, null, null, null, true)
            .failure("TYPE:burn", "Ожоги (все)", EffectKind.GOOD, -10.0, 2.0, null, null, null, true)
            .failure("organdamage", "Повреждение органа", EffectKind.BAD, 9.0, 5.0, null, null, null, true);
        b.medicine("antibloodloss1", MedCategory.HEAL, 40)
            .success("bloodloss", "Кровопотеря", EffectKind.GOOD, -25.0, 10.0, null, null, null, false)
            .success("infection", "Зараженная рана", EffectKind.GOOD, -5.0, 10.0, null, null, null, true)
            .failure("bloodloss", "Кровопотеря", EffectKind.GOOD, -12.5, 10.0, null, null, null, false)
            .failure("infection", "Зараженная рана", EffectKind.GOOD, -3.0, 10.0, null, null, null, true);
        b.medicine("antibloodloss2", MedCategory.HEAL, 40)
            .success("bloodloss", "Кровопотеря", EffectKind.GOOD, -120.0, 15.0, null, null, null, false)
            .failure("bloodloss", "Кровопотеря", EffectKind.GOOD, -60.0, 15.0, null, null, null, false);
        b.medicine("antidama1", MedCategory.HEAL, 50)
            .success("opiatewithdrawal", "Наркотическая ломка", EffectKind.GOOD, -75.0, 20.0, null, null, null, false)
            .success("TYPE:damage", "Травмы (весь урон)", EffectKind.GOOD, -40.0, 20.0, null, null, null, false)
            .success("TYPE:burn", "Ожоги (все)", EffectKind.GOOD, -2.0, 20.0, null, null, null, false)
            .success("oxygenlow", "Нехватка кислорода", EffectKind.BAD, 20.0, 20.0, null, null, null, false)
            .success("opiateaddiction", "Наркотическая зависимость", EffectKind.BAD, 20.0, 20.0, null, null, null, false)
            .success("opiateoverdose", "Передозировка опиатами", EffectKind.BAD, 20.0, 20.0, null, null, null, false)
            .failure("opiatewithdrawal", "Наркотическая ломка", EffectKind.GOOD, -75.0, 20.0, null, null, null, false)
            .failure("TYPE:damage", "Травмы (весь урон)", EffectKind.GOOD, -25.0, 20.0, null, null, null, false)
            .failure("TYPE:burn", "Ожоги (все)", EffectKind.GOOD, -1.0, 20.0, null, null, null, false)
            .failure("opiateaddiction", "Наркотическая зависимость", EffectKind.BAD, 40.0, 20.0, null, null, null, false)
            .failure("oxygenlow", "Нехватка кислорода", EffectKind.BAD, 30.0, 20.0, null, null, null, false)
            .failure("opiateoverdose", "Передозировка опиатами", EffectKind.BAD, 30.0, 20.0, null, null, null, false);
        b.medicine("antidama2", MedCategory.HEAL, 72)
            .success("opiatewithdrawal", "Наркотическая ломка", EffectKind.GOOD, -100.0, 25.0, null, null, null, false)
            .success("TYPE:damage", "Травмы (весь урон)", EffectKind.GOOD, -75.0, 5.0, null, null, null, false)
            .success("TYPE:burn", "Ожоги (все)", EffectKind.GOOD, -5.0, 5.0, null, null, null, false)
            .success("oxygenlow", "Нехватка кислорода", EffectKind.BAD, 25.0, 5.0, null, null, null, false)
            .success("opiateoverdose", "Передозировка опиатами", EffectKind.BAD, 25.0, 5.0, null, null, null, false)
            .success("opiateaddiction", "Наркотическая зависимость", EffectKind.BAD, 25.0, 25.0, null, null, null, false)
            .failure("opiatewithdrawal", "Наркотическая ломка", EffectKind.GOOD, -100.0, 25.0, null, null, null, false)
            .failure("TYPE:damage", "Травмы (весь урон)", EffectKind.GOOD, -50.0, 5.0, null, null, null, false)
            .failure("TYPE:burn", "Ожоги (все)", EffectKind.GOOD, -3.75, 5.0, null, null, null, false)
            .failure("opiateaddiction", "Наркотическая зависимость", EffectKind.BAD, 75.0, 25.0, null, null, null, false)
            .failure("opiateoverdose", "Передозировка опиатами", EffectKind.BAD, 40.0, 5.0, null, null, null, false)
            .failure("oxygenlow", "Нехватка кислорода", EffectKind.BAD, 37.5, 5.0, null, null, null, false);
        b.medicine("antinarc", MedCategory.ANTIDOTE, 40)
            .success("opiatewithdrawal", "Наркотическая ломка", EffectKind.GOOD, -60.0, 30.0, null, null, null, false)
            .success("opiateaddiction", "Наркотическая зависимость", EffectKind.GOOD, -60.0, 30.0, null, null, null, false)
            .success("opiateoverdose", "Передозировка опиатами", EffectKind.GOOD, -60.0, 30.0, null, null, null, false)
            .failure("opiatewithdrawal", "Наркотическая ломка", EffectKind.GOOD, -30.0, 30.0, null, null, null, false)
            .failure("opiateaddiction", "Наркотическая зависимость", EffectKind.GOOD, -30.0, 30.0, null, null, null, false)
            .failure("opiateoverdose", "Передозировка опиатами", EffectKind.GOOD, -30.0, 30.0, null, null, null, false)
            .failure("oxygenlow", "Нехватка кислорода", EffectKind.BAD, 30.0, 30.0, null, null, null, false);
        b.medicine("antiparalysis", MedCategory.ANTIDOTE, 64)
            .success("paralysisresistance", "Устойчивость к параличу", EffectKind.BUFF, 800.0, 1.0, null, null, null, false)
            .success("psychosis", "Психоз", EffectKind.BAD, 5.0, 1.0, null, null, null, false)
            .failure("paralysisresistance", "Устойчивость к параличу", EffectKind.BUFF, 390.0, 60.0, null, null, null, false)
            .failure("psychosis", "Психоз", EffectKind.BAD, 45.0, 60.0, null, null, null, false);
        b.medicine("antipsychosis", MedCategory.ANTIDOTE, 37)
            .success("psychosis", "Психоз", EffectKind.GOOD, -100.0, 1.0, null, null, null, false)
            .success("hallucinating", "Галлюцинации", EffectKind.GOOD, -100.0, 1.0, null, null, null, false)
            .failure("psychosis", "Психоз", EffectKind.GOOD, -25.0, 1.0, null, null, null, false)
            .failure("hallucinating", "Галлюцинации", EffectKind.GOOD, -25.0, 1.0, null, null, null, false);
        b.medicine("antirad", MedCategory.ANTIDOTE, 25)
            .success("radiationsickness", "Лучевая болезнь", EffectKind.GOOD, -100.0, 1.0, null, null, null, false)
            .failure("radiationsickness", "Лучевая болезнь", EffectKind.GOOD, -25.0, 1.0, null, null, null, false);
        b.medicine("beerbottle1", MedCategory.OTHER, null)
            .success("psychosis", "Психоз", EffectKind.GOOD, -12.0, 12.0, null, null, null, false)
            .success("hallucinating", "Галлюцинации", EffectKind.GOOD, -12.0, 12.0, null, null, null, false)
            .success("TYPE:damage", "Травмы (весь урон)", EffectKind.GOOD, -6.0, 12.0, null, null, null, false)
            .success("drunk", "Опьянение", EffectKind.BAD, 24.0, 12.0, null, null, null, false);
        b.medicine("beerbottle2", MedCategory.OTHER, null);
        b.medicine("calyxanide", MedCategory.ANTIDOTE, 38)
            .success("huskinfection", "Заражение паразитами", EffectKind.GOOD, -100.0, 1.0, null, null, null, false, "если заражение меньше 100%")
            .success("organdamage", "Повреждение органа", EffectKind.BAD, 600.0, 5.0, null, null, null, false, "только хаскам")
            .success("organdamage", "Повреждение органа", EffectKind.BAD, 30.0, 10.0, null, null, null, false, "если заражение 100%")
            .failure("huskinfection", "Заражение паразитами", EffectKind.GOOD, -60.0, 1.0, null, null, null, false, "если заражение меньше 100%")
            .failure("organdamage", "Повреждение органа", EffectKind.BAD, 300.0, 3.0, null, null, null, false, "только хаскам");
        b.medicine("chloralhydrate", MedCategory.TOXIN, 30)
            .success("incrementalstun", "Нарастающее оглушение", EffectKind.BAD, 125.0, 5.0, null, null, null, false)
            .success("organdamage", "Повреждение органа", EffectKind.BAD, 2.5, 5.0, null, null, null, false)
            .failure("incrementalstun", "Нарастающее оглушение", EffectKind.BAD, 62.5, 2.5, null, null, null, false)
            .failure("organdamage", "Повреждение органа", EffectKind.BAD, 1.25, 2.5, null, null, null, false);
        b.medicine("combatstimulantsyringe", MedCategory.BUFF, 35)
            .success("combatstimulant", "Боевой стимулятор", EffectKind.BUFF, 75.0, 5.0, null, null, 75.0, false)
            .success("chemaddiction", "Химическая зависимость", EffectKind.BAD, 10.0, 5.0, null, null, null, false)
            .failure("combatstimulant", "Боевой стимулятор", EffectKind.BUFF, 75.0, 5.0, null, null, 75.0, false)
            .failure("chemaddiction", "Химическая зависимость", EffectKind.BAD, 20.0, 5.0, null, null, null, false);
        b.medicine("cyanide", MedCategory.TOXIN, 60)
            .success("cyanidepoisoning", "Отравление цианидом", EffectKind.BAD, 20.0, null, null, null, null, false, "не людям (монстрам)")
            .success("cyanidepoisoning", "Отравление цианидом", EffectKind.BAD, 11.0, null, null, null, null, false, "людям")
            .failure("cyanidepoisoning", "Отравление цианидом", EffectKind.BAD, 10.0, null, null, null, null, false);
        b.medicine("cyanideantidote", MedCategory.ANTIDOTE, 40)
            .success("cyanidepoisoning", "Отравление цианидом", EffectKind.GOOD, -100.0, 10.0, null, null, null, false)
            .failure("cyanidepoisoning", "Отравление цианидом", EffectKind.GOOD, -50.0, 5.0, null, null, null, false);
        b.medicine("deliriumine", MedCategory.TOXIN, 35)
            .success("deliriuminepoisoning", "Отравление бредогеном", EffectKind.BAD, 20.0, null, null, null, null, false)
            .failure("deliriuminepoisoning", "Отравление бредогеном", EffectKind.BAD, 10.0, null, null, null, null, false);
        b.medicine("deliriumineantidote", MedCategory.ANTIDOTE, 40)
            .success("deliriuminepoisoning", "Отравление бредогеном", EffectKind.GOOD, -200.0, 10.0, null, null, null, false)
            .failure("deliriuminepoisoning", "Отравление бредогеном", EffectKind.GOOD, -100.0, 5.0, null, null, null, false);
        b.medicine("deusizine", MedCategory.HEAL, 72)
            .success("strengthen", "Решительность", EffectKind.BUFF, 200.0, null, null, null, 200.0, false)
            .success("oxygenlow", "Нехватка кислорода", EffectKind.GOOD, -125.0, 22.0, 1.0, null, null, false)
            .success("TYPE:damage", "Травмы (весь урон)", EffectKind.GOOD, -60.0, 20.0, 1.0, null, null, false)
            .success("bloodloss", "Кровопотеря", EffectKind.GOOD, -50.0, 20.0, 1.0, null, null, false)
            .success("infection", "Зараженная рана", EffectKind.GOOD, -25.0, 20.0, 1.0, null, null, false)
            .success("bleeding", "Кровотечение", EffectKind.GOOD, -20.0, 20.0, 1.0, null, null, false)
            .success("internaldamage", "Внутренняя травма", EffectKind.GOOD, -20.0, 20.0, 1.0, null, null, false)
            .success("stun", "Оглушение", EffectKind.GOOD, -6.0, 20.0, 1.0, null, null, false)
            .success("burn", "Ожог", EffectKind.BAD, 4.0, 20.0, 1.0, null, null, false)
            .failure("strengthen", "Решительность", EffectKind.BUFF, 200.0, null, null, null, 200.0, false)
            .failure("TYPE:damage", "Травмы (весь урон)", EffectKind.GOOD, -40.0, 20.0, 1.0, null, null, false)
            .failure("oxygenlow", "Нехватка кислорода", EffectKind.GOOD, -35.0, 6.0, 1.0, null, null, false)
            .failure("bloodloss", "Кровопотеря", EffectKind.GOOD, -25.0, 20.0, 1.0, null, null, false)
            .failure("bleeding", "Кровотечение", EffectKind.GOOD, -10.0, 20.0, 1.0, null, null, false)
            .failure("internaldamage", "Внутренняя травма", EffectKind.GOOD, -10.0, 20.0, 1.0, null, null, false)
            .failure("infection", "Зараженная рана", EffectKind.GOOD, -10.0, 20.0, 1.0, null, null, false)
            .failure("stun", "Оглушение", EffectKind.GOOD, -3.0, 20.0, 1.0, null, null, false)
            .failure("burn", "Ожог", EffectKind.BAD, 20.0, 20.0, 1.0, null, null, false);
        b.medicine("elastin", MedCategory.OTHER, null);
        b.medicine("endocrinebooster", MedCategory.BUFF, 60)
            .success("talent", "Случайный талант чужой профессии (навсегда)", EffectKind.BUFF, null, null, null, null, null, false)
            .failure("talent", "Случайный талант чужой профессии (навсегда)", EffectKind.BUFF, null, null, null, null, null, false);
    }
}
