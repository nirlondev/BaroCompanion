package baro.app;


import baro.App;

import javax.swing.*;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Проверка обновлений: при запуске, в фоне, один запрос к последнему выпуску на GitHub (тег «v1.1» или «1.1»).
 * Любая ошибка (нет сети, нет выпусков) — молча ничего не показываем. Выключается настройкой update.check=false.
 */
public final class UpdateCheck {
    private static final String API = "https://api.github.com/repos/nirlondev/BaroCompanion/releases/latest";
    private static final Pattern TAG = Pattern.compile("\"tag_name\"\\s*:\\s*\"v?([^\"]+)\"");
    private static final Pattern PAGE = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+/releases/tag/[^\"]+)\"");

    private UpdateCheck() {}

    /** @param found (версия, страница выпуска) — вызывается в потоке Swing, только если есть версия новее текущей */
    public static void start(BiConsumer<String, String> found) {
        Thread t = new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) URI.create(API).toURL().openConnection();
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);
                c.setRequestProperty("Accept", "application/vnd.github+json");
                c.setRequestProperty("User-Agent", "BaroCompanion/" + App.VERSION);
                if (c.getResponseCode() != 200) return;
                String json;
                try (InputStream in = c.getInputStream()) { json = new String(in.readAllBytes(), StandardCharsets.UTF_8); }
                Matcher tag = TAG.matcher(json), page = PAGE.matcher(json);
                if (!tag.find() || !page.find()) return;
                String version = tag.group(1);
                if (compare(version, App.VERSION) > 0) SwingUtilities.invokeLater(() -> found.accept(version, page.group(1)));
            } catch (Exception e) {
                System.err.println("Проверка обновлений не удалась: " + e);
            }
        }, "update-check");
        t.setDaemon(true);
        t.start();
    }

    /** Сравнение версий «1.9» < «1.10». */
    static int compare(String a, String b) {
        String[] x = a.split("\\."), y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? num(x[i]) : 0, q = i < y.length ? num(y[i]) : 0;
            if (p != q) return Integer.compare(p, q);
        }
        return 0;
    }

    private static int num(String s) { try { return Integer.parseInt(s.replaceAll("\\D", "")); } catch (NumberFormatException e) { return 0; } }
}
