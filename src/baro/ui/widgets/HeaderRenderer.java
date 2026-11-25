package baro.ui.widgets;

import baro.ui.Theme;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/** Шапка таблицы: тёмно-синяя, без яркого градиента Nimbus. */
public final class HeaderRenderer implements TableCellRenderer {
    private final JLabel label = new JLabel();

    public HeaderRenderer() {
        label.setOpaque(true);
        label.setBackground(Theme.HEADER);
        label.setForeground(Theme.ACCENT);
        label.setFont(Theme.font(12.5f, true));
        label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.HEADER_LINE),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));
    }

    @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
        label.setText(v == null ? "" : v.toString());
        return label;
    }
}
