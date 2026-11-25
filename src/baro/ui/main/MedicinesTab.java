package baro.ui.main;


import static baro.Tr.tr;
import baro.data.AfflictionIndex;
import baro.data.GameCatalog;
import baro.model.*;
import baro.pricing.Format;
import baro.search.EffectMode;
import baro.search.ItemSearch;
import baro.search.MedicineMatch;
import baro.search.MedicineSearch;
import baro.ui.Theme;
import baro.ui.views.AfflictionView;
import baro.ui.views.BestPicksView;
import baro.ui.views.Blocks;
import baro.ui.views.MedicineDetailView;
import baro.ui.widgets.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Predicate;

/**
 * Вкладка «Медицина»: препараты (поиск по названию и по эффекту, разделы, лучшие) и болезни —
 * «от проблемы»: что это, чем лечить, что вызывает, что защищает.
 */
final class MedicinesTab extends JSplitPane {
    private enum Mode { MEDICINES, AFFLICTIONS, TREATMENT }
    private enum Kind { AILMENTS, BUFFS, ALL }

    private final GameCatalog catalog;
    private final MedicineSearch search;
    private final Toggles<Mode> mode = new Toggles<>(3);
    private final SearchField name = new SearchField(tr("Название препарата…"), 16);
    private final SearchField effect = new SearchField(tr("Эффект: кровь, скорость, хаск…"), 16);
    private final JComboBox<EffectMode> effectMode = new JComboBox<>(EffectMode.values());
    private final Toggles<MedCategory> categories = new Toggles<>(3);
    private final SearchField ailment = new SearchField(tr("Недуг: кровотечение, хаск, яд…"), 16);
    private final Toggles<Kind> kinds = new Toggles<>(3);
    private final JPanel filters = new JPanel(new BorderLayout());
    private Stack medFilters, ailFilters, treatFilters;
    private final SearchField treatSearch = new SearchField(tr("Найти недуг…"), 16);
    private final JSpinner skill = new JSpinner(new SpinnerNumberModel(50, 0, 100, 5));
    /** Отмеченные недуги для плана лечения (по порядку отметки). */
    private final java.util.LinkedHashSet<Affliction> chosen = new java.util.LinkedHashSet<>();
    private final Blocks blocks;
    private final DefaultListModel<Object> model = new DefaultListModel<>();
    private final JList<Object> list = new JList<>(model);
    private final DetailPane<Object> detail;
    private boolean updating;

