package baro.planner;

import baro.model.*;
import baro.pricing.PriceCalculator;
import baro.pricing.StoreOffer;

import java.util.*;

/**
 * «Хочу сделать X — что нужно с нуля»: рецепт раскрывается до сырья (того, что не крафтится или выгоднее купить),
 * считается суммарное количество и цена покупки сырья.
 */
public final class CraftTree {
    private static final int MAX_DEPTH = 7;

    /** Что делать с узлом: скрафтить, купить, добыть (не крафтится и не продаётся), повтор в цепочке. */
    public enum Mode { CRAFT, BUY, GATHER, CYCLE }

    /**
     * Узел дерева.
     * @param count сколько штук нужно
     * @param crafts сколько раз запустить рецепт (с учётом выхода рецепта)
     * @param options для «любой из группы» — подпись группы, иначе null
     */
    public record Node(Item item, int count, Mode mode, Recipe recipe, int crafts, List<Node> children, String options) {
        public boolean isLeaf() { return children.isEmpty(); }
    }

    private final PriceCalculator prices;

    public CraftTree(PriceCalculator prices) { this.prices = prices; }

    /** @param buyIfCheaper покупать промежуточное, если купить дешевле, чем скрафтить из купленного сырья */
    public Node build(Item item, int count, boolean buyIfCheaper) {
        return node(item, count, null, new HashSet<>(), 0, buyIfCheaper, true);
    }

    private Node node(Item item, int count, String options, Set<Item> path, int depth, boolean buyIfCheaper, boolean root) {
        Recipe r = recipe(item);
        Optional<StoreOffer> buy = prices.cheapestBuy(item);
        if (path.contains(item)) return new Node(item, count, buy.isPresent() ? Mode.BUY : Mode.CYCLE, null, 0, List.of(), options);
        if (r == null || depth >= MAX_DEPTH) return new Node(item, count, buy.isPresent() ? Mode.BUY : Mode.GATHER, null, 0, List.of(), options);
        int crafts = (count + r.outputCount() - 1) / r.outputCount();
        path.add(item);
        List<Node> kids = new ArrayList<>();
        for (Ingredient g : r.ingredients()) {
            Item pick; String label = null;
            if (g instanceof ItemIngredient ii) pick = ii.item();
            else {
                TagIngredient t = (TagIngredient) g;
                if (t.options().isEmpty()) continue;
                pick = cheapest(t.options());
                label = t.displayName();
            }
            kids.add(node(pick, g.count() * crafts, label, path, depth + 1, buyIfCheaper, false));
        }
        path.remove(item);
        Node crafted = new Node(item, count, Mode.CRAFT, r, crafts, merge(kids), options);
        if (!root && buyIfCheaper && buy.isPresent()) {
            OptionalLong c = cost(crafted);
            if (c.isEmpty() || (long) buy.get().price() * count <= c.getAsLong()) return new Node(item, count, Mode.BUY, null, 0, List.of(), options);
        }
        return crafted;
    }

    /** Рецепт, которым удобнее всего делать: без автомата и без чертежа, если можно. */
    public static Recipe recipe(Item item) {
        Recipe best = null;
        for (Recipe r : item.recipes()) {
            if (r.isVending() || r.ingredients().isEmpty()) continue;
            if (best == null || best.needsUnlock() && !r.needsUnlock()) best = r;
        }
        return best;
    }

    private Item cheapest(List<Item> options) {
        Item best = options.get(0);
        int bp = Integer.MAX_VALUE;
        for (Item o : options) {
            int p = prices.cheapestBuy(o).map(StoreOffer::price).orElse(Integer.MAX_VALUE);
            if (p < bp) { bp = p; best = o; }
        }
        return best;
    }

    /** Одинаковые ингредиенты одной строкой. */
    private static List<Node> merge(List<Node> kids) {
        Map<String, Node> m = new LinkedHashMap<>();
        for (Node k : kids) {
            // скрафченные поддеревья не складываем — у каждого свои количества внутри
            String key = k.mode() == Mode.CRAFT ? "#" + m.size() : k.item().id() + "|" + k.mode() + "|" + k.options();
            Node prev = m.get(key);
            if (prev == null) { m.put(key, k); continue; }
            m.put(key, new Node(k.item(), prev.count() + k.count(), k.mode(), null, 0, List.of(), k.options()));
        }
        return new ArrayList<>(m.values());
    }

    /** Цена покупки всех листьев дерева; пусто, если что-то не продаётся. */
    public OptionalLong cost(Node n) {
        if (n.mode() == Mode.BUY) return prices.cheapestBuy(n.item()).map(o -> OptionalLong.of((long) o.price() * n.count())).orElse(OptionalLong.empty());
        if (n.mode() != Mode.CRAFT) return OptionalLong.empty();
        long sum = 0;
        for (Node k : n.children()) {
            OptionalLong c = cost(k);
            if (c.isEmpty()) return c;
            sum += c.getAsLong();
        }
        return OptionalLong.of(sum);
    }

    /** Итого сырья: что купить или добыть, и сколько (по всему дереву). */
    public static Map<Item, Integer> leaves(Node n) {
        Map<Item, Integer> m = new LinkedHashMap<>();
        collect(n, m);
        return m;
    }

    private static void collect(Node n, Map<Item, Integer> m) {
        if (n.mode() != Mode.CRAFT) { m.merge(n.item(), n.count(), Integer::sum); return; }
        for (Node k : n.children()) collect(k, m);
    }

    /** Сколько всего запусков фабрикатора и секунд. */
    public static double seconds(Node n) {
        if (n.mode() != Mode.CRAFT) return 0;
        double s = n.recipe().seconds() * n.crafts();
        for (Node k : n.children()) s += seconds(k);
        return s;
    }
}
