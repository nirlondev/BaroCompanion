package baro.ui.widgets;


import static baro.Tr.tr;
import baro.model.Skill;
import baro.ui.Theme;

import javax.swing.*;
import java.awt.*;

/** Всплывающее описание навыка рядом с местом клика. */
final class SkillPopup {
    private SkillPopup() {}

    static void show(Component anchor, Skill skill, String note) {
        Stack s = new Stack(4).padding(8, 10, 8, 10);
        s.with(new RichText(13f, null).word(tr("Навык «") + skill.title() + "»", Style.ACCENT));
        s.with(new RichText(12.5f, null).text(skill.description(), Style.NORMAL));
        if (note != null) s.with(new RichText(12.5f, null).text(note, Style.WARN));
        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(Theme.PANEL);
        box.setBorder(BorderFactory.createLineBorder(Theme.ACCENT_DARK));
        int w = 380;
        s.setSize(w, 10);
        box.add(s);
        box.setPreferredSize(new Dimension(w, s.preferredHeight(w) + 2));
        JPopupMenu p = new JPopupMenu();
        p.setBorder(BorderFactory.createEmptyBorder());
        p.add(box);
        p.show(anchor, 0, anchor.getHeight() + 2);
    }
}
