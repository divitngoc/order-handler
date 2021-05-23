package com.iggroup.handler;

import static com.iggroup.factory.OrderFactory.createOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.NavigableSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iggroup.exception.OrderModificationException;
import com.iggroup.factory.OrderFactory;
import com.iggroup.model.Order;
import com.iggroup.model.Side;
import com.iggroup.provider.OrderBookProvider;

@ExtendWith(MockitoExtension.class)
class DefaultOrderHandlerTest {

    private DefaultOrderHandler orderHandler;
    private BlockingQueue<Order> tradeQueue = new ArrayBlockingQueue<>(500);

    private OrderBookProvider provider = OrderBookProvider.getInstance();
    private static final String SYMBOL_IGG = "IGG";

    @BeforeEach
    void setup() {
        provider.getOrderBooks().clear();
        tradeQueue.clear();
        orderHandler = new DefaultOrderHandler(provider, tradeQueue);
    }

    @Test
    void testAdd() {
        // Given
        Order order1 = createOrder();
        Order order2 = createOrder();
        Order order3 = createOrder(Side.SELL, BigDecimal.valueOf(40));

        // When
        orderHandler.addOrder(order1);
        orderHandler.addOrder(order2);
        orderHandler.addOrder(order3);

        // Then
        // buy orders
        assertThat(provider.getOrderBookBySymbol(SYMBOL_IGG).getBuyOrders()).hasSize(1)
                                                                            .containsOnlyKeys(order1.getPrice().get())
                                                                            .extractingByKey(order1.getPrice().get())
                                                                            .extracting(NavigableSet::size)
                                                                            .isEqualTo(2);
        // sell orders
        assertThat(provider.getOrderBookBySymbol(SYMBOL_IGG).getSellOrders()).hasSize(1)
                                                                             .containsOnlyKeys(order3.getPrice().get())
                                                                             .extractingByKey(order3.getPrice().get())
                                                                             .extracting(NavigableSet::size)
                                                                             .isEqualTo(1);
        assertThat(tradeQueue).hasSize(3);
    }

    @Test
    void testModify() throws OrderModificationException {
        // Given
        Order order = createOrder(Side.BUY, BigDecimal.TEN);
        provider.getOrderBookBySymbol(SYMBOL_IGG)
                .getBuyOrders()
                .computeIfAbsent(order.getPrice().get(), k -> new ConcurrentSkipListSet<>())
                .add(order);
        Order modifiedOrder = createOrder(Side.BUY, BigDecimal.ONE);
        modifiedOrder.setId(order.getId());
        modifiedOrder.setQuantity(new AtomicInteger(3));

        // When
        orderHandler.modifyOrder(order, modifiedOrder);

        // Then
        assertThat(provider.getOrderBookBySymbol(SYMBOL_IGG).getBuyOrders()).hasSize(1)
                                                                            .containsOnlyKeys(modifiedOrder.getPrice().get());

        assertThat(provider.getOrderBookBySymbol(SYMBOL_IGG)
                           .getBuyOrders()
                           .get(modifiedOrder.getPrice().get())).extracting(o -> o.getModification().get(),
                                                                            o -> o.getQuantity().get(),
                                                                            o -> o.getPrice().get())
                                                                .containsOnly(tuple(1,
                                                                                    modifiedOrder.getQuantity().get(),
                                                                                    modifiedOrder.getPrice().get()));
        assertThat(tradeQueue).hasSize(1);
    }

    @Test
    void testModifyModificationException() throws OrderModificationException {
        // Given
        Order order = createOrder(Side.BUY, BigDecimal.TEN);
        order.setModification(new AtomicInteger(5));
        provider.getOrderBookBySymbol(SYMBOL_IGG)
                .getBuyOrders()
                .computeIfAbsent(order.getPrice().get(), k -> new ConcurrentSkipListSet<>())
                .add(order);
        Order modifiedOrder = createOrder(Side.BUY, BigDecimal.ONE);
        modifiedOrder.setId(order.getId());
        modifiedOrder.setQuantity(new AtomicInteger(3));

        // When & Then
        assertThrows(OrderModificationException.class, () -> orderHandler.modifyOrder(order, modifiedOrder));
        assertThat(tradeQueue).isEmpty();
    }

    @Test
    void testRemove() {
        // Given
        Order order = createOrder();
        NavigableSet<Order> orders = provider.getOrderBookBySymbol(SYMBOL_IGG)
                                             .getBuyOrders()
                                             .computeIfAbsent(order.getPrice().get(), k -> new ConcurrentSkipListSet<>());
        orders.add(order);

        // When
        orderHandler.removeOrder(order);

        // Then
        assertThat(provider.getOrderBookBySymbol(SYMBOL_IGG).getBuyOrders()).isEmpty();
        assertThat(tradeQueue).isEmpty();
    }

    @Test
    void testGetPrice() {
        // Given
        OrderFactory.initIGGOrders();

        // When
        double averagePrice = orderHandler.getPrice(SYMBOL_IGG, 100, Side.BUY);

        // Then
        // ((40 * 50) + (60 * 49)) / 100
        assertEquals(49.4, averagePrice);
        assertThat(tradeQueue).isEmpty();
    }
}
