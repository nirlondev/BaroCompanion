package baro.model;

/** Числовой бонус таланта из файлов игры: тип характеристики, её название по-русски (может быть пустым), значение. */
public record TalentStat(String type, String target, double value) {}
