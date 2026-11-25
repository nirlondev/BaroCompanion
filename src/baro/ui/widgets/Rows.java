package baro.ui.widgets;

import baro.ui.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Строка списка: название слева, значение справа (без HTML).
 * Правая часть занимает не больше 55% ширины — длинные значения обрезаются «…», название всегда видно.
 */
public final class Rows extends JPanel {
    private final JLabel left = new JLabel(), right = new JLabel();

    public Rows(float size) {
        super(null);
        setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
        left.setFont(Theme.font(size, false));
        right.setFont(Theme.font(size, true));
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        left.setIconTextGap(5);
        add(left);
        add(right);
    }

    @Override public void doLayout() {
        Insets in = getInsets();
        int w = getWidth() - in.left - in.right, h = getHeight() - in.top - in.bottom, gap = 8;
        int rw = Math.min(right.getPreferredSize().width, (int) (w * 0.55));
        if (right.getText().isEmpty()) rw = 0;
        right.setBounds(in.left + w - rw, in.top, rw, h);
        left.setBounds(in.left, in.top, Math.max(0, w - rw - (rw > 0 ? gap : 0)), h);
    }

    @Override public Dimension getPreferredSize() {
        Insets in = getInsets();
        return new Dimension(left.getPreferredSize().width + right.getPreferredSize().width + 8 + in.left + in.right,
                Math.max(left.getPreferredSize().height, right.getPreferredSize().height) + in.top + in.bottom);
    }

    public Rows set(String l, Color lc, String r, Color rc, boolean selected) { return set(l, lc, r, rc, selected, null); }

    /** @param icon значок слева от названия (например, звезда «лучший») или null */
    public Rows set(String l, Color lc, String r, Color rc, boolean selected, Icon icon) {
        left.setIcon(icon);
        left.setText(l);
        left.setForeground(selected ? Theme.WHITE : lc);
        right.setText(r == null ? "" : r);
        right.setForeground(rc);
        right.setToolTipText(r);
        setBackground(selected ? Theme.ACCENT_DARK : Theme.SUNK);
        setOpaque(true);
        return this;
    }
}
