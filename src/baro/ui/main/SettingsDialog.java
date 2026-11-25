package baro.ui.main;


import static baro.Tr.tr;
import baro.data.gamefiles.GameInstall;
import baro.settings.AppSettings;
import baro.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Настройки вместо ручной правки settings.properties: папка игры, моды, язык текстов, сейвы, тема, размер шрифта и проверка обновлений.
 * Данные игры, язык, тема и шрифт применяются после перезапуска (кнопка «Перезапустить»).
 */
final class SettingsDialog extends JDialog {
    private static final String[][] SCALES = {{"90%", "0.9"}, {"100%", "1.0"}, {"110%", "1.1"}, {"125%", "1.25"}, {"150%", "1.5"}};

    private final AppSettings settings;
    private final JTextField gameDir = new JTextField(32);
    private final JLabel gameStatus = new JLabel();
    private final JCheckBox gameFiles = new JCheckBox(tr("Читать данные из файлов игры (иначе — только встроенные)"));
    private final JCheckBox mods = new JCheckBox(tr("Учитывать включённые моды"));
    /** Язык интерфейса и текстов игры: русский или английский. */
    private final JComboBox<String> language = new JComboBox<>(new String[]{tr("Русский"), "English"});
    private final JCheckBox updates = new JCheckBox(tr("Проверять обновления при запуске"));
    private final JCheckBox saveAuto = new JCheckBox(tr("Следить за сейвами (сложность, станция, цены, миссии)"));
    private final JTextField saveDir = new JTextField(32);
    private final JComboBox<String> theme = new JComboBox<>(new String[]{tr("Тёмная"), tr("Светлая")});
    private final JComboBox<String> scale = new JComboBox<>();
    private final JTextField hotkey = new JTextField(14);
    private String hotkeySpec;
    private JPanel hotkeyRow;

