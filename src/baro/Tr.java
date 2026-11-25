package baro;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Язык интерфейса. Строки в коде написаны по-русски и обёрнуты в tr("…"); для английского перевод берётся из
 * /data/lang/en.txt (строки «"русский"␉"english"» в записи Java). Нет перевода — остаётся русский текст.
 * Язык выбирается до создания окон (ui.language: ru или en; по умолчанию — по языку системы).
 */
public final class Tr {
    private static Map<String, String> table;
    private static String language = "ru";

    private Tr() {}

    public static String tr(String ru) {
        if (table == null) return ru;
        String v = table.get(ru);
        return v != null ? v : ru;
    }

    /** ru или en. */
    public static String language() { return language; }

    public static boolean english() { return "en".equals(language); }

    /** Язык из настроек или системы; «en» — загрузить словарь. */
    public static void configure(String lang) {
        language = "en".equalsIgnoreCase(lang) ? "en" : "ru";
        table = english() ? load("/data/lang/en.txt") : null;
    }

    /** Язык по умолчанию: русский для русской, казахской, украинской и белорусской систем, иначе английский. */
    public static String systemDefault() {
        // DISPLAY — язык интерфейса Windows; без категории Java берёт региональный формат (даты, числа).
        String l = Locale.getDefault(Locale.Category.DISPLAY).getLanguage();
        return l.equals("ru") || l.equals("kk") || l.equals("uk") || l.equals("be") ? "ru" : "en";
    }

    /** Язык текстов игры для выбранного языка интерфейса (как в Content/Texts). */
    public static String gameLanguage() { return english() ? "English" : "Russian"; }

    /** Словарь: каждая строка — два литерала Java через табуляцию. */
    public static Map<String, String> load(String resource) {
        Map<String, String> m = new HashMap<>();
        try (InputStream in = Tr.class.getResourceAsStream(resource)) {
            if (in == null) return m;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = r.readLine()) != null; ) {
                int tab = line.indexOf('\t');
                if (tab <= 0 || !line.startsWith("\"")) continue;
                String ru = unquote(line.substring(0, tab).trim()), en = unquote(line.substring(tab + 1).trim());
                if (ru != null && en != null) m.put(ru, en);
            }
        } catch (Exception e) {
            System.err.println("Словарь не прочитан: " + resource + " — " + e);
        }
        return m;
    }

    /** Литерал Java в кавычках → строка (\n, \t, \", \\, \\uXXXX). */
    static String unquote(String lit) {
        if (lit.length() < 2 || lit.charAt(0) != '"' || lit.charAt(lit.length() - 1) != '"') return null;
        StringBuilder b = new StringBuilder();
        for (int i = 1; i < lit.length() - 1; i++) {
            char c = lit.charAt(i);
            if (c != '\\' || i + 1 >= lit.length() - 1) { b.append(c); continue; }
            char n = lit.charAt(++i);
            switch (n) {
                case 'n' -> b.append('\n');
                case 't' -> b.append('\t');
                case 'u' -> { b.append((char) Integer.parseInt(lit.substring(i + 1, i + 5), 16)); i += 4; }
                default -> b.append(n);
            }
        }
        return b.toString();
    }
}
