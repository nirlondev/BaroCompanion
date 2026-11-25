package baro.data.catalog;

import baro.data.CatalogBuilder;

/** Таланты всех профессий: деревья, описания, открываемые рецепты. Сгенерировано из файлов игры (Content/Talents). */
final class TalentsData {
    private TalentsData() {}

    static void register(CatalogBuilder b) {
        part0(b);
        part1(b);
    }

    private static void part0(CatalogBuilder b) {
        b.talentTheme("combat", "Бой и оружие");
        b.talentTheme("medical", "Лечение и препараты");
        b.talentTheme("repair", "Ремонт и механика");
        b.talentTheme("electric", "Электрика и реактор");
        b.talentTheme("helm", "Управление подлодкой");
        b.talentTheme("trade", "Торговля и деньги");
        b.talentTheme("craft", "Крафт и разбор");
        b.talentTheme("genetic", "Генетика");
        b.talentTheme("diving", "Плавание и снаружи");
        b.talentTheme("xp", "Опыт и навыки");
        b.talentTheme("team", "Помощь команде");
        b.job("captain", "Капитан")
            .subtree("captain_primary", "Основа", true, "")
                .group(2, 6, "bountyhunter", "logisticsexpert", "prodigy", "inspirationalleader", "veteran", "travelingtradesman")
                .group(2, 6, "emergencymaneuvers", "sailorwithnoname", "camaraderie", "downwiththeship", "gunsmith", "steadytune")
            .subtree("gunslinger", "Стрелок", false, "captain_primary", "skipper", "politician")
                .group(1, 1, "quickdraw")
                .group(1, 1, "lonewolf", "deputy")
                .group(1, 1, "drunkensailor")
                .group(1, 1, "bigguns")
            .subtree("skipper", "Шкипер", false, "captain_primary", "gunslinger", "politician")
                .group(1, 1, "leadingbyexample")
                .group(1, 1, "helmsman")
                .group(1, 1, "inspiringpresence", "trickledown")
                .group(1, 1, "family")
            .subtree("politician", "Политик", false, "captain_primary", "gunslinger", "skipper")
                .group(1, 1, "commendations")
                .group(1, 1, "yourreputation", "affiliation")
                .group(1, 1, "campaigning", "networking")
                .group(1, 1, "figurehead")
            .done();
        b.job("securityofficer", "Офицер охраны")
            .subtree("security_primary", "Основа", true, "")
                .group(2, 6, "protectandserve", "warstories", "firstaidtraining", "physicalconditioning", "swole", "buff")
                .group(2, 5, "weaponsmith", "bythebook", "dontpushit", "bootcamp", "munitionsexpertise")
            .subtree("frogman", "Ныряльщик", false, "security_primary", "enforcer", "gunner")
                .group(1, 1, "boardingparty")
                .group(1, 1, "scavenger", "slayer")
                .group(1, 1, "easyturtle", "daringdolphin")
                .group(1, 1, "specops")
            .subtree("enforcer", "Боец", false, "security_primary", "frogman", "gunner")
                .group(1, 1, "pacificationkit")
                .group(1, 1, "beatcop", "stonewall")
                .group(1, 1, "crustyseaman", "inordinateexsanguination")
                .group(1, 1, "implacable")
            .subtree("gunner", "Стрелок", false, "security_primary", "frogman", "enforcer")
                .group(1, 1, "rifleman")
                .group(1, 1, "tandemfire", "commando")
                .group(1, 1, "gunrunner", "extrapowder")
                .group(1, 1, "warlord")
            .done();
        b.job("assistant", "Помощник")
            .subtree("assistant_primary", "Основа", true, "")
                .group(2, 6, "playingcatchup", "mailman", "skedaddle", "crewlayabout", "starterquest", "mule")
                .group(2, 6, "revengesquad", "inspiringtunes", "bagitup", "peerlearning", "indenturedservitude", "jengamaster")
            .subtree("grayshirt", "Помощник", false, "assistant_primary", "apprentice", "clown")
                .group(1, 1, "thewaitinglist")
                .group(1, 1, "nonthreatening", "tastytarget")
                .group(1, 1, "insurancepolicy", "anappleaday")
                .group(1, 1, "thefriendswemade")
            .subtree("apprentice", "Стажер", false, "assistant_primary", "grayshirt", "clown")
                .group(1, 1)
                .group(1, 1, "logisticssystems", "journeyman")
                .group(1, 1, "loyalassistant", "disloyalscum")
                .group(1, 1, "graduationceremony")
            .subtree("clown", "Клоун", false, "assistant_primary", "grayshirt", "apprentice")
                .group(1, 1, "enrollintoclowncollege")
                .group(1, 1, "waterprankster")
                .group(1, 1, "psychoclown", "chonkyhonks")
                .group(1, 1, "truepotential")
            .done();
        b.job("engineer", "Инженер")
            .subtree("engineer_primary", "Основа", true, "")
                .group(2, 5, "stationengineer", "junctionjunkie", "egghead", "grounded", "remotemonitor")
                .group(2, 5, "funwithfission", "melodicrespite", "submarineofthings", "aggressiveengineering", "samplecollection")
            .subtree("weaponsengineer", "Инженер-оружейник", false, "engineer_primary", "electrician", "physicist")
                .group(1, 1, "militaryapplications")
                .group(1, 1, "heavyhitter", "armsrace")
                .group(1, 1, "weaponartisan")
                .group(1, 1, "nuclearoption")
            .subtree("electrician", "Электрик", false, "engineer_primary", "weaponsengineer", "physicist")
                .group(1, 1, "gridmaintainer")
                .group(1, 1, "pyromaniac", "unlimitedpower")
                .group(1, 1, "betterthannew")
                .group(1, 1, "lightningwizard")
            .subtree("physicist", "Физик", false, "engineer_primary", "weaponsengineer", "electrician")
                .group(1, 1, "unstoppablecuriosity")
                .group(1, 1, "cruising", "buzzing")
                .group(1, 1, "dangerzone")
                .group(1, 1, "phdinnuclearphysics")
            .done();
        b.job("mechanic", "Механик")
            .subtree("mechanic_primary", "Основа", true, "")
                .group(2, 6, "salvagecrew", "machinemaniac", "safetyfirst", "multifunctional", "engineengineer", "ballastdenizen")
                .group(2, 5, "modularrepairs", "oiledmachinery", "pumpndump", "ironman", "retrofit")
            .subtree("scrapper", "Сборщик хлама", false, "mechanic_primary", "machinist", "brawler")
                .group(1, 1, "miner")
                .group(1, 1, "residualwaste", "massproduction")
                .group(1, 1, "scrapsavant", "toolmaintenance")
                .group(1, 1, "ironstorm")
            .subtree("machinist", "Машинист", false, "mechanic_primary", "scrapper", "brawler")
                .group(1, 1, "hullfixer")
                .group(1, 1, "tinkerer", "quickfixer")
                .group(1, 1, "letitdrain")
                .group(1, 1, "robotics")
            .subtree("brawler", "Буян", false, "mechanic_primary", "machinist", "scrapper")
                .group(1, 1, "iamthatguy")
                .group(1, 1, "heavylifting", "mudraptorwrestler")
                .group(1, 1, "foolhardy", "berserker")
                .group(1, 1, "powerarmor")
            .done();
        b.job("medicaldoctor", "Врач")
            .subtree("medical_primary", "Основа", true, "")
                .group(2, 6, "healthinsurance", "nobodyimportantdies", "exampleofhealth", "laresistance", "selfcare", "stayinalive")
                .group(2, 5, "firemanscarry", "medicalcompanion", "dontdieonme", "nopressure", "blooddonor")
            .subtree("xenologist", "Ксенолог", false, "medical_primary", "medic", "scientist")
                .group(1, 1, "geneharvester")
                .group(1, 1, "bloodybusiness", "genesplicer")
                .group(1, 1, "blackmarketgenes", "genetampering")
                .group(1, 1, "geneticgenious")
            .subtree("medic", "Медик", false, "medical_primary", "xenologist", "scientist")
                .group(1, 1, "medicalexpertise")
                .group(1, 1, "medicalassistance", "emergencyresponse")
                .group(1, 1, "drsubmarine", "vitaminsupplements")
                .group(1, 1, "miracleworker")
            .subtree("scientist", "Химик", false, "medical_primary", "xenologist", "medic")
                .group(1, 1, "plaguedoctor")
                .group(1, 1, "deliverysystem", "labcontacts")
                .group(1, 1, "supersoldiers", "whatastench")
                .group(1, 1, "macrodosing")
            .done();
        b.talent("affiliation", "Склонность", "Репутация с фракциями повышается на 50% быстрее.\nВыполняя задания для поддерживаемой фракции, вы получаете на 10% больше кредитов.\nЕсли таких талантов несколько, эффект не суммируется.\nПоддерживаемая фракция: фракция, в которой у вас наивысшая репутация.").themes("trade", "xp").stat("ReputationGainMultiplier", "", 0.5);
        b.talent("aggressiveengineering", "Агрессивная инженерия", "Вы наносите втрое больше урона отвертками.").themes("combat");
        b.talent("anappleaday", "Правильное питание", "Уменьшает количество потерянных навыков при смерти на 100%, а также на 75% – при выборе немедленного воскрешения вместо ожидания.").themes("xp");
        b.talent("armsrace", "Гонка вооружений", "Доступен рецепт: Магазин для ПП с обедненным топливом, Патрон револьвера с обедненным топливом.").recipes("smgmagazinedepletedfuel", "revolverrounddepletedfuel").themes("craft", "trade");
        b.talent("bagitup", "Все свое ношу с собой", "Доступен рецепт: Рюкзак.").recipes("backpack").themes("craft");
        b.talent("ballastdenizen", "Обитатель камеры балласта", "Вы задерживаете дыхание на 50% дольше.").themes("diving").stat("HoldBreathMultiplier", "", 0.5);
        b.talent("beatcop", "Патрульный", "Оглушение действует на 25% сильнее.\nВдвое эффективнее против монстров внутри подлодки.").themes("combat", "helm");
        b.talent("berserker", "Берсерк", "При уровне здоровья ниже 50% вы получаете следующее:\nИсходящий урон в ближнем бою +20%").themes("combat", "medical", "xp");
        b.talent("betterthannew", "Лучше нового", "После вашего ремонта состояние электроприборов ухудшается на 30% медленнее.").themes("electric", "repair");
        b.talent("bigguns", "Большие пушки", "Вы наносите на 30% больше урона монстрам, используя револьверы и бомбарды.\nСнижает штраф за ношение стрелкового оружия в обеих руках на 50%.\nДоступен рецепт: Бомбарда, Патрон бомбарды.").recipes("handcannon", "handcannonround").themes("combat", "craft").stat("DualWieldingPenaltyReduction", "", 0.5);
        b.talent("blackmarketgenes", "Подпольные гены", "Генетические материалы продаются в магазинах исследовательских станций и городов.\nВы можете продать генетические материалы на 50% дороже.").themes("genetic", "trade").stat("StoreSellMultiplier", "Ценность продажи", 0.5).stat("StoreSellMultiplier", "Ценность продажи", 0.5);
        b.talent("blooddonor", "Донор крови", "После завершения хотя бы одной миссии вы получаете в инвентарь один пакет с кровью.");
        b.talent("bloodybusiness", "Грязные дела", "При приеме инопланетной крови у вас на 50% слабее психоз.\nКрупные существа  дают +1 Инопланетная кровь.");
        b.talent("boardingparty", "Абордажная команда", "Доступен рецепт: Абордажный топор, Гидрокостюм, Гарпунное электроружье.").recipes("boardingaxe", "slipsuit", "harpooncoilrifle").themes("combat", "craft", "electric");
        b.talent("bootcamp", "Учебка", "Вы будете получать +50% опыта, пока не наберете 5 очков таланта.\nКогда у вас будет 5 или более очков таланта, вы получите: +15 Бонусный навык оружия.").themes("combat", "xp").stat("WeaponsSkillBonus", "Бонусный навык оружия", 15.0);
        b.talent("bountyhunter", "Охотник за головами", "Дает на 15% больше опыта и на 15% больше денег за задания типа \"Чудовище\" или \"Гнездо\".").themes("combat", "trade", "xp");
        b.talent("buff", "Усиление", "Вы получаете дополнительно 50% (Длительность усиления).\nВы получаете дополнительно 25% устойчивости (Наркотическая зависимость).").themes("medical").stat("BuffDurationMultiplier", "Длительность усиления", 0.5);
        b.talent("buzzing", "Жужжалка", "Максимальная мощность вашего реактора увеличена на 10%.").themes("electric");
        b.talent("bythebook", "По уставу", "По завершении хотя бы одной миссии ваша команда получает дополнительно 1250 кредитов и 200 опыта за каждого захваченного живым пленного-человека на борту вашего корабля, максимум - 2500 кредитов и 400 опыта.").themes("team", "trade", "xp");
        b.talent("camaraderie", "Товарищество", "Пока ни один член экипажа не погиб во время миссии, вы будете получать +10% опыта и +5% сопротивления физическому урону.\nСмерти помощников не считаются.").themes("combat", "team", "xp");
        b.talent("campaigning", "Кампании", "Верфи поддерживаемой фракции дают вам скидку 10% на улучшения и покупку подлодок.").themes("trade").stat("ShipyardBuyMultiplierAffiliated", "", 0.1);
        b.talent("chonkyhonks", "Классный клаксон", "Гудок клаксона с вероятностью 50% оглушает существ рядом на одну секунду.").themes("medical");
        b.talent("commando", "Спецназовец", "При использовании оружия дальнего боя в приседе рассеивание уменьшается на 70%, и вы наносите на 10% больше урона.").themes("combat");
        b.talent("commendations", "Благодарности", "После завершения хотя бы одной миссии вы получаете благодарность.\nПрименив благодарность к другому персонажу, вы дадите ему 275 опыта.").themes("xp");
        b.talent("crewlayabout", "Дармоед", "Потеряно 40% (Скорость ремонта).\nВы теряете 15 по всем навыкам.\nСоюзники рядом с вами ремонтируются на 25% быстрее и получают дополнительно 10 ко всем навыкам.").themes("combat", "electric", "helm", "medical", "repair", "team", "xp").stat("ElectricalSkillBonus", "Бонусный навык электрика", -15.0).stat("HelmSkillBonus", "Бонусный навык управления", -15.0).stat("MechanicalSkillBonus", "Бонусный навык механика", -15.0).stat("MedicalSkillBonus", "Бонусный навык медика", -15.0).stat("WeaponsSkillBonus", "Бонусный навык оружия", -15.0).stat("RepairSpeed", "Скорость ремонта", -0.4).ally("ElectricalSkillBonus", 10.0, "", "рядом").ally("HelmSkillBonus", 10.0, "", "рядом").ally("MechanicalSkillBonus", 10.0, "", "рядом").ally("MedicalSkillBonus", 10.0, "", "рядом").ally("WeaponsSkillBonus", 10.0, "", "рядом");
        b.talent("cruising", "Давилка", "Эффективность использования топлива у вашего реактора увеличена на 20%.").themes("electric");
        b.talent("crustyseaman", "Суровый моряк", "Восстановление после кровотечения происходит намного быстрее.");
        b.talent("dangerzone", "Опасная зона", "Вы получаете на 50% устойчивости к лучевой болезни, когда носите защитный комплект.\nДоступен рецепт: Нестабильный грозовиевый топливный стержень.").recipes("fulguriumfuelrodvolatile").themes("craft", "electric");
        b.talent("daringdolphin", "Дерзкий дельфин", "Вы получаете дополнительно 25% (Скорость плавания).").themes("diving").stat("SwimmingSpeed", "Скорость плавания", 0.25);
        b.talent("deliverysystem", "Служба доставки", "Доступен рецепт: Улучшенный шприц-пистолет.").recipes("advancedsyringegun").themes("combat", "craft", "medical");
        b.talent("deputy", "Помощник", "Помощники рядом получают +20 к навыку Управление и +20 к навыку Оружие, и повышают эти навыки вдвое быстрее.").themes("combat", "medical", "team", "xp").ally("HelmSkillBonus", 20.0, "assistant", "рядом").ally("WeaponsSkillBonus", 20.0, "assistant", "рядом").ally("WeaponsSkillGainSpeed", 1.0, "assistant", "рядом").ally("HelmSkillGainSpeed", 1.0, "assistant", "рядом");
        b.talent("disloyalscum", "Мерзкий предатель", "Вы и все члены команды с профессией, по которой вы НЕ стажируетесь, выполняют ремонт и сварку, а также повышают навыки на 20% быстрее.").themes("repair", "team", "xp").ally("SkillGainSpeed", 0.2, "", "профессиям, по которым помощник НЕ стажируется");
        b.talent("dontdieonme", "Не умирай!", "Ваши реанимационные действия гораздо эффективнее обычных.").themes("medical").stat("CPRBoost", "", 1.0);
        b.talent("dontpushit", "Не нарывайся", "Доступен рецепт: Патронташ, Разрывная пуля.").recipes("bandolier", "shotgunslugexplosive").themes("combat", "craft");
        b.talent("downwiththeship", "На дно", "Находясь на своей подлодке, которая на 50% затоплена, вы получаете +50% к скорости плавания и +50% к скорости ремонта корпуса на 60 сек.").themes("diving", "helm", "repair");
        b.talent("drsubmarine", "Подводный доктор", "Применяемые вами медицинские предметы на 25% более эффективны.").themes("medical").stat("MedicalItemApplyingMultiplier", "", 0.25);
        b.talent("drunkensailor", "Пьяный матрос", "Вы получаете 75% устойчивости к оглушению во время опьянения и иммунитет к неприятным эффектам опьянения.\nДоступен рецепт: Ром.").recipes("rum").themes("craft", "medical");
        b.talent("easyturtle", "Спокойная черепаха", "В воде вы получаете на 20% меньше урона от атак.").themes("combat", "diving");
        b.talent("egghead", "Яйцеголовый", "Скорость освоения навыков +100%").themes("xp");
        b.talent("emergencymaneuvers", "Аварийные маневры", "Когда монстр повреждает вашу подлодку, вы получаете максимальный навык управления на 5 сек.").themes("combat", "helm", "xp");
        b.talent("emergencyresponse", "Экстренная помощь", "Помощники могут перетаскивать персонажей без потери скорости.\nЕсли кто-то из команды без сознания, вы движетесь на 40% быстрее.").themes("team");
        b.talent("engineengineer", "Инженер по двигателям", "Макс. скорость двигателей подлодки получает +2.5% за ваш уровень, максимум – 20%.\nЕсли таких талантов несколько, эффект не суммируется.").themes("helm", "xp");
        b.talent("enrollintoclowncollege", "Зачисление в клоуны", "Пока вы носите полный костюм клоуна, вам доступна Сила клоуна.\nСила клоуна позволяет:\nСкорость передвижения +10%\nУстойчивость к физическому урону +5%\nДоступен рецепт: Клоунский сундук.").recipes("clowncrate").themes("combat", "craft", "diving");
        b.talent("exampleofhealth", "Образцовое здоровье", "Вы получаете дополнительно 20% (Максимальное здоровье).").themes("combat", "medical").stat("MaximumHealthMultiplier", "Максимальное здоровье", 0.2);
        b.talent("extrapowder", "Суперпорох", "Взрывы на 20% больше в радиусе и наносят на 10% больше урона.\nСозданная вами взрывчатка на 1 выше качеством.").themes("combat", "craft").stat("ExplosionRadiusMultiplier", "", 0.2).stat("ExplosionDamageMultiplier", "", 0.1).stat("IncreaseFabricationQuality", "", 1.0);
        b.talent("family", "Семья", "После разблокировки: каждый член команды получает плюс один уровень.\nЕсли на задании никто из команды не погиб, высокая мораль улучшается до отличной, и преимущества удваиваются.\nСмерти помощников не считаются.").themes("team", "xp");
        b.talent("figurehead", "Подставное лицо", "Благодарности заменяются на медали, дающие 550 опыта. Если уровень персонажа ниже 5, он получит опыт для достижения уровня 5.\nВсе услуги на аванпостах поддерживаемой фракции дешевле на 10%.").themes("trade", "xp").stat("StoreBuyMultiplierAffiliated", "", 0.1).stat("ShipyardBuyMultiplierAffiliated", "", 0.1);
        b.talent("firemanscarry", "Перенос на плече", "Вы можете перетаскивать персонажей без потери скорости.");
        b.talent("firstaidtraining", "Обучение первой помощи", "Вы получаете дополнительно 15 к навыку Медицина.\nПри использовании на вас медицинские предметы на 35% более эффективны.").themes("medical", "xp").stat("MedicalSkillBonus", "Бонусный навык медика", 15.0).stat("MedicalItemEffectivenessMultiplier", "", 0.35);
        b.talent("foolhardy", "Удаль", "При уровне здоровья ниже 50% вы получаете следующее:\nУстойчивость к физическому урону +20%").themes("combat", "medical", "xp");
        b.talent("funwithfission", "Веселое расщепление", "Созданные вами топливные стержни на 1 выше качеством.").themes("craft").stat("IncreaseFabricationQuality", "", 1.0);
        b.talent("geneharvester", "Сборщик генов", "Из останков монстра вы с вероятностью 20% получите генетический материал.").themes("combat", "genetic");
        b.talent("genesplicer", "Спайщик генов", "Очистка генетических материалов дополнительно улучшает их потенциал на 10%.").themes("genetic").stat("GeneticMaterialRefineBonus", "", 10.0);
        b.talent("genetampering", "Настройка генов", "Все члены команды получают на 10% больше здоровья и движутся на 5% быстрее.").themes("medical", "team");
        b.talent("geneticgenious", "Генетический гений", "При сочетании разных типов генетических материалов их загрязнение на 99% менее вероятно.\nДоступен рецепт: Улучшенный спайщик генов.").recipes("advancedgenesplicer").themes("craft", "genetic").stat("GeneticMaterialTaintedProbabilityReductionOnCombine", "", 0.99);
        b.talent("graduationceremony", "Выпускной", "Вы получаете все таланты случайной ветки специализации дерева талантов профессии вашей стажировки.\nДоступен рецепт: Церемониальный меч.").recipes("ceremonialsword").themes("combat", "craft", "team", "xp");
        b.talent("gridmaintainer", "Ремонт сетей", "Доступен рецепт: Ручной электромонитор.\nВы получаете дополнительно 50% (Скорость ремонта).").recipes("handheldelectricalmonitor").themes("craft", "electric", "repair");
        b.talent("grounded", "Заземление", "Вы получаете дополнительно 10 к навыку Электрика.\nВы получаете дополнительно 20% устойчивости (Ожог).").themes("electric", "xp").stat("ElectricalSkillBonus", "Бонусный навык электрика", 10.0);
        b.talent("gunrunner", "Торговец оружием", "Вы переносите боеприпасы для орудий на 25% быстрее.\nВы ходите на 20% при прицеливании.").themes("diving");
        b.talent("gunsmith", "Оружейник", "Доступен рецепт: Револьвер, Патрон револьвера.").recipes("revolver", "revolverround").themes("combat", "craft");
        b.talent("healthinsurance", "Медстраховка", "Если во время задания ваше здоровье упадет ниже 30%, вы получите 100 кред.\nМедицинские предметы можно купить на 10% дешевле.").themes("medical", "trade").stat("StoreBuyMultiplier", "", 0.1);
        b.talent("heavyhitter", "Крепкий кулак", "Доступен рецепт: Закаленный гаечный ключ, Закаленная отвертка, Закаленный лом, Закаленный нож ныряльщика.").recipes("wrenchhardened", "screwdriverhardened", "crowbarhardened", "divingknifehardened").themes("craft");
        b.talent("heavylifting", "Поднятие тяжестей", "Вы на 20% быстрее переносите металлические ящики или артефакты.").themes("diving");
        b.talent("helmsman", "Кормчий", "Дает +30 к навыку управления.\nПовышает эффективность двигателей и насосов на 20%.\nЕсли таких талантов несколько, эффект не суммируется.").themes("helm", "repair", "xp").stat("HelmSkillBonus", "Бонусный навык управления", 30.0);
        b.talent("hullfixer", "Ремонтник корпуса", "Доступен рецепт: Граната с монтажной пеной, Ручной монитор статуса.\nСкорость ремонта корпуса +25%").recipes("fixfoamgrenade", "handheldstatusmonitor").themes("craft", "repair").stat("RepairToolStructureRepairMultiplier", "Скорость ремонта корпуса", 0.25);
        b.talent("iamthatguy", "Я тот самый человек", "Вы наносите на 20% больше урона ударами тупых предметов (оружием ближнего боя).\nВы получаете дополнительно 20 к навыку Бонусный навык оружия.\nДоступен рецепт: Тяжелый гаечный ключ.").recipes("heavywrench").themes("combat", "craft", "xp").stat("WeaponsSkillBonus", "Бонусный навык оружия", 20.0);
        b.talent("implacable", "Неумолимый", "Когда ваше здоровье будет опускаться ниже 0, вы будете оставаться в сознании, а ваши атаки будут на 25% сильнее в течение 15 сек.\nЭтот эффект может накладываться лишь один раз за жизнь.").themes("combat", "medical");
        b.talent("indenturedservitude", "Долговое рабство", "Нанять помощников стоит на 20% меньше.\nДругие помощники получают +10% опыта, который накапливается вплоть до 50%.").themes("trade", "xp").stat("HireCostMultiplier", "", -0.2);
        b.talent("inordinateexsanguination", "Неумеренное кровопускание", "Кровотечение наносит на 100% больше урона.").themes("combat");
        b.talent("inspirationalleader", "Вдохновитель", "Относится ко всей команде:\nСкорость освоения навыков +25%").themes("team", "xp").ally("SkillGainSpeed", 0.25, "", "");
        b.talent("inspiringpresence", "Вдохновляющее присутствие", "Члены команды в отсеке, получающем очко навыка, получают 3. Члены команды ходят на 5% быстрее.").themes("team", "xp");
        b.talent("inspiringtunes", "Вдохновляющий мотив", "Когда вы гудите в клаксон, союзники рядом получают +15 ко всем навыкам на 10 с.").themes("medical", "team", "xp").ally("ElectricalSkillBonus", 15.0, "", "рядом, на время, после действия").ally("HelmSkillBonus", 15.0, "", "рядом, на время, после действия").ally("MechanicalSkillBonus", 15.0, "", "рядом, на время, после действия").ally("MedicalSkillBonus", 15.0, "", "рядом, на время, после действия").ally("WeaponsSkillBonus", 15.0, "", "рядом, на время, после действия");
        b.talent("insurancepolicy", "Страховка", "Если вы погибаете от недружественных причин во время миссии, ваша команда получает по 200 кредитов за каждую миссию, в которой вы участвовали со времени последней гибели.").themes("team", "trade");
        b.talent("ironman", "Железный человек", "Доступен рецепт: Железный шлем, Самодельный бронежилет.").recipes("ironhelmet", "makeshiftarmor").themes("craft");
        b.talent("ironstorm", "Железный шторм", "Подлодки уровня 1 и уровня 2 можно улучшать как подлодки уровня 3.\nДоступен рецепт: Хлам-пушка.").recipes("scrapcannon").themes("combat", "craft", "helm", "xp");
        b.talent("jengamaster", "Мастер дженги", "Взаимодействие со шкафчиками на подлодке постоянно увеличивет размер штабеля этих шкафчиков на +4.\nЕсли таких талантов несколько, эффект не суммируется.").themes("craft", "helm", "xp");
        b.talent("journeyman", "Странник", "Вы и все члены команды с профессией, по которой вы стажируетесь, получат 10 основного навыка этой профессии и будут повышать этот навык на 50% быстрее.").themes("combat", "electric", "helm", "medical", "repair", "team", "xp").stat("HelmSkillBonus", "Бонусный навык управления", 10.0).stat("HelmSkillGainSpeed", "", 0.5).stat("MedicalSkillBonus", "Бонусный навык медика", 10.0).stat("MedicalSkillGainSpeed", "", 0.5).stat("WeaponsSkillBonus", "Бонусный навык оружия", 10.0).stat("WeaponsSkillGainSpeed", "", 0.5).stat("ElectricalSkillBonus", "Бонусный навык электрика", 10.0).stat("ElectricalSkillGainSpeed", "", 0.5).stat("MechanicalSkillBonus", "Бонусный навык механика", 10.0).stat("MechanicalSkillGainSpeed", "", 0.5).ally("HelmSkillBonus", 10.0, "captain", "профессии, по которой стажируется помощник").ally("HelmSkillGainSpeed", 0.5, "captain", "профессии, по которой стажируется помощник").ally("WeaponsSkillBonus", 10.0, "securityofficer", "профессии, по которой стажируется помощник").ally("WeaponsSkillGainSpeed", 0.5, "securityofficer", "профессии, по которой стажируется помощник").ally("MedicalSkillBonus", 10.0, "medicaldoctor", "профессии, по которой стажируется помощник").ally("MedicalSkillGainSpeed", 0.5, "medicaldoctor", "профессии, по которой стажируется помощник").ally("MechanicalSkillBonus", 10.0, "mechanic", "профессии, по которой стажируется помощник").ally("MechanicalSkillGainSpeed", 0.5, "mechanic", "профессии, по которой стажируется помощник").ally("ElectricalSkillBonus", 10.0, "engineer", "профессии, по которой стажируется помощник").ally("ElectricalSkillGainSpeed", 0.5, "engineer", "профессии, по которой стажируется помощник");
        b.talent("junctionjunkie", "Фанат соединений", "Вы будете получать разовые награды за ремонт определенного количества электрических устройств:\n- 30: Дает +10 к навыку \"Бонусный навык электрика\" и +500 ОО.\n- 50: Улучшает кабельные коробки подлодки до 1 ур.\n- 100: Навсегда увеличивает скорость ремонта электроприборов на 50%").themes("electric", "helm", "repair", "trade", "xp").stat("ElectricalSkillBonus", "Бонусный навык электрика", 10.0).stat("ElectricalRepairSpeed", "", 0.5);
        b.talent("labcontacts", "Лабораторные контакты", "Химикаты можно купить на 30% дешевле.\nВы создаете медицинские предметы на 100% быстрее.").themes("craft", "medical", "trade").stat("FabricationSpeed", "", 1.0).stat("StoreBuyMultiplier", "", 0.3);
        b.talent("laresistance", "Сопротивление", "Вы получаете дополнительно 50% устойчивости (Наркотическая зависимость).\nВы получаете дополнительно 50% устойчивости (Химическая зависимость).");
        b.talent("leadingbyexample", "По примеру", "Пока вы живы, у всех членов команды высокая мораль, которая дает такие преимущества:\nСкорость ремонта +20%\nСкорость ремонта корпуса +20%\nСкорость передвижения +5%").themes("diving", "repair", "team");
        b.talent("letitdrain", "Пусть течет", "Вы можете создавать портативные насосы и устанавливать их куда надо.\nНа вашу подлодку можно установить не больше 2 таких предметов.\nДоступен рецепт: Портативный насос.").recipes("portablepump").themes("craft", "helm", "repair").stat("MaxAttachableCount", "", 2.0);
        b.talent("lightningwizard", "Электромаг", "Если вас не атакуют в течение 10 сек., следующая атака ближнего боя против вас будет на 80% слабее, и атакующий получит оглушение на 4 сек.\nДоступен рецепт: Дуговой разрядник.").recipes("arcemitter").themes("combat", "craft", "medical");
        b.talent("logisticsexpert", "Эксперт по логистике", "Дает на 20% больше опыта и на 20% больше денег за задания типа \"Груз\" или \"Перевозка пассажиров\".").themes("trade", "xp");
        b.talent("logisticssystems", "Системы логистики", "Доступен рецепт: Ручной искатель предметов, Самодельные полки.\nНа вашу подлодку можно установить не больше 3 таких предметов.").recipes("makeshiftshelf", "handhelditemfinder").themes("craft", "helm").stat("MaxAttachableCount", "", 3.0);
        b.talent("lonewolf", "Одинокий волк", "Если рядом нет членов команды, вы получаете:\nУстойчивость к физическому урону +20%\nУстойчивость к оглушению +20%\nИсходящий урон в ближнем бою +20%").themes("combat", "medical", "team");
        b.talent("loyalassistant", "Верный помощник", "Вы и все члены команды с профессией, по которой вы стажируетесь, изготавливают предметы на 1 выше качеством.").themes("craft", "team", "xp");
        b.talent("machinemaniac", "Механик-маньяк", "Вы будете получать разовые награды за ремонт определенного количества механических устройств:\n- 12: Дает +10 к навыку \"Бонусный навык механика\" и 500 ОО.\n- 20: Улучшает насосы подлодки до 1 ур.\n- 40: Навсегда увеличивает скорость ремонта механических приборов на 50%").themes("helm", "repair", "trade", "xp").stat("MechanicalSkillBonus", "Бонусный навык механика", 10.0).stat("MechanicalRepairSpeed", "", 0.5);
    }

