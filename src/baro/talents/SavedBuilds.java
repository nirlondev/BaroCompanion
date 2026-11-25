package baro.talents;


import static baro.Tr.tr;
import baro.data.GameCatalog;
import baro.model.Job;
import baro.model.Talent;
import baro.settings.AppSettings;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Именованные сборки талантов (talents.saved.&lt;профессия&gt; = «имя=id;id,имя2=…») и код сборки для обмена:
 * «BARO1:профессия:id,id,…» в base64url — его можно вставить в чат, а друг — вставить в свой справочник.
 */
public final class SavedBuilds {
    private static final String PREFIX = "BARO1:";
    private final AppSettings settings;

    public SavedBuilds(AppSettings settings) { this.settings = settings; }

    /** Сохранённые сборки профессии: имя → идентификаторы талантов (по порядку). */
    public Map<String, List<String>> of(Job job) {
        Map<String, List<String>> m = new LinkedHashMap<>();
        for (String e : settings.getList("talents.saved." + job.id())) {
            int i = e.indexOf('=');
            if (i <= 0) continue;
            String name = URLDecoder.decode(e.substring(0, i), StandardCharsets.UTF_8);
            List<String> ids = new ArrayList<>();
            for (String id : e.substring(i + 1).split(";")) if (!id.isBlank()) ids.add(id.trim());
            m.put(name, ids);
        }
        return m;
    }

    public void save(Job job, String name, Collection<Talent> talents) {
        Map<String, List<String>> m = of(job);
        List<String> ids = new ArrayList<>();
        for (Talent t : talents) ids.add(t.id());
        m.put(name, ids);
        write(job, m);
    }

    public void delete(Job job, String name) {
        Map<String, List<String>> m = of(job);
        if (m.remove(name) != null) write(job, m);
    }

    private void write(Job job, Map<String, List<String>> m) {
        List<String> l = new ArrayList<>();
        m.forEach((name, ids) -> l.add(URLEncoder.encode(name, StandardCharsets.UTF_8) + "=" + String.join(";", ids)));
        settings.putList("talents.saved." + job.id(), l);
        settings.save();
    }

    /** Таланты по идентификаторам (неизвестные — из выключенного мода — пропускаются). */
    public static List<Talent> resolve(GameCatalog catalog, List<String> ids) {
        List<Talent> l = new ArrayList<>();
        for (String id : ids) catalog.talent(id).ifPresent(l::add);
        return l;
    }

    // ---------- код сборки ----------

    public static String code(Job job, Collection<Talent> talents) {
        StringJoiner j = new StringJoiner(",");
        for (Talent t : talents) j.add(t.id());
        String raw = job.id() + ":" + j;
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** Разобранный код: профессия и таланты. */
    public record Decoded(Job job, List<Talent> talents, int unknown) { }

    /** Разобрать код сборки; пусто — код не похож на код справочника или профессии нет. */
    public static Optional<Decoded> decode(GameCatalog catalog, String code) {
        if (code == null) return Optional.empty();
        String c = code.trim();
        int at = c.indexOf(PREFIX);
        if (at < 0) return Optional.empty();
        c = c.substring(at + PREFIX.length()).split("\\s")[0];
        String raw;
        try { raw = new String(Base64.getUrlDecoder().decode(c), StandardCharsets.UTF_8); } catch (IllegalArgumentException e) { return Optional.empty(); }
        int i = raw.indexOf(':');
        if (i <= 0) return Optional.empty();
        String jobId = raw.substring(0, i);
        Job job = null;
        for (Job j : catalog.jobs()) if (j.id().equalsIgnoreCase(jobId)) job = j;
        if (job == null) return Optional.empty();
        List<String> ids = new ArrayList<>();
        for (String id : raw.substring(i + 1).split(",")) if (!id.isBlank()) ids.add(id.trim());
        List<Talent> t = resolve(catalog, ids);
        return Optional.of(new Decoded(job, t, ids.size() - t.size()));
    }

    /** Сборка текстом для чата: профессия, затем таланты по веткам. */
    public static String text(Job job, Collection<Talent> talents) {
        StringBuilder sb = new StringBuilder(tr("Сборка «") + job.name() + tr("» — талантов ") + talents.size() + "\n");
        Map<String, List<String>> bySub = new LinkedHashMap<>();
        for (Talent t : talents) bySub.computeIfAbsent(t.subtree().name(), k -> new ArrayList<>()).add(t.name());
        bySub.forEach((sub, names) -> sb.append(sub).append(": ").append(String.join(", ", names)).append('\n'));
        sb.append(code(job, talents));
        return sb.toString();
    }
}
