package baro.planner;

import baro.model.Ingredient;
import baro.model.Item;
import baro.model.Recipe;

/** Вариант для ингредиента-«тега» («любой из группы»): какой предмет идёт в рецепт и номер его переменной в задаче. */
record TagOption(Recipe recipe, Ingredient ingredient, Item item, int var) {}