    private static void part1(CatalogBuilder b) {
        b.talent("macrodosing", "Макродозы", "Ваши усиления действуют на 25% дольше.\nЯды действуют на 100% сильнее.\nДоступен рецепт: Гормональный усилитель.\nЭтот прибор дает постоянное улучшение характеристик.").recipes("endocrinebooster").themes("craft", "medical").stat("BuffItemApplyingMultiplier", "", 0.25).stat("PoisonMultiplier", "", 1.0);
        b.talent("mailman", "Почтальон", "Дает на 25% больше опыта и на 250 больше денег за задания типа \"Груз\".").themes("trade", "xp");
        b.talent("massproduction", "Массовое производство", "При изготовлении предметов с вероятностью 40% не расходуется один из ингредиентов.").themes("craft");
        b.talent("medicalassistance", "Медицинская помощь", "Помощники в том же отсеке получают +30 к навыку медика.").themes("team", "xp").ally("MedicalSkillBonus", 30.0, "assistant", "в том же отсеке");
        b.talent("medicalcompanion", "Медицинский компаньон", "Доступен рецепт: Гарнитура с инжектором.").recipes("autoinjectorheadset").themes("craft", "medical");
        b.talent("medicalexpertise", "Медицинская экспертиза", "Дает +30 к навыку медика. Ваши повязки более эффективны.").themes("medical", "xp").stat("MedicalSkillBonus", "Бонусный навык медика", 30.0);
        b.talent("melodicrespite", "Музыкальная пауза", "Когда вы играете на гитаре в течение 10 с. или дольше, Скорость ремонта у вас и союзников рядом повышается на +25% на 60 с.").themes("medical", "repair", "team");
        b.talent("militaryapplications", "Военное применение", "Вы получаете дополнительно 20 к навыку Оружие.\nДоступен рецепт: Обедненное топливо, Ящик с боеприпасами с обедненным топливом для магнитной пушки.").recipes("depletedfuel", "coilgunammoboxdepletedfuel").themes("combat", "craft", "electric", "xp").stat("WeaponsSkillBonus", "Бонусный навык оружия", 20.0);
        b.talent("miner", "Шахтер", "При деконструкции руды вы с вероятностью 20% получите вдвое больше продукта.\nОтделение руды резаками происходит на 100% быстрее.").themes("repair").stat("RepairToolDeattachTimeMultiplier", "", 1.0);
        b.talent("miracleworker", "Чудотворец", "Члены команды рядом не погибают от ранений, если не сдаются.\nНа вас не действует \"Сотворение чуда\".").themes("medical", "team");
        b.talent("modularrepairs", "Модульный ремонт", "Доступен рецепт: Мультитул.\nУлучшение подлодки: Модульный ремонт, до 1 ур.").recipes("repairpack").themes("craft", "helm", "repair");
        b.talent("mudraptorwrestler", "Борьба с рапторами", "Вы наносите на 50% больше урона грязевым рапторам в ближнем бою.\nВы получаете дополнительно 10% (Устойчивость к физическому урону).").themes("combat");
        b.talent("mule", "Мул", "Размер штабеля в вашем инвентаре увеличивается на +2 для материалов, которые можно штабелировать.").themes("craft").stat("InventoryExtraStackSize", "", 2.0);
        b.talent("multifunctional", "Универсал", "Вы наносите на 50% больше урона гаечными ключами и ломами.").themes("combat");
        b.talent("munitionsexpertise", "Эксперт по боеприпасам", "Доступен рецепт: Магазин для ПП, Патрон для дробовика.").recipes("smgmagazine", "shotgunshell").themes("combat", "craft", "trade");
        b.talent("networking", "Сети", "Магазины поддерживаемой фракции дают вам скидку 10%. И вдвое больше за уникальные предметы фракции.").themes("trade").stat("StoreBuyMultiplierAffiliated", "", 0.1).stat("StoreBuyMultiplierAffiliated", "", 0.1).stat("StoreBuyMultiplierAffiliated", "", 0.1).stat("StoreBuyMultiplierAffiliated", "", 0.1).stat("StoreBuyMultiplierAffiliated", "", 0.1);
        b.talent("nobodyimportantdies", "Все, кто надо, живы", "Медицинские предметы становятся на +10% мощнее, если их применять не к помощникам. Лечение помощников будет на 20% менее эффективно.").themes("medical");
        b.talent("nonthreatening", "Безвредный", "Враги не заметят вас, если вы упадете и будете лежать дольше 2 сек.");
        b.talent("nopressure", "Без давления", "Доступен рецепт: Стабилизатор давления.").recipes("pressurestabilizer").themes("craft", "medical");
        b.talent("nuclearoption", "Крайняя мера", "Ядерные снаряды и ядерные глубинные бомбы гораздо дешевле создавать из неизрасходованных топливных стержней.\nДоступен рецепт: Быстрый ускоритель распада.").recipes("nucleargun", "nuclearshellcheap", "nucleardepthchargecheap", "nucleardepthdecoycheap").themes("combat", "craft", "medical");
        b.talent("oiledmachinery", "Идеальный механизм", "Фабрикаторы и деконструкторы работают на 50% быстрее.\nЕсли таких талантов несколько, эффект не суммируется.").themes("craft", "xp");
        b.talent("pacificationkit", "Набор для умиротворения", "Доступен рецепт: Автоматический дробовик, Грозовиевый дротик для электрошокера, Полицейский щит.").recipes("autoshotgun", "stungundartfulgurium", "riotshield").themes("combat", "craft", "electric");
        b.talent("peerlearning", "Обмен опытом", "После завершения задания члены экипажа с минимальным уровнем получают на 50% больше опыта.").themes("xp");
        b.talent("phdinnuclearphysics", "Кандидат наук по ядерной физике", "Ваши атаки с ломом на 200% сильнее.\nНе суммируется с эффектом, который дает Неуемное любопытство.\nДоступен рецепт: УЗК.").recipes("pucs").themes("combat", "craft", "diving", "xp");
        b.talent("physicalconditioning", "Физическая подготовка", "Вы получаете дополнительно 10% (Скорость передвижения).\nВы получаете дополнительно 30% устойчивости (Нехватка кислорода).").themes("diving").stat("MovementSpeed", "Скорость передвижения", 0.1);
        b.talent("plaguedoctor", "Чумной доктор", "Вы получаете дополнительно 50% устойчивости (Ожог кислотой).\nДоступен рецепт: Европовка, Алая кислота.").recipes("europabrew", "sulphuricacidsyringe").themes("craft", "medical");
        b.talent("playingcatchup", "Тормоз", "Если ваш уровень на 2 или больше ниже самого высокого уровня в вашей команде, вы получаете на 100% опыта за миссии.").themes("team", "xp");
        b.talent("powerarmor", "МЕХаник", "Вы двигаетесь на 25% быстрее в экзокостюме.\nДоступен рецепт: Экзокостюм.").recipes("exosuit").themes("craft", "diving");
        b.talent("prodigy", "Дарование", "Скорость освоения навыков +50%").themes("xp");
        b.talent("protectandserve", "Служить и защищать", "Вы будете получать +30% опыта и +100% репутации при выполнении миссии - Сопровождение.").themes("trade", "xp");
        b.talent("psychoclown", "Клоун-псих", "Сила клоуна позволяет вам быстрее атаковать оружием ближнего боя – чем сильнее ваш психоз, тем быстрее, до 150%.").themes("combat");
        b.talent("pumpndump", "Надуть и сбросить", "После вашего ремонта Максимальный поток насосов получает +10%.").themes("electric", "repair");
        b.talent("pyromaniac", "Пироманьяк", "Вы наносите на 25% больше урона огнем. И вдвое эффективнее против флоры балласта.\nДоступен рецепт: Огнемет.").recipes("flamer").themes("combat", "craft", "helm").stat("BallastFloraDamageMultiplier", "", 0.5);
        b.talent("quickdraw", "Быстрый стрелок", "Если вы применяли пистолет более 8 сек. назад, ваш следующий выстрел из пистолета нанесет на 25% больше урона.\nСнижает штраф за ношение стрелкового оружия в обеих руках на 50%.").themes("combat").stat("DualWieldingPenaltyReduction", "", 0.5);
        b.talent("quickfixer", "Экспресс-ремонт", "После ремонта механического прибора вы двигаетесь на 20% быстрее в течение 10 с.\nВы ремонтируете механические приборы вдвое быстрее.").themes("repair").stat("MechanicalRepairSpeed", "", 1.0);
        b.talent("remotemonitor", "Монитор наблюдения", "Доступен рецепт: Пульт реактора.").recipes("reactorpda").themes("craft", "electric");
        b.talent("residualwaste", "Остаточные отходы", "Деконструкция предмета с вероятностью 20% дает вдвое больше материалов.");
        b.talent("retrofit", "Улучшенная модель", "Доступны улучшения корпуса подлодки на один уровень выше.\nЕсли таких талантов несколько, эффект не суммируется.").themes("helm", "xp");
        b.talent("revengesquad", "Мститель", "После вашей смерти товарищи по команде получают 30% устойчивости к физическому урону на 120 с.").themes("combat", "team");
        b.talent("rifleman", "Автоматчик", "Вы наносите на 10% больше урона из штурмовой винтовки.\nДоступен рецепт: Штурмовая винтовка, Магазин штурмовой винтовки, Ружье, Патрон для ружья.").recipes("assaultrifle", "assaultriflemagazine", "rifle", "riflebullet").themes("combat", "craft", "trade");
        b.talent("robotics", "Робототехника", "Создает мощный (но хрупкий) дрон для прикрытия.\nОдновременно можно активировать до 2 защитных ботов на команду.\nДоступен рецепт: Защитный бот, Боеприпасы защитного бота.").recipes("defensebotspawner", "defensebotammobox").themes("combat", "craft", "team");
        b.talent("safetyfirst", "Техника безопасности", "Доступен рецепт: Страховочные ремни.").recipes("safetyharness").themes("craft");
        b.talent("sailorwithnoname", "Безымянный моряк", "Вы получаете дополнительно 20 к навыку Медицина.\nДоступен рецепт: Сигара.").recipes("cigar").themes("craft", "medical", "xp").stat("MedicalSkillBonus", "Бонусный навык медика", 20.0);
        b.talent("salvagecrew", "Сбор добычи", "Дает на 30% больше опыта за задания типа \"Добыча\".\nТакже повышает на 50% скорость плавания и на 10% устойчивость к физическому урону внутри обломков.").themes("combat", "diving", "xp");
        b.talent("samplecollection", "Коллекция образцов", "Доступен рецепт: Грузовой скутер.").recipes("cargoscooter").themes("craft", "diving");
        b.talent("scavenger", "Мусорщик", "Впервые открыв контейнер вне своей подлодки, вы с вероятностью 25% найдете дополнительные предметы.").themes("helm");
        b.talent("scrapsavant", "Гений утилизации", "Деконструкция хлама дает вдвое больше материалов.\nВпервые открыв контейнер в обломках корабля, вы с вероятностью 20% найдете дополнительный хлам.");
        b.talent("selfcare", "Самосохранение", "При использовании на вас медицинские предметы на 20% более эффективны.").themes("medical").stat("MedicalItemEffectivenessMultiplier", "", 0.2);
        b.talent("skedaddle", "Бегство", "Вы передвигаетесь на 10% быстрее. Когда вас атакуют, это значение повышается до 30% на 5 сек.").themes("combat", "diving").stat("MovementSpeed", "Скорость передвижения", 0.1);
        b.talent("slayer", "Убийца", "Ваши атаки гарпуном становятся на 25% сильнее.\nВ открытом море ваши атаки гарпуном становятся на 50% сильнее.").themes("combat");
        b.talent("specops", "Спецотдел", "Доступен рецепт: Тепловизор.\nВраги с полным здоровьем получают вдвое больше урона (не из орудий).").recipes("thermalgoggles").themes("combat", "craft", "medical");
        b.talent("starterquest", "Стартовый квест", "Выполните задание, чтобы получить усиление соответствующего навыка и опыт:\n- Убить ползуна (+400 ОО и +10 Бонусный навык оружия)\n- Починить кабельную коробку (+300 ОО и +10 Бонусный навык электрика)\n- Заварить течь (+200 ОО и +10 Бонусный навык механика)").themes("combat", "electric", "medical", "repair", "xp").stat("WeaponsSkillBonus", "Бонусный навык оружия", 10.0).stat("ElectricalSkillBonus", "Бонусный навык электрика", 10.0).stat("MechanicalSkillBonus", "Бонусный навык механика", 10.0);
        b.talent("stationengineer", "Инженер станции", "Дает на 30% больше опыта за восстановление работы маяка.\nТакже дает +50% к навыку \"Скорость ремонта\" и +50% к навыку \"Скорость ремонта корпуса\" при нахождении на радиомаяке.").themes("repair", "xp");
        b.talent("stayinalive", "Остаться в живых", "Проведение реанимации членами экипажа устраняет нехватку кислорода на 20% эффективнее.").themes("diving", "medical", "team");
        b.talent("steadytune", "Устойчивая мелодия", "Когда вы играете на губной гармошке в течение 10 с. или дольше, Устойчивость к психозу у вас и союзников рядом повышается на +100% на 60 с.").themes("medical", "team");
        b.talent("stonewall", "Каменная стена", "Когда вас атакуют, вы получаете +25% устойчивости к физическому урону и +25% устойчивости к оглушению на 4 сек.").themes("combat");
        b.talent("submarineofthings", "Подлодка вещей", "Для изготовления схем на базе ППВМ и кабельных коробок вам нужно гораздо меньше материалов.").recipes("fpgacircuit", "circuitbox").themes("craft", "electric");
        b.talent("supersoldiers", "Суперсолдаты", "Ваши усиления действуют на 25% дольше.\nДоступен рецепт: Боевой стимулятор.").recipes("combatstimulantsyringe").themes("craft", "medical").stat("BuffItemApplyingMultiplier", "", 0.25);
        b.talent("swole", "Качок", "Вы получаете дополнительно 10% (Максимальное здоровье).\nВы получаете дополнительно 25% (Сила ближнего боя).").themes("combat", "medical").stat("MaximumHealthMultiplier", "Максимальное здоровье", 0.1).stat("MeleeAttackMultiplier", "Сила ближнего боя", 0.25);
        b.talent("tandemfire", "Сила напарника", "Пока вы обслуживаете орудие вместе с другим членом команды, атаки у вас обоих становятся на 20% мощнее. Эффект распространяется только на ближайшего члена вашей команды.").themes("combat", "team");
        b.talent("tastytarget", "Вкусная цель", "Враги в первую очередь атакуют вас.\nВы получаете на 15% меньше физического урона.").themes("combat");
        b.talent("thefriendswemade", "Везде друзья", "Вы делаете невылупившихся рапторов дружелюбными в медицинском фабрикаторе.\nОдновременно можно активировать до 2 на команду.\nДоступен рецепт: Яйцо ручного раптора.").recipes("petraptoregg").themes("craft", "medical", "team");
        b.talent("thewaitinglist", "Список ожидания", "В ожидании реанимации жизнь покидает вас на 75% медленнее.").themes("medical");
        b.talent("tinkerer", "Самоделкин", "Ремонтируя механические устройства, вы восстанавливаете их исправность на 40% лучше максимального.").themes("repair").stat("MaxRepairConditionMultiplierMechanical", "", 0.4);
        b.talent("toolmaintenance", "Обслуживание инструментов", "Качество инструментов, создаваемых всеми членами команды, на 1 выше.").themes("craft", "team").stat("IncreaseFabricationQuality", "", 1.0).stat("IncreaseFabricationQuality", "", 1.0);
        b.talent("travelingtradesman", "Бродячий торговец", "Относится ко всей команде:\nВ продаже есть дополнительные предметы: 3.\nЦенность продажи +20%\nЕсли таких талантов несколько, эффект не суммируется.").themes("team", "trade", "xp").stat("ExtraSpecialSalesCount", "", 3.0).stat("StoreSellMultiplier", "Ценность продажи", 0.2);
        b.talent("trickledown", "Перераспределение", "Когда вы получаете навык, его получают все члены команды, где бы они ни находились. Члены команды плавают на 10% быстрее.").themes("diving", "team", "xp").ally("SkillShare", 1.0, "", "когда вы повышаете навык");
        b.talent("truepotential", "Истинный талант", "Ударив потерявшего сознание союзника игрушечным молотом, вы оживите его, но у него будет Боевой стимулятор в течение 15 с. Требуется Сила клоуна.\nДоступен рецепт: Веселитель-3000.").themes("craft", "team");
        b.talent("unlimitedpower", "Безграничная сила", "Созданные вами батареи на 1 выше качеством.\nДает +10% к емкости батарей и суперконденсаторов.").themes("craft", "electric").stat("IncreaseFabricationQuality", "", 1.0);
        b.talent("unstoppablecuriosity", "Неуемное любопытство", "Двери открываются на 50% быстрее.\nВаши атаки с ломом на 100% сильнее.").themes("combat");
        b.talent("veteran", "Ветеран", "Дает 20 к навыку управления и 5 к остальным навыкам.\nСкорость ремонта +10%").themes("combat", "electric", "helm", "medical", "repair", "xp").stat("HelmSkillBonus", "Бонусный навык управления", 20.0).stat("ElectricalSkillBonus", "Бонусный навык электрика", 5.0).stat("MechanicalSkillBonus", "Бонусный навык механика", 5.0).stat("MedicalSkillBonus", "Бонусный навык медика", 5.0).stat("WeaponsSkillBonus", "Бонусный навык оружия", 5.0).stat("RepairSpeed", "Скорость ремонта", 0.1);
        b.talent("vitaminsupplements", "Витаминные добавки", "Если вы примените лекарство к союзнику, он получит +20% здоровья до конца задания.").themes("medical", "team");
        b.talent("warlord", "Военачальник", "Вы наносите на 10% больше урона из стрелкового оружия каждому врагу рядом. Максимум – 50%.\nДоступен рецепт: ТП, Магазин для ТП.").recipes("hmg", "hmgmagazine").themes("combat", "craft", "medical", "trade");
        b.talent("warstories", "Военные истории", "В конце каждого раунда, в котором вы убили не менее 3 врагов, в ваш инвентарь добавляется Собрание военных историй. Прочтение повышает навык Оружие, в зависимости от уже имеющегося у читателя навыка и от качества книги.").themes("combat", "craft", "xp");
        b.talent("waterprankster", "Водный хулиган", "Доступен рецепт: Подводная маска клоуна.\nСила клоуна позволяет:\nСкорость плавания +50%").recipes("clowndivingmask").themes("craft", "diving");
        b.talent("weaponartisan", "Искусный оружейник", "Созданное вами оружие на 1 выше качеством.").themes("combat", "craft").stat("IncreaseFabricationQuality", "", 1.0);
        b.talent("weaponsmith", "Оружейник", "Созданное вами оружие на 1 выше качеством.\nДоступен рецепт: Дробовик, Пистолет-пулемет.").recipes("shotgun", "smg").themes("combat", "craft").stat("IncreaseFabricationQuality", "", 1.0);
        b.talent("whatastench", "Ну и вонь!", "Кислотные ожоги наносят на 15% больше урона.\nДоступен рецепт: Кислотная граната, 40-мм кислотная граната.").recipes("chemgrenade", "40mmchemgrenade").themes("combat", "craft");
        b.talent("yourreputation", "Ваша репутация...", "Заходя в новый аванпост, во фракции которого у вас хорошая репутация, вы получаете 200 кред.\nА в город – вдвое больше.\nВы можете выбрать дополнительную миссию.").themes("trade").stat("ExtraMissionCount", "", 1.0);
    }
}
