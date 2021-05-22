package com.iggroup.provider;

import java.util.concurrent.ConcurrentHashMap;

import com.iggroup.model.OrderBook;

import lombok.Getter;

public class OrderBookProvider {

    @Getter
    private ConcurrentHashMap<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    private static final OrderBookProvider INSTANCE = new OrderBookProvider();

    private OrderBookProvider() {}

    public OrderBook getOrderBookBySymbol(final String symbol) {
        return orderBooks.computeIfAbsent(symbol, OrderBook::new);
    }

    public static OrderBookProvider getInstance() {
        return INSTANCE;
    }

}
