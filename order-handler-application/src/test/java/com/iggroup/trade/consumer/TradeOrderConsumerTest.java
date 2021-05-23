package com.iggroup.trade.consumer;

import static com.iggroup.factory.OrderFactory.createOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.iggroup.factory.OrderFactory;
import com.iggroup.model.Order;
import com.iggroup.provider.OrderBookProvider;
import com.iggroup.trade.consumer.TradeOrderConsumer;

class TradeOrderConsumerTest {

    private OrderBookProvider provider = OrderBookProvider.getInstance();
    private TradeOrderConsumer tradeService;
    private BlockingQueue<Order> tradeQueue;
    private List<Order> orderToBeRemoved;

    @BeforeEach
    void setup() {
        tradeQueue = new ArrayBlockingQueue<>(500);
        orderToBeRemoved = new ArrayList<>();
        provider.getOrderBooks().clear();
        tradeService = new TradeOrderConsumer(provider, tradeQueue, o ->orderToBeRemoved.add(o));
        OrderFactory.initIGGOrders();
    }

    @Test
    void testExecuteTradePlan() {
        /*
         * Initial (Collapsed) @see OrderFactory#initIGGOrders for more
         * | Order Count| Ask Quantity| Ask Price| Level| Bid price| Bid Quantity| Order Count|
         * |==================================================================================|
         * | 6          | 60          | 51       | 1    | 50       | 40          | 4          |
         * | 6          | 60          | 52       | 2    | 49       | 70          | 7          |
         * |.........
         * 
         */
        // Given
        Order buyOrder = createOrder();
        buyOrder.setArrivalDateTime(Instant.now().plusSeconds(1));
        buyOrder.setPrice(BigDecimal.valueOf(52));
        buyOrder.setQuantity(new AtomicInteger(80));
        provider.getOrderBookBySymbol("IGG").getBuyOrders().computeIfAbsent(buyOrder.getPrice(), k -> new ConcurrentSkipListSet<>()).add(buyOrder);
        NavigableSet<Order> firstLevel = provider.getOrderBookBySymbol("IGG").getSellOrders().get(BigDecimal.valueOf(51));
        NavigableSet<Order> secondLevel = provider.getOrderBookBySymbol("IGG").getSellOrders().get(BigDecimal.valueOf(52));
        List<Order> expectedRemoved = firstLevel.stream().collect(Collectors.toList()); // Already sorted by price and arrivaldatetime
        Iterator<Order> it = secondLevel.iterator();
        expectedRemoved.add(it.next());
        expectedRemoved.add(it.next());
        expectedRemoved.add(buyOrder);

        // When
        tradeService.executeTradePlan(buyOrder);

        // Then
        assertEquals(expectedRemoved, orderToBeRemoved);
    }

    @Test
    void testExecuteTradePlan_OrderExecutedFirstWithArrival() {
        // Given
        Order buyOrder = createOrder();
        buyOrder.setPrice(BigDecimal.valueOf(51));
        buyOrder.setQuantity(new AtomicInteger(10));
        provider.getOrderBookBySymbol("IGG").getBuyOrders().computeIfAbsent(buyOrder.getPrice(), k -> new ConcurrentSkipListSet<>()).add(buyOrder);
        NavigableSet<Order> firstLevel = provider.getOrderBookBySymbol("IGG").getSellOrders().get(BigDecimal.valueOf(51));
        List<Order> expectedRemoved = new ArrayList<>(); // Already sorted by price and arrivaldatetime
        expectedRemoved.add(firstLevel.first());
        expectedRemoved.add(buyOrder);

        // When
        tradeService.executeTradePlan(buyOrder);
        // Then
        assertEquals(expectedRemoved, orderToBeRemoved);
    }
    
    @Test
    void testExecuteTradePlan_PartiallyFilled() {
        // Given
        Order buyOrder = createOrder();
        buyOrder.setPrice(BigDecimal.valueOf(51));
        buyOrder.setQuantity(new AtomicInteger(5));
        provider.getOrderBookBySymbol("IGG").getBuyOrders().computeIfAbsent(buyOrder.getPrice(), k -> new ConcurrentSkipListSet<>()).add(buyOrder);
        NavigableSet<Order> firstLevel = provider.getOrderBookBySymbol("IGG").getSellOrders().get(BigDecimal.valueOf(51));
        List<Order> expectedRemoved = new ArrayList<>(); // Already sorted by price and arrivaldatetime
        expectedRemoved.add(buyOrder);

        // When
        tradeService.executeTradePlan(buyOrder);

        // Then
        assertEquals(expectedRemoved, orderToBeRemoved);
        assertEquals(5, firstLevel.first().getQuantity().get());
    }
    
    @Test
    void testExecuteTradePlan_NoPriceMatch() {
        // Given
        Order buyOrder = createOrder();
        buyOrder.setPrice(BigDecimal.valueOf(1));

        // When
        tradeService.executeTradePlan(buyOrder);

        // Then
        assertTrue(orderToBeRemoved.isEmpty());
    }
    
}
