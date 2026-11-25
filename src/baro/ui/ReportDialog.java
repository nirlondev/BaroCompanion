package baro.ui;


import static baro.Tr.tr;
import javax.swing.*;
import java.awt.*;

/**
 * «Сообщить о неточности»: игрок описывает, что не так, — отчёт с версией программы, источником данных и записью
 * копируется в буфер обмена, чтобы отправить разработчику (сообщением на Boosty). Сама программа ничего никуда не отправляет.
 */
public final class ReportDialog {
    private ReportDialog() {}

    /** @param subject о чём отчёт («Предмет: Плазменный резак (plasmacutter)») @param context версия, источник данных и т. п. */
    public static void show(Component parent, String subject, String context) {
        JTextArea text = new JTextArea(6, 46);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        JPanel p = new JPanel(new BorderLayout(0, 6));
        JLabel head = new JLabel("<html><b>" + escape(subject) + tr("</b><br>Что не так? Например: «в игре цена 340, а здесь 320» или «рецепт изменился в патче».</html>"));
        p.add(head, BorderLayout.NORTH);
        p.add(new JScrollPane(text), BorderLayout.CENTER);
        JLabel note = new JLabel(tr("Отчёт скопируется в буфер обмена — вставьте его в сообщение разработчику на Boosty."));
        note.setForeground(Theme.MUTED);
        p.add(note, BorderLayout.SOUTH);
        int r = JOptionPane.showConfirmDialog(parent, p, tr("Сообщить о неточности"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        String report = "Baro Companion " + baro.App.VERSION + "\n" + subject + "\n\n" + text.getText().trim() + "\n\n---\n" + context
                + "\nJava " + System.getProperty("java.version") + ", " + System.getProperty("os.name");
        String log = baro.app.Log.tail();
        if (!log.isEmpty()) report += tr("\n\n--- журнал (последние строки) ---\n") + log;
        Links.copy(report);
        Object[] options = {tr("Открыть Boosty"), tr("Закрыть")};
        int open = JOptionPane.showOptionDialog(parent, tr("Отчёт скопирован в буфер обмена. Вставьте его в сообщение разработчику на Boosty."),
                tr("Сообщить о неточности"), JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
        if (open == 0) Links.open(parent, Links.BOOSTY);
    }

    private static String escape(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
}
