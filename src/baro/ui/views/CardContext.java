package baro.ui.views;

import baro.app.UserLists;
import baro.data.GameCatalog;
import baro.save.SaveSnapshot;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Что нужно карточкам сверх цен и ссылок: каталог (недуги, иконки), личные списки игрока, последний сейв
 * (наличие товара на станции) и действия окна — открыть сравнение и список покупок.
 */
public record CardContext(GameCatalog catalog, UserLists lists, Supplier<Optional<SaveSnapshot>> save, Actions actions) {
    /** Действия, которые выполняет главное окно. */
    public interface Actions {
        void openCompare();
        void openShopping();
        /** Добавить существо бестиария к сравнению (или убрать). */
        void compare(Object creature);
    }
}
