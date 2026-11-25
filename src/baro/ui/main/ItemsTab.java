package baro.ui.main;


import static baro.Tr.tr;
import baro.app.UserLists;
import baro.data.GameCatalog;
import baro.model.Item;
import baro.model.ItemCategory;
import baro.pricing.Format;
import baro.pricing.PriceCalculator;
import baro.search.ItemSearch;
import baro.search.SortMode;
import baro.settings.AppSettings;
import baro.ui.Theme;
import baro.ui.views.Blocks;
import baro.ui.views.ItemDetailView;
import baro.ui.widgets.*;
import baro.ui.widgets.StarBadge;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Вкладка «Предметы»: поиск, категории, избранное и недавние, сортировка, карточка. */
final class ItemsTab extends JSplitPane {
    /** Что показывать в списке: всё, избранное или недавно открытые. */
    private enum Scope { ALL, FAVORITES, RECENT, ON_BOARD, CRAFTABLE }

    private final ItemSearch search;
    private final Blocks blocks;
    private final PriceCalculator prices;
    private final UserLists lists;
    private final AppSettings settings;
    private final SearchField query = new SearchField(tr("Название, англ. название, сленг («плазма»)…"), 18);
    private final JComboBox<SortMode> sort = new JComboBox<>(SortMode.values());
    private final Toggles<ItemCategory> categories = new Toggles<>(3);
    private final Toggles<Scope> scope = new Toggles<>(3);
    private final DefaultListModel<Item> model = new DefaultListModel<>();
    private final JList<Item> list = new JList<>(model);
    private final JLabel count = new JLabel();
    private final DetailPane<Item> detail;
    private final JCheckBox bestOnly = new JCheckBox(tr("Только лучшие"), StarBadge.of(14));
    private boolean updating;

