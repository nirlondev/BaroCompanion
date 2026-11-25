package baro.data.catalog;

import baro.data.CatalogBuilder;

/** Вероятности для разбора «выпадает случайное»: число выпадений и веса. Сгенерировано из файлов игры. */
final class RandomDeconData {
    private RandomDeconData() {}

    static void register(CatalogBuilder b) {
        b.randomDecon("alienblueprint", 1).weight("alienphysicoriumammoblueprint", 1.0).weight("aliengravityammoblueprint", 1.0).weight("alienincendiumblueprint", 1.0);
        b.randomDecon("brokendivingsuit", 1).weight("titanium", 60.0).weight("rubber", 50.0).weight("aluminium", 40.0).weight("titaniumaluminiumalloy", 30.0);
        b.randomDecon("diversremains", 2).weight("rubber", 0.5).weight("iron", 0.2).weight("steel", 0.1).weight("aluminium", 0.5).weight("carbon", 0.5);
        b.randomDecon("geneticmaterial_unresearched", 1).weight("geneticmaterialcrawler", 6.0).weight("geneticmaterialmudraptor", 6.0).weight("geneticmaterialmoloch", 6.0).weight("geneticmaterialmantis", 6.0).weight("geneticmaterialthresher", 6.0).weight("geneticmaterialhammerhead", 6.0).weight("geneticmaterialhammerheadmatriarch", 6.0).weight("geneticmaterialspineling", 6.0).weight("geneticmaterialhusk", 6.0).weight("geneticmaterialmollusc", 6.0).weight("geneticmaterialskitter", 6.0).weight("geneticmaterialhunter", 6.0);
        b.randomDecon("scrap", 2).weight("iron", 0.8).weight("thorium", 0.2).weight("steel", 0.6).weight("aluminium", 0.6).weight("titaniumaluminiumalloy", 0.3).weight("copper", 0.8).weight("rubber", 0.5).weight("carbon", 0.6).weight("lead", 0.7);
    }
}
