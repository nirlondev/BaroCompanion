package baro.ui.main;


import static baro.Tr.tr;
import baro.bestiary.MissionInfo;
import baro.bestiary.MissionKind;
import baro.data.GameCatalog;
import baro.model.CargoMission;
import baro.planner.PlanCache;
import baro.pricing.CargoValuation;
import baro.pricing.Format;
import baro.ui.Theme;
import baro.ui.views.Blocks;
import baro.ui.views.LoreViews;
import baro.ui.views.MissionDetailView;
import baro.ui.widgets.RichText;
import baro.ui.widgets.Stack;
import baro.ui.widgets.Style;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Вкладка «Миссии»: все миссии игры любого типа, по группам, с выбором типа. Доставка — грузовые миссии
 * с содержимым ящиков, разбором и подбором лучшей переработки груза.
 */
final class AllMissionsTab extends LoreTab<AllMissionsTab.Section> {
    /** Разделы: обычные (выпадают на станциях), особые (сюжет, события, побочные цели), все. */
    enum Section { REGULAR, SPECIAL, ALL }

    /** Вариант выбора типа: null — все типы. */
    record TypeOption(MissionKind kind) {
        @Override public String toString() { return kind == null ? tr("Все типы") : kind.title(); }
    }

    private final Blocks blocks;
    private final baro.app.SaveWatcher saves;
    private final CargoValuation valuation;
    private final PlanCache cache;
    private final JComboBox<TypeOption> type = new JComboBox<>();
    private boolean typesFilled, filling;
    private Map<CargoMission, MissionInfo> loreLinks;

