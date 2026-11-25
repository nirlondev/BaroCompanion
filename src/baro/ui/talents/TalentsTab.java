package baro.ui.talents;


import static baro.Tr.tr;
import baro.data.GameCatalog;
import baro.model.*;
import baro.pricing.Format;
import baro.settings.AppSettings;
import baro.talents.*;
import baro.ui.Navigator;
import baro.ui.Theme;
import baro.ui.views.Blocks;
import baro.ui.widgets.*;
import baro.ui.widgets.Stack;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

/**
 * Вкладка «Таланты»: дерево талантов каждой профессии, выбор талантов, итог «что я получу»
 * и сочетания, которые усиливают друг друга.
 */
public final class TalentsTab extends JPanel {
    private final GameCatalog catalog;
    private final AppSettings settings;
    private final Blocks blocks;
    private final SynergyFinder synergies;
    private final Map<Job, TalentBuild> builds = new LinkedHashMap<>();
    private final SavedBuilds saved;
    private final Toggles<Job> jobs = new Toggles<>(6);
    private final JScrollPane treeScroll = Theme.scroll(null);
    private final Stack side = new Stack(6).padding(10, 12, 16, 12);
    private final JScrollPane sideScroll = Theme.scroll(side);
    private final JLabel points = new JLabel();
    private final EffectsTable effects = new EffectsTable(t -> show(t));
    private TalentTreePanel tree;
    private Job job;
    private Talent focused;

