package baro.ui.views;


import static baro.Tr.tr;
import baro.model.*;
import baro.planner.PlanCache;
import baro.pricing.CargoValuation;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import baro.pricing.Format;
import baro.ui.widgets.RichText;
import baro.ui.widgets.Stack;
import baro.ui.widgets.Style;

import java.util.Map;
import java.util.Optional;

/** Карточка миссии доставки: ящики, содержимое, что выпадает при разборе, планы переработки. */
public final class MissionDetailView extends Stack {
    public MissionDetailView(CargoMission m, Blocks b, CargoValuation valuation, PlanCache cache) {
        super(4);
        padding(10, 12, 16, 12);
        with(b.text().text(m.name(), Style.TITLE).newLine()
                .word((tr("Доставка · ") + m.crateName() + tr(" · ящиков: ") + crates(m)).toUpperCase(), Style.MUTED));

        // награда
        Stack reward = new Stack(1);
        reward.with(b.text().text(tr("За ящик ≈ "), Style.MUTED).word(Format.money(valuation.reward(m)), Style.SELL)
                .text(tr(" · за ") + m.maxCrates() + tr(" ящ. ≈ "), Style.MUTED).word(Format.money((long) valuation.reward(m) * m.maxCrates()), Style.SELL));
        reward.with(b.text().text(tr("В ящике ") + m.itemsPerCrate() + tr(" предм. (с самим ящиком). Миссия засчитывается при доставке 98% предметов — ")
                + tr("при ") + m.maxCrates() + tr(" ящ. можно переработать до ") + m.allowedLoss() + tr(" предм. без провала."), Style.MUTED));
        with(b.section(tr("Награда за доставку (ориентир)"), reward));

        // содержимое
        Stack cont = new Stack(1);
        for (Map.Entry<Item, Integer> e : m.contents().entrySet()) {
            RichText t = b.text().word(e.getValue() + "× ", Style.NORMAL).link(e.getKey());
            if (!e.getKey().deconstruction().isPresent()) t.text(tr("  не разбирается"), Style.WARN);
            cont.with(t);
        }
        with(b.section(tr("Содержимое одного ящика"), cont));

        // разбор
        CargoValuation.DeconResult d = valuation.deconstruct(m);
        Stack dec = new Stack(1);
        if (d.outputs().isEmpty()) dec.with(b.text().text(tr("Ничего не разбирается — груз можно только доставить."), Style.MUTED));
        else {
            Stack list = new Stack(1).padding(0, 14, 0, 0);
            for (Map.Entry<Item, Integer> e : d.outputs().entrySet()) {
                RichText t = b.text().word(e.getValue() + "× ", Style.NORMAL).link(e.getKey());
                int unit = b.prices().sellPrice(e.getKey()).orElse(0);
                if (d.isChance(e.getKey())) t.text(tr("  не всегда"), Style.MUTED);
                else if (unit > 0 && e.getValue() > 1) t.text("  = " + Format.money((long) unit * e.getValue()), Style.MUTED);
                list.with(t);
            }
            dec.with(list);
            dec.with(b.text().text(tr("Продать всё полученное: "), Style.MUTED).word(Format.money(d.sellValue()), Style.SELL)
                    .text(tr(" за ящик · за ") + m.maxCrates() + tr(" ящ.: "), Style.MUTED).word(Format.money((long) d.sellValue() * m.maxCrates()), Style.SELL)
                    .text(tr(" · разбор ящика ≈ ") + Format.seconds(d.seconds()), Style.MUTED));
            if (!d.notDeconstructible().isEmpty()) {
                RichText t = b.text().text(tr("Не разбираются (пропадут, продать груз нельзя): "), Style.WARN);
                dec.with(t.join(d.notDeconstructible(), Style.MUTED, t::link));
            }
        }
        with(b.section(tr("Что выпадает при разборе одного ящика"), dec));

        // планы: подбираются автоматически по текущим рецептам и ценам
        Stack plansBox = new Stack(8);
        JSpinner crates = new JSpinner(new SpinnerNumberModel(1, 1, Math.max(1, m.maxCrates()), 1));
        crates.setMaximumSize(new java.awt.Dimension(70, 26));
        javax.swing.JPanel cratesRow = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
        cratesRow.setOpaque(false);
        javax.swing.JLabel cl = new javax.swing.JLabel(tr("Считать для ящиков:"));
        cl.setForeground(baro.ui.Theme.MUTED);
        cratesRow.add(cl); cratesRow.add(crates);
        Stack plansSection = b.section(tr("Как выгоднее переработать груз (подбор по текущим ценам и рецептам)"), cratesRow, plansBox);
        with(plansSection);
        Runnable load = () -> {
            int n = (Integer) crates.getValue();
            plansBox.removeAll();
            plansBox.with(b.text().text(tr("Подбираю лучшие варианты… (несколько секунд)"), Style.MUTED));
            plansBox.revalidate(); plansBox.repaint();
            cache.request(m, n, plans -> {
                plansBox.removeAll();
                plansBox.with(PlanView.all(plans, b, cache.planner()));
                int bestValue = cache.planner().best(plans).map(r -> cache.planner().value(r.plan())).orElse(0);
                long rewardN = (long) valuation.reward(m) * n;
                RichText verdict = b.text();
                if (bestValue <= 0) verdict.text(tr("Переработка ничего не даёт — только доставлять."), Style.ACCENT);
                else if (rewardN > bestValue) verdict.text(tr("Доставка выгоднее переработки (") + Format.money(rewardN) + tr(" против ") + Format.money(bestValue) + ").", Style.ACCENT);
                else verdict.text(tr("Переработка выгоднее доставки (") + Format.money(bestValue) + tr(" против ") + Format.money(rewardN) + ").", Style.ACCENT);
                plansBox.with(verdict);
                plansBox.revalidate(); plansBox.repaint();
                java.awt.Container top = plansBox.getParent();
                while (top != null && !(top instanceof javax.swing.JViewport)) { top.revalidate(); top = top.getParent(); }
            });
        };
        crates.addChangeListener(e -> load.run());
        load.run();

        with(b.section(tr("Важно"), b.text().text(
                tr("Груз миссии нельзя продать как есть — игра не даёт его продать и удаляет в конце миссии. ")
                + tr("Всё, что вы из него разобрали или скрафтили, — ваше и продаётся. Докупка на «Бездне» стоит ×2,0, продажа даёт 30% цены. ")
                + tr("Планы подбираются автоматически по всем рецептам игры и проверяются пошагово; время — для одного фабрикатора без бонусов."), Style.MUTED)));
    }

    private static String crates(CargoMission m) { return m.minCrates() == m.maxCrates() ? String.valueOf(m.maxCrates()) : m.minCrates() + "–" + m.maxCrates(); }

    private static void step(Stack steps, Blocks b, String label, Map<Item, Integer> m) {
        if (!m.isEmpty()) steps.with(list(b.text().text(label, Style.MUTED), m));
    }

    private static RichText list(RichText t, Map<Item, Integer> m) { return PlanView.list(t, m); }
}