    MedicinesTab(GameCatalog catalog, Blocks blocks, baro.ui.Navigator external) {
        super(HORIZONTAL_SPLIT);
        this.catalog = catalog;
        this.search = new MedicineSearch(catalog.medicines());
        this.blocks = blocks;
        this.detail = new DetailPane<>((v, nav) -> {
            Blocks b = blocks.withNavigator(nav);
            if (v instanceof Affliction a) return new AfflictionView(a, catalog, b);
            Item item = (Item) v;
            Medicine m = catalog.medicineOf(item).orElse(null);
            return m == null ? baro.ui.views.ItemViews.forItem(item, catalog, b)
                    : new MedicineDetailView(m, b, effectHit(), catalog.missionsWith(item));
        }, nav -> mode.selected() == Mode.TREATMENT ? treatmentView(blocks.withNavigator(nav))
                : mode.selected() == Mode.AFFLICTIONS ? afflictionsOverview(blocks.withNavigator(nav))
                : new BestPicksView(catalog, blocks.withNavigator(nav), categories.selected()),
                (item, nav) -> baro.ui.views.ItemViews.forItem(item, catalog, blocks.withNavigator(nav)),
                item -> item, new baro.ui.Navigator() {
                    @Override public void open(Item item) { external.open(item); }
                    @Override public void openMission(CargoMission m) { external.openMission(m); }
                    @Override public void openTalent(Talent t) { external.openTalent(t); }
                    @Override public void openLore(Object entry) { if (entry instanceof Affliction a) MedicinesTab.this.open(a); else external.openLore(entry); }
                });

        mode.option(tr("Препараты"), Mode.MEDICINES).option(tr("Болезни"), Mode.AFFLICTIONS).option(tr("План лечения"), Mode.TREATMENT);
        mode.setToolTipText(tr("Болезни — «от проблемы»: у меня кровотечение / хаск / отравление — что делать"));
        mode.onSelect(m -> { showFilters(m); refill(); detail.clear(); });

        categories.option(tr("Все"), null);
        for (MedCategory c : MedCategory.values()) categories.option(c.title().split(" ")[0], c);
        categories.onSelect(c -> { refill(); if (list.getSelectedValue() == null) detail.clear(); });
        name.onChange(this::refill);
        effect.onChange(() -> { refill(); detail.refresh(); });
        effectMode.addActionListener(e -> { refill(); detail.refresh(); });
        kinds.option(tr("Недуги"), Kind.AILMENTS).option(tr("Баффы и защиты"), Kind.BUFFS).option(tr("Все"), Kind.ALL);
        kinds.onSelect(k -> refill());
        ailment.onChange(this::refill);
        treatSearch.onChange(this::refill);
        skill.setToolTipText(tr("Навык «Медицина» того, кто лечит: ниже нужного — препарат может подействовать хуже (провал)"));
        skill.addChangeListener(e -> detail.clear());

        Rows row = new Rows(13f);
        JLabel header = new JLabel();
        header.setFont(Theme.font(12f, true));
        header.setForeground(Theme.ACCENT);
        header.setOpaque(true);
        header.setBackground(Theme.PANEL);
        header.setBorder(BorderFactory.createEmptyBorder(6, 6, 2, 6));
        list.setFixedCellWidth(40); // ширина по окну, а не по самой длинной строке
        list.setCellRenderer((l, v, i, sel, focus) -> {
            if (v instanceof String h) { header.setText(h.toUpperCase(java.util.Locale.ROOT)); return header; }
            if (v instanceof Affliction a && mode.selected() == Mode.TREATMENT) {
                boolean on = chosen.contains(a);
                int cures = catalog.afflictions().treatments(a).size();
                return row.set(a.name(), on ? Theme.ACCENT : Theme.FG, cures == 0 ? tr("лечения нет") : on ? tr("отмечено") : "", on ? Theme.SELL : Theme.WARN, false,
                        new GlyphIcon(on ? "☑" : "☐", on ? Theme.ACCENT : Theme.MUTED, Theme.px(16)));
            }
            if (v instanceof Affliction a) {
                int cures = a.isBuff() ? 0 : catalog.afflictions().treatments(a).size();
                String right = a.isBuff() ? Affliction.typeTitle(a.type()) : cures == 0 ? tr("лечения нет") : tr("лечат: ") + cures;
                return row.set(a.name(), Theme.FG, right, a.isBuff() ? Theme.ACCENT : cures == 0 ? Theme.WARN : Theme.SELL, sel, null);
            }
            MedicineMatch mm = (MedicineMatch) v;
            Medicine m = mm.medicine();
            String price = blocks.prices().sellPrice(m.item()).map(Format::money).orElse("—");
            String right = mm.effect().map(e -> {
                double val = e.value().orElse(0.0);
                String n = e.name().length() > 28 ? e.name().substring(0, 27) + "…" : e.name();
                return (Math.abs(val) < 0.05 ? "" : Format.signed(val) + " ") + n + " · " + price;
            }).orElse(price);
            Icon ic = blocks.icon(m.item(), 20);
            Icon star = m.isBest() ? StarBadge.of(14) : null;
            return row.set(m.item().displayName(), m.isBest() ? Theme.ACCENT : Theme.FG, right, Theme.SELL, sel, IconRowIcon.of(ic, star));
        });
        list.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || updating) return;
            Object v = list.getSelectedValue();
            if (mode.selected() == Mode.TREATMENT) {
                if (v instanceof Affliction a) { if (!chosen.remove(a)) chosen.add(a); }
                SwingUtilities.invokeLater(() -> { updating = true; list.clearSelection(); updating = false; list.repaint(); detail.clear(); });
                return;
            }
            if (v instanceof MedicineMatch mm) detail.show(mm.medicine().item());
            else if (v instanceof Affliction a) detail.show(a);
        });

        JPanel effRow = new JPanel(new BorderLayout(4, 0));
        effRow.setOpaque(false);
        effRow.add(effect, BorderLayout.CENTER);
        effRow.add(effectMode, BorderLayout.EAST);
        medFilters = new Stack(4);
        medFilters.with(fixed(name), fixed(effRow), fixed(categories));
        ailFilters = new Stack(4);
        ailFilters.with(fixed(ailment), fixed(kinds));
        JButton reset = new JButton(tr("Снять отметки"));
        reset.setFocusable(false);
        reset.addActionListener(e -> { chosen.clear(); list.repaint(); detail.clear(); });
        JPanel skillRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        skillRow.setOpaque(false);
        JLabel sl = new JLabel(tr("Навык «Медицина»:"));
        sl.setForeground(Theme.MUTED);
        skillRow.add(sl); skillRow.add(skill); skillRow.add(reset);
        JLabel hint = new JLabel(tr("Отметьте щелчком всё, что у персонажа:"));
        hint.setForeground(Theme.MUTED);
        treatFilters = new Stack(4);
        treatFilters.with(fixed(hint), fixed(treatSearch), fixed(skillRow));
        filters.setOpaque(false);
        showFilters(Mode.MEDICINES);
        Stack top = new Stack(4).padding(6, 6, 4, 6);
        top.with(fixed(mode), fixed(filters));

        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(Theme.PANEL);
        left.add(top, BorderLayout.NORTH);
        left.add(Theme.scroll(list), BorderLayout.CENTER);
        left.setMinimumSize(new Dimension(300, 200));
        setLeftComponent(left);
        setRightComponent(detail);
        setDividerLocation(400);
        setBorder(null);
        refill();
    }

    private static JComponent fixed(JComponent c) { c.setAlignmentX(LEFT_ALIGNMENT); return c; }

    private Predicate<Effect> effectHit() {
        String q = effect.getText();
        if (q.trim().isEmpty()) return e -> false;
        java.util.Set<String> names = new java.util.HashSet<>();
        for (Medicine m : catalog.medicines()) for (Effect e : m.onSuccess()) names.add(e.name());
        baro.search.EffectMatcher em = baro.search.EffectMatcher.of(q, (EffectMode) effectMode.getSelectedItem(), names);
        return em::matches;
    }

    private void refill() {
        updating = true;
        Object keep = list.getSelectedValue();
        model.clear();
        if (mode.selected() == Mode.TREATMENT) {
            String q = ItemSearch.normalize(treatSearch.getText());
            AfflictionIndex idx = catalog.afflictions();
            List<Affliction> all = catalog.afflictions().all().stream().filter(a -> idx.isAilment(a) && idx.isListed(a) && a.nameContains(q)).toList();
            List<Affliction> common = all.stream().filter(a -> COMMON.contains(a.id())).toList();
            group(tr("Частые"), common);
            group(tr("Все недуги"), all.stream().filter(a -> !COMMON.contains(a.id())).toList());
        } else if (mode.selected() == Mode.AFFLICTIONS) {
            String q = ItemSearch.normalize(ailment.getText());
            Kind k = kinds.selected();
            List<Affliction> all = catalog.afflictions().all();
            AfflictionIndex idx = catalog.afflictions();
            if (k != Kind.BUFFS) group(tr("Недуги"), all.stream().filter(a -> idx.isAilment(a) && idx.isListed(a) && a.nameContains(q)).toList());
            if (k != Kind.AILMENTS) group(tr("Баффы, защиты и генетика"), all.stream().filter(a -> !idx.isAilment(a) && idx.isListed(a) && a.nameContains(q)).toList());
            if (keep instanceof Affliction && model.contains(keep)) list.setSelectedValue(keep, true);
        } else {
            List<MedicineMatch> found = search.find(name.getText(), effect.getText(), (EffectMode) effectMode.getSelectedItem(), categories.selected());
            for (MedicineMatch m : found) model.addElement(m);
            if (keep instanceof MedicineMatch km) for (int i = 0; i < model.size(); i++)
                if (model.get(i) instanceof MedicineMatch mm && mm.medicine() == km.medicine()) { list.setSelectedIndex(i); break; }
        }
        updating = false;
    }

    private void group(String title, List<Affliction> l) {
        if (l.isEmpty()) return;
        model.addElement(title + " · " + l.size());
        for (Affliction a : l) model.addElement(a);
    }

    private static final java.util.Set<String> COMMON = java.util.Set.of("bleeding", "bloodloss", "burn", "blunttrauma", "lacerations", "gunshotwound",
            "bitewounds", "explosiondamage", "internaldamage", "huskinfection", "psychosis", "radiationsickness", "paralysis", "oxygenlow",
            "opiateoverdose", "opiatewithdrawal", "infection", "acidburn", "organdamage", "stun", "nausea", "drunk");

    /** План лечения отмеченных недугов: какие препараты, что каждый снимает, побочные эффекты, где взять. */
    private Component treatmentView(Blocks b) {
        Stack s = new Stack(4).padding(10, 12, 16, 12);
        s.with(b.text().text(tr("План лечения"), Style.TITLE));
        if (chosen.isEmpty()) {
            s.with(b.text().text(tr("Отметьте слева всё, что у персонажа (например, кровотечение, ожог и кровопотерю), и укажите навык «Медицина» лечащего. ")
                    + tr("Справочник подберёт наименьший набор препаратов, который снимает всё сразу; что есть на борту (по сейву) — в приоритете."), Style.MUTED));
            return s;
        }
        RichText what = b.text().text(tr("Отмечено: "), Style.MUTED);
        s.with(what.join(chosen, Style.MUTED, a -> what.lore(a.name(), a, null)));
        int sk = (Integer) skill.getValue();
        baro.data.TreatmentPlanner.Plan plan = baro.data.TreatmentPlanner.plan(catalog.afflictions(), new java.util.ArrayList<>(chosen), sk,
                b::onBoard, it -> b.onBoard(it) > 0 || b.prices().cheapestBuy(it).isPresent() || !it.recipes().isEmpty());
        int n = 1;
        for (baro.data.TreatmentPlanner.Pick p : plan.picks()) {
            Stack one = new Stack(1);
            RichText head = b.text().word(n++ + ".  ", Style.ACCENT).link(p.medicine().item());
            if (p.medicine().isBest()) head.text("  ", Style.MUTED).badge();
            one.with(head);
            RichText cures = b.text().text(tr("    снимает: "), Style.MUTED);
            one.with(cures.join(p.cures().entrySet(), Style.MUTED, e -> cures.text(e.getKey().name() + " ", Style.NORMAL).word("−" + Format.num(e.getValue()), Style.GOOD)));
            if (!p.sideEffects().isEmpty()) {
                RichText side = b.text().text(tr("    побочно: "), Style.MUTED);
                one.with(side.join(p.sideEffects(), Style.MUTED, e -> side.text(e.name() + " ", Style.NORMAL).word("+" + Format.num(e.value().orElse(0.0)), Style.BAD)));
            }
            RichText where = b.text().text("    ", Style.MUTED);
            if (p.onBoard() > 0) where.word(tr("на борту ") + p.onBoard() + tr(" шт."), Style.GOOD);
            else b.prices().cheapestBuy(p.medicine().item()).ifPresentOrElse(
                    o -> where.text(tr("купить: ") + o.store().title() + " ", Style.MUTED).word(Format.money(o.price()), Style.BUY),
                    () -> where.text(p.medicine().item().recipes().isEmpty() ? tr("не продаётся") : tr("только крафт"), Style.WARN));
            if (p.skillTooLow()) where.text(tr("  · нужен навык «Медицина» ") + p.medicine().useSkill().orElse(0) + tr(" — при меньшем возможен провал"), Style.WARN);
            one.with(where);
            s.with(one);
        }
        if (!plan.untreated().isEmpty()) {
            RichText u = b.text().text(tr("Нет препарата от: "), Style.WARN);
            s.with(u.join(plan.untreated(), Style.MUTED, a -> u.lore(a.name(), a, null)));
            s.with(b.text().text(tr("Такие недуги проходят сами, лечатся покоем, кислородом или хирургией."), Style.MUTED));
        }
        s.with(b.text().text(tr("Сила лечения — итог за всё время действия препарата. Порядок — по пользе: первым то, что снимает больше отмеченного."), Style.MUTED));
        return s;
    }

    private Component afflictionsOverview(Blocks b) {
        Stack s = new Stack(4).padding(10, 12, 16, 12);
        s.with(b.text().text(tr("Болезни и состояния"), Style.TITLE));
        s.with(b.text().text(tr("Выберите недуг слева — карточка покажет, чем его лечить (сильнее — выше), какие баффы защищают и что его вызывает: ")
                + tr("оружие, яды, побочные эффекты препаратов. Ссылки на недуги есть и в карточках препаратов."), Style.MUTED));
        Stack quick = new Stack(2);
        for (String id : new String[]{"bleeding", "bloodloss", "burn", "huskinfection", "psychosis", "radiationsickness", "paralysis", "oxygenlow", "opiateoverdose", "infection"}) {
            catalog.afflictions().get(id).ifPresent(a -> {
                int n = catalog.afflictions().treatments(a).size();
                quick.with(b.text().lore(a.name(), a, null).text(n == 0 ? tr(" — лечения нет") : tr(" — лечат ") + n + tr(" препарат(ов)"), Style.MUTED));
            });
        }
        if (quick.getComponentCount() > 0) s.with(b.section(tr("Частые случаи"), quick));
        if (!catalog.fromGame()) s.with(b.text().text(tr("Игра не найдена: список недугов собран из эффектов встроенных препаратов, без описаний и типов."), Style.WARN));
        return s;
    }

    boolean contains(Item item) { return catalog.medicineOf(item).isPresent(); }

    void open(Item item) {
        if (mode.selected() != Mode.MEDICINES) switchMode(Mode.MEDICINES);
        updating = true;
        list.clearSelection();
        for (int i = 0; i < model.size(); i++)
            if (model.get(i) instanceof MedicineMatch mm && mm.medicine().item() == item) { list.setSelectedIndex(i); list.ensureIndexIsVisible(i); break; }
        updating = false;
        detail.show(item);
    }

    /** Открыть карточку недуга (по ссылке из эффекта препарата). */
    void open(Affliction a) {
        if (mode.selected() != Mode.AFFLICTIONS) switchMode(Mode.AFFLICTIONS);
        updating = true;
        if (!model.contains(a)) { ailment.setText(""); kinds.select(Kind.ALL); updating = false; refill(); updating = true; }
        list.setSelectedValue(a, true);
        updating = false;
        detail.show(a);
    }

    private void switchMode(Mode m) {
        mode.select(m);
        showFilters(m);
        refill();
    }

    private void showFilters(Mode m) {
        filters.removeAll();
        filters.add(m == Mode.TREATMENT ? treatFilters : m == Mode.AFFLICTIONS ? ailFilters : medFilters);
        filters.revalidate();
        filters.repaint();
    }

    void pricesChanged() { list.repaint(); detail.refresh(); }

    void focusSearch() { name.requestFocusInWindow(); }
}
