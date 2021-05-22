package com.iggroup.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class OrderBookProviderTest {

    private OrderBookProvider provider = OrderBookProvider.getInstance();

    @Test
    void testSingleton() {
        assertSame(provider, OrderBookProvider.getInstance());
    }

    @Test
    void testGetOrderBookBySymbol() {
        assertNotNull(provider.getOrderBookBySymbol("IGG"));
    }
}
