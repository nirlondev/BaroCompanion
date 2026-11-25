package baro.ui.main;


import static baro.Tr.tr;
import baro.App;
import baro.settings.AppSettings;
import baro.ui.Links;
import baro.ui.Theme;
import baro.ui.views.Blocks;
import baro.ui.widgets.Stack;
import baro.ui.widgets.Style;

import javax.swing.*;
import java.awt.*;

/**
 * Первый запуск — несколько подсказок. Запуск отмечается в настройках (app.version),
 * после обновлений окно больше не показывается.
 */
final class WelcomeDialog {
    private WelcomeDialog() {}

    /** Показать, если это первый запуск. */
    static void maybeShow(Frame owner, AppSettings settings, Blocks blocks) {
        hotkey = settings.getString("hotkey", baro.hotkey.GlobalHotkey.DEFAULT);
        String seen = settings.getString("app.version", "");
        if (seen.equals(App.VERSION)) return;
        settings.put("app.version", App.VERSION);
        settings.save();
        if (seen.isEmpty()) show(owner, blocks);
    }

    static void show(Frame owner, Blocks blocks) {
        Stack s = new Stack(6).padding(12, 14, 12, 14);
        s.with(blocks.text().text(tr("Добро пожаловать в Baro Companion"), Style.TITLE));
        s.with(blocks.text().text(tr("Несколько подсказок, чтобы начать:"), Style.MUTED));
        tip(s, blocks, hotkeyName(), tr("в игре открывает маленькое окно поверх игры с поиском. Esc — скрыть. Правая кнопка по окну — меню: прозрачность, ширина, что показывать, когда поиск пуст."));
        tip(s, blocks, tr("Сейвы"), tr("подхватываются сами: сложность, станция, наценки магазинов, взятые миссии и монстры биома. Кнопка «Сейв ▾» сверху — выбрать сейв."));
        tip(s, blocks, tr("Карточки"), tr("ссылки открываются сбоку, «Назад / Вперёд» — Alt+← / Alt+→. В карточке предмета: избранное, список покупок, сравнение, вики, дерево крафта до сырья."));
        tip(s, blocks, tr("Поиск"), tr("понимает английские названия, сленг («плазма», «шотган») и опечатки."));
        tip(s, blocks, tr("Настройки"), tr("папка игры, моды, язык, тема и размер шрифта — кнопка «Настройки» внизу окна."));
        tip(s, blocks, "Boosty", tr("поддержать справочник можно кнопкой «Boosty» внизу окна: ") + Links.BOOSTY);
        JScrollPane sc = Theme.scroll(s);
        sc.setPreferredSize(new Dimension(Theme.px(620), Theme.px(420)));
        JOptionPane.showMessageDialog(owner, sc, "Baro Companion", JOptionPane.PLAIN_MESSAGE);
    }

    private static String hotkey = baro.hotkey.GlobalHotkey.DEFAULT;

    private static String hotkeyName() { return baro.hotkey.GlobalHotkey.name(hotkey); }

    private static void tip(Stack s, Blocks b, String head, String text) {
        s.with(b.text().word(head + " ", Style.ACCENT).text("— " + text, Style.NORMAL));
    }
}
