package baro.data.catalog;

import baro.data.CatalogBuilder;
import baro.model.ItemCategory;

/** Грузовые миссии (доставка) и рассчитанные планы переработки груза. Сгенерировано из файлов игры. */
final class MissionsData {
    private MissionsData() {}

    static void register(CatalogBuilder b) {
        b.item("nitroglycerin", "Нитроглицерин", ItemCategory.MISC, "Груз миссий доставки. Не разбирается и не продаётся.", null);
        b.item("volatilenitroglycerin", "Нестабильный нитроглицерин", ItemCategory.MISC, "Груз миссий доставки. Не разбирается и не продаётся.", null);
        b.mission("arms", "Доставка оружия (Arms Delivery)", "Защищённый металлический ящик", 1, 4, 480)
            .content("smg", 1)
            .content("smgmagazine", 9)
            .content("fraggrenade", 2)
            .done();
        b.mission("bio", "Биологическая угроза (Biohazard)", "Химический ящик", 1, 12, 200)
            .content("huskeggsbasic", 8)
            .done();
        b.mission("chem1", "Химическая поставка (Chemical shipment)", "Химический ящик", 1, 20, 112)
            .content("antidama2", 5)
            .content("sulphuricacid", 5)
            .content("morbusine", 5)
            .content("flashpowder", 3)
            .content("chloralhydrate", 2)
            .done();
        b.mission("chem2", "Химическая поставка, вариант 2", "Химический ящик (+протекающие)", 2, 20, 128)
            .content("antidama2", 5)
            .content("potassium", 5)
            .content("lithium", 5)
            .content("flashpowder", 3)
            .content("chloralhydrate", 2)
            .done();
        b.mission("medA", "Перевозка медикаментов, вариант A", "Медицинский ящик", 1, 1, 800)
            .content("morbusineantidote", 5)
            .done();
        b.mission("medB", "Перевозка медикаментов, вариант B", "Медицинский ящик", 1, 20, 96)
            .content("antidama1", 5)
            .content("antidama2", 5)
            .content("antibloodloss1", 5)
            .content("antibiotics", 5)
            .content("antibleeding1", 16)
            .content("antibleeding3", 6)
            .done();
        b.mission("expl", "Взрывоопасный груз (Explosive cargo)", "Ящик для взрывчатки", 1, 15, 160)
            .content("compoundn", 20)
            .content("detonator", 10)
            .done();
        b.mission("vexpl", "Нестабильный взрывоопасный груз", "Ящик для взрывчатки", 1, 10, 320)
            .content("volatilecompoundn", 20)
            .content("detonator", 10)
            .done();
        b.mission("nitro", "Обращаться осторожно (Handle with care)", "Ящик для взрывчатки", 1, 10, 280)
            .content("nitroglycerin", 20)
            .done();
        b.mission("vnitro", "Обращаться предельно осторожно", "Ящик с магнитной подвеской", 1, 4, 640)
            .content("volatilenitroglycerin", 10)
            .done();
        b.mission("mineA", "Материалы для шахтёрской станции, вариант A", "Металлический ящик", 1, 25, 160)
            .content("iron", 10)
            .content("lead", 10)
            .content("copper", 10)
            .content("titanium", 10)
            .done();
        b.mission("mineB", "Материалы для шахтёрской станции, вариант B", "Металлический ящик", 1, 25, 80)
            .content("magnesium", 5)
            .content("copper", 5)
            .content("steel", 5)
            .content("lead", 5)
            .content("aluminium", 5)
            .content("iron", 10)
            .content("titanium", 5)
            .done();
        b.mission("honk", "Хвала Хонкоматери (Praise the honkmother)", "Металлический ящик", 1, 10, 400)
            .content("clownmask", 5)
            .content("clowncostume", 5)
            .content("bikehorn", 10)
            .done();
    }
}
