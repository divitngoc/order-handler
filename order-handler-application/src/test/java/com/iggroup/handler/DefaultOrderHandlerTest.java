package com.iggroup.handler;

import static com.iggroup.factory.OrderFactory.createOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iggroup.exception.OrderModificationException;
import com.iggroup.factory.OrderFactory;
import com.iggroup.model.Order;
import com.iggroup.model.Side;
import com.iggroup.provider.OrderBookProvider;
import com.iggroup.service.TradeService;

@ExtendWith(MockitoExtension.class)
class DefaultOrderHandlerTest {

    @Mock
    private TradeService tradeService;
    private DefaultOrderHandler orderHandler;

    private OrderBookProvider provider = OrderBookProvider.getInstance();
    private static final String SYMBOL_IGG = "IGG";

    @BeforeEach
    void setup() {
        provider.getOrderBooks().clear();
        orderHandler = new DefaultOrderHandler(provider, tradeService);
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
                                                                            .containsOnlyKeys(order1.getPrice())
                                                                            .extractingByKey(order1.getPrice())
                                                                            .extracting(NavigableSet::size)
                                                                            .isEqualTo(2);
        // sell orders
        assertThat(provider.getOrderBookBySymbol(SYMBOL_IGG).getSellOrders()).hasSize(1)
                                                                             .containsOnlyKeys(order3.getPrice())
                                                                             .extractingByKey(order3.getPrice())
                                                                             .extracting(NavigableSet::size)
                                                                             .isEqualTo(1);
        verify(tradeService, times(3)).executeTradePlan(any(), any());
    }

    @Test
    void testModify() throws OrderModificationException {
        // Given
        Order order = createOrder(Side.BUY, BigDecimal.TEN);
        provider.getOrderBookBySymbol(SYMBOL_IGG)
                .getBuyOrders()
                .computeIfAbsent(order.getPrice(), k -> new ConcurrentSkipListSet<>())
                .add(order);
        Order modifiedOrder = createOrder(Side.BUY, BigDecimal.ONE);
        modifiedOrder.setId(order.getId());
        modifiedOrder.setQuantity(3);

        // When
        orderHandler.modifyOrder(order, modifiedOrder);

        // Then
        assertThat(provider.getOrderBookBySymbol(SYMBOL_IGG).getBuyOrders()).hasSize(1)
                                                                            .containsOnlyKeys(modifiedOrder.getPrice())
                                                                            .extractingByKey(modifiedOrder.getPrice());

        assertThat(provider.getOrderBookBySymbol(SYMBOL_IGG)
                           .getBuyOrders()
                           .get(modifiedOrder.getPrice())).extracting(Order::getModification,
                                                                      Order::getQuantity,
                                                                      Order::getPrice)
                                                          .containsOnly(tuple(modifiedOrder.getModification(),
                                                                              modifiedOrder.getQuantity(),
                                                                              modifiedOrder.getPrice()));
        verify(tradeService, times(1)).executeTradePlan(any(), any());
    }

    @Test
    void testModifyModificationException() throws OrderModificationException {
        // Given
        Order order = createOrder(Side.BUY, BigDecimal.TEN);
        order.setModification(5);
        provider.getOrderBookBySymbol(SYMBOL_IGG)
                .getBuyOrders()
                .computeIfAbsent(order.getPrice(), k -> new ConcurrentSkipListSet<>())
                .add(order);
        Order modifiedOrder = createOrder(Side.BUY, BigDecimal.ONE);
        modifiedOrder.setId(order.getId());
        modifiedOrder.setQuantity(3);

        // When & Then
        assertThrows(OrderModificationException.class, () -> orderHandler.modifyOrder(order, modifiedOrder));
        verifyNoInteractions(tradeService);
    }

    @Test
    void testRemove() {
        // Given
        Order order = createOrder();
        NavigableSet<Order> orders = provider.getOrderBookBySymbol(SYMBOL_IGG)
                                             .getBuyOrders()
                                             .computeIfAbsent(order.getPrice(), k -> new ConcurrentSkipListSet<>());
        orders.add(order);

        // When
        orderHandler.removeOrder(order);

        // Then
        assertThat(provider.getOrderBookBySymbol(SYMBOL_IGG).getBuyOrders()).isEmpty();
        verifyNoInteractions(tradeService);
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
        verifyNoInteractions(tradeService);
    }
}
