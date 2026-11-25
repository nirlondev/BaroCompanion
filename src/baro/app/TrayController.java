package baro.app;


import static baro.Tr.tr;
import baro.ui.AppIcon;

import java.awt.*;
import javax.swing.SwingUtilities;

/** Значок в области уведомлений Windows: справочник, окно поверх игры, выход. */
final class TrayController {
    private TrayController() {}

    static boolean install(Runnable showMain, Runnable toggleOverlay, Runnable exit, String hotkey) {
        if (!SystemTray.isSupported()) return false;
        try {
            PopupMenu m = new PopupMenu();
            MenuItem a = new MenuItem(tr("Справочник")); a.addActionListener(e -> SwingUtilities.invokeLater(showMain));
            MenuItem b = new MenuItem(tr("Окно поверх игры (") + hotkey + ")"); b.addActionListener(e -> SwingUtilities.invokeLater(toggleOverlay));
            MenuItem c = new MenuItem(tr("Выход")); c.addActionListener(e -> SwingUtilities.invokeLater(exit));
            m.add(a); m.add(b); m.addSeparator(); m.add(c);
            TrayIcon t = new TrayIcon(AppIcon.create(32), "Baro Companion — " + hotkey, m);
            t.setImageAutoSize(true);
            t.addActionListener(e -> SwingUtilities.invokeLater(showMain));
            SystemTray.getSystemTray().add(t);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
