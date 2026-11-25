package baro.ui.main;


import static baro.Tr.tr;
import baro.app.SubmarineSource;
import baro.data.GameCatalog;
import baro.data.WiringGuide;
import baro.data.gamefiles.SubmarineReader;
import baro.model.Device;
import baro.model.Item;
import baro.search.ItemSearch;
import baro.ui.Theme;
import baro.ui.views.Blocks;
import baro.ui.widgets.*;
import baro.ui.widgets.Stack;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.*;
import java.util.List;

/**
 * Вкладка «Проводка»: сигнальные компоненты, датчики и устройства подлодки — какие у них входы и выходы,
 * что делает компонент и какие у него настройки; на обзорной странице — типовые схемы.
 */
final class WiringTab extends JSplitPane {
    private static final String[][] GROUPS = {{"LOGIC", tr("Логика и сигналы")}, {"MATH", tr("Математика")}, {"SENSOR", tr("Датчики")},
            {"OUTPUT", tr("Свет, звук, кнопки, экраны")}, {"MACHINE", tr("Устройства подлодки")}, {"OTHER", tr("Прочее")}};

    private final SubmarineSource source;
    private final GameCatalog catalog;
    private final Blocks blocks;
    private final SearchField query = new SearchField(tr("Компонент, датчик, устройство…"), 16);
    private final DefaultListModel<Object> model = new DefaultListModel<>();
    private final JList<Object> list = new JList<>(model);
    private final JScrollPane cardScroll = Theme.scroll(null);
    private List<Device> devices;
    private boolean requested;

