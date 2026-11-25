package baro.ui.main;


import static baro.Tr.tr;
import baro.data.GameCatalog;
import baro.model.Item;
import baro.pricing.Format;
import baro.pricing.PriceSettings;
import baro.search.DeconFinder;
import baro.search.DeconSource;
import baro.search.ItemSearch;
import baro.search.SortMode;
import baro.ui.Navigator;
import baro.ui.Theme;
import baro.ui.widgets.Rows;
import baro.ui.widgets.SearchField;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** «Где добыть разбором»: выбираете материал — видите, что выгоднее всего купить и разобрать. */
final class DeconFinderDialog extends JDialog {
    private final DeconFinder finder;
    private final ItemSearch search;
    private final SearchField query = new SearchField(tr("Материал: олово, медь, сталь…"), 18);
    private final DefaultListModel<Item> materials = new DefaultListModel<>();
    private final JList<Item> materialList = new JList<>(materials);
    private final SourcesModel table = new SourcesModel();
    private final JLabel caption = new JLabel(" ");
    private boolean bestOnly;
    private JCheckBox bestOnlyBox;

    DeconFinderDialog(Frame owner, GameCatalog catalog, baro.pricing.PriceCalculator prices, Navigator nav) {
        super(owner, tr("Где добыть разбором"), false);
        this.finder = new DeconFinder(prices);
        List<Item> withSources = catalog.items().stream().filter(i -> !i.obtainedFrom().isEmpty()).collect(Collectors.toList());
        this.search = new ItemSearch(withSources, prices);

        Rows row = new Rows(13f);
        materialList.setFixedCellHeight(Theme.px(22));
        materialList.setFixedCellWidth(40); // ширина по окну, а не по самой длинной строке
        materialList.setCellRenderer((l, it, i, sel, f) -> row.set(it.displayName(), Theme.FG,
                prices.sellPrice(it).map(Format::money).orElse("—"), Theme.SELL, sel, it.isBest() ? baro.ui.widgets.StarBadge.of(14) : null));
        materialList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) showSources(); });
        query.onChange(this::refill);

        JTable t = new JTable(table);
        t.getTableHeader().setDefaultRenderer(new baro.ui.widgets.HeaderRenderer());
        t.setRowHeight(Theme.px(22));
        t.setAutoCreateRowSorter(false);
        int[] widths = {230, 115, 95, 175, 85, 120, 55};
        for (int c = 0; c < widths.length; c++) t.getColumnModel().getColumn(c).setPreferredWidth(widths[c]);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c = 1; c < table.getColumnCount(); c++) if (c != 3) t.getColumnModel().getColumn(c).setCellRenderer(right);
        // лучшие предметы — со значком
        t.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean f, int r, int c) {
                super.getTableCellRendererComponent(tb, v, sel, f, r, c);
                Item it = table.rows.get(r).source();
                setIcon(it.isBest() ? baro.ui.widgets.StarBadge.of(14) : null);
                setIconTextGap(5);
                setToolTipText(it.bestReason().map(x -> tr("Лучший выбор: ") + x).orElse(null));
                return this;
            }
        });
        JCheckBox bestOnly = new JCheckBox(tr("Только лучшие"), new baro.ui.widgets.DimIcon(baro.ui.widgets.StarBadge.of(14)));
        bestOnly.setSelectedIcon(baro.ui.widgets.StarBadge.of(14));
        bestOnly.setOpaque(false);
        bestOnly.setToolTipText(tr("Показать только лучшие предметы"));
        bestOnly.addActionListener(e -> { this.bestOnly = bestOnly.isSelected(); showSources(); });
        this.bestOnlyBox = bestOnly;
        t.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int r = t.rowAtPoint(e.getPoint());
                if (e.getClickCount() == 2 && r >= 0) nav.open(table.rows.get(r).source());
            }
        });
        prices.settings().addListener(this::showSources);

        JPanel left = new JPanel(new BorderLayout(0, 4));
        left.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        left.add(query, BorderLayout.NORTH);
        left.add(Theme.scroll(materialList), BorderLayout.CENTER);
        left.setPreferredSize(new Dimension(260, 400));

        JPanel right2 = new JPanel(new BorderLayout(0, 4));
        right2.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 6));
        caption.setForeground(Theme.MUTED);
        JPanel capRow = new JPanel(new BorderLayout());
        capRow.setOpaque(false);
        capRow.add(caption, BorderLayout.CENTER);
        capRow.add(bestOnlyBox, BorderLayout.EAST);
        right2.add(capRow, BorderLayout.NORTH);
        JScrollPane ts = new JScrollPane(t);
        ts.getViewport().setBackground(Theme.SUNK);
        right2.add(ts, BorderLayout.CENTER);
        JLabel hint = new JLabel(tr("Сортировка: от самой дешёвой цены за единицу (для шансов — в среднем). Двойной щелчок — открыть предмет."));
        hint.setForeground(Theme.MUTED);
        right2.add(hint, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout());
        root.add(left, BorderLayout.WEST);
        root.add(right2, BorderLayout.CENTER);
        setContentPane(root);
        setSize(1160, 600);
        baro.ui.WindowChrome.install(this);
        setLocationRelativeTo(owner);
        refill();
    }

    void select(Item material) {
        query.setText("");
        materialList.setSelectedValue(material, true);
    }

    private void refill() {
        Item keep = materialList.getSelectedValue();
        materials.clear();
        for (Item i : search.find(query.getText(), null, SortMode.NAME, 0)) materials.addElement(i);
        if (keep != null && materials.contains(keep)) materialList.setSelectedValue(keep, true);
        else if (!materials.isEmpty() && !query.getText().trim().isEmpty()) materialList.setSelectedIndex(0);
    }

    private void showSources() {
        Item m = materialList.getSelectedValue();
        List<DeconSource> rows = m == null ? new ArrayList<>() : finder.sourcesOf(m);
        if (bestOnly) rows.removeIf(r -> !r.source().isBest());
        table.set(rows);
        caption.setText(m == null ? tr("Выберите материал слева") : "«" + m.displayName() + tr("» выпадает при разборе ") + table.rows.size() + tr(" предметов"));
    }

    private static final class SourcesModel extends AbstractTableModel {
        private static final String[] COLS = {tr("Предмет"), tr("Выход"), tr("Купить"), tr("Где дешевле"), tr("За 1 шт."), tr("Остальное (продажа)"), tr("Время")};
        List<DeconSource> rows = new ArrayList<>();

        void set(List<DeconSource> r) { rows = r; fireTableDataChanged(); }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }

        @Override public Object getValueAt(int r, int c) {
            DeconSource s = rows.get(r);
            switch (c) {
                case 0: return s.source().displayName();
                case 1: return s.yieldText();
                case 2: return s.cheapestOffer().map(o -> Format.money(o.price())).orElse(tr("не продаётся"));
                case 3: return s.cheapestOffer().map(o -> o.store().title()).orElse("—");
                case 4: return s.unitPrice().map(u -> Format.money(Math.round(u))).orElse("—");
                case 5: return s.otherValue() > 0 ? Format.money(s.otherValue()) : "—";
                default: return Format.seconds(s.seconds());
            }
        }
    }
}
