package baro.ui.talents;


import static baro.Tr.tr;
import baro.model.Talent;
import baro.talents.EffectTotals;
import baro.ui.Theme;
import baro.ui.widgets.HeaderRenderer;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Таблица «Сумма эффектов»: все эффекты выбранных талантов, одинаковые объединены, числа сложены.
 * Щелчок по строке открывает талант, который даёт эффект.
 */
final class EffectsTable extends JPanel {
    private static final String[] COLUMNS = {tr("Эффект"), tr("Итого"), tr("Когда действует"), tr("Таланты")};
    private final Model model = new Model();
    private final JTable table = new JTable(model);
    private final JLabel title = new JLabel();

    EffectsTable(Consumer<Talent> open) {
        super(new BorderLayout());
        setBackground(Theme.PANEL);
        title.setForeground(Theme.ACCENT);
        title.setFont(Theme.font(14, true));
        title.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        add(title, BorderLayout.NORTH);

        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        table.setBackground(Theme.PANEL);
        table.setForeground(Theme.FG);
        table.setGridColor(Theme.LINE);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(Theme.HIT);
        table.setSelectionForeground(Theme.WHITE);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.setDefaultRenderer(Object.class, new Wrap());
        TableColumnModel cm = table.getColumnModel();
        int[] widths = {430, 120, 230, 200};
        for (int i = 0; i < widths.length; i++) cm.getColumn(i).setPreferredWidth(widths[i]);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r < 0 || r >= model.rows.size()) return;
                List<Talent> src = model.rows.get(r).sources();
                if (src.isEmpty()) return;
                // в колонке «Таланты» — тот, по чьему имени щёлкнули (по горизонтали), иначе первый
                int c = table.columnAtPoint(e.getPoint());
                Talent t = src.get(0);
                if (c == 3 && src.size() > 1) {
                    Rectangle cell = table.getCellRect(r, c, false);
                    int line = table.getFontMetrics(Theme.font(13f, false)).getHeight();
                    int idx = (e.getY() - cell.y - 3) / Math.max(1, line);
                    t = src.get(Math.max(0, Math.min(src.size() - 1, idx)));
                }
                open.accept(t);
            }
        });
        table.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { fitRows(); }
        });
        table.getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            public void columnAdded(javax.swing.event.TableColumnModelEvent e) {}
            public void columnRemoved(javax.swing.event.TableColumnModelEvent e) {}
            public void columnMoved(javax.swing.event.TableColumnModelEvent e) {}
            public void columnMarginChanged(javax.swing.event.ChangeEvent e) { SwingUtilities.invokeLater(EffectsTable.this::fitRows); }
            public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {}
        });
        add(Theme.scroll(table), BorderLayout.CENTER);
    }

    void show(String jobName, Collection<Talent> chosen) {
        model.rows = EffectTotals.of(chosen);
        model.fireTableDataChanged();
        title.setText(chosen.isEmpty()
                ? tr("Сумма эффектов — ") + jobName + tr(": выберите таланты в дереве")
                : tr("Сумма эффектов — ") + jobName + tr(" (талантов: ") + chosen.size() + tr(", эффектов: ") + model.rows.size() + ")");
        fitRows();
    }

    /** Высота строки по самому длинному (перенесённому) тексту. */
    private void fitRows() {
        TableCellRenderer r = table.getDefaultRenderer(Object.class);
        for (int row = 0; row < table.getRowCount(); row++) {
            int h = 22;
            for (int c = 0; c < table.getColumnCount(); c++) {
                Component comp = r.getTableCellRendererComponent(table, table.getValueAt(row, c), false, false, row, c);
                int w = table.getColumnModel().getColumn(c).getWidth();
                comp.setSize(w, Short.MAX_VALUE);
                h = Math.max(h, comp.getPreferredSize().height);
            }
            if (table.getRowHeight(row) != h) table.setRowHeight(row, h);
        }
    }

    private static final class Model extends AbstractTableModel {
        List<EffectTotals.Row> rows = new ArrayList<>();

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int c) { return COLUMNS[c]; }

        @Override public Object getValueAt(int r, int c) {
            EffectTotals.Row row = rows.get(r);
            switch (c) {
                case 0: return row.text();
                case 1: return row.total() + (row.nonStacking() ? tr("\n(не складывается — берётся наибольшее)") : "");
                case 2: return row.condition().isEmpty() ? tr("всегда") : row.condition();
                default:
                    StringBuilder s = new StringBuilder();
                    for (Talent t : row.sources()) { if (s.length() > 0) s.append('\n'); s.append(t.name()); }
                    return s.toString();
            }
        }
    }

    /** Ячейка с переносом строк. */
    private final class Wrap extends JTextArea implements TableCellRenderer {
        Wrap() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        }

        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
            setText(v == null ? "" : v.toString());
            EffectTotals.Row row = r < model.rows.size() ? model.rows.get(r) : null;
            setFont(Theme.font(c == 1 ? 13.5f : 13f, c == 1));
            Color fg = Theme.FG;
            if (c == 1) fg = row != null && row.numeric() ? Theme.SELL : Theme.MUTED;
            else if (c == 2) fg = row != null && row.condition().isEmpty() ? Theme.MUTED : Theme.WARN;
            else if (c == 3) fg = Theme.ACCENT;
            setForeground(sel ? Theme.WHITE : fg);
            setBackground(sel ? Theme.HIT : (r % 2 == 0 ? Theme.PANEL : Theme.SUNK));
            setToolTipText(c == 3 ? tr("Нажмите, чтобы открыть талант") : null);
            return this;
        }
    }
}
