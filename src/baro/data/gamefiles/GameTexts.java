package baro.data.gamefiles;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Тексты игры (названия, описания, таланты) на выбранном языке, с запасным английским.
 * Файлы локализации читаются простым поиском, как в игре: ключи вида entityname.carbon — это имена тегов
 * ({@code <ключ>текст</ключ>}). Без регулярного выражения: на мегабайтах текста Java компилировала его, занимая ~150 МБ на старте.
 */
final class GameTexts {
    private static final Pattern LANGUAGE = Pattern.compile("language\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private final Map<String, String> main = new HashMap<>(), fallback = new HashMap<>();

    GameTexts(GameInstall game, String language) {
        for (Path f : game.files("Text")) {
            String s;
            try { s = new String(Files.readAllBytes(f), StandardCharsets.UTF_8); } catch (Exception e) { continue; }
            if (!s.isEmpty() && s.charAt(0) == '\uFEFF') s = s.substring(1);
            Matcher lm = LANGUAGE.matcher(s.substring(0, Math.min(s.length(), 2000)));
            String lang = lm.find() ? lm.group(1) : "";
            Map<String, String> target = lang.equalsIgnoreCase(language) ? main : lang.equalsIgnoreCase("English") ? fallback : null;
            if (target == null) continue;
            entries(s, target);
        }
    }

    /** Все записи {@code <ключ>текст</ключ>}; поздние пакеты (моды) перекрывают ранние. */
    private static void entries(String s, Map<String, String> target) {
        for (int i = s.indexOf('<'); i >= 0; i = s.indexOf('<', i + 1)) {
            int j = i + 1;
            while (j < s.length() && isKeyChar(s.charAt(j))) j++;
            if (j == i + 1 || j >= s.length() || s.charAt(j) != '>') continue;
            int end = s.indexOf('<', j + 1);
            String key = s.substring(i + 1, j);
            if (end < 0 || !s.startsWith("</" + key + ">", end)) continue;
            target.put(key.toLowerCase(Locale.ROOT), unescape(s.substring(j + 1, end)));
            i = end + key.length() + 2;
        }
    }

    private static boolean isKeyChar(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '.' || c == '-';
    }

    /** Текст по ключу или null. */
    String get(String key) {
        if (key == null) return null;
        String k = key.toLowerCase(Locale.ROOT);
        String v = main.get(k);
        if (v == null || v.isBlank()) v = fallback.get(k);
        return v == null || v.isBlank() ? null : v;
    }

    String get(String key, String def) { String v = get(key); return v != null ? v : def; }

    boolean isEmpty() { return main.isEmpty() && fallback.isEmpty(); }

    private static String unescape(String s) {
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&").trim();
    }
}