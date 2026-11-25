package baro.search;

import baro.model.Item;
import baro.model.ItemCategory;
import baro.pricing.PriceCalculator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Поиск предметов по названию (и английскому названию) с фильтром по категории и сортировкой.
 * Сленг игроков («плазма», «шотган») подмешивается через {@link Synonyms}; если по запросу ничего нет —
 * ищутся названия с опечаткой (одна ошибка на короткое слово, две — на длинное).
 */
public final class ItemSearch {
    private final Collection<Item> items;
    private final PriceCalculator prices;

    public ItemSearch(Collection<Item> items, PriceCalculator prices) { this.items = items; this.prices = prices; }

    /**
     * @param category null — все категории
     * @param limit    0 — без ограничения
     */
    public List<Item> find(String query, ItemCategory category, SortMode sort, int limit) {
        String q = normalize(query);
        Comparator<Item> order = comparator(sort);
        // внутри категории (Оружие, Медицина…) лучшие идут первыми; в общем списке «Все» — обычный порядок
        if (category != null) order = Comparator.comparing((Item i) -> !i.isBest()).thenComparing(order);
        List<String> slang = q.isEmpty() ? List.of() : Synonyms.of(q);
        if (!q.isEmpty()) order = Comparator.comparingInt((Item i) -> slang.contains(i.id()) ? 0 : rank(i, q) + 1).thenComparing(order);
        List<Item> res = items.stream()
                .filter(i -> category == null || i.category() == category)
                .filter(i -> q.isEmpty() || i.nameContains(q) || i.id().contains(q) || slang.contains(i.id()))
                .sorted(order)
                .collect(Collectors.toList());
        if (res.isEmpty() && q.length() >= 3) {
            Map<Item, Integer> fuzzy = new HashMap<>();
            for (Item i : items) {
                if (category != null && i.category() != category) continue;
                int d = typoDistance(i.searchKey(), q);
                if (d <= allowed(q)) fuzzy.put(i, d);
            }
            Comparator<Item> byName = comparator(sort);
            res = fuzzy.keySet().stream().sorted(Comparator.comparingInt((Item i) -> fuzzy.get(i)).thenComparing(byName)).collect(Collectors.toList());
        }
        return limit > 0 && res.size() > limit ? new ArrayList<>(res.subList(0, limit)) : res;
    }

    public static String normalize(String s) { return s == null ? "" : s.trim().toLowerCase(Locale.ROOT).replace('ё', 'е'); }

    /** 0 — название начинается с запроса, 1 — слово начинается, 2 — просто содержит. */
    private static int rank(Item i, String q) {
        if (i.nameStartsWith(q)) return 0;
        if (i.nameContains(" " + q) || i.nameContains("-" + q)) return 1;
        return 2;
    }

    private static int allowed(String q) { return q.length() >= 7 ? 2 : 1; }

    /**
     * Сколько опечаток отделяет запрос от названия: запрос сравнивается с началом каждого слова названия
     * (той же длины ± 1) расстоянием Дамерау — Левенштейна. Многословный запрос — по словам, берётся худшее.
     */
    static int typoDistance(String key, String q) {
        String[] words = key.split("[\\s\\-(),|]+");
        int worst = 0;
        for (String part : q.split("\\s+")) {
            if (part.isEmpty()) continue;
            int best = Integer.MAX_VALUE;
            for (String w : words) {
                if (w.isEmpty()) continue;
                for (int len = part.length() - 1; len <= part.length() + 1; len++) {
                    if (len < 1 || len > w.length()) continue;
                    best = Math.min(best, damerau(w.substring(0, len), part));
                }
            }
            worst = Math.max(worst, best);
        }
        return worst;
    }

    private static int damerau(String a, String b) {
        int n = a.length(), m = b.length();
        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) d[i][0] = i;
        for (int j = 0; j <= m; j++) d[0][j] = j;
        for (int i = 1; i <= n; i++)
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1))
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
            }
        return d[n][m];
    }

    private Comparator<Item> comparator(SortMode sort) {
        Comparator<Item> byName = Comparator.comparing(Item::displayName, String.CASE_INSENSITIVE_ORDER);
        switch (sort) {
            case SELL: return Comparator.comparingInt((Item i) -> -prices.sellPrice(i).orElse(-1)).thenComparing(byName);
            case BUY: return Comparator.comparingInt((Item i) -> prices.cheapestBuy(i).map(o -> o.price()).orElse(Integer.MAX_VALUE)).thenComparing(byName);
            case BASE: return Comparator.comparingInt((Item i) -> -i.price().map(p -> p.basePrice()).orElse(-1)).thenComparing(byName);
            default: return byName;
        }
    }
}
