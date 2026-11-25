package baro.ui.main;


import static baro.Tr.tr;
import baro.app.SaveWatcher;
import baro.model.Store;
import baro.pricing.Difficulty;
import baro.pricing.PriceSettings;
import baro.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

/** Верхняя панель: сложность, магазин, репутация, наценка магазина, множитель покупки, данные из сейва. */
final class PriceBar extends JPanel {
    private final PriceSettings settings;
    private final SaveWatcher saves;
    private final JComboBox<Difficulty> difficulty = new JComboBox<>(Difficulty.values());
    private final JComboBox<Store> store = new JComboBox<>(Store.values());
    private final JSlider reputation = new JSlider(-100, 100, 0);
    private final JLabel repLabel = new JLabel();
    private final JSpinner markup = new JSpinner(new SpinnerNumberModel(0, -5, 5, 1));
    private final JSpinner multiplier = new JSpinner(new SpinnerNumberModel(2.0, 0.1, 9.0, 0.01));
    private final JLabel station = new JLabel();
    private boolean syncing;

    PriceBar(PriceSettings settings, SaveWatcher saves, Runnable onDeconFinder) {
        super(new baro.ui.widgets.FlowWrapLayout(8, 4));
        this.settings = settings;
        this.saves = saves;
        setBackground(Theme.HEADER);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.HEADER_LINE), BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        reputation.setPreferredSize(new Dimension(150, 24));
        reputation.setOpaque(false);
        repLabel.setPreferredSize(new Dimension(125, 24));
        multiplier.setEditor(new JSpinner.NumberEditor(multiplier, "0.00"));
        ((JSpinner.DefaultEditor) multiplier.getEditor()).getTextField().setColumns(4);
        difficulty.setToolTipText(tr("Уровень сложности кампании: задаёт множитель цен покупки и наград за миссии"));
        markup.setToolTipText(tr("Случайная наценка выбранного магазина −5…+5 %, у каждого магазина каждой станции своя"));
        multiplier.setToolTipText(tr("Множитель цен покупки (сложность × скидки). «Бездна» = 2,0"));
        station.setForeground(Theme.MUTED);

        add(pair(muted(tr("Сложность")), difficulty));
        add(pair(muted(tr("Магазин")), store));
        addComponentListener(new java.awt.event.ComponentAdapter() { @Override public void componentResized(java.awt.event.ComponentEvent e) { revalidate(); } });
        add(pair(muted(tr("Репутация")), reputation, repLabel));
        add(pair(muted(tr("Наценка, %")), markup));
        add(pair(muted(tr("Множитель")), multiplier));
        JButton save = new JButton(tr("Сейв ▾"));
        save.setToolTipText(tr("Взять сложность, станцию, наценки, репутацию и требуемые товары из сейва игры"));
        save.addActionListener(e -> saveMenu().show(save, 0, save.getHeight()));
        JButton dec = new JButton(tr("Где добыть разбором…"));
        dec.addActionListener(e -> onDeconFinder.run());
        add(save); add(dec);
        add(station);

        difficulty.addActionListener(e -> { if (!syncing) settings.setDifficulty((Difficulty) difficulty.getSelectedItem()); });
        store.addActionListener(e -> { if (!syncing) settings.setStore((Store) store.getSelectedItem()); });
        reputation.addChangeListener(e -> { updateRepLabel(); if (!syncing && !reputation.getValueIsAdjusting()) settings.setReputation(reputation.getValue()); });
        markup.addChangeListener(e -> { if (!syncing) settings.setStationMarkup((Integer) markup.getValue()); });
        multiplier.addChangeListener(e -> { if (!syncing) settings.setBuyMultiplier(((Number) multiplier.getValue()).floatValue()); });
        settings.addListener(this::sync);
        saves.addListener(this::sync);
        sync();
    }

    private JPopupMenu saveMenu() {
        JPopupMenu m = new JPopupMenu();
        JMenuItem now = new JMenuItem(tr("Загрузить из сейва сейчас"));
        now.addActionListener(e -> saves.loadNow());
        m.add(now);
        JCheckBoxMenuItem auto = new JCheckBoxMenuItem(tr("Следить за сейвом (подхватывать каждое сохранение)"), saves.isAuto());
        auto.addActionListener(e -> saves.setAuto(auto.isSelected()));
        m.add(auto);
        JMenuItem forget = new JMenuItem(tr("Забыть данные станции (требуемые товары, скидки)"));
        forget.addActionListener(e -> settings.clearStation());
        m.add(forget);
        m.addSeparator();
        ButtonGroup g = new ButtonGroup();
        Path pinned = saves.pinned();
        JRadioButtonMenuItem latest = new JRadioButtonMenuItem(tr("Самый свежий сейв"), pinned == null);
        latest.addActionListener(e -> saves.pin(null));
        g.add(latest); m.add(latest);
        int n = 0;
        for (Path p : saves.saves()) {
            if (n++ >= 15) break;
            String name = p.getFileName().toString().replaceFirst("\\.save$", "");
            String where = p.getParent().getFileName().toString().equalsIgnoreCase("Multiplayer") ? tr("сетевая") : tr("одиночная");
            JRadioButtonMenuItem it = new JRadioButtonMenuItem(name + "  (" + where + ", "
                    + new java.text.SimpleDateFormat("dd.MM.yy HH:mm").format(new java.util.Date(baro.save.SaveReader.modified(p))) + ")", p.equals(pinned));
            it.addActionListener(e -> saves.pin(p));
            g.add(it); m.add(it);
        }
        if (n == 0) { JMenuItem none = new JMenuItem(tr("Сейвы игры не найдены")); none.setEnabled(false); m.add(none); }
        return m;
    }

    @Override public Dimension getPreferredSize() {
        int w = getParent() != null && getParent().getWidth() > 0 ? getParent().getWidth() : 1200;
        return new Dimension(w, ((baro.ui.widgets.FlowWrapLayout) getLayout()).heightFor(this, w));
    }

    private static JPanel pair(JComponent... cs) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        p.setOpaque(false);
        for (JComponent c : cs) p.add(c);
        return p;
    }

    private static JLabel muted(String s) { JLabel l = new JLabel(s); l.setForeground(Theme.MUTED); return l; }

    private void updateRepLabel() {
        int v = reputation.getValue();
        repLabel.setText((v > 0 ? "+" : "") + v + "  " + new baro.pricing.Reputation(v).tier().title());
    }

    private void sync() {
        syncing = true;
        difficulty.setSelectedItem(settings.difficulty());
        store.setSelectedItem(settings.store());
        reputation.setValue(settings.reputation().value());
        markup.setValue(settings.stationMarkup());
        multiplier.setValue((double) Math.round(settings.buyMultiplier() * 1000) / 1000.0);
        updateRepLabel();
        String info = settings.stationInfo().orElse("");
        int req = settings.requested(settings.store()).size(), spec = settings.dailySpecials(settings.store()).size();
        station.setText(info.isEmpty() ? "" : tr("Станция: ") + info + (req + spec > 0 ? tr(" · требует товаров: ") + req + tr(", скидок дня: ") + spec : ""));
        station.setToolTipText(saves.status().isEmpty() ? null : saves.status());
        syncing = false;
    }
}