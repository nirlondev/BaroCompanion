package baro.ui.main;


import static baro.Tr.tr;
import baro.app.SubmarineSource;
import baro.data.gamefiles.SubmarineReader;
import baro.model.Submarine;
import baro.pricing.Format;
import baro.search.ItemSearch;
import baro.ui.Links;
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
 * Вкладка «Подлодки»: подлодки игрока (цена, класс, уровень, экипаж, размеры, трюм, вооружение, оборудование)
 * и улучшения подлодок с ценой каждого уровня и максимумом для уровня и класса подлодки. Только из файлов игры.
 */
final class SubmarinesTab extends JSplitPane {
    private final SubmarineSource source;
    private final Blocks blocks;
    private final SearchField query = new SearchField(tr("Подлодка или класс…"), 16);
    private final DefaultListModel<Object> model = new DefaultListModel<>();
    private final JList<Object> list = new JList<>(model);
    private final JScrollPane cardScroll = Theme.scroll(null);
    private SubmarineReader.Result data;
    private boolean requested, failed;

    SubmarinesTab(SubmarineSource source, Blocks blocks) {
        super(HORIZONTAL_SPLIT);
        this.source = source;
        this.blocks = blocks;
        Rows row = new Rows(13f);
        JLabel header = new JLabel();
        header.setFont(Theme.font(12f, true));
        header.setForeground(Theme.ACCENT);
        header.setOpaque(true);
        header.setBackground(Theme.PANEL);
        header.setBorder(BorderFactory.createEmptyBorder(6, 6, 2, 6));
        list.setFixedCellWidth(40);
        list.setCellRenderer((l, v, i, sel, focus) -> {
            if (v instanceof String h) { header.setText(h.toUpperCase(Locale.ROOT)); return header; }
            Submarine s = (Submarine) v;
            return row.set(s.name(), Theme.FG, s.price() > 0 ? Format.money(s.price()) : "", Theme.BUY, sel, null);
        });
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && list.getSelectedValue() instanceof Submarine s) show(s);
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
        setDividerLocation(Theme.px(320));
        setBorder(null);
        cardScroll.setViewportView(message(tr("Загружаю подлодки из файлов игры…")));
        addHierarchyListener(e -> { if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) load(); });
    }

    private void load() {
        if (requested) return;
        requested = true;
        source.get(r -> {
            data = r;
            failed = r == null || r.submarines().isEmpty();
            refill();
            cardScroll.setViewportView(failed ? message(tr("Подлодки читаются только из файлов игры, а игра не найдена. Укажите папку игры в настройках.")) : overview());
        });
    }

    private Component message(String text) { return new Stack(4).padding(10, 12, 16, 12).with(blocks.text().text(text, Style.MUTED)); }

    private void refill() {
        model.clear();
        if (data == null) return;
        String q = ItemSearch.normalize(query.getText());
        Map<String, List<Submarine>> groups = new LinkedHashMap<>();
        for (Submarine s : data.submarines()) {
            if (!q.isEmpty() && !s.nameContains(q)) continue;
            groups.computeIfAbsent(s.className().isEmpty() ? tr("Другие") : s.className(), k -> new ArrayList<>()).add(s);
        }
        for (Map.Entry<String, List<Submarine>> g : groups.entrySet()) {
            model.addElement(g.getKey() + " · " + g.getValue().size());
            for (Submarine s : g.getValue()) model.addElement(s);
        }
    }

    void pricesChanged() {
        if (data == null) return;
        Object v = list.getSelectedValue();
        if (v instanceof Submarine s) show(s); else cardScroll.setViewportView(overview());
    }

    private static String tier(int t) { return t == 1 ? "I" : t == 2 ? "II" : t == 3 ? "III" : String.valueOf(t); }

    private void show(Submarine s) {
        Stack c = new Stack(4).padding(10, 12, 16, 12);
        c.with(blocks.text().text(s.name(), Style.TITLE).newLine()
                .word(((s.className().isEmpty() ? tr("Подлодка") : s.className()) + (s.tier() > 0 ? tr(" · уровень ") + tier(s.tier()) : "")
                        + (s.mod() != null ? tr(" · мод «") + s.mod() + "»" : "")).toUpperCase(), Style.MUTED));
        if (s.preview() != null) {
            JLabel img = new JLabel(new ImageIcon(s.preview()));
            img.setAlignmentX(LEFT_ALIGNMENT);
            img.setHorizontalAlignment(SwingConstants.LEFT);
            c.with(img);
        }
        RichText links = blocks.text().action(tr("Вики"), tr("Страница подлодки в официальной вики игры"), () -> Links.open(this, Links.page(s.id())));
        c.with(links);
        if (!s.description().isEmpty()) c.with(blocks.text().text(s.description(), Style.MUTED));

        Stack main = new Stack(1);
        main.with(blocks.text().word(tr("Цена  "), Style.MUTED).word(s.price() > 0 ? Format.money(s.price()) : "—", Style.BUY));
        if (s.crewMax() > 0) main.with(blocks.text().word(tr("Экипаж  "), Style.MUTED).word(s.crewMin() + "–" + s.crewMax() + tr(" чел."), Style.BOLD)
                .text(s.crewExperience().isEmpty() ? "" : tr(" · опыт: ") + s.crewExperience().toLowerCase(Locale.ROOT), Style.MUTED));
        if (s.widthM() > 0) main.with(blocks.text().word(tr("Размеры  "), Style.MUTED).word(Math.round(s.widthM()) + " × " + Math.round(s.heightM()) + tr(" м"), Style.BOLD));
        main.with(blocks.text().word(tr("Трюм  "), Style.MUTED).word(s.cargoCapacity() > 0 ? s.cargoCapacity() + tr(" ящиков") : tr("нет"), Style.BOLD));
        c.with(blocks.section(tr("Главное"), main));

        c.with(blocks.section(tr("Вооружение"), counts(s.turrets(), tr("Орудий нет"))));
        c.with(blocks.section(tr("Оборудование"), counts(s.equipment(), "—")));
        c.with(blocks.section(tr("Реактор и энергия"), power(s)));
        if (!data.upgrades().isEmpty()) c.with(blocks.section(tr("Улучшения для этой подлодки"), upgrades(s)));
        cardScroll.setViewportView(c);
        SwingUtilities.invokeLater(() -> cardScroll.getVerticalScrollBar().setValue(0));
    }

    /** Потребители энергии и калькулятор реактора с нагрузкой этой подлодки. */
    private Stack power(Submarine s) {
        Stack st = new Stack(3);
        if (s.reactor() != null) st.with(blocks.text().text(tr("Реактор: "), Style.MUTED).word(s.reactor().name() + " — " + Format.num(s.reactor().maxPower()) + tr(" кВт"), Style.BOLD));
        else st.with(blocks.text().text(tr("Своего реактора нет — питание от батарей или от основной подлодки."), Style.MUTED));
        RichText use = blocks.text().text(tr("Постоянная нагрузка на полной мощности ≈ "), Style.MUTED).word(Format.num(Math.round(s.maxLoad())) + tr(" кВт"), Style.BOLD).text(": ", Style.MUTED);
        List<Map.Entry<String, Double>> top = new ArrayList<>(s.powerUse().entrySet()).subList(0, Math.min(6, s.powerUse().size()));
        use.join(top, Style.MUTED, e -> use.text(e.getKey() + " " + Format.num(Math.round(e.getValue())), Style.NORMAL));
        st.with(use);
        if (!s.peakUse().isEmpty()) {
            RichText peak = blocks.text().text(tr("Кратковременно (зарядка орудий, прожекторы): "), Style.MUTED);
            peak.join(s.peakUse().entrySet(), Style.MUTED, e -> peak.text(e.getKey() + tr(" до ") + Format.num(Math.round(e.getValue())), Style.NORMAL));
            st.with(peak);
        }
        if (s.reactor() != null && !data.fuels().isEmpty()) st.with(new ReactorCalc(blocks, data.reactors(), data.fuels(), s));
        return st;
    }

    private RichText counts(Map<String, Integer> m, String empty) {
        RichText t = blocks.text();
        if (m.isEmpty()) return t.text(empty, Style.MUTED);
        return t.join(m.entrySet(), Style.MUTED, e -> t.text(e.getKey(), Style.NORMAL).word(e.getValue() > 1 ? " ×" + e.getValue() : "", Style.ACCENT));
    }

    /** Улучшения по категориям: наибольший уровень для этой подлодки и цена каждого уровня с учётом сложности. */
    private Stack upgrades(Submarine s) {
        float mult = blocks.prices().settings().buyMultiplier();
        Stack st = new Stack(3);
        st.with(blocks.text().text(tr("Цены уровней — с множителем сложности ×") + Format.num(mult) + tr(" (без наценки станции и скидок талантов). ")
                + tr("Наибольший уровень зависит от уровня и класса подлодки."), Style.MUTED));
        Map<String, List<Submarine.Upgrade>> byCat = new LinkedHashMap<>();
        for (Submarine.Upgrade u : data.upgrades()) byCat.computeIfAbsent(u.category(), k -> new ArrayList<>()).add(u);
        for (Map.Entry<String, List<Submarine.Upgrade>> e : byCat.entrySet()) {
            st.with(blocks.text().word(data.categoryNames().getOrDefault(e.getKey(), e.getKey()), Style.BOLD));
            Stack l = new Stack(1).padding(0, 14, 0, 0);
            for (Submarine.Upgrade u : e.getValue()) {
                int max = u.maxFor(Math.max(1, s.tier()), s.subClass());
                RichText r = blocks.text().word(u.name(), Style.NORMAL).text(tr(" — до уровня "), Style.MUTED).word(String.valueOf(max), max > 0 ? Style.ACCENT : Style.MUTED);
                if (max > 0) {
                    List<String> p = new ArrayList<>();
                    for (int lv = 1; lv <= max; lv++) p.add(Format.money(Math.round(u.price(lv) * mult)));
                    r.text(": " + String.join(" · ", p), Style.BUY);
                }
                if (!u.description().isEmpty()) r.newLine().text("    " + u.description(), Style.MUTED);
                if (!u.resources().isEmpty()) r.newLine().text(tr("    Ресурсы: ") + String.join(", ", u.resources()), Style.WARN);
                l.with(r);
            }
            st.with(l);
        }
        return st;
    }

    private Component overview() {
        Stack c = new Stack(4).padding(10, 12, 16, 12);
        c.with(blocks.text().text(tr("Подлодки"), Style.TITLE));
        c.with(blocks.text().text(tr("Выберите подлодку слева: превью, вооружение, оборудование и все улучшения с ценами по уровням."), Style.MUTED));
        Stack t = new Stack(1);
        for (Submarine s : data.submarines()) {
            RichText r = blocks.text().word(s.name(), Style.BOLD)
                    .text("  " + (s.className().isEmpty() ? "" : s.className().toLowerCase(Locale.ROOT)) + (s.tier() > 0 ? tr(", ур. ") + tier(s.tier()) : ""), Style.MUTED)
                    .text(" · ", Style.MUTED).word(s.price() > 0 ? Format.money(s.price()) : "—", Style.BUY);
            if (s.crewMax() > 0) r.text(tr(" · экипаж ") + s.crewMin() + "–" + s.crewMax(), Style.MUTED);
            r.text(tr(" · трюм ") + s.cargoCapacity(), Style.MUTED);
            int guns = s.turrets().values().stream().mapToInt(Integer::intValue).sum();
            r.text(tr(" · орудий ") + guns, Style.MUTED);
            t.with(r);
        }
        c.with(blocks.section(tr("Все подлодки — по уровню и цене"), t));
        if (!data.reactors().isEmpty() && !data.fuels().isEmpty())
            c.with(blocks.section(tr("Калькулятор реактора"), new ReactorCalc(blocks, data.reactors(), data.fuels(), null)));
        return c;
    }
}
