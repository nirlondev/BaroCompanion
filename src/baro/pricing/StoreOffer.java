package baro.pricing;

import baro.model.Store;

/** Предложение магазина: где и почём. */
public record StoreOffer(Store store, int price) {}
