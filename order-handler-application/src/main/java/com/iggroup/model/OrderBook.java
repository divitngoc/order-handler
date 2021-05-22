package com.iggroup.model;

import java.math.BigDecimal;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import com.iggroup.exception.UnhandledSideException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OrderBook {

    private final String symbol;
    private final ConcurrentNavigableMap<BigDecimal, NavigableSet<Order>> buyOrders = new ConcurrentSkipListMap<>((p1, p2) -> p2.compareTo(p1));
    private final ConcurrentNavigableMap<BigDecimal, NavigableSet<Order>> sellOrders = new ConcurrentSkipListMap<>((p1, p2) -> p1.compareTo(p2));

    public ConcurrentNavigableMap<BigDecimal, NavigableSet<Order>> getOrders(final Side side) {
        switch (side) {
        case BUY:
            return buyOrders;
        case SELL:
            return sellOrders;
        default:
            throw new UnhandledSideException("Unhandled side type when getting orders");
        }
    }
}
