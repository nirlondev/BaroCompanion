package baro.data.catalog;

import baro.data.CatalogBuilder;
import baro.model.DamageKind;

/** Характеристики оружия, боеприпасов и взрывчатки. Сгенерировано из файлов игры. */
final class WeaponsData {
    private WeaponsData() {}

    static void register(CatalogBuilder b) {
        b.weapon("40mmchemgrenade")
            .hit(true, DamageKind.EXPLOSION, 500.0, null).affliction("Ожог кислотой", 30.0, null).affliction("Ожог кислотой", 15.0, 0.2).affliction("Оглушение", 0.5, null);
        b.weapon("40mmgrenade")
            .hit(true, DamageKind.HIT, null, 40.0).affliction("Повреждение глубоких тканей", 50.0, null)
            .hit(true, DamageKind.EXPLOSION, 300.0, 20.0).affliction("Ожог", 100.0, null).affliction("Кровотечение", 400.0, 0.05).affliction("Оглушение", 5.0, null);
        b.weapon("40mmstungrenade")
            .hit(true, DamageKind.EXPLOSION, 250.0, null).affliction("Повреждение глубоких тканей", 5.0, null).affliction("Оглушение", 10.0, null).affliction("Ослепление", 30.0, null).affliction("Сенсорная перегрузка", 15.0, null);
        b.weapon("alienpistol").ranged(0.5, false, 0.0, 2.0)
            .hit(true, DamageKind.EXPLOSION, 150.0, null).affliction("Психоз", 2.5, null);
        b.weapon("alienpowercell").rounds(25)
            .hit(false, DamageKind.HIT, null, 5.0).affliction("Повреждение глубоких тканей", 10.5, null).affliction("Ожог", 10.5, null).affliction("Оглушение", 0.1, null);
        b.weapon("alienshell")
            .hit(true, DamageKind.HIT, null, 75.0).affliction("Повреждение глубоких тканей", 150.0, null).affliction("Кровотечение", 15.0, null).affliction("Оглушение", 5.0, null)
            .hit(true, DamageKind.EXPLOSION, 150.0, 600.0).affliction("Повреждение глубоких тканей", 700.0, null).affliction("Кровотечение", 200.0, null).affliction("Оглушение", 15.0, null);
        b.weapon("alienspear")
            .hit(true, DamageKind.HIT, null, 20.0).affliction("Рваная рана", 60.0, null).affliction("Кровотечение", 60.0, null).affliction("Оглушение", 0.5, null);
        b.weapon("arcemitter").ranged(5.0, false, 30.0, null).ammo("batterycell", "fulguriumbatterycell")
            .hit(true, DamageKind.HIT, null, null).affliction("Оглушение", 5.0, null).affliction("Ожог", 10.0, null).affliction("Оглушение", 80.0, null);
        b.weapon("assaultrifle").ranged(0.24, true, 4.0, 16.0).ammo("assaultriflemagazine");
        b.weapon("assaultriflemagazine").rounds(30)
            .hit(false, DamageKind.HIT, null, 6.0).affliction("Огнестрельная рана", 25.0, null).affliction("Кровотечение", 15.0, null).affliction("Оглушение", 0.25, null).affliction("Оглушение", 0.5, 0.2);
        b.weapon("autoshotgun").ranged(0.45, true, 5.0, 18.0).ammo("shotgunshell", "shotgunshellblunt", "shotgunslugexplosive");
        b.weapon("badcreepingorange")
            .hit(true, DamageKind.EXPLOSION, 50.0, null).affliction("Ожог", 50.0, null);
        b.weapon("badraptorbane")
            .hit(true, DamageKind.EXPLOSION, 150.0, null).affliction("Тошнота", 50.0, null);
        b.weapon("boardingaxe").melee(1.2, 155.0)
            .hit(true, DamageKind.HIT, null, 15.0).affliction("Рваная рана", 36.0, null).affliction("Кровотечение", 10.0, null).affliction("Оглушение", 0.5, null);
        b.weapon("boardingpod")
            .hit(true, DamageKind.HIT, null, 300.0).affliction("Рваная рана", 40.0, null).affliction("Оглушение", 1.0, null)
            .hit(true, DamageKind.EXPLOSION, 150.0, 600.0);
        b.weapon("c4block")
            .hit(true, DamageKind.EXPLOSION, 750.0, 300.0).affliction("Повреждение глубоких тканей", 250.0, null).affliction("Ожог", 15.0, 0.2).affliction("Кровотечение", 40.0, 0.05).affliction("Оглушение", 10.0, null);
        b.weapon("ceremonialsword").melee(0.75, 75.0)
            .hit(true, DamageKind.HIT, null, 1.0).affliction("Рваная рана", 19.0, null).affliction("Кровотечение", 14.0, null).affliction("Оглушение", 0.25, null);
        b.weapon("chemgrenade")
            .hit(true, DamageKind.EXPLOSION, 500.0, null).affliction("Ожог кислотой", 30.0, null).affliction("Ожог кислотой", 15.0, 0.2).affliction("Оглушение", 0.5, null);
        b.weapon("compoundn")
            .hit(true, DamageKind.EXPLOSION, 500.0, 150.0).affliction("Повреждение глубоких тканей", 125.0, null).affliction("Ожог", 15.0, 0.2).affliction("Кровотечение", 40.0, 0.05).affliction("Оглушение", 7.0, null);
        b.weapon("crawlereggprojectile")
            .hit(true, DamageKind.HIT, null, 10.0).affliction("Оглушение", 3.0, 1.0).affliction("Сильный ушиб", 20.0, 1.0)
            .hit(true, DamageKind.EXPLOSION, 100.0, 200.0).affliction("Оглушение", 0.5, 1.0).affliction("Ожог кислотой", 25.0, 1.0);
        b.weapon("crowbar").melee(1.0, 100.0)
            .hit(true, DamageKind.HIT, null, 8.0).affliction("Сильный ушиб", 10.0, null).affliction("Оглушение", 0.5, null);
        b.weapon("crowbardementonite").melee(null, null)
            .hit(true, DamageKind.HIT, null, null).affliction("Сильный ушиб", 18.0, null).affliction("Психоз", 5.0, null).affliction("Оглушение", 0.5, null);
        b.weapon("crowbarhardened").melee(null, null)
            .hit(true, DamageKind.HIT, null, null).affliction("Сильный ушиб", 18.0, null).affliction("Лучевая болезнь", 1.0, null).affliction("Оглушение", 0.6, null);
        b.weapon("deepdiverduck")
            .hit(true, DamageKind.EXPLOSION, 50.0, 10.0)
            .hit(true, DamageKind.HIT, null, 5.0).affliction("Сильный ушиб", 5.0, null).affliction("Оглушение", 0.5, null);
        b.weapon("depthchargeshell")
            .hit(true, DamageKind.EXPLOSION, 700.0, 175.0).affliction("Повреждение глубоких тканей", 150.0, null).affliction("Ожог", 15.0, 0.2).affliction("Кровотечение", 40.0, 0.05).affliction("Оглушение", 7.0, null);
        b.weapon("divingknife").melee(0.75, 50.0)
            .hit(true, DamageKind.HIT, null, 2.0).affliction("Рваная рана", 10.0, null).affliction("Кровотечение", 10.0, null).affliction("Оглушение", 0.2, null);
        b.weapon("divingknifedementonite").melee(null, null)
            .hit(true, DamageKind.HIT, null, null).affliction("Рваная рана", 10.0, null).affliction("Кровотечение", 10.0, null).affliction("Психоз", 5.0, null).affliction("Оглушение", 0.2, null);
        b.weapon("divingknifehardened").melee(null, null)
            .hit(true, DamageKind.HIT, null, null).affliction("Рваная рана", 15.0, null).affliction("Кровотечение", 5.0, null).affliction("Лучевая болезнь", 1.0, null).affliction("Оглушение", 0.2, null);
        b.weapon("divingknifeunique").melee(0.75, 50.0)
            .hit(true, DamageKind.HIT, null, 2.0).affliction("Рваная рана", 10.0, null).affliction("Кровотечение", 10.0, null).affliction("Оглушение", 0.2, null).affliction("Отравление морбузином", 4.0, null);
        b.weapon("empgrenade")
            .hit(true, DamageKind.EXPLOSION, 500.0, null).affliction("Повреждение глубоких тканей", 10.0, null).affliction("Оглушение", 5.0, null).affliction("ЭМИ (выводит из строя электронику)", 100.0, null);
        b.weapon("explosivespear")
            .hit(true, DamageKind.HIT, null, 20.0).affliction("Повреждение глубоких тканей", 25.0, null)
            .hit(true, DamageKind.EXPLOSION, 200.0, 10.0).affliction("Ожог", 50.0, null).affliction("Оглушение", 1.5, null);
        b.weapon("faradayartifact")
            .hit(true, DamageKind.EXPLOSION, 200.0, null).affliction("ЭМИ (выводит из строя электронику)", 25.0, null)
            .hit(true, DamageKind.HIT, null, null).affliction("Оглушение", 2.0, null).affliction("Оглушение", 60.0, null).affliction("Ожог", 5.0, null);
        b.weapon("flamer")
            .hit(true, DamageKind.EXPLOSION, 150.0, null).affliction("Ожог", 25.0, null).affliction("Оглушение", 5.0, null)
            .hit(true, DamageKind.EXPLOSION, 150.0, null).affliction("Ожог", 50.0, null).affliction("Оглушение", 10.0, null);
        b.weapon("flamerunique")
            .hit(true, DamageKind.EXPLOSION, 150.0, null).affliction("Ожог", 25.0, null).affliction("Оглушение", 5.0, null)
            .hit(true, DamageKind.EXPLOSION, 150.0, null).affliction("Ожог", 50.0, null).affliction("Оглушение", 10.0, null);
        b.weapon("flashpowder")
            .hit(true, DamageKind.EXPLOSION, 250.0, null).affliction("Ожог", 50.0, null).affliction("Оглушение", 5.0, null);
        b.weapon("fraggrenade")
            .hit(true, DamageKind.EXPLOSION, 750.0, 20.0).affliction("Повреждение глубоких тканей", 150.0, null).affliction("Кровотечение", 100.0, null).affliction("Кровотечение", 50.0, 0.1).affliction("Оглушение", 7.0, null);
        b.weapon("fuelrod").rounds(4)
            .hit(false, DamageKind.HIT, null, 35.0).affliction("Повреждение глубоких тканей", 50.0, null).affliction("Лучевая болезнь", 25.0, null).affliction("Оглушение", 0.5, null)
            .hit(false, DamageKind.EXPLOSION, 125.0, null).affliction("Ожог", 25.0, null).affliction("Лучевая болезнь", 80.0, null).affliction("Оглушение", 1.0, null);
        b.weapon("fulguriumfuelrod").rounds(12)
            .hit(false, DamageKind.HIT, null, 35.0).affliction("Повреждение глубоких тканей", 50.0, null).affliction("Лучевая болезнь", 25.0, null).affliction("Оглушение", 0.5, null)
            .hit(false, DamageKind.EXPLOSION, 125.0, null).affliction("Ожог", 25.0, null).affliction("Лучевая болезнь", 80.0, null).affliction("Оглушение", 1.0, null);
        b.weapon("fulguriumfuelrodvolatile").rounds(20)
            .hit(false, DamageKind.HIT, null, 35.0).affliction("Повреждение глубоких тканей", 50.0, null).affliction("Лучевая болезнь", 25.0, null).affliction("Оглушение", 0.5, null)
            .hit(false, DamageKind.EXPLOSION, 125.0, null).affliction("Ожог", 25.0, null).affliction("Лучевая болезнь", 80.0, null).affliction("Оглушение", 1.0, null);
        b.weapon("grenadelauncher").ranged(0.8, false, 1.0, 10.0).ammo("40mmchemgrenade", "40mmgrenade", "40mmstungrenade");
        b.weapon("handcannon").ranged(1.0, false, 0.0, 8.0).ammo("handcannonround");
        b.weapon("handcannonround")
            .hit(true, DamageKind.HIT, null, 20.0).affliction("Огнестрельная рана", 70.0, null).affliction("Кровотечение", 50.0, null).affliction("Оглушение", 1.5, null);
        b.weapon("harpooncoilrifle").ranged(1.5, true, 0.2, 5.0).ammo("alienspear", "explosivespear", "spear", "spearunique");
        b.weapon("harpoongun").ranged(1.75, false, 1.0, 10.0).ammo("alienspear", "explosivespear", "spear", "spearunique");
        b.weapon("heavywrench").melee(1.1, 100.0)
            .hit(true, DamageKind.HIT, null, 9.0).affliction("Сильный ушиб", 25.0, null).affliction("Оглушение", 1.0, null);
        b.weapon("hmg").ranged(0.1, true, 6.0, 50.0).ammo("hmgmagazine");
        b.weapon("hmgmagazine").rounds(100)
            .hit(false, DamageKind.HIT, null, 4.0).affliction("Огнестрельная рана", 12.0, null).affliction("Кровотечение", 10.0, null).affliction("Оглушение", 0.1, null).affliction("Оглушение", 0.5, 0.01);
        b.weapon("ic4block")
            .hit(true, DamageKind.EXPLOSION, 750.0, 50.0).affliction("Повреждение глубоких тканей", 50.0, null).affliction("Ожог", 250.0, null).affliction("Ожог", 30.0, 0.2).affliction("Оглушение", 7.0, null);
        b.weapon("incendium")
            .hit(true, DamageKind.EXPLOSION, 250.0, null).affliction("Ожог", 50.0, null).affliction("Ожог", 5.0, 0.2).affliction("Оглушение", 5.0, null);
        b.weapon("incendiumfueltank")
            .hit(true, DamageKind.EXPLOSION, 150.0, null).affliction("Ожог", 100.0, null).affliction("Оглушение", 10.0, null);
        b.weapon("incendiumgrenade")
            .hit(true, DamageKind.EXPLOSION, 300.0, null).affliction("Ожог", 75.0, null).affliction("Горение", 15.0, null).affliction("Ожог", 15.0, 0.2).affliction("Оглушение", 7.0, null);
        b.weapon("iron")
            .hit(true, DamageKind.HIT, null, 1.0).affliction("Рваная рана", 6.0, null).affliction("Кровотечение", 6.0, null).affliction("Оглушение", 0.15, null);
        b.weapon("lead")
            .hit(true, DamageKind.HIT, null, 2.0).affliction("Рваная рана", 7.0, null).affliction("Кровотечение", 6.0, null).affliction("Оглушение", 0.2, null);
        b.weapon("liquidoxygenite").melee(1.0, null)
            .hit(true, DamageKind.EXPLOSION, 200.0, 10.0).affliction("Ожог", 50.0, null).affliction("Оглушение", 1.5, null);
        b.weapon("lithium")
            .hit(true, DamageKind.EXPLOSION, 150.0, null).affliction("Ожог", 25.0, null).affliction("Оглушение", 5.0, null);
        b.weapon("machinepistol").ranged(0.175, true, 10.0, 18.0).ammo("smgmagazine");
        b.weapon("nucleardepthcharge")
            .hit(true, DamageKind.EXPLOSION, 1500.0, 600.0).affliction("Повреждение глубоких тканей", 1000.0, null).affliction("Ожог", 1000.0, null).affliction("Лучевая болезнь", 100.0, null).affliction("Кровотечение", 500.0, 0.05).affliction("Оглушение", 30.0, null)
            .hit(true, DamageKind.EXPLOSION, 2000.0, null).affliction("ЭМИ (выводит из строя электронику)", 50.0, null);
        b.weapon("nucleardepthdecoy")
            .hit(true, DamageKind.EXPLOSION, 1500.0, 600.0).affliction("Повреждение глубоких тканей", 1000.0, null).affliction("Ожог", 1000.0, null).affliction("Лучевая болезнь", 100.0, null).affliction("Кровотечение", 500.0, 0.05).affliction("Оглушение", 30.0, null)
            .hit(true, DamageKind.EXPLOSION, 2000.0, null).affliction("ЭМИ (выводит из строя электронику)", 50.0, null);
        b.weapon("nucleargun").ranged(0.5, true, 0.0, 10.0).ammo("fuelrod", "fulguriumfuelrod", "fulguriumfuelrodvolatile", "thoriumfuelrod");
        b.weapon("nuclearshell")
            .hit(true, DamageKind.HIT, null, 100.0).affliction("Повреждение глубоких тканей", 250.0, null).affliction("Кровотечение", 50.0, null).affliction("Оглушение", 15.0, null)
            .hit(true, DamageKind.EXPLOSION, 1500.0, 600.0).affliction("Повреждение глубоких тканей", 1000.0, null).affliction("Ожог", 1000.0, null).affliction("Лучевая болезнь", 100.0, null).affliction("Кровотечение", 40.0, 0.05).affliction("Оглушение", 30.0, null)
            .hit(true, DamageKind.EXPLOSION, 2000.0, null).affliction("ЭМИ (выводит из строя электронику)", 50.0, null);
        b.weapon("oxygeniteshard")
            .hit(true, DamageKind.EXPLOSION, 200.0, 20.0).affliction("Ожог", 80.0, null).affliction("Оглушение", 5.0, null);
        b.weapon("oxygenitetank")
            .hit(true, DamageKind.EXPLOSION, 150.0, null).affliction("Ожог", 50.0, null).affliction("Оглушение", 10.0, null);
        b.weapon("oxygentank")
            .hit(true, DamageKind.EXPLOSION, 150.0, null).affliction("Ожог", 25.0, null).affliction("Оглушение", 5.0, null);
        b.weapon("plasmacutter")
            .hit(true, DamageKind.EXPLOSION, 150.0, null).affliction("Ожог", 25.0, null).affliction("Оглушение", 5.0, null)
            .hit(true, DamageKind.EXPLOSION, 150.0, null).affliction("Ожог", 100.0, null).affliction("Оглушение", 10.0, null);
        b.weapon("potassium")
            .hit(true, DamageKind.EXPLOSION, 150.0, null).affliction("Ожог", 25.0, null).affliction("Оглушение", 5.0, null);
        b.weapon("railgunharpoon")
            .hit(true, DamageKind.HIT, null, 300.0).affliction("Рваная рана", 100.0, null).affliction("Кровотечение", 100.0, null).affliction("Оглушение", 1.5, null);
        b.weapon("railgunshell")
            .hit(true, DamageKind.HIT, null, 75.0).affliction("Повреждение глубоких тканей", 100.0, null).affliction("Кровотечение", 10.0, null).affliction("Оглушение", 3.0, null)
            .hit(true, DamageKind.EXPLOSION, 150.0, 400.0).affliction("Повреждение глубоких тканей", 350.0, null).affliction("Кровотечение", 100.0, null).affliction("Оглушение", 15.0, null);
        b.weapon("revolver").ranged(0.5, false, 0.1, 6.0).ammo("revolverround");
        b.weapon("revolverround")
            .hit(true, DamageKind.HIT, null, 10.0).affliction("Огнестрельная рана", 24.0, null).affliction("Кровотечение", 16.0, null).affliction("Оглушение", 0.3, null);
        b.weapon("revolverrounddepletedfuel")
            .hit(true, DamageKind.HIT, null, 10.0).affliction("Кровотечение", 10.0, null).affliction("Огнестрельная рана", 35.0, null).affliction("Оглушение", 0.3, null);
        b.weapon("rifle").ranged(2.0, false, null, 2.0).ammo("40mmchemgrenade", "40mmgrenade", "40mmstungrenade");
        b.weapon("riflebullet")
            .hit(true, DamageKind.HIT, null, 15.0).affliction("Огнестрельная рана", 55.0, null).affliction("Кровотечение", 33.0, null).affliction("Оглушение", 0.6, null);
        b.weapon("saltbulb")
            .hit(true, DamageKind.EXPLOSION, 50.0, null).affliction("Ожог", 20.0, null).affliction("Оглушение", 5.0, null);
        b.weapon("scrap")
            .hit(true, DamageKind.HIT, null, 2.0).affliction("Рваная рана", 7.5, null).affliction("Кровотечение", 7.5, null).affliction("Оглушение", 0.3, null);
        b.weapon("scrapcannon").ranged(3.0, false, 0.0, 4.0);
        b.weapon("screwdriver").melee(1.0, 50.0)
            .hit(true, DamageKind.HIT, null, null).affliction("Рваная рана", 5.0, null).affliction("Кровотечение", 2.0, null).affliction("Оглушение", 0.05, null);
        b.weapon("screwdriverdementonite").melee(null, null)
            .hit(true, DamageKind.HIT, null, null).affliction("Рваная рана", 7.5, null).affliction("Психоз", 5.0, null).affliction("Оглушение", 0.05, null);
        b.weapon("screwdriverhardened").melee(null, null)
            .hit(true, DamageKind.HIT, null, null).affliction("Рваная рана", 7.0, null).affliction("Лучевая болезнь", 1.0, null).affliction("Оглушение", 0.08, null).affliction("Кровотечение", 2.0, null);
        b.weapon("shotgun").ranged(1.0, false, 0.1, 3.0).ammo("shotgunshell", "shotgunshellblunt", "shotgunslugexplosive");
        b.weapon("shotgunshell")
            .hit(true, DamageKind.HIT, null, 1.0).affliction("Огнестрельная рана", 8.0, null).affliction("Кровотечение", 6.0, null).affliction("Оглушение", 0.1, null);
        b.weapon("shotgunshellblunt")
            .hit(true, DamageKind.HIT, null, null).affliction("Сильный ушиб", 3.0, null).affliction("Оглушение", 0.3, null);
        b.weapon("shotgunslugexplosive")
            .hit(true, DamageKind.HIT, null, 20.0).affliction("Повреждение глубоких тканей", 20.0, null).affliction("Кровотечение", 20.0, 0.5).affliction("Оглушение", 0.8, null)
            .hit(true, DamageKind.EXPLOSION, 70.0, 8.0).affliction("Ожог", 60.0, null);
        b.weapon("shotgununique").ranged(0.1, false, 1.0, 4.0).ammo("shotgunshell", "shotgunshellblunt", "shotgunslugexplosive");
        b.weapon("smallcrawleregg")
            .hit(true, DamageKind.EXPLOSION, 100.0, 200.0).affliction("Оглушение", 0.5, 1.0).affliction("Ожог кислотой", 25.0, 1.0);
        b.weapon("smg").ranged(0.19, true, 10.0, 16.0).ammo("smgmagazine");
        b.weapon("smgmagazine").rounds(21)
            .hit(false, DamageKind.HIT, null, 4.0).affliction("Огнестрельная рана", 12.5, null).affliction("Кровотечение", 12.5, null).affliction("Оглушение", 0.2, 0.1);
        b.weapon("smgmagazinedepletedfuel").rounds(21)
            .hit(false, DamageKind.HIT, null, 5.0).affliction("Кровотечение", 5.0, null).affliction("Огнестрельная рана", 15.0, null).affliction("Оглушение", 0.15, null);
        b.weapon("smgunique").ranged(0.1, true, 2.0, 8.0).ammo("smgmagazine");
        b.weapon("sodium")
            .hit(true, DamageKind.EXPLOSION, 150.0, null).affliction("Ожог", 25.0, null).affliction("Оглушение", 5.0, null);
        b.weapon("spear")
            .hit(true, DamageKind.HIT, null, 15.0).affliction("Рваная рана", 35.0, null).affliction("Кровотечение", 35.0, null).affliction("Оглушение", 0.5, null);
        b.weapon("spearunique")
            .hit(true, DamageKind.HIT, null, 20.0).affliction("Рваная рана", 80.0, null).affliction("Кровотечение", 80.0, null).affliction("Оглушение", 1.0, null);
        b.weapon("spinelingspikeloot").melee(1.0, 50.0)
            .hit(true, DamageKind.HIT, null, 2.0).affliction("Рваная рана", 5.0, null).affliction("Кровотечение", 10.0, null).affliction("Оглушение", 0.2, null);
        b.weapon("steel")
            .hit(true, DamageKind.HIT, null, 2.0).affliction("Рваная рана", 7.5, null).affliction("Кровотечение", 7.5, null).affliction("Оглушение", 0.3, null);
        b.weapon("stunbaton").melee(0.95, 100.0).ammo("batterycell", "fulguriumbatterycell")
            .hit(true, DamageKind.HIT, null, null).affliction("Сильный ушиб", 4.0, null).affliction("Оглушение", 0.2, null);
        b.weapon("stungrenade")
            .hit(true, DamageKind.EXPLOSION, 750.0, null).affliction("Оглушение", 4.0, null).affliction("Ослепление", 15.0, null).affliction("Сенсорная перегрузка", 15.0, null)
            .hit(true, DamageKind.EXPLOSION, 500.0, null).affliction("Оглушение", 10.0, null).affliction("Ослепление", 25.0, null).affliction("Сенсорная перегрузка", 30.0, null)
            .hit(true, DamageKind.EXPLOSION, 200.0, null).affliction("Ожог", 10.0, null).affliction("Контузия", 10.0, null).affliction("Сильное ослепление", 10.0, null);
        b.weapon("stungun").ranged(null, false, 0.0, 2.0).ammo("stungundart", "stungundartfulgurium");
        b.weapon("stungundart")
            .hit(true, DamageKind.HIT, null, null).affliction("Рваная рана", 0.5, null);
        b.weapon("stungundartfulgurium")
            .hit(true, DamageKind.HIT, null, null).affliction("Рваная рана", 1.0, null);
        b.weapon("sulphuriteshard")
            .hit(true, DamageKind.EXPLOSION, 200.0, 3.0).affliction("Ожог кислотой", 20.0, null).affliction("Оглушение", 5.0, null);
        b.weapon("thermalartifact")
            .hit(true, DamageKind.EXPLOSION, 200.0, 3.0);
        b.weapon("thoriumfuelrod").rounds(8)
            .hit(false, DamageKind.HIT, null, 35.0).affliction("Повреждение глубоких тканей", 50.0, null).affliction("Лучевая болезнь", 25.0, null).affliction("Оглушение", 0.5, null)
            .hit(false, DamageKind.EXPLOSION, 125.0, null).affliction("Ожог", 25.0, null).affliction("Лучевая болезнь", 80.0, null).affliction("Оглушение", 1.0, null);
        b.weapon("toolbox").melee(1.0, 50.0).ammo("40mmchemgrenade", "40mmgrenade", "40mmstungrenade", "abscomponent", "acoscomponent", "addercomponent", "alarmbuzzer", "alienblueprint", "aliengravityammoblueprint", "alienincendiumblueprint", "alienphysicoriumammoblueprint", "andcomponent")
            .hit(true, DamageKind.HIT, null, null).affliction("Сильный ушиб", 2.0, null).affliction("Оглушение", 0.6, null);
        b.weapon("toyhammer").melee(1.0, 100.0)
            .hit(true, DamageKind.HIT, null, null).affliction("Оглушение", 0.2, null);
        b.weapon("uex")
            .hit(true, DamageKind.EXPLOSION, 500.0, 150.0).affliction("Повреждение глубоких тканей", 125.0, null).affliction("Ожог", 15.0, 0.2).affliction("Кровотечение", 40.0, 0.05).affliction("Оглушение", 7.0, null);
        b.weapon("uranium")
            .hit(true, DamageKind.HIT, null, 1.0).affliction("Рваная рана", 8.0, null).affliction("Кровотечение", 6.0, null).affliction("Оглушение", 0.3, null).affliction("Лучевая болезнь", 5.0, null);
        b.weapon("volatilecompoundn")
            .hit(true, DamageKind.EXPLOSION, 500.0, 150.0).affliction("Повреждение глубоких тканей", 150.0, null).affliction("Ожог", 20.0, 0.2).affliction("Кровотечение", 50.0, 0.05).affliction("Оглушение", 8.0, null);
        b.weapon("weldingfueltank")
            .hit(true, DamageKind.EXPLOSION, 150.0, null).affliction("Ожог", 25.0, null).affliction("Оглушение", 5.0, null);
        b.weapon("weldingtool")
            .hit(true, DamageKind.EXPLOSION, 150.0, null).affliction("Ожог", 25.0, null).affliction("Оглушение", 5.0, null)
            .hit(true, DamageKind.EXPLOSION, 150.0, null).affliction("Ожог", 50.0, null).affliction("Оглушение", 10.0, null);
        b.weapon("wrench").melee(1.0, 50.0)
            .hit(true, DamageKind.HIT, null, null).affliction("Сильный ушиб", 4.0, null).affliction("Оглушение", 0.2, null);
        b.weapon("wrenchdementonite").melee(null, null)
            .hit(true, DamageKind.HIT, null, null).affliction("Сильный ушиб", 8.5, null).affliction("Психоз", 5.0, null).affliction("Оглушение", 0.2, null);
        b.weapon("wrenchhardened").melee(null, null)
            .hit(true, DamageKind.HIT, null, null).affliction("Сильный ушиб", 8.0, null).affliction("Лучевая болезнь", 1.0, null).affliction("Оглушение", 0.3, null);
    }
}
