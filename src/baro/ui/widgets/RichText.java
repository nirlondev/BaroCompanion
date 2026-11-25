package baro.ui.widgets;


import static baro.Tr.tr;
import baro.model.Item;
import baro.ui.Navigator;
import baro.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.Collections;
import java.util.Map;

/**
 * Переносимый текст из цветных фрагментов и ссылок на предметы (обычные компоненты Swing, без HTML).
 * Методы возвращают this, чтобы строить строку цепочкой.
 */
public class RichText extends JPanel implements WidthAware {
    private final float size;
    private Navigator navigator;
    /** Цена предмета для подписи рядом со ссылкой (null — не показывать). */
    private java.util.function.Function<Item, java.util.Optional<Integer>> pricer;
    /** Значок предмета рядом со ссылкой (null — без значков). */
    private java.util.function.Function<Item, Icon> iconer;

    public RichText() { this(13f, null); }

    public RichText(float size, Navigator navigator) {
        super(new FlowWrapLayout(1));
        this.size = size;
        this.navigator = navigator;
        setOpaque(false);
        setAlignmentX(LEFT_ALIGNMENT);
    }

    public RichText withNavigator(Navigator n) { this.navigator = n; return this; }

    /** Показывать цену рядом с каждой ссылкой на предмет. */
    public RichText withPrices(java.util.function.Function<Item, java.util.Optional<Integer>> pricer) { this.pricer = pricer; return this; }

    /** Показывать значок предмета перед каждой ссылкой на предмет. */
    public RichText withIcons(java.util.function.Function<Item, Icon> iconer) { this.iconer = iconer; return this; }

    /** Картинка (иконка предмета, превью подлодки) как отдельный фрагмент строки. */
    public RichText icon(Icon icon) {
        if (icon == null) return this;
        JLabel l = new JLabel(icon);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        add(l);
        return this;
    }