    WiringTab(SubmarineSource source, GameCatalog catalog, Blocks blocks) {
        super(HORIZONTAL_SPLIT);
        this.source = source;
        this.catalog = catalog;
        this.blocks = blocks;
        Rows row = new Rows(13f);
        JLabel header = new JLabel();
        header.setFont(Theme.font(12f, true));
        header.setForeground(Theme.ACCENT);
        header.setOpaque(true);
        header.setBackground(Theme.PANEL);
        header.setBorder(BorderFactory.createEmptyBorder(6, 6, 2, 6));
        list.setFixedCellWidth(40);
        list.setFixedCellHeight(-1);
        list.setCellRenderer((l, v, i, sel, focus) -> {
            if (v instanceof String h) { header.setText(h.toUpperCase(Locale.ROOT)); return header; }
            Device d = (Device) v;
            Icon ic = catalog.item(d.id()).map(it -> blocks.icon(it, 20)).orElse(null);
            return row.set(d.name(), Theme.FG, d.inputs().size() + tr(" вх. · ") + d.outputs().size() + tr(" вых."), Theme.MUTED, sel, ic);
        });
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && list.getSelectedValue() instanceof Device d) show(d);
        });
        query.onChange(this::refill);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(6, 6, 4, 6));
        top.add(query);
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(Theme.PANEL);
        left.add(top, BorderLayout.NORTH);
        left.add(Theme.scroll(list), BorderLayout.CENTER);
        left.setMinimumSize(new Dimension(260, 200));
        setLeftComponent(left);
        setRightComponent(cardScroll);
        setDividerLocation(Theme.px(340));
        setBorder(null);
        cardScroll.setViewportView(message(tr("Загружаю компоненты из файлов игры…")));
        addHierarchyListener(e -> { if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) load(); });
    }

    private void load() {
        if (requested) return;
        requested = true;
        source.get(r -> {
            devices = r == null ? List.of() : r.wiring();
            refill();
            cardScroll.setViewportView(overview(r));
        });
    }

    private Component message(String text) { return new Stack(4).padding(10, 12, 16, 12).with(blocks.text().text(text, Style.MUTED)); }

    private void refill() {
        model.clear();
        if (devices == null) return;
        String q = ItemSearch.normalize(query.getText());
        for (String[] g : GROUPS) {
            List<Device> l = devices.stream().filter(d -> d.group().equals(g[0]) && (q.isEmpty() || d.nameContains(q))).toList();
            if (l.isEmpty()) continue;
            model.addElement(g[1] + " · " + l.size());
            for (Device d : l) model.addElement(d);
        }
    }

    private void show(Device d) {
        Stack c = new Stack(4).padding(10, 12, 16, 12);
        Optional<Item> item = catalog.item(d.id());
        String group = Arrays.stream(GROUPS).filter(g -> g[0].equals(d.group())).map(g -> g[1]).findFirst().orElse("");
        if (item.isPresent()) c.with(blocks.title(item.get(), group));
        else c.with(blocks.text().text(d.name(), Style.TITLE).newLine().word(group.toUpperCase(Locale.ROOT), Style.MUTED));
        if (d.mod() != null) c.with(blocks.text().text(tr("Из мода «") + d.mod() + "»", Style.MUTED));
        if (!d.description().isEmpty()) c.with(blocks.text().text(d.description(), Style.MUTED));
        Stack how = new Stack(3);
        for (String comp : d.components())
            WiringGuide.of(comp).ifPresent(en -> {
                how.with(blocks.text().text(en.what(), Style.NORMAL));
                if (!en.settings().isEmpty()) {
                    Stack s = new Stack(1).padding(0, 14, 0, 0);
                    for (String line : en.settings()) s.with(blocks.text().word("•  ", Style.ACCENT).text(line, Style.MUTED));
                    how.with(blocks.text().word(tr("Настройки (в игре — гаечным ключом):"), Style.BOLD), s);
                }
            });
        if (how.getComponentCount() > 0) c.with(blocks.section(tr("Как работает"), how));
        c.with(blocks.section(tr("Входы (") + d.inputs().size() + ")", ports(d.inputs())));
        c.with(blocks.section(tr("Выходы (") + d.outputs().size() + ")", ports(d.outputs())));
        item.ifPresent(it -> {
            c.with(blocks.section(tr("Где взять"), blocks.priceLine(it)));
            if (!it.recipes().isEmpty()) c.with(blocks.recipes(it, true));
        });
        c.with(blocks.text().text(tr("Подписи разъёмов — как на панели проводки в игре (открывается отвёрткой)."), Style.MUTED));
        cardScroll.setViewportView(c);
        SwingUtilities.invokeLater(() -> cardScroll.getVerticalScrollBar().setValue(0));
    }

    private Stack ports(List<Device.Port> l) {
        Stack s = new Stack(1);
        if (l.isEmpty()) { s.with(blocks.text().text("—", Style.MUTED)); return s; }
        for (Device.Port p : l) s.with(blocks.text().word("•  ", Style.ACCENT).word(p.label(), Style.BOLD).text(p.label().equals(p.name()) ? "" : "   (" + p.name() + ")", Style.MUTED));
        return s;
    }

    private Component overview(SubmarineReader.Result r) {
        Stack c = new Stack(4).padding(10, 12, 16, 12);
        c.with(blocks.text().text(tr("Проводка"), Style.TITLE));
        if (r == null) c.with(blocks.text().text(tr("Разъёмы читаются из файлов игры, а игра не найдена. Ниже — типовые схемы."), Style.WARN));
        else c.with(blocks.text().text(tr("Слева — все компоненты, датчики и устройства с панелью проводки: входы, выходы и что делают. ")
                + tr("Сигнал «0» и пустой сигнал — это «ложь», всё остальное — «истина»."), Style.MUTED));
        for (WiringGuide.Circuit k : WiringGuide.circuits()) {
            Stack s = new Stack(1);
            s.with(blocks.text().text(k.goal(), Style.MUTED));
            Stack steps = new Stack(1).padding(0, 14, 0, 0);
            int n = 1;
            for (String step : k.steps()) steps.with(blocks.text().word((n++) + ".  ", Style.ACCENT).text(step, Style.NORMAL));
            s.with(steps);
            c.with(blocks.section(k.title(), s));
        }
        return c;
    }
}
