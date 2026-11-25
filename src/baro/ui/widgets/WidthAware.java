package baro.ui.widgets;

/** Компонент, высота которого зависит от доступной ширины (переносимый текст). */
public interface WidthAware {
    int preferredHeight(int width);

    static int heightOf(java.awt.Component c, int width) {
        return c instanceof WidthAware ? ((WidthAware) c).preferredHeight(width) : c.getPreferredSize().height;
    }
}
