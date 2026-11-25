package baro.ui.main;


import static baro.Tr.tr;
import baro.model.Submarine;
import baro.pricing.Format;
import baro.ui.Theme;
import baro.ui.views.Blocks;
import baro.ui.widgets.RichText;
import baro.ui.widgets.Stack;
import baro.ui.widgets.Style;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Калькулятор реактора: нагрузка и стержни в слотах → выработка турбины и скорость деления для зелёной зоны,
 * хватит ли тепла, наибольшая мощность и сколько минут прослужит стержень. Формулы — из Reactor.cs игры.
 */
final class ReactorCalc extends Stack {
    private final Blocks blocks;
    private final List<Submarine.Fuel> fuels;
    private final JComboBox<Object> reactor = new JComboBox<>();
    private final JSpinner load;
    private final java.util.List<JComboBox<Object>> slots = new java.util.ArrayList<>();
    private final Stack result = new Stack(1);

    ReactorCalc(Blocks blocks, List<Submarine.Reactor> reactors, List<Submarine.Fuel> fuels, Submarine sub) {
        super(4);
        this.blocks = blocks;
        this.fuels = fuels;
        Submarine.Reactor def = sub != null && sub.reactor() != null ? sub.reactor() : reactors.isEmpty() ? null : reactors.get(0);
        for (Submarine.Reactor r : reactors) if (!r.id().contains("wrecked")) reactor.addItem(r);
        if (def != null) reactor.setSelectedItem(def);
        reactor.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                Object text = v instanceof Submarine.Reactor r ? r.name() + " — " + Format.num(r.maxPower()) + tr(" кВт") : v;
                return super.getListCellRendererComponent(l, text, i, s, f);
            }
        });
        double defLoad = sub != null && sub.maxLoad() > 0 ? Math.round(sub.maxLoad() / 100.0) * 100 : 3000;
        load = new JSpinner(new SpinnerNumberModel(defLoad, 0, 100000, 100));
        ((JSpinner.DefaultEditor) load.getEditor()).getTextField().setColumns(6);
        JPanel row1 = row(label(tr("Реактор")), reactor, label(tr("  Нагрузка, кВт")), load);
        JPanel row2 = row(label(tr("Стержни")));
        for (int i = 0; i < 4; i++) {
            JComboBox<Object> c = new JComboBox<>();
            c.addItem(tr("— пусто —"));
            for (Submarine.Fuel f : fuels) c.addItem(f);
            c.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                    Object text = v instanceof Submarine.Fuel fu ? fu.name() + tr(" (тепло ") + Format.num(fu.heat()) + ")" : v;
                    return super.getListCellRendererComponent(l, text, i, s, f);
                }
            });
            if (i < 2 && !fuels.isEmpty()) c.setSelectedIndex(1);
            c.addActionListener(e -> recalc());
            slots.add(c);
            row2.add(c);
        }
        reactor.addActionListener(e -> recalc());
        load.addChangeListener(e -> recalc());
        with(row1, row2, result);
        recalc();
    }

    private static JPanel row(Component... cs) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        for (Component c : cs) p.add(c);
        return p;
    }

    private static JLabel label(String s) { JLabel l = new JLabel(s); l.setForeground(Theme.MUTED); return l; }

    private void recalc() {
        result.removeAll();
        Submarine.Reactor r = (Submarine.Reactor) reactor.getSelectedItem();
        if (r == null) { result.with(blocks.text().text(tr("Реакторы не найдены в файлах игры."), Style.MUTED)); revalidate(); return; }
        double fuel = 0;
        java.util.Map<Submarine.Fuel, Integer> rods = new java.util.LinkedHashMap<>();
        for (JComboBox<Object> c : slots) if (c.getSelectedItem() instanceof Submarine.Fuel f) { fuel += f.heat(); rods.merge(f, 1, Integer::sum); }
        double want = ((Number) load.getValue()).doubleValue();
        if (rods.isEmpty()) {
            result.with(blocks.text().text(tr("Вставьте хотя бы один стержень."), Style.WARN));
            revalidate(); repaint();
            return;
        }
        Submarine.ReactorPlan p = Submarine.ReactorPlan.of(r, want, fuel, 100, rods.size());
        // наибольшая мощность при этом топливе: турбина t, при которой тепло (при наибольшем делении) ≥ t + 50
        double maxHeat = p.maxFission() * fuel / 100 * 2;
        double maxTurbine = Math.max(0, Math.min(100, maxHeat - 50));
        double maxOut = maxTurbine / 100 * r.maxPower();
        RichText t1 = blocks.text().text(tr("Турбина ≈ "), Style.MUTED).word(Format.num(Math.round(p.turbine())) + " %", Style.BOLD)
                .text(tr("   ·   скорость деления ≈ "), Style.MUTED).word(Format.num(Math.round(Math.min(p.fission(), 999))) + " %", p.enough() ? Style.BOLD : Style.BAD)
                .text(tr(" (не больше ") + Format.num(Math.round(p.maxFission())) + tr(" % при этом топливе)"), Style.MUTED);
        result.with(t1);
        if (want > r.maxPower()) result.with(blocks.text().text(tr("Нагрузка больше наибольшей мощности реактора (") + Format.num(r.maxPower()) + tr(" кВт) — нужен второй реактор или меньше потребителей."), Style.BAD));
        else if (!p.enough()) result.with(blocks.text().text(tr("Тепла не хватает: добавьте стержень или замените на более «горячий». Сейчас реактор даст ≈ ")
                + Format.num(Math.round(maxOut)) + tr(" кВт."), Style.BAD));
        else result.with(blocks.text().text(tr("Тепла хватает. Наибольшая мощность при этом топливе ≈ ") + Format.num(Math.round(maxOut)) + tr(" кВт."), Style.GOOD));
        double f = Math.min(p.fission(), p.maxFission());
        for (java.util.Map.Entry<Submarine.Fuel, Integer> e : rods.entrySet()) {
            double perSecond = f / 100 * r.fuelConsumption();
            double minutes = perSecond <= 0 ? Double.POSITIVE_INFINITY : e.getKey().health() / perSecond / 60;
            result.with(blocks.text().word("•  ", Style.ACCENT).text(e.getKey().name() + (e.getValue() > 1 ? " ×" + e.getValue() : "") + tr(" — хватит на ≈ "), Style.NORMAL)
                    .word(Double.isInfinite(minutes) ? "∞" : Math.round(minutes) + tr(" мин"), Style.ACCENT).text(tr(" при этой нагрузке"), Style.MUTED));
        }
        result.with(blocks.text().text(tr("Температура держится в середине зелёной зоны, когда тепло ≈ турбина + 50. Все стержни в реакторе теряют состояние с одной скоростью — ")
                + tr("дольше живёт тот, у кого больше запас. Таланты и улучшения реактора не учтены."), Style.MUTED));
        revalidate();
        repaint();
    }
}
