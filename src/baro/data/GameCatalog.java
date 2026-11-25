package baro.data;


import static baro.Tr.tr;
import baro.model.CargoMission;
import baro.model.Job;
import baro.model.Talent;
import baro.model.Item;
import baro.model.ItemCategory;
import baro.model.Medicine;

import java.util.*;

/** Хранилище всех предметов и медикаментов. Только чтение. */
public final class GameCatalog {
    private final Map<String, Item> items;
    private final Map<Item, Medicine> medicines;
    private final List<CargoMission> missions;

    private final List<Job> jobs;
    private final Map<String, Talent> talents;
    private final Map<String, String> themes;

    private final AfflictionIndex afflictions;
    private ItemIcons icons = ItemIcons.none();

    GameCatalog(Map<String, Item> items, Map<Item, Medicine> medicines, List<CargoMission> missions,
                List<Job> jobs, Map<String, Talent> talents, Map<String, String> themes, List<baro.model.Affliction> afflictions) {
        this.jobs = Collections.unmodifiableList(jobs);
        this.talents = Collections.unmodifiableMap(talents);
        this.themes = Collections.unmodifiableMap(themes);
        this.items = Collections.unmodifiableMap(items);
        this.medicines = Collections.unmodifiableMap(medicines);
        this.missions = Collections.unmodifiableList(missions);
        this.afflictions = new AfflictionIndex(afflictions, this.medicines.values(), this.items.values());
    }

    /** Недуги и состояния: чем лечить, что вызывает, что защищает. */
    public AfflictionIndex afflictions() { return afflictions; }

    /** Иконки предметов из файлов игры (пусто, если игра не найдена). */
    public ItemIcons icons() { return icons; }

    /** Данные взяты из установленной игры (а не встроенные). */
    public boolean fromGame() { return source.startsWith(tr("Файлы игры")); }

    /** Профессии с деревьями талантов. */
    public List<Job> jobs() { return jobs; }
    public Optional<Talent> talent(String id) { return Optional.ofNullable(talents.get(id)); }
    /** Название темы таланта («Бой и оружие»…). */
    public String themeTitle(String id) { return themes.getOrDefault(id, id); }

    /** Грузовые миссии (доставка). */
    public List<CargoMission> missions() { return missions; }

    /** Миссии, в ящиках которых есть этот предмет. */
    public List<CargoMission> missionsWith(Item item) {
        List<CargoMission> r = new ArrayList<>();
        for (CargoMission m : missions) if (m.contains(item)) r.add(m);
        return r;
    }

    private String source = tr("Встроенные данные");

    /** Откуда взяты данные: файлы игры (папка, моды) или встроенные. */
    public String source() { return source; }

    /** Собрать каталог: встроенные данные, поверх — файлы установленной игры (если найдены). */
    public static GameCatalog load() { return load(baro.settings.AppSettings.openDefault()); }

    /**
     * Настройки: game.files (true) — читать файлы игры, game.dir — папка игры, game.mods (true) — учитывать
     * включённые моды, game.language (Russian) — язык текстов. Если с файлами игры что-то не так — встроенные данные.
     */
    public static GameCatalog load(baro.settings.AppSettings settings) {
        if (settings.getBoolean("game.files", true)) {
            try {
                baro.data.gamefiles.GameInstall game = baro.data.gamefiles.GameInstall.locate(settings);
                if (game != null) {
                    CatalogBuilder b = new CatalogBuilder();
                    baro.data.catalog.Catalogs.registerAll(b);
                    baro.data.gamefiles.GameFilesReader.Result r = baro.data.gamefiles.GameFilesReader.register(b, game, settings.getString("game.language", baro.Tr.gameLanguage()));
                    GameCatalog c = b.build();
                    c.source = r.source();
                    c.icons = new ItemIcons(r.icons(), settings.dataDir().resolve("icons.cache"));
                    return c;
                }
            } catch (Throwable t) {
                System.err.println(tr("Файлы игры не загружены, используются встроенные данные: ") + t);
                t.printStackTrace();
            }
        }
        return embedded();
    }

    /** Только данные, встроенные в программу. */
    public static GameCatalog embedded() {
        CatalogBuilder b = new CatalogBuilder();
        baro.data.catalog.Catalogs.registerAll(b);
        return b.build();
    }

    public Collection<Item> items() { return items.values(); }
    public Optional<Item> item(String id) { return Optional.ofNullable(items.get(id)); }
    public Collection<Medicine> medicines() { return medicines.values(); }
    public Optional<Medicine> medicineOf(Item item) { return Optional.ofNullable(medicines.get(item)); }
    public int count(ItemCategory category) {
        int n = 0;
        for (Item i : items.values()) if (category == null || i.category() == category) n++;
        return n;
    }
}