    ItemsTab(GameCatalog catalog, PriceCalculator prices, Blocks blocks, baro.ui.Navigator external, UserLists lists, AppSettings settings) {
        super(HORIZONTAL_SPLIT);
        this.prices = prices;
        this.lists = lists;
        this.settings = settings;
        this.search = new ItemSearch(catalog.items(), prices);
        this.blocks = blocks;
        this.detail = new DetailPane<>(
                (item, nav) -> new ItemDetailView(item, catalog.medicineOf(item), blocks.withNavigator(nav), catalog.missionsWith(item)),
                nav -> new baro.ui.views.BestItemsView(catalog, blocks.withNavigator(nav), categories.selected()),
                (item, nav) -> baro.ui.views.ItemViews.forItem(item, catalog, blocks.withNavigator(nav)),
                item -> item, external);
        detail.onShow(item -> { if (item != null) { lists.opened(item); settings.put("items.last", item.id()); } });

        categories.option(tr("Все (") + catalog.items().size() + ")", null);
        for (ItemCategory c : ItemCategory.values()) {
            int n = catalog.count(c);
            if (n > 0) categories.option(c.title(), c);
        }
        categories.onSelect(c -> { refill(); if (list.getSelectedValue() == null) detail.clear(); });
        scope.option(tr("Все предметы"), Scope.ALL).option(tr("★ Избранное"), Scope.FAVORITES).option(tr("Недавние"), Scope.RECENT)
                .option(tr("На борту"), Scope.ON_BOARD).option(tr("Скрафтить из запасов"), Scope.CRAFTABLE);
        scope.setToolTipText(tr("Избранное — отмечается в карточке предмета; недавние — последние открытые карточки; ")
                + tr("на борту и «скрафтить из запасов» — по содержимому контейнеров подлодки и инвентарю экипажа из сейва"));
        scope.onSelect(s -> refill());
        lists.addListener(() -> { if (scope.selected() != Scope.ALL) refill(); list.repaint(); });
        bestOnly.setOpaque(false);
        bestOnly.setSelectedIcon(StarBadge.of(14));
        bestOnly.setIcon(new baro.ui.widgets.DimIcon(StarBadge.of(14)));
        bestOnly.setToolTipText(tr("Показать только лучшие предметы категории"));
        bestOnly.addActionListener(e -> refill());
        query.setToolTipText(tr("Можно искать по-английски, сленгом («шотган», «плазма») и с опечатками"));
        query.onChange(this::refill);
        sort.addActionListener(e -> refill());

        Rows row = new Rows(13f);
        Icon fav = new GlyphIcon("★", Theme.FAVORITE, Theme.px(14));
        list.setFixedCellHeight(Theme.px(24));
        list.setFixedCellWidth(40); // ширина по окну, а не по самой длинной строке
        list.setCellRenderer((l, item, i, sel, focus) -> row.set(item.displayName(), Theme.FG, priceText(item), sortColor(), sel,
                IconRowIcon.of(blocks.icon(item, 20), item.isBest() ? StarBadge.of(14) : null, lists.isFavorite(item) ? fav : null)));
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !updating && list.getSelectedValue() != null) detail.show(list.getSelectedValue());
        });

        JPanel top = new JPanel(new BorderLayout(4, 4));
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(6, 6, 4, 6));
        JPanel searchRow = new JPanel(new BorderLayout(4, 4));
        searchRow.setOpaque(false);
        searchRow.add(query, BorderLayout.NORTH);
        searchRow.add(scope, BorderLayout.SOUTH);
        top.add(searchRow, BorderLayout.NORTH);
        top.add(categories, BorderLayout.CENTER);
        JPanel sortRow = new JPanel(new BorderLayout(4, 0));
        sortRow.setOpaque(false);
        count.setForeground(Theme.MUTED);
        sortRow.add(bestOnly, BorderLayout.WEST);
        sortRow.add(sort, BorderLayout.CENTER);
        sortRow.add(count, BorderLayout.EAST);
        top.add(sortRow, BorderLayout.SOUTH);

        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(Theme.PANEL);
        left.add(top, BorderLayout.NORTH);
        left.add(Theme.scroll(list), BorderLayout.CENTER);
        left.setMinimumSize(new Dimension(300, 200));
        setLeftComponent(left);
        setRightComponent(detail);
        setDividerLocation(Theme.px(380));
        setBorder(null);
        refill();
    }

    /** Хватает ли запасов на борту на один запуск хотя бы одного рецепта (без автомата). */
    private boolean craftableFromStock(Item item) {
        for (baro.model.Recipe r : item.recipes()) {
            if (r.isVending() || r.ingredients().isEmpty()) continue;
            boolean ok = true;
            for (baro.model.Ingredient g : r.ingredients()) {
                int have = 0;
                if (g instanceof baro.model.ItemIngredient ii) have = blocks.onBoard(ii.item());
                else for (Item o : ((baro.model.TagIngredient) g).options()) have += blocks.onBoard(o);
                if (have < g.count()) { ok = false; break; }
            }
            if (ok) return true;
        }
        return false;
    }

    /** Сейв обновился — запасы на борту могли измениться. */
    void saveChanged() { if (scope.selected() == Scope.ON_BOARD || scope.selected() == Scope.CRAFTABLE) refill(); list.repaint(); }

    private String priceText(Item item) {
        if (scope.selected() == Scope.ON_BOARD) return blocks.onBoard(item) + tr(" шт.");
        switch ((SortMode) sort.getSelectedItem()) {
            case BUY: return prices.cheapestBuy(item).map(o -> Format.money(o.price())).orElse("—") + (prices.isDailySpecial(item) ? " ↓" : "");
            case BASE: return item.price().map(p -> Format.money(p.basePrice())).orElse("—");
            default: return prices.sellPrice(item).map(Format::money).orElse("—") + (prices.isRequested(item) ? " ×2" : "");
        }
    }

    private Color sortColor() {
        SortMode m = (SortMode) sort.getSelectedItem();
        return m == SortMode.BUY ? Theme.BUY : m == SortMode.BASE ? Theme.MUTED : Theme.SELL;
    }

    private void refill() {
        updating = true;
        Item keep = list.getSelectedValue();
        List<Item> found = search.find(query.getText(), categories.selected(), (SortMode) sort.getSelectedItem(), 0);
        if (bestOnly.isSelected()) found.removeIf(i -> !i.isBest());
        if (scope.selected() == Scope.FAVORITES) found.retainAll(lists.favorites());
        if (scope.selected() == Scope.ON_BOARD) found.removeIf(i -> blocks.onBoard(i) == 0);
        if (scope.selected() == Scope.CRAFTABLE) found.removeIf(i -> !craftableFromStock(i));
        if (scope.selected() == Scope.RECENT) {
            List<Item> recent = lists.recent();
            found.retainAll(recent);
            if (ItemSearch.normalize(query.getText()).isEmpty()) found.sort(java.util.Comparator.comparingInt(recent::indexOf));
        }
        model.clear();
        for (Item i : found) model.addElement(i);
        if (keep != null && found.contains(keep)) list.setSelectedValue(keep, true);
        count.setText(found.size() + tr(" шт."));
        updating = false;
    }

    void pricesChanged() { refill(); list.repaint(); detail.refresh(); }

    void open(Item item) {
        updating = true;
        list.setSelectedValue(item, true);
        updating = false;
        detail.show(item);
    }

    Item selected() { return detail.currentValue(); }

    void focusSearch() { query.requestFocusInWindow(); query.selectAll(); }
}
