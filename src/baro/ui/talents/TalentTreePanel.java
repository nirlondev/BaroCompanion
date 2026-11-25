package baro.ui.talents;


import static baro.Tr.tr;
import baro.model.*;
import baro.talents.SynergyFinder;
import baro.talents.TalentBuild;
import baro.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Дерево талантов профессии: слева «Основа», правее — три специализации сверху вниз по рядам. */
final class TalentTreePanel extends JPanel {
    private final TalentBuild build;
    private final SynergyFinder synergies;
    private final List<TalentTile> tiles = new ArrayList<>();
    private final List<JLabel> headers = new ArrayList<>();
    private final List<TalentSubtree> headerTrees = new ArrayList<>();
    private Talent focused;

    TalentTreePanel(TalentBuild build, SynergyFinder synergies, Consumer<Talent> onFocus, Consumer<Talent> onToggle) {
        super(new GridBagLayout());
        this.build = build;
        this.synergies = synergies;
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0; c.anchor = GridBagConstraints.NORTH; c.insets = new Insets(0, 0, 0, 14);
        int col = 0;
        for (TalentSubtree s : build.job().subtrees()) {
            c.gridx = col++;
            add(column(s, onFocus, onToggle), c);
        }
        c.gridx = col; c.weightx = 1; c.weighty = 1;
        JPanel filler = new JPanel(); filler.setOpaque(false);
        add(filler, c);
        refresh();
    }

    private JComponent column(TalentSubtree s, Consumer<Talent> onFocus, Consumer<Talent> onToggle) {
        Column col = new Column();
        JLabel head = new JLabel();
        head.setFont(Theme.font(14, true));
        head.setForeground(Theme.ACCENT);
        head.setAlignmentX(CENTER_ALIGNMENT);
        headers.add(head);
        headerTrees.add(s);
        col.add(head);
        col.add(Box.createVerticalStrut(8));
        for (TalentGroup g : s.groups()) {
            int cols = s.isPrimary() ? 2 : Math.max(1, g.talents().size());
            JPanel row = new JPanel(new GridLayout(0, cols, 6, 6));
            row.setOpaque(false);
            for (Talent t : g.talents()) {
                TalentTile tile = new TalentTile(t);
                tile.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() >= 2 || SwingUtilities.isRightMouseButton(e)) onToggle.accept(t);
                        else onFocus.accept(t);
                    }
                });
                tiles.add(tile);
                row.add(tile);
            }
            row.setAlignmentX(CENTER_ALIGNMENT);
            row.setMaximumSize(row.getPreferredSize());
            col.add(row);
            col.rows.add(row);
            if (s.isPrimary()) {
                JLabel note = new JLabel(tr("нужно взять ") + Math.min(g.required(), g.talents().size()) + tr(" из ") + g.talents().size() + (g.max() >= g.talents().size() ? tr(" · можно взять все") : tr(" · максимум ") + g.max()));
                note.setFont(Theme.font(11, false));
                note.setForeground(Theme.MUTED);
                note.setAlignmentX(CENTER_ALIGNMENT);
                col.add(Box.createVerticalStrut(2));
                col.add(note);
            }
            col.add(Box.createVerticalStrut(18));
        }
        return col;
    }

    /** Колонка ветки: рисует линии между рядами. */
    private static final class Column extends JPanel {
        final List<JPanel> rows = new ArrayList<>();
        Column() { setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false); }
        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setColor(Theme.HEADER_LINE.brighter());
            g.setStroke(new BasicStroke(2f));
            for (int i = 0; i + 1 < rows.size(); i++) {
                Rectangle a = rows.get(i).getBounds(), b = rows.get(i + 1).getBounds();
                int x = getWidth() / 2;
                g.drawLine(x, a.y + a.height + 2, x, b.y - 2);
                g.fillPolygon(new int[]{x - 5, x + 5, x}, new int[]{b.y - 8, b.y - 8, b.y - 2}, 3);
            }
            g.dispose();
        }
    }

    void focus(Talent t) { this.focused = t; refresh(); scrollToFocus(); }

    private void scrollToFocus() {
        for (TalentTile tile : tiles) if (tile.talent == focused) {
            SwingUtilities.invokeLater(() -> tile.scrollRectToVisible(new Rectangle(-20, -40, tile.getWidth() + 40, tile.getHeight() + 80)));
        }
    }

    void refresh() {
        Set<Talent> related = new HashSet<>();
        if (focused != null) synergies.forTalent(focused).stream().limit(8).forEach(s -> related.add(s.other(focused)));
        for (TalentTile tile : tiles) tile.update(build.state(tile.talent), tile.talent == focused, related.contains(tile.talent));
        for (int i = 0; i < headers.size(); i++) {
            TalentSubtree s = headerTrees.get(i);
            String status;
            boolean any = false;
            for (TalentGroup g : s.groups()) for (Talent t : g.talents()) any |= build.isChosen(t);
            if (s.isPrimary()) status = build.full(s) ? tr("прокачана") : build.complete(s) ? tr("пройдена") : tr("начните отсюда");
            else if (any) status = build.full(s) ? tr("прокачана") : tr("в процессе");
            else status = build.isBlocked(s) ? tr("закрыта") : "";
            headers.get(i).setText(s.name() + (status.isEmpty() ? "" : "  ·  " + status));
            headers.get(i).setForeground(status.equals(tr("закрыта")) ? Theme.MUTED : Theme.ACCENT);
        }
        repaint();
    }
}
