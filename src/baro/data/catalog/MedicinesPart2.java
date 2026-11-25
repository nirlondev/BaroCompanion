package baro.data.catalog;

import baro.data.CatalogBuilder;
import baro.model.*;

/** Сгенерировано из файлов игры: эффекты медикаментов (итог за всё время действия). Не редактировать вручную. */
final class MedicinesPart2 {
    private MedicinesPart2() {}

    static void register(CatalogBuilder b) {
        b.medicine("energydrink", MedCategory.BUFF, null)
            .success("haste", "Гиперактивность", EffectKind.BUFF, 100.0, null, null, null, 100.0, false)
            .success("nausea", "Тошнота", EffectKind.BAD, 50.0, null, null, 0.25, null, false);
        b.medicine("ethanol", MedCategory.HEAL, null)
            .success("infection", "Зараженная рана", EffectKind.GOOD, -8.0, 10.0, null, null, null, true)
            .success("burn", "Ожог", EffectKind.BAD, 1.5, 10.0, null, null, null, true);
        b.medicine("europabrew", MedCategory.ANTIDOTE, null)
            .success("TYPE:poison", "Все отравления", EffectKind.GOOD, -30.0, 30.0, null, null, null, false)
            .success("acidreaction", "Эффекты \"Европовки\"", EffectKind.BAD, 30.0, null, null, null, null, false);
        b.medicine("hallucinogenicbufotoxin", MedCategory.TOXIN, null)
            .success("paralysis", "Паралич", EffectKind.GOOD, -40.0, 20.0, null, null, null, false)
            .success("psychosis", "Психоз", EffectKind.BAD, 20.0, 20.0, null, null, null, false);
        b.medicine("hyperzine", MedCategory.BUFF, 50)
            .success("haste", "Гиперактивность", EffectKind.BUFF, 400.0, null, null, null, 400.0, false)
            .success("strengthen", "Решительность", EffectKind.BUFF, 400.0, null, null, null, 400.0, false)
            .success("chemwithdrawal", "Химическая ломка", EffectKind.GOOD, -90.0, 60.0, null, null, null, false)
            .success("stun", "Оглушение", EffectKind.GOOD, -54.0, 60.0, null, null, null, false)
            .success("oxygenlow", "Нехватка кислорода", EffectKind.GOOD, -15.0, 60.0, null, null, null, false)
            .success("TYPE:damage", "Травмы (весь урон)", EffectKind.GOOD, -12.0, 60.0, null, null, null, false)
            .success("chemaddiction", "Химическая зависимость", EffectKind.BAD, 12.0, 60.0, null, null, null, false)
            .failure("haste", "Гиперактивность", EffectKind.BUFF, 400.0, null, null, null, 400.0, false)
            .failure("strengthen", "Решительность", EffectKind.BUFF, 400.0, null, null, null, 400.0, false)
            .failure("chemwithdrawal", "Химическая ломка", EffectKind.GOOD, -90.0, 60.0, null, null, null, false)
            .failure("stun", "Оглушение", EffectKind.GOOD, -15.0, 60.0, null, null, null, false)
            .failure("TYPE:damage", "Травмы (весь урон)", EffectKind.GOOD, -6.0, 60.0, null, null, null, false)
            .failure("oxygenlow", "Нехватка кислорода", EffectKind.GOOD, -6.0, 60.0, null, null, null, false)
            .failure("chemaddiction", "Химическая зависимость", EffectKind.BAD, 24.0, 60.0, null, null, null, false)
            .failure("burn", "Ожог", EffectKind.BAD, 7.5, 60.0, null, null, null, false);
        b.medicine("liquidoxygenite", MedCategory.HEAL, 50)
            .success("oxygenlow", "Нехватка кислорода", EffectKind.GOOD, -275.0, 60.0, 1.0, null, null, false)
            .success("organdamage", "Повреждение органа", EffectKind.BAD, 7.5, 5.0, 2.0, null, null, false)
            .failure("oxygenlow", "Нехватка кислорода", EffectKind.GOOD, -50.0, 10.0, 1.0, null, null, false)
            .failure("organdamage", "Повреждение органа", EffectKind.BAD, 15.0, 10.0, 2.0, null, null, false);
        b.medicine("meth", MedCategory.BUFF, 35)
            .success("haste", "Гиперактивность", EffectKind.BUFF, 420.0, null, null, null, 420.0, false)
            .success("oxygenlow", "Нехватка кислорода", EffectKind.GOOD, -90.0, 30.0, null, null, null, false)
            .success("chemwithdrawal", "Химическая ломка", EffectKind.GOOD, -90.0, 30.0, null, null, null, false)
            .success("stun", "Оглушение", EffectKind.GOOD, -22.5, 30.0, null, null, null, false)
            .success("organdamage", "Повреждение органа", EffectKind.BAD, 15.0, 30.0, null, null, null, false)
            .success("chemaddiction", "Химическая зависимость", EffectKind.BAD, 7.5, 30.0, null, null, null, false)
            .failure("haste", "Гиперактивность", EffectKind.BUFF, 300.0, null, null, null, 300.0, false)
            .failure("chemwithdrawal", "Химическая ломка", EffectKind.GOOD, -90.0, 30.0, null, null, null, false)
            .failure("oxygenlow", "Нехватка кислорода", EffectKind.GOOD, -45.0, 30.0, null, null, null, false)
            .failure("stun", "Оглушение", EffectKind.GOOD, -22.5, 30.0, null, null, null, false)
            .failure("organdamage", "Повреждение органа", EffectKind.BAD, 30.0, 30.0, null, null, null, false)
            .failure("chemaddiction", "Химическая зависимость", EffectKind.BAD, 15.0, 30.0, null, null, null, false);
        b.medicine("mindsense", MedCategory.BUFF, 60)
            .success("mindsense", "Сенсор разума", EffectKind.BUFF, 420.0, null, null, null, null, false)
            .failure("mindsense", "Сенсор разума", EffectKind.BUFF, 240.0, null, null, null, null, false);
        b.medicine("mindwipe", MedCategory.BUFF, null)
            .success("refund", "Сбрасывает все таланты — очки можно распределить заново", EffectKind.BUFF, null, null, null, null, null, false);
        b.medicine("morbusine", MedCategory.TOXIN, 50)
            .success("morbusinepoisoning", "Отравление морбузином", EffectKind.BAD, 20.0, null, null, null, null, false, "не людям (монстрам)")
            .success("morbusinepoisoning", "Отравление морбузином", EffectKind.BAD, 11.0, null, null, null, null, false, "людям")
            .failure("morbusinepoisoning", "Отравление морбузином", EffectKind.BAD, 10.0, null, null, null, null, false);
        b.medicine("morbusineantidote", MedCategory.ANTIDOTE, 40)
            .success("morbusinepoisoning", "Отравление морбузином", EffectKind.GOOD, -100.0, 10.0, null, null, null, false)
            .failure("morbusinepoisoning", "Отравление морбузином", EffectKind.GOOD, -50.0, 5.0, null, null, null, false);
        b.medicine("opium", MedCategory.HEAL, 40)
            .success("opiatewithdrawal", "Наркотическая ломка", EffectKind.GOOD, -35.0, 25.0, null, null, null, false)
            .success("TYPE:damage", "Травмы (весь урон)", EffectKind.GOOD, -20.0, 25.0, null, null, null, false)
            .success("TYPE:burn", "Ожоги (все)", EffectKind.GOOD, -5.0, 25.0, null, null, null, false)
            .success("opiateaddiction", "Наркотическая зависимость", EffectKind.BAD, 25.0, 25.0, null, null, null, false)
            .success("opiateoverdose", "Передозировка опиатами", EffectKind.BAD, 25.0, 25.0, null, null, null, false)
            .success("oxygenlow", "Нехватка кислорода", EffectKind.BAD, 17.5, 25.0, null, null, null, false)
            .failure("opiatewithdrawal", "Наркотическая ломка", EffectKind.GOOD, -35.0, 25.0, null, null, null, false)
            .failure("TYPE:damage", "Травмы (весь урон)", EffectKind.GOOD, -16.25, 25.0, null, null, null, false)
            .failure("TYPE:burn", "Ожоги (все)", EffectKind.GOOD, -3.75, 25.0, null, null, null, false)
            .failure("opiateaddiction", "Наркотическая зависимость", EffectKind.BAD, 43.75, 25.0, null, null, null, false)
            .failure("opiateoverdose", "Передозировка опиатами", EffectKind.BAD, 37.5, 25.0, null, null, null, false)
            .failure("oxygenlow", "Нехватка кислорода", EffectKind.BAD, 30.0, 25.0, null, null, null, false);
        b.medicine("paralyzant", MedCategory.TOXIN, 60)
            .success("paralysis", "Паралич", EffectKind.BAD, 20.0, null, null, null, null, false, "не людям (монстрам)")
            .success("paralysis", "Паралич", EffectKind.BAD, 11.0, null, null, null, null, false, "людям")
            .failure("paralysis", "Паралич", EffectKind.BAD, 10.0, null, null, null, null, false);
        b.medicine("pomegrenadeextract", MedCategory.HEAL, 25)
            .success("TYPE:damage", "Травмы (весь урон)", EffectKind.GOOD, -8.0, 25.0, null, null, null, false)
            .success("TYPE:burn", "Ожоги (все)", EffectKind.GOOD, -8.0, 25.0, null, null, null, false)
            .success("oxygenlow", "Нехватка кислорода", EffectKind.BAD, 20.0, 1.0, null, null, null, false)
            .failure("TYPE:damage", "Травмы (весь урон)", EffectKind.GOOD, -4.0, 25.0, null, null, null, false)
            .failure("TYPE:burn", "Ожоги (все)", EffectKind.GOOD, -4.0, 25.0, null, null, null, false)
            .failure("oxygenlow", "Нехватка кислорода", EffectKind.BAD, 20.0, 1.0, null, null, null, false);
        b.medicine("pressurestabilizer", MedCategory.BUFF, 35)
            .success("pressurestabilized", "Давление стабилизировано", EffectKind.BUFF, 900.0, 10.0, null, null, 900.0, false)
            .failure("pressurestabilized", "Давление стабилизировано", EffectKind.BUFF, 450.0, 10.0, null, null, 450.0, false);
        b.medicine("proteinbar", MedCategory.HEAL, null)
            .success("strengthen", "Решительность", EffectKind.BUFF, 15.0, 7.5, null, null, 15.0, false)
            .success("TYPE:damage", "Травмы (весь урон)", EffectKind.GOOD, -7.5, 7.5, null, null, null, false)
            .success("TYPE:burn", "Ожоги (все)", EffectKind.GOOD, -7.5, 7.5, null, null, null, false);
        b.medicine("radiotoxin", MedCategory.TOXIN, 30)
            .success("radiationsickness", "Лучевая болезнь", EffectKind.BAD, 120.0, 10.0, null, null, null, false)
            .failure("radiationsickness", "Лучевая болезнь", EffectKind.BAD, 60.0, 10.0, null, null, null, false);
        b.medicine("raptorbaneextract", MedCategory.TOXIN, 35)
            .success("organdamage", "Повреждение органа", EffectKind.BAD, 200.0, 20.0, null, null, null, false, "только грязевым рапторам", "ручному раптору")
            .success("nausea", "Тошнота", EffectKind.BAD, 100.0, 20.0, null, null, null, false)
            .failure("organdamage", "Повреждение органа", EffectKind.BAD, 100.0, 10.0, null, null, null, false, "только грязевым рапторам", "ручному раптору");
        b.medicine("rum", MedCategory.HEAL, null)
            .success("infection", "Зараженная рана", EffectKind.GOOD, -15.0, 10.0, null, null, null, true)
            .success("burn", "Ожог", EffectKind.BAD, 1.5, 10.0, null, null, null, true);
        b.medicine("stabilozine", MedCategory.OTHER, 25)
            .success("stabilozineeffect", "Стабилоцин", EffectKind.NEUTRAL, 120.0, null, null, null, null, false)
            .failure("stabilozineeffect", "Стабилоцин", EffectKind.NEUTRAL, 60.0, null, null, null, null, false);
        b.medicine("steroids", MedCategory.BUFF, 35)
            .success("strengthen", "Решительность", EffectKind.BUFF, 420.0, null, null, null, 420.0, false)
            .success("chemwithdrawal", "Химическая ломка", EffectKind.GOOD, -90.0, 60.0, null, null, null, false)
            .success("oxygenlow", "Нехватка кислорода", EffectKind.GOOD, -12.0, 60.0, null, null, null, false)
            .success("chemaddiction", "Химическая зависимость", EffectKind.BAD, 6.0, 60.0, null, null, null, false)
            .failure("strengthen", "Решительность", EffectKind.BUFF, 300.0, null, null, null, 300.0, false)
            .failure("chemwithdrawal", "Химическая ломка", EffectKind.GOOD, -90.0, 60.0, null, null, null, false)
            .failure("oxygenlow", "Нехватка кислорода", EffectKind.GOOD, -12.0, 60.0, null, null, null, false)
            .failure("chemaddiction", "Химическая зависимость", EffectKind.BAD, 12.0, 60.0, null, null, null, false);
        b.medicine("sufforin", MedCategory.TOXIN, 40)
            .success("sufforinpoisoning", "Отравление страданитом", EffectKind.BAD, 20.0, null, null, null, null, false, "не людям (монстрам)")
            .success("sufforinpoisoning", "Отравление страданитом", EffectKind.BAD, 11.0, null, null, null, null, false, "людям")
            .failure("sufforinpoisoning", "Отравление страданитом", EffectKind.BAD, 10.0, null, null, null, null, false);
        b.medicine("sufforinantidote", MedCategory.ANTIDOTE, 40)
            .success("sufforinpoisoning", "Отравление страданитом", EffectKind.GOOD, -200.0, 10.0, null, null, null, false)
            .failure("sufforinpoisoning", "Отравление страданитом", EffectKind.GOOD, -100.0, 5.0, null, null, null, false);
        b.medicine("sulphuricacid", MedCategory.TOXIN, null)
            .success("acidburn", "Ожог кислотой", EffectKind.BAD, 15.0, 5.0, null, null, null, false);
        b.medicine("sulphuricacidsyringe", MedCategory.TOXIN, null)
            .success("slow", "Замедление", EffectKind.BAD, 400.0, 20.0, null, null, null, false, "цели массой от 3000")
            .success("slow", "Замедление", EffectKind.BAD, 200.0, 10.0, null, null, null, false, "цели массой до 3000")
            .success("acidburn", "Ожог кислотой", EffectKind.BAD, 155.0, 20.0, null, null, null, false, "цели массой от 3000")
            .success("acidburn", "Ожог кислотой", EffectKind.BAD, 95.0, 10.0, null, null, null, false, "цели массой до 3000");
        b.medicine("tonicliquid", MedCategory.BUFF, null)
            .success("durationincrease", "Медленный метаболизм", EffectKind.BUFF, 300.0, null, null, null, null, false)
            .success("TYPE:damage", "Травмы (весь урон)", EffectKind.GOOD, -12.0, 120.0, null, null, null, false);
    }
}
