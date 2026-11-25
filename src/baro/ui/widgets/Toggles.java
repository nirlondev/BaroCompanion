package baro.ui.widgets;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Группа взаимоисключающих кнопок-переключателей (вкладки категорий). */
public final class Toggles<T> extends JPanel {
    private final List<JToggleButton> buttons = new ArrayList<>();
    private final List<T> values = new ArrayList<>();
    private final ButtonGroup group = new ButtonGroup();
    private T selected;

    public Toggles(int columns) {
        super(new GridLayout(0, columns, 3, 3));
        setOpaque(false);
    }

    public Toggles<T> option(String title, T value) {
        JToggleButton b = new JToggleButton(title);
        b.setMargin(new Insets(1, 4, 1, 4));
        b.setFocusable(false);
        group.add(b);
        buttons.add(b);
        values.add(value);
        add(b);
        if (buttons.size() == 1) { b.setSelected(true); selected = value; }
        return this;
    }

    public void onSelect(Consumer<T> c) {
        for (int i = 0; i < buttons.size(); i++) {
            T v = values.get(i);
            buttons.get(i).addActionListener(e -> { selected = v; c.accept(v); });
        }
    }

    public T selected() { return selected; }

    /** Выбрать значение программно (без вызова обработчика). */
    public void select(T value) {
        for (int i = 0; i < values.size(); i++) if (values.get(i) == value) { buttons.get(i).setSelected(true); selected = value; }
    }
}
