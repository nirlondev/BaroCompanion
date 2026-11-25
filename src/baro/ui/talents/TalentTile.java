package baro.ui.talents;


import static baro.Tr.tr;
import baro.model.Talent;
import baro.talents.TalentBuild;
import baro.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedString;

/** Плитка таланта в дереве: цвет показывает, выбран ли талант, можно ли его взять или он закрыт. */
final class TalentTile extends JComponent {
    static final int W = 138, H = 50;
    final Talent talent;
    private TalentBuild.State state = TalentBuild.State.LOCKED;
    private boolean focused, related;

    TalentTile(Talent talent) {
        this.talent = talent;
        setPreferredSize(new Dimension(W, H));
        setMinimumSize(new Dimension(W, H));
        setMaximumSize(new Dimension(W, H));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(talent.name() + tr(" — щелчок: подробно, двойной щелчок: взять/убрать"));
    }

    void update(TalentBuild.State s, boolean focused, boolean related) { this.state = s; this.focused = focused; this.related = related; repaint(); }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Color bg, border, text;
        switch (state) {
            case CHOSEN: bg = Theme.ACCENT_DARK; border = Theme.ACCENT; text = Theme.WHITE; break;
            case AVAILABLE: bg = Theme.PANEL; border = Theme.HEADER_LINE.brighter(); text = Theme.FG; break;
            case FULL: bg = Theme.SUNK; border = Theme.LINE; text = Theme.MUTED; break;
            case BLOCKED: bg = new Color(0x101416); border = new Color(0x1b2224); text = new Color(0x55666a); break;
            default: bg = Theme.SUNK; border = Theme.LINE; text = new Color(0x6f8486);
        }
        int w = getWidth(), h = getHeight();
        g.setColor(bg);
        g.fillRoundRect(1, 1, w - 2, h - 2, 10, 10);
        if (related && state != TalentBuild.State.CHOSEN) { border = Theme.SELL; }
        g.setColor(border);
        g.setStroke(new BasicStroke(state == TalentBuild.State.CHOSEN || related ? 2f : 1f));
        g.drawRoundRect(1, 1, w - 3, h - 3, 10, 10);
        if (focused) {
            g.setColor(Theme.WARN);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{4f, 3f}, 0f));
            g.drawRoundRect(3, 3, w - 7, h - 7, 8, 8);
        }
        // название (до двух строк)
        g.setColor(text);
        Font f = Theme.font(12.5f, state == TalentBuild.State.CHOSEN);
        AttributedString as = new AttributedString(talent.name());
        as.addAttribute(TextAttribute.FONT, f);
        FontRenderContext frc = g.getFontRenderContext();
        LineBreakMeasurer m = new LineBreakMeasurer(as.getIterator(), frc);
        float y = 7, maxW = w - 16;
        int lines = 0;
        while (m.getPosition() < talent.name().length() && lines < 2) {
            TextLayout tl = m.nextLayout(maxW);
            y += tl.getAscent();
            tl.draw(g, 8, y);
            y += tl.getDescent() + tl.getLeading();
            lines++;
        }
        // пометка «открывает рецепт»
        if (!talent.recipes().isEmpty()) {
            g.setFont(Theme.font(10f, true));
            String r = tr("рецепт");
            int rw = g.getFontMetrics().stringWidth(r) + 8;
            g.setColor(state == TalentBuild.State.BLOCKED ? new Color(0x3a3020) : new Color(0x5a4419));
            g.fillRoundRect(w - rw - 5, h - 17, rw, 13, 6, 6);
            g.setColor(Theme.WARN);
            g.drawString(r, w - rw - 1, h - 7);
        }
        g.dispose();
    }
}