    /** Кнопка-ссылка: подчёркнутый текст, по щелчку — действие. */
    public RichText action(String text, String tip, Runnable r) {
        JLabel l = label(text, Style.ACCENT);
        underline(l);
        if (tip != null) l.setToolTipText(tip);
        l.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (SwingUtilities.isLeftMouseButton(e)) r.run(); }
            @Override public void mouseEntered(MouseEvent e) { l.setForeground(Theme.WHITE); }
            @Override public void mouseExited(MouseEvent e) { l.setForeground(Theme.ACCENT); }
        });
        add(l);
        return this;
    }

    /** Текст, разбитый на слова (переносится по пробелам). */
    public RichText text(String text, Style style) {
        if (text == null || text.isEmpty()) return this;
        String[] lines = text.split("\n", -1);
        for (int li = 0; li < lines.length; li++) {
            if (li > 0) newLine();
            String line = lines[li];
            int start = 0;
            while (start < line.length()) {
                int sp = line.indexOf(' ', start);
                int end = sp < 0 ? line.length() : sp + 1;
                add(label(line.substring(start, end), style));
                start = end;
            }
        }
        return this;
    }

    public RichText text(String text) { return text(text, Style.NORMAL); }

    /** Неразрывный фрагмент. */
    public RichText word(String text, Style style) { add(label(text, style)); return this; }

    /** Элементы через запятую (её стиль — comma); each дописывает один элемент. */
    public <T> RichText join(Iterable<T> items, Style comma, java.util.function.Consumer<T> each) {
        boolean first = true;
        for (T x : items) { if (!first) word(", ", comma); first = false; each.accept(x); }
        return this;
    }

    /** Значок «лучший» (звезда в зелёной рамке). */
    public RichText badge() {
        JLabel l = new JLabel(StarBadge.of(Math.round(size + 2)));
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        add(l);
        return this;
    }

    /** Ссылка на талант: клик открывает его на вкладке «Таланты». */
    public RichText link(baro.model.Talent talent) {
        JLabel l = label(talent.name(), Style.ACCENT);
        if (navigator == null) { add(l); return this; }
        underline(l);
        l.setToolTipText(tr("Талант: ") + talent.owner() + tr(" — нажмите, чтобы открыть"));
        Navigator nav = navigator;
        l.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (SwingUtilities.isLeftMouseButton(e)) nav.openTalent(talent); }
            @Override public void mouseEntered(MouseEvent e) { l.setForeground(Theme.WHITE); }
            @Override public void mouseExited(MouseEvent e) { l.setForeground(Theme.ACCENT); }
        });
        add(l);
        return this;
    }

    /** Ссылка на миссию доставки. */
    public RichText link(baro.model.CargoMission mission) {
        JLabel l = label(mission.name(), Style.NORMAL);
        if (navigator == null) { add(l); return this; }
        underline(l);
        Navigator nav = navigator;
        l.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (SwingUtilities.isLeftMouseButton(e)) nav.openMission(mission); }
            @Override public void mouseEntered(MouseEvent e) { l.setForeground(Theme.ACCENT); }
            @Override public void mouseExited(MouseEvent e) { l.setForeground(Theme.FG); }
        });
        add(l);
        return this;
    }

    /** Ссылка на запись бестиария или миссию (Creature, Npc, CrewJob, MissionInfo). */
    public RichText lore(String text, Object entry, String tip) {
        JLabel l = label(text, Style.NORMAL);
        if (navigator == null || entry == null) { add(l); return this; }
        underline(l);
        if (tip != null) l.setToolTipText(tip);
        Navigator nav = navigator;
        l.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (SwingUtilities.isLeftMouseButton(e)) nav.openLore(entry); }
            @Override public void mouseEntered(MouseEvent e) { l.setForeground(Theme.ACCENT); }
            @Override public void mouseExited(MouseEvent e) { l.setForeground(Theme.FG); }
        });
        add(l);
        return this;
    }

    private static void underline(JLabel l) {
        @SuppressWarnings("unchecked")
        Map<TextAttribute, Object> attrs = (Map<TextAttribute, Object>) (Map<?, ?>) Collections.singletonMap(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
        l.setFont(l.getFont().deriveFont(attrs));
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** Навык: клик показывает, за что он отвечает и что значит требование. */
    public RichText skill(baro.model.Skill skill, int level, String note) {
        JLabel l = label(level > 0 ? skill.title() + " " + level : tr("(навык «") + skill.title() + "»)", Style.MUTED);
        underline(l);
        l.setToolTipText(tr("Нажмите — что даёт навык"));
        l.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { SkillPopup.show(l, skill, note); }
            @Override public void mouseEntered(MouseEvent e) { l.setForeground(Theme.ACCENT); }
            @Override public void mouseExited(MouseEvent e) { l.setForeground(Theme.MUTED); }
        });
        add(l);
        return this;
    }

    public RichText newLine() { add(new FlowWrapLayout.Break()); return this; }

    /** Ссылка на предмет: клик открывает его карточку. */
    public RichText link(Item item) { return link(item.displayName(), item); }

    public RichText link(String text, Item item) {
        JLabel l = label(text, Style.NORMAL);
        if (iconer != null) {
            Icon ic = iconer.apply(item);
            if (ic != null) { l.setIcon(ic); l.setIconTextGap(3); }
        }
        if (navigator == null) { add(l); price(item); return this; }
        underline(l);
        l.setToolTipText(item.isBest() ? tr("Лучший выбор: ") + item.bestReason().get() : item.category().title());
        Navigator nav = navigator;
        l.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (SwingUtilities.isLeftMouseButton(e)) nav.open(item); }
            @Override public void mouseEntered(MouseEvent e) { l.setForeground(Theme.ACCENT); }
            @Override public void mouseExited(MouseEvent e) { l.setForeground(Theme.FG); }
        });
        add(l);
        price(item);
        return this;
    }

    /** Подпись цены продажи рядом с предметом. */
    private void price(Item item) {
        if (pricer == null) return;
        pricer.apply(item).ifPresent(p -> {
            JLabel pl = label(" " + baro.pricing.Format.money(p), Style.SELL);
            pl.setFont(Theme.font(size - 1.5f, false));
            pl.setForeground(Theme.SELL.darker());
            pl.putClientProperty(FlowWrapLayout.GLUE, Boolean.TRUE);
            add(pl);
        });
    }

    /** Подсветить фон всей строки. */
    public RichText highlight(Color bg) {
        setOpaque(bg != null);
        if (bg != null) setBackground(bg);
        return this;
    }

    private JLabel label(String s, Style style) {
        JLabel l = new JLabel(s);
        if (!s.isEmpty() && ",.;:)".indexOf(s.charAt(0)) >= 0) l.putClientProperty(FlowWrapLayout.GLUE, Boolean.TRUE);
        l.setFont(Theme.font(style == Style.TITLE ? size + 7 : size, style.bold()));
        l.setForeground(style.color());
        return l;
    }

    @Override public int preferredHeight(int width) { return ((FlowWrapLayout) getLayout()).heightFor(this, width); }

    @Override public Dimension getPreferredSize() {
        int w = getWidth() > 0 ? getWidth() : 360;
        return new Dimension(w, preferredHeight(w));
    }

    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
}
