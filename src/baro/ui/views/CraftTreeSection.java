package baro.ui.views;


import static baro.Tr.tr;
import baro.model.Item;
import baro.ui.Theme;
import baro.ui.widgets.Stack;

import javax.swing.*;
import java.awt.*;

/** Раздел «Из чего с нуля» с выбором количества и режимом «покупать промежуточное, если дешевле». */
final class CraftTreeSection extends Stack {
    private static int lastCount = 1;
    private static boolean lastBuy = true;
    private final Item item;
    private final Blocks b;
    private final Stack body = new Stack(1);
    private final JSpinner count = new JSpinner(new SpinnerNumberModel(lastCount, 1, 999, 1));
    private final JCheckBox buy = new JCheckBox(tr("покупать промежуточное, если дешевле"), lastBuy);

    CraftTreeSection(Item item, Blocks b) {
        super(2);
        this.item = item;
        this.b = b;
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        JLabel l = new JLabel(tr("Сколько сделать:"));
        l.setForeground(Theme.MUTED);
        ((JSpinner.DefaultEditor) count.getEditor()).getTextField().setColumns(3);
        buy.setOpaque(false);
        buy.setToolTipText(tr("Если промежуточный предмет дешевле купить, чем скрафтить из купленного сырья, — покупать его"));
        row.add(l); row.add(count); row.add(buy);
        row.setAlignmentX(LEFT_ALIGNMENT);
        count.addChangeListener(e -> { lastCount = (Integer) count.getValue(); render(); });
        buy.addActionListener(e -> { lastBuy = buy.isSelected(); render(); });
        with(row, body);
        render();
    }

    private void render() {
        body.removeAll();
        Stack t = b.craftTree(item, (Integer) count.getValue(), buy.isSelected());
        if (t != null) body.with(t);
        body.revalidate();
        revalidate();
        repaint();
    }
}
