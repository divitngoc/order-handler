package com.iggroup.provider;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.iggroup.model.Order;
import com.iggroup.model.OrderBook;

import lombok.Getter;

public class OrderBookProvider {

    @Getter
    private ConcurrentHashMap<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    private static final OrderBookProvider INSTANCE = new OrderBookProvider();

    private OrderBookProvider() {}

    public boolean checkIfOrderExists(final Order order) {
        return getOrderBookBySymbol(order.getSymbol()).getOrders(order.getSide())
                                                      .getOrDefault(order.getPrice().get(), new ConcurrentSkipListSet<>())
                                                      .contains(order);
    }

    public OrderBook getOrderBookBySymbol(final String symbol) {
        return orderBooks.computeIfAbsent(symbol, OrderBook::new);
    }

    public static OrderBookProvider getInstance() {
        return INSTANCE;
    }

}