    public TalentsTab(GameCatalog catalog, AppSettings settings, Blocks blocks, Navigator external) {
        super(new BorderLayout());
        this.catalog = catalog;
        this.settings = settings;
        this.synergies = new SynergyFinder(catalog);
        this.saved = new SavedBuilds(settings);
        Navigator inner = new Navigator() {
            @Override public void open(Item item) { external.open(item); }
            @Override public void openMission(CargoMission m) { external.openMission(m); }
            @Override public void openTalent(Talent t) { show(t); }
        };
        this.blocks = blocks.withNavigator(inner);
        setBackground(Theme.BG);
        for (Job j : catalog.jobs()) {
            TalentBuild b = new TalentBuild(j);
            List<Talent> saved = new ArrayList<>();
            for (String id : settings.getString("talents." + j.id(), "").split(","))
                catalog.talent(id).ifPresent(saved::add);
            b.setAll(saved);
            builds.put(j, b);
            jobs.option(j.name(), j);
        }
        jobs.onSelect(this::selectJob);

        JButton reset = new JButton(tr("Сбросить выбор"));
        reset.setFocusable(false);
        reset.addActionListener(e -> { builds.get(job).clear(); save(); tree.refresh(); renderSide(); });
        JButton buildsButton = new JButton(tr("Сборки ▾"));
        buildsButton.setFocusable(false);
        buildsButton.setToolTipText(tr("Сохранить сборку под именем, загрузить, сравнить две сборки, поделиться кодом сборки"));
        buildsButton.addActionListener(e -> buildsMenu().show(buildsButton, 0, buildsButton.getHeight()));
        points.setForeground(Theme.FG);
        points.setFont(Theme.font(13, true));
        JLabel hint = new JLabel(tr("Щелчок — подробно · двойной щелчок или правая кнопка — взять / убрать талант"));
        hint.setForeground(Theme.MUTED);

        JPanel top = new JPanel(new BorderLayout(10, 4));
        top.setBackground(Theme.HEADER);
        top.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        top.add(jobs, BorderLayout.CENTER);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(points); right.add(buildsButton); right.add(reset);
        top.add(right, BorderLayout.EAST);
        top.add(hint, BorderLayout.SOUTH);

        JSplitPane left = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treeScroll, effects);
        left.setResizeWeight(0.55);
        left.setBorder(null);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, sideScroll);
        split.setResizeWeight(0.66);
        split.setBorder(null);
        treeScroll.getHorizontalScrollBar().setUnitIncrement(20);
        treeScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        selectJob(catalog.jobs().get(0));
        SwingUtilities.invokeLater(() -> { split.setDividerLocation(0.66); left.setDividerLocation(0.55); });
    }

    private void selectJob(Job j) {
        if (j == null) return;
        job = j;
        if (focused != null && focused.job() != j) focused = null;
        tree = new TalentTreePanel(builds.get(j), synergies, this::focus, this::toggle);
        tree.focus(focused);
        treeScroll.setViewportView(tree);
        renderSide();
    }

    /** Показать талант (переход по ссылке из любой вкладки). */
    public void show(Talent t) {
        if (job != t.job()) { jobs.select(t.job()); selectJob(t.job()); }
        focus(t);
    }

    private void focus(Talent t) { focused = t; tree.focus(t); renderSide(); sideScroll.getVerticalScrollBar().setValue(0); }

    private void toggle(Talent t) {
        TalentBuild b = builds.get(t.job());
        b.toggle(t); // если взять нельзя — причина показана в карточке справа
        focused = t;
        save();
        tree.focus(t);
        renderSide();
    }

    private void save() {
        for (Map.Entry<Job, TalentBuild> e : builds.entrySet()) {
            StringBuilder s = new StringBuilder();
            for (Talent t : e.getValue().chosen()) { if (s.length() > 0) s.append(','); s.append(t.id()); }
            settings.put("talents." + e.getKey().id(), s);
        }
        settings.save();
    }

    // ---------------- сохранённые сборки и обмен ----------------

    private JPopupMenu buildsMenu() {
        JPopupMenu m = new JPopupMenu();
        TalentBuild b = builds.get(job);
        JMenuItem saveAs = new JMenuItem(tr("Сохранить текущую как…"));
        saveAs.setEnabled(!b.chosen().isEmpty());
        saveAs.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, tr("Название сборки (") + job.name() + "):", tr("Сохранить сборку"), JOptionPane.PLAIN_MESSAGE);
            if (name != null && !name.isBlank()) saved.save(job, name.trim(), b.chosen());
        });
        m.add(saveAs);
        Map<String, List<String>> mine = saved.of(job);
        if (!mine.isEmpty()) {
            m.addSeparator();
            for (Map.Entry<String, List<String>> e : mine.entrySet()) {
                JMenuItem load = new JMenuItem(tr("Загрузить «") + e.getKey() + "» (" + e.getValue().size() + ")");
                load.addActionListener(a -> { b.setAll(SavedBuilds.resolve(catalog, e.getValue())); save(); tree.refresh(); renderSide(); });
                m.add(load);
            }
            JMenu cmp = new JMenu(tr("Сравнить текущую с…"));
            JMenu del = new JMenu(tr("Удалить сборку"));
            for (Map.Entry<String, List<String>> e : mine.entrySet()) {
                JMenuItem c = new JMenuItem(e.getKey());
                c.addActionListener(a -> compareWith(e.getKey(), SavedBuilds.resolve(catalog, e.getValue())));
                cmp.add(c);
                JMenuItem d = new JMenuItem(e.getKey());
                d.addActionListener(a -> {
                    if (JOptionPane.showConfirmDialog(this, tr("Удалить сборку «") + e.getKey() + "»?", tr("Сборки"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
                        saved.delete(job, e.getKey());
                });
                del.add(d);
            }
            m.add(cmp);
            m.add(del);
        }
        m.addSeparator();
        JMenuItem code = new JMenuItem(tr("Копировать код сборки"));
        code.setToolTipText(tr("Короткий код: друг вставит его в свой справочник («Вставить код сборки…»)"));
        code.addActionListener(e -> baro.ui.Links.copy(SavedBuilds.code(job, b.chosen())));
        JMenuItem text = new JMenuItem(tr("Копировать текстом (для чата)"));
        text.addActionListener(e -> baro.ui.Links.copy(SavedBuilds.text(job, b.chosen())));
        JMenuItem paste = new JMenuItem(tr("Вставить код сборки…"));
        paste.addActionListener(e -> pasteCode());
        m.add(code); m.add(text); m.add(paste);
        return m;
    }

    private void pasteCode() {
        String clip = "";
        try { clip = String.valueOf(Toolkit.getDefaultToolkit().getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor)); } catch (Exception ignored) { }
        String code = (String) JOptionPane.showInputDialog(this, tr("Код сборки (BARO1:…):"), tr("Вставить сборку"), JOptionPane.PLAIN_MESSAGE, null, null,
                clip.contains("BARO1:") ? clip.trim() : "");
        if (code == null || code.isBlank()) return;
        java.util.Optional<SavedBuilds.Decoded> d = SavedBuilds.decode(catalog, code);
        if (d.isEmpty()) { JOptionPane.showMessageDialog(this, tr("Это не код сборки справочника."), tr("Вставить сборку"), JOptionPane.WARNING_MESSAGE); return; }
        Job j = d.get().job();
        TalentBuild b = builds.get(j);
        b.setAll(d.get().talents());
        jobs.select(j);
        selectJob(j);
        save();
        if (d.get().unknown() > 0)
            JOptionPane.showMessageDialog(this, tr("Не найдено талантов: ") + d.get().unknown() + tr(" (другая версия игры или моды)."), tr("Вставить сборку"), JOptionPane.INFORMATION_MESSAGE);
    }

    /** Сравнение текущей сборки с сохранённой: чем различаются таланты и прибавки к навыкам. */
    private void compareWith(String name, List<Talent> other) {
        TalentBuild cur = builds.get(job);
        TalentBuild that = new TalentBuild(job);
        that.setAll(other);
        Stack s = new Stack(4).padding(10, 12, 12, 12);
        s.with(blocks.text().text(tr("Текущая сборка / «") + name + "»", Style.TITLE));
        List<Talent> onlyCur = new ArrayList<>(), onlyThat = new ArrayList<>(), both = new ArrayList<>();
        for (Talent t : cur.chosen()) (that.isChosen(t) ? both : onlyCur).add(t);
        for (Talent t : that.chosen()) if (!cur.isChosen(t)) onlyThat.add(t);
        s.with(blocks.section(tr("Только в текущей (") + onlyCur.size() + ")", talentList(onlyCur)));
        s.with(blocks.section(tr("Только в «") + name + "» (" + onlyThat.size() + ")", talentList(onlyThat)));
        s.with(blocks.section(tr("В обеих (") + both.size() + ")", talentList(both)));
        Map<String, Double> a = new BuildSummary(cur, synergies).skillBonuses(), b = new BuildSummary(that, synergies).skillBonuses();
        java.util.Set<String> skills = new java.util.LinkedHashSet<>(a.keySet());
        skills.addAll(b.keySet());
        Stack sk = new Stack(1);
        for (String k : skills) {
            double x = a.getOrDefault(k, 0.0), y = b.getOrDefault(k, 0.0);
            sk.with(blocks.text().text(k + ": ", Style.MUTED).word(Format.signed(x), x >= y ? Style.GOOD : Style.NORMAL)
                    .text(" / ", Style.MUTED).word(Format.signed(y), y >= x ? Style.GOOD : Style.NORMAL));
        }
        if (skills.isEmpty()) sk.with(blocks.text().text(tr("Прибавок к навыкам нет ни в одной."), Style.MUTED));
        s.with(blocks.section(tr("Прибавка к навыкам: текущая / «") + name + "»", sk));
        JScrollPane sc = Theme.scroll(s);
        sc.setPreferredSize(new Dimension(Theme.px(640), Theme.px(520)));
        JOptionPane.showMessageDialog(this, sc, tr("Сравнение сборок — ") + job.name(), JOptionPane.PLAIN_MESSAGE);
    }

    private RichText talentList(List<Talent> l) {
        RichText r = blocks.text();
        if (l.isEmpty()) return r.text("—", Style.MUTED);
        return r.join(l, Style.MUTED, t -> r.link(t).text(" (" + t.subtree().name() + ")", Style.MUTED));
    }

    // ---------------- правая панель ----------------

    private void renderSide() {
        TalentBuild b = builds.get(job);
        points.setText(job.name() + tr(": выбрано талантов ") + b.chosen().size());
        effects.show(job.name(), b.chosen());
        side.removeAll();
        if (focused != null) side.with(talentCard(focused, b));
        side.with(summary(b));
        side.with(crew());
        side.with(jobSynergies());
        side.revalidate();
        side.repaint();
    }

    private Stack talentCard(Talent t, TalentBuild b) {
        Stack s = new Stack(3);
        s.with(blocks.text().text(t.name(), Style.TITLE).newLine().word((tr("Талант · ") + t.owner()).toUpperCase(), Style.MUTED));
        TalentBuild.State st = b.state(t);
        RichText state = blocks.text();
        switch (st) {
            case CHOSEN: state.word(tr("Выбран"), Style.GOOD); break;
            case AVAILABLE: state.word(tr("Можно взять"), Style.ACCENT); break;
            default: state.text(b.reason(t), Style.WARN);
        }
        s.with(state);
        JButton act = new JButton(st == TalentBuild.State.CHOSEN ? tr("Убрать талант") : tr("Взять талант"));
        act.setEnabled(st == TalentBuild.State.CHOSEN || st == TalentBuild.State.AVAILABLE);
        act.setFocusable(false);
        act.addActionListener(e -> toggle(t));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.add(act);
        s.with(row);
        s.with(blocks.text().text(t.description(), Style.NORMAL));
        if (!t.recipes().isEmpty()) {
            Stack rec = new Stack(1);
            for (Item i : t.recipes()) rec.with(blocks.text().word("• ", Style.ACCENT).link(i).text("  " + i.category().title(), Style.MUTED));
            s.with(blocks.section(tr("Открывает рецепты"), rec));
        }
        if (!t.themes().isEmpty()) {
            StringBuilder th = new StringBuilder();
            for (String x : t.themes()) { if (th.length() > 0) th.append(" · "); th.append(catalog.themeTitle(x)); }
            s.with(blocks.text().text(tr("Темы: ") + th, Style.MUTED));
        }
        List<Synergy> syn = synergies.forTalent(t);
        Stack sy = new Stack(3);
        if (syn.isEmpty()) sy.with(blocks.text().text(tr("Явных сочетаний не найдено."), Style.MUTED));
        for (Synergy x : syn.subList(0, Math.min(6, syn.size()))) sy.with(synergyLine(x, t));
        s.with(blocks.section(tr("Лучше всего сочетается с"), sy));
        return s;
    }

    private RichText synergyLine(Synergy x, Talent from) {
        RichText r = blocks.text();
        if (from != null) {
            Talent o = x.other(from);
            r.link(o).text(" (" + o.subtree().name() + ")", Style.MUTED);
        } else r.link(x.a()).word(" + ", Style.MUTED).link(x.b());
        r.text(" — " + String.join("; ", x.reasons()), Style.MUTED);
        return r;
    }

    private Stack summary(TalentBuild b) {
        BuildSummary sum = new BuildSummary(b, synergies);
        Stack s = new Stack(3);
        if (b.chosen().isEmpty()) {
            s.with(blocks.text().text(tr("Выберите таланты в дереве — здесь появится, что вы получите."), Style.MUTED));
            return blocks.section(tr("Что я получу"), s);
        }
        s.with(blocks.text().text(tr("Талантов: ") + sum.points() + tr(" (одно очко таланта за каждый уровень персонажа)."), Style.MUTED));
        Map<String, Double> sk = sum.skillBonuses();
        if (!sk.isEmpty()) {
            RichText r = blocks.text().text(tr("Прибавка к навыкам: "), Style.MUTED);
            s.with(r.join(sk.entrySet(), Style.MUTED, e -> r.text(e.getKey() + " ", Style.NORMAL).word(Format.signed(e.getValue()), Style.GOOD)));
        }
        List<Item> rec = sum.recipes();
        if (!rec.isEmpty()) {
            RichText r = blocks.text().text(tr("Открытые рецепты: "), Style.MUTED);
            for (int i = 0; i < rec.size(); i++) { if (i > 0) r.word(", ", Style.MUTED); r.link(rec.get(i)); }
            s.with(r);
        }
        Stack eff = new Stack(2).padding(0, 10, 0, 0);
        for (Talent t : b.chosen()) eff.with(blocks.text().link(t).text(" (" + t.subtree().name() + "): " + t.description().replace('\n', ' '), Style.NORMAL));
        s.with(blocks.text().word(tr("Все эффекты:"), Style.BOLD), eff);
        List<Synergy> act = sum.activeSynergies();
        if (!act.isEmpty()) {
            Stack a = new Stack(2).padding(0, 10, 0, 0);
            for (Synergy x : act) a.with(blocks.text().badge().link(x.a()).word(" + ", Style.MUTED).link(x.b()).text(" — " + String.join("; ", x.reasons()), Style.MUTED));
            s.with(blocks.text().word(tr("Работающие сочетания:"), Style.BOLD), a);
        }
        List<Synergy> sug = sum.suggestions(5);
        if (!sug.isEmpty()) {
            Stack a = new Stack(2).padding(0, 10, 0, 0);
            for (Synergy x : sug) {
                Talent missing = b.isChosen(x.a()) ? x.b() : x.a(), have = b.isChosen(x.a()) ? x.a() : x.b();
                a.with(blocks.text().text(tr("взять "), Style.MUTED).link(missing).text(" (" + missing.subtree().name() + tr(") — сочетается с «") + have.name() + "»: "
                        + String.join("; ", x.reasons()), Style.MUTED));
            }
            s.with(blocks.text().word(tr("Что добрать к выбранному:"), Style.BOLD), a);
        }
        return blocks.section(tr("Что я получу — ") + job.name(), s);
    }

    /** Кто из других профессий поднимет навыки этой профессии своими талантами. */
    private Stack crew() {
        Stack s = new Stack(3);
        StringBuilder main = new StringBuilder();
        for (AllyBuff.Skill k : CrewSynergy.mainSkills(job)) { if (main.length() > 0) main.append(", "); main.append(k.title()); }
        s.with(blocks.text().text(tr("Важные навыки профессии: ") + main + tr(". Учтены только бонусы к навыкам, которые таланты других профессий дают союзникам (по файлам игры)."), Style.MUTED));
        List<String> none = new ArrayList<>();
        int place = 0;
        for (CrewSynergy.Provider p : CrewSynergy.forJob(job, catalog.jobs())) {
            if (p.offers().isEmpty()) { none.add(p.job().name()); continue; }
            place++;
            s.with(blocks.text().word(place + ". " + p.job().name(), Style.BOLD)
                    .text(place == 1 ? tr("  — максимальная синергия") : "", Style.GOOD)
                    .text(tr("  (польза ") + Math.round(p.score()) + ")", Style.MUTED));
            Stack offers = new Stack(2).padding(0, 14, 0, 0);
            TalentBuild theirs = builds.get(p.job());
            for (CrewSynergy.Offer o : p.offers()) {
                RichText r = blocks.text().link(o.talent()).text(" (" + o.talent().subtree().name() + "): " + describe(o.buffs()), Style.NORMAL);
                if (theirs != null && theirs.isChosen(o.talent())) r.text(tr("  ✓ выбран в сборке «") + p.job().name() + "»", Style.GOOD);
                offers.with(r);
            }
            s.with(offers);
        }
        if (place == 0) s.with(blocks.text().text(tr("Ни одна другая профессия не даёт союзникам бонусов к навыкам этой профессии."), Style.MUTED));
        if (!none.isEmpty()) s.with(blocks.text().text(tr("Не дают союзникам бонусов к навыкам: ") + String.join(", ", none) + ".", Style.MUTED));
        return blocks.section(tr("Кто из экипажа усилит навыки — ") + job.name(), s);
    }

    /** «+10 ко всем навыкам (рядом); все навыки растут на 25% быстрее». */
    private static String describe(List<AllyBuff> buffs) {
        Map<String, List<AllyBuff>> byCond = new LinkedHashMap<>();
        for (AllyBuff b : buffs) byCond.computeIfAbsent(b.condition(), k -> new ArrayList<>()).add(b);
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, List<AllyBuff>> e : byCond.entrySet()) {
            List<String> items = new ArrayList<>();
            Map<Double, Integer> bonusAll = new LinkedHashMap<>();
            for (AllyBuff b : e.getValue()) if (b.isBonus() && b.skill() != null) bonusAll.merge(b.value(), 1, Integer::sum);
            for (Map.Entry<Double, Integer> v : bonusAll.entrySet())
                if (v.getValue() == AllyBuff.Skill.values().length) items.add("+" + Format.signed(v.getKey()).replace("+", "") + tr(" ко всем навыкам"));
            for (AllyBuff b : e.getValue()) {
                if (b.isBonus() && b.skill() != null && bonusAll.getOrDefault(b.value(), 0) == AllyBuff.Skill.values().length) continue;
                items.add(b.describe());
            }
            parts.add(String.join(", ", items) + (e.getKey().isEmpty() ? "" : " (" + e.getKey() + ")"));
        }
        return String.join("; ", parts);
    }

    private Stack jobSynergies() {
        Stack s = new Stack(3);
        List<Synergy> list = synergies.forJob(job);
        for (Synergy x : list.subList(0, Math.min(10, list.size()))) s.with(synergyLine(x, null));
        if (list.isEmpty()) s.with(blocks.text().text(tr("Сочетаний не найдено."), Style.MUTED));
        s.with(blocks.text().text(tr("Сочетания найдены по данным игры: общие характеристики (бонусы складываются), общие темы и цепочки рецептов. ")
                + tr("Учитываются только таланты, которые можно взять вместе."), Style.MUTED));
        return blocks.section(tr("Самые сильные сочетания — ") + job.name(), s);
    }
}