    AllMissionsTab(GameCatalog catalog, BestiarySource source, Blocks blocks, baro.ui.Navigator external, baro.app.SaveWatcher saves,
                   CargoValuation valuation, PlanCache cache) {
        super(catalog, source, blocks, external, tr("Миссия, монстр, предмет…"), 3);
        this.blocks = blocks;
        this.saves = saves;
        this.valuation = valuation;
        this.cache = cache;
        saves.addListener(this::pricesChanged);
        sections.option(tr("Обычные"), Section.REGULAR).option(tr("Сюжет и события"), Section.SPECIAL).option(tr("Все"), Section.ALL);
        sectionsReady();

        type.addItem(new TypeOption(null));
        if (!catalog.missions().isEmpty()) type.addItem(new TypeOption(MissionKind.CARGO));
        type.setFocusable(false);
        type.setMaximumRowCount(20);
        type.addActionListener(e -> { if (!filling) refilter(); });
        JLabel label = new JLabel(tr("Тип:"));
        label.setForeground(Theme.MUTED);
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.add(label, BorderLayout.WEST);
        row.add(type, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        addFilter(row);

        // выручка переработки груза считается в фоне; после смены цен — заново
        cache.addListener(this::repaintList);
        cache.addListener(() -> cache.prefetch(catalog.missions()));
        cache.prefetch(catalog.missions());
    }

    private MissionKind selectedKind() {
        TypeOption o = (TypeOption) type.getSelectedItem();
        return o == null ? null : o.kind();
    }

    /** Типы в выборе — только те, что есть в игре (с модами), в порядке групп списка. */
    private void fillTypes() {
        if (typesFilled || data == null) return;
        typesFilled = true;
        MissionKind keep = selectedKind();
        filling = true;
        type.removeAllItems();
        type.addItem(new TypeOption(null));
        for (MissionKind k : MissionKind.values()) {
            boolean present = k == MissionKind.CARGO && !catalog.missions().isEmpty();
            for (MissionInfo m : data.missions()) if (m.kind() == k) { present = true; break; }
            if (present) type.addItem(new TypeOption(k));
        }
        for (int i = 0; i < type.getItemCount(); i++) if (type.getItemAt(i).kind() == keep) type.setSelectedIndex(i);
        filling = false;
    }

    @Override protected List<Object> entries(Section section, String q) {
        fillTypes();
        MissionKind only = selectedKind();
        List<Object> out = new ArrayList<>();
        for (MissionKind k : MissionKind.values()) {
            if (only != null && k != only) continue;
            List<Object> l = new ArrayList<>();
            // доставка — грузовые миссии с расчётом переработки (все выпадают на станциях); остальные миссии доставки — как есть
            boolean cargo = k == MissionKind.CARGO;
            if (cargo && section != Section.SPECIAL)
                for (CargoMission m : catalog.missions()) if (q.isEmpty() || cargoMatches(m, q)) l.add(m);
            for (MissionInfo m : data.missions()) {
                if (m.kind() != k || cargo && hasCargo(m)) continue;
                if (section == Section.REGULAR && m.commonness() <= 0 || section == Section.SPECIAL && m.commonness() > 0) continue;
                if (!q.isEmpty() && !m.nameContains(q) && !mentions(m, q)) continue;
                l.add(m);
            }
            if (!l.isEmpty()) { out.add(new Header(k.title() + " · " + l.size())); out.addAll(l); }
        }
        return out;
    }

    /** У миссии есть грузовая версия с расчётом переработки (тогда в списке показывается она). */
    private boolean hasCargo(MissionInfo m) { return lore().containsValue(m); }

    /**
     * Грузовая миссия → та же миссия из файлов игры (тексты, репутация). Id у них разные (грузовые описаны в справочнике
     * вручную), поэтому сопоставляются по содержимому ящиков: больше всего общих предметов при наименьшем лишнем.
     */
    private Map<CargoMission, MissionInfo> lore() {
        if (loreLinks != null || data == null) return loreLinks != null ? loreLinks : Map.of();
        loreLinks = new HashMap<>();
        for (CargoMission c : catalog.missions()) {
            Set<String> ids = new HashSet<>();
            for (baro.model.Item i : c.contents().keySet()) ids.add(i.id().toLowerCase(java.util.Locale.ROOT));
            MissionInfo best = null;
            double bestScore = 0;
            for (MissionInfo m : data.missions()) {
                if (m.kind() != MissionKind.CARGO) continue;
                Set<String> other = new HashSet<>();
                for (MissionInfo.Count x : m.items()) other.add(x.id().toLowerCase(java.util.Locale.ROOT));
                int common = 0;
                for (String id : ids) if (other.contains(id)) common++;
                double score = common == 0 ? 0 : (double) common / (ids.size() + other.size() - common);
                if (score > bestScore) { bestScore = score; best = m; }
            }
            if (best != null) loreLinks.put(c, best);
        }
        return loreLinks;
    }

    /** Поиск миссии доставки по названию и по предметам в грузе. */
    private static boolean cargoMatches(CargoMission m, String q) {
        if (m.nameContains(q)) return true;
        for (baro.model.Item i : m.contents().keySet()) if (i.nameContains(q)) return true;
        return false;
    }

    /** Поиск и по участникам: монстрам, людям, предметам. */
    private boolean mentions(MissionInfo m, String q) {
        for (MissionInfo.Count c : m.monsters())
            if (data.creature(c.id()).map(x -> x.nameContains(q)).orElse(c.id().toLowerCase().contains(q))) return true;
        for (MissionInfo.Count c : m.npcs()) if (data.npc(c.id()).map(x -> x.nameContains(q)).orElse(false)) return true;
        for (MissionInfo.Count c : m.items()) {
            String name = catalog.item(c.id()).map(baro.model.Item::displayName).orElse(data.itemName(c.id()));
            if (baro.search.ItemSearch.normalize(name).contains(q)) return true;
        }
        return false;
    }

    @Override protected String[] right(Object e) {
        if (e instanceof CargoMission m) {
            Integer v = bestValue(m);
            if (v == null) return new String[]{tr("считаю…")};
            if (v <= 0) return new String[]{"—"};
            return new String[]{tr("до ") + Format.money(v), v > valuation.reward(m) ? "sell*" : "sell"};
        }
        if (e instanceof MissionInfo m) {
            float mul = blocks.prices().settings().rewardMultiplier();
            return new String[]{(m.reward() > 0 ? Format.money(Math.round(m.reward() * mul)) : "—") + "  " + LoreViews.stars(m.difficulty())};
        }
        return new String[]{""};
    }

    /** Выручка лучшего плана для одного ящика, или null — ещё считается. */
    private Integer bestValue(CargoMission m) {
        return cache.get(m, 1).map(l -> cache.planner().best(l).map(r -> cache.planner().value(r.plan())).orElse(0)).orElse(null);
    }

    /** Миссия доставки — расчёт переработки груза, ниже — та же миссия из файлов игры (сюжет и оплата). */
    @Override protected Component card(Object entry, Blocks b) {
        if (!(entry instanceof CargoMission m)) return super.card(entry, b);
        MissionDetailView view = new MissionDetailView(m, b, valuation, cache);
        MissionInfo lore = lore().get(m);
        if (lore == null) return view;
        return new Stack(0).with(view, new LoreViews(data, catalog, b).mission(lore));
    }

    /** Ссылки на миссию доставки (из сейва, бестиария) открывают её грузовую версию с расчётом переработки. */
    @Override void open(Object entry) {
        if (entry instanceof MissionInfo mi)
            for (Map.Entry<CargoMission, MissionInfo> e : lore().entrySet()) if (e.getValue() == mi) { entry = e.getKey(); break; }
        super.open(entry);
    }

    /** Обзор: для доставки — сравнение миссий по выручке; иначе миссии из сейва, монстры биома и общий обзор. */
    @Override protected Component overview(LoreViews v) {
        if (selectedKind() == MissionKind.CARGO) return cargoOverview(v.blocks());
        Component general = v.missionsOverview();
        baro.save.SaveSnapshot snap = saves.last().orElse(null);
        if (snap == null || snap.missions().isEmpty() && snap.biome().isEmpty()) return general;
        Blocks b = v.blocks();
        Stack s = new Stack(4).padding(10, 12, 0, 12);
        s.with(b.section(tr("Ваши миссии — из сейва (") + snap.locationName() + ")", baro.ui.views.SaveViews.missions(snap, v.bestiary(), b, false)));
        if (!snap.biome().isEmpty())
            s.with(b.section(tr("Монстры биома «") + baro.ui.views.SaveViews.biomeName(snap, v.bestiary()) + tr("» — самые опасные"), baro.ui.views.SaveViews.biomeMonsters(snap, v.bestiary(), b, 10)));
        Stack all = new Stack(0);
        return all.with(s, general);
    }

    private Component cargoOverview(Blocks b) {
        Stack s = new Stack(4).padding(10, 12, 16, 12);
        s.with(b.text().text(tr("Миссии доставки"), Style.TITLE).newLine()
                .text(tr("Выберите миссию: что лежит в ящиках, что выпадает при разборе и как выгоднее всего переработать груз. ")
                        + tr("Справа в списке — выручка с одного ящика при лучшей переработке по текущим ценам."), Style.MUTED));
        s.with(b.text().badge().text(tr("— переработка груза выгоднее, чем награда за доставку ящика."), Style.MUTED));
        Stack rows = new Stack(2);
        for (CargoMission m : catalog.missions()) {
            Integer v = bestValue(m);
            RichText t = b.text();
            if (v != null && v > valuation.reward(m)) t.badge();
            t.link(m).text(tr("  ·  доставка ≈ ") + Format.money(valuation.reward(m)) + tr("/ящ. · переработка ")
                    + (v == null ? tr("считаю…") : v > 0 ? Format.money(v) + tr("/ящ.") : "—"), Style.MUTED);
            rows.with(t);
        }
        s.with(b.section(tr("Все миссии"), rows));
        return s;
    }

    @Override protected Section sectionOf(Object e) {
        return e instanceof CargoMission || e instanceof MissionInfo m && m.commonness() > 0 ? Section.REGULAR : Section.SPECIAL;
    }

    /** Ссылка на миссию другого типа — показать все типы. */
    @Override protected void reveal(Object entry) {
        filling = true;
        type.setSelectedIndex(0);
        filling = false;
    }
}
