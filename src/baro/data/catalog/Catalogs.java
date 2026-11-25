package baro.data.catalog;

import baro.data.CatalogBuilder;

/** Точка входа в данные, встроенные в программу. */
public final class Catalogs {
    private Catalogs() {}

    public static void registerAll(CatalogBuilder b) {
        ItemsPart01.register(b);
        ItemsPart02.register(b);
        ItemsPart03.register(b);
        ItemsPart04.register(b);
        ItemsPart05.register(b);
        ItemsPart06.register(b);
        ItemsPart07.register(b);
        ItemsPart08.register(b);
        ItemsPart09.register(b);
        ItemsPart10.register(b);
        ItemsPart11.register(b);
        ItemsPart12.register(b);
        ItemsPart13.register(b);
        ItemsPart14.register(b);
        MedicinesPart1.register(b);
        MedicinesPart2.register(b);
        MissionsData.register(b);
        WeaponsData.register(b);
        RandomDeconData.register(b);
        TalentsData.register(b);
    }
}