    /** @param restart перезапустить программу, чтобы изменения применились */
    SettingsDialog(Frame owner, AppSettings settings, Runnable restart) {
        super(owner, tr("Настройки"), true);
        this.settings = settings;
        gameDir.setText(settings.getString("game.dir", ""));
        gameFiles.setSelected(settings.getBoolean("game.files", true));
        mods.setSelected(settings.getBoolean("game.mods", true));
        language.setSelectedIndex(baro.Tr.english() ? 1 : 0);
        saveAuto.setSelected(settings.getBoolean("save.auto", true));
        updates.setSelected(settings.getBoolean("update.check", true));
        saveDir.setText(settings.getString("save.dir", ""));
        theme.setSelectedIndex("light".equalsIgnoreCase(settings.getString("ui.theme", "dark")) ? 1 : 0);
        double sc = settings.getDouble("ui.scale", 1.0);
        int pick = 1;
        for (int i = 0; i < SCALES.length; i++) { scale.addItem(SCALES[i][0]); if (Math.abs(Double.parseDouble(SCALES[i][1]) - sc) < 0.01) pick = i; }
        scale.setSelectedIndex(pick);
        hotkeySpec = settings.getString("hotkey", baro.hotkey.GlobalHotkey.DEFAULT);
        hotkey.setText(baro.hotkey.GlobalHotkey.name(hotkeySpec));
        hotkey.setEditable(false);
        hotkey.setToolTipText(tr("Щёлкните и нажмите нужное сочетание (с Ctrl, Alt или Shift)"));
        hotkey.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) {
                int code = e.getKeyCode();
                if (code == java.awt.event.KeyEvent.VK_SHIFT || code == java.awt.event.KeyEvent.VK_CONTROL || code == java.awt.event.KeyEvent.VK_ALT
                        || code == java.awt.event.KeyEvent.VK_META || code == java.awt.event.KeyEvent.VK_UNDEFINED) return;
                KeyStroke ks = KeyStroke.getKeyStroke(code, e.getModifiersEx());
                boolean fKey = code >= java.awt.event.KeyEvent.VK_F1 && code <= java.awt.event.KeyEvent.VK_F24;
                if (e.getModifiersEx() == 0 && !fKey) { hotkey.setText(tr("нужен Ctrl, Alt или Shift (или F1–F12)")); e.consume(); return; }
                hotkeySpec = ks.toString().replace("pressed ", "");
                hotkey.setText(baro.hotkey.GlobalHotkey.name(hotkeySpec));
                e.consume();
            }
        });
        JButton resetKey = new JButton("Shift+T");
        resetKey.setToolTipText(tr("Вернуть сочетание по умолчанию"));
        resetKey.addActionListener(e -> { hotkeySpec = baro.hotkey.GlobalHotkey.DEFAULT; hotkey.setText(baro.hotkey.GlobalHotkey.name(hotkeySpec)); });
        hotkeyRow = new JPanel(new BorderLayout(6, 0));
        hotkeyRow.add(hotkey, BorderLayout.CENTER);
        hotkeyRow.add(resetKey, BorderLayout.EAST);
        gameDir.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { checkGame(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { checkGame(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { checkGame(); }
        });

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 14, 8, 14));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.WEST; c.insets = new Insets(3, 0, 3, 8); c.fill = GridBagConstraints.HORIZONTAL;
        heading(form, c, tr("Игра"));
        row(form, c, tr("Папка игры"), withBrowse(gameDir, true));
        row(form, c, "", gameStatus);
        row(form, c, "", gameFiles);
        row(form, c, "", mods);
        row(form, c, tr("Язык") + " / Language", language);
        heading(form, c, tr("Сейвы"));
        row(form, c, "", saveAuto);
        row(form, c, tr("Своя папка сейвов"), withBrowse(saveDir, true));
        heading(form, c, tr("Вид"));
        row(form, c, tr("Тема"), theme);
        row(form, c, tr("Размер шрифта"), scale);
        row(form, c, tr("Окно поверх игры"), hotkeyRow);
        row(form, c, "", updates);
        JLabel note = new JLabel(tr("Данные игры, язык, тема, шрифт и сочетание клавиш применятся после перезапуска программы.") + (baro.Tr.english() ? "" : " / Restart to apply the language."));
        note.setForeground(Theme.MUTED);
        c.gridx = 0; c.gridwidth = 2; c.insets = new Insets(10, 0, 0, 0);
        form.add(note, c);
        checkGame();

        JButton ok = new JButton(tr("Сохранить"));
        JButton okRestart = new JButton(tr("Сохранить и перезапустить"));
        JButton cancel = new JButton(tr("Отмена"));
        ok.addActionListener(e -> { save(); dispose(); });
        okRestart.addActionListener(e -> { save(); dispose(); restart.run(); });
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.add(okRestart); buttons.add(ok); buttons.add(cancel);

        JPanel root = new JPanel(new BorderLayout());
        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(ok);
        pack();
        setLocationRelativeTo(owner);
    }

    private void checkGame() {
        String d = gameDir.getText().trim();
        boolean ok = !d.isEmpty() && GameInstall.isGameDir(Paths.get(d));
        if (d.isEmpty()) {
            GameInstall found = GameInstall.locate(settings);
            gameStatus.setText(found != null ? tr("Найдена сама: ") + found.dir() : tr("Игра не найдена в библиотеках Steam — укажите папку"));
            gameStatus.setForeground(found != null ? Theme.SELL : Theme.WARN);
        } else {
            gameStatus.setText(ok ? tr("Папка игры — верно") : tr("Здесь нет игры (нужна папка, где лежит Barotrauma.exe и Content)"));
            gameStatus.setForeground(ok ? Theme.SELL : Theme.BUY);
        }
    }

    private void save() {
        settings.put("game.dir", gameDir.getText().trim());
        settings.put("game.files", gameFiles.isSelected());
        settings.put("game.mods", mods.isSelected());
        boolean en = language.getSelectedIndex() == 1;
        settings.put("ui.language", en ? "en" : "ru");
        settings.put("game.language", en ? "English" : "Russian");
        settings.put("save.auto", saveAuto.isSelected());
        settings.put("save.dir", saveDir.getText().trim());
        settings.put("ui.theme", theme.getSelectedIndex() == 1 ? "light" : "dark");
        settings.put("ui.scale", SCALES[scale.getSelectedIndex()][1]);
        settings.put("hotkey", hotkeySpec);
        settings.put("update.check", updates.isSelected());
        settings.saveNow();
    }

    private JPanel withBrowse(JTextField field, boolean dirs) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        JButton b = new JButton(tr("Обзор…"));
        b.addActionListener(e -> {
            JFileChooser ch = new JFileChooser();
            ch.setFileSelectionMode(dirs ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
            String cur = field.getText().trim();
            if (!cur.isEmpty() && Files.isDirectory(Path.of(cur))) ch.setCurrentDirectory(Path.of(cur).toFile());
            if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) field.setText(ch.getSelectedFile().getAbsolutePath());
        });
        p.add(field, BorderLayout.CENTER);
        p.add(b, BorderLayout.EAST);
        return p;
    }

    private static void heading(JPanel form, GridBagConstraints c, String title) {
        JLabel l = new JLabel(title);
        l.setFont(Theme.font(14, true));
        l.setForeground(Theme.ACCENT);
        c.gridx = 0; c.gridwidth = 2; c.insets = new Insets(c.gridy == 0 ? 0 : 12, 0, 4, 0);
        form.add(l, c);
        c.gridy++; c.gridwidth = 1; c.insets = new Insets(3, 0, 3, 8);
    }

    private static void row(JPanel form, GridBagConstraints c, String label, JComponent field) {
        c.gridx = 0; c.weightx = 0;
        form.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 1;
        form.add(field, c);
        c.gridy++;
    }
}
