package baro.ui.widgets;

import baro.ui.Theme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/** Поле ввода с подсказкой внутри и уведомлением о каждом изменении текста. */
public class SearchField extends JTextField {
    private final String hint;

    public SearchField(String hint, int columns) {
        super(columns);
        this.hint = hint;
    }

    public void onChange(Runnable r) {
        getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { r.run(); }
            @Override public void removeUpdate(DocumentEvent e) { r.run(); }
            @Override public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getText().isEmpty() && hint != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Theme.MUTED);
            g2.setFont(getFont());
            Insets in = getInsets();
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(hint, in.left + 2, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }
}
