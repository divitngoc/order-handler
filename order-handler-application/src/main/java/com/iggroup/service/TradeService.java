package com.iggroup.service;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;

import com.iggroup.model.Order;
import com.iggroup.model.Side;
import com.iggroup.provider.OrderBookProvider;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TradeService {

    private static final TradeService INSTANCE = new TradeService(OrderBookProvider.getInstance());
    private static final String SIDE_TRADE_EXECUTED_LOG_FORMAT = "OrderId [{}]: {} TRADE EXECUTED at price: [{}] and amount [{}] against OrderId [{}]";

    private final OrderBookProvider provider;

    public static TradeService getInstance() {
        return INSTANCE;
    }
    
    /**
     * 
     * Checks if order price matches against the order (level 1 of order book).
     * If matches, execute trade.
     * <br>
     * Scenario 1: Order fills the opposite side order but with quantity left, 
     *             continue to look for next level for price match and repeat
     * <br>
     * Scenario 2: Order completely fills the opposite side order with all of the quantity
     * <br>
     * Scenario 3: Order unable to completely fills the opposite side order.
     * <br>
     * @param order
     * @param removeConsumer
     */
    @Synchronized
    public void executeTradePlan(final Order order, final Consumer<Order> removeConsumer) {
        // Double check if order is still valid
        if (!provider.getOrderBookBySymbol(order.getSymbol())
                     .getOrders(order.getSide())
                     .getOrDefault(order.getPrice(), new ConcurrentSkipListSet<>())
                     .contains(order))
            return;
        
        final ConcurrentNavigableMap<BigDecimal, NavigableSet<Order>> orderMap = getOppositeSideOrderMap(order.getSymbol(), order.getSide());
        log.debug("Checking if price matches for trade...");
        int quantityLeft = order.getQuantity();

        for (Entry<BigDecimal, NavigableSet<Order>> entry : orderMap.entrySet()) {
            if (!priceMatch(entry.getKey(), order)) {
                log.debug("No further price match for trade.");
                return;
            }

            // Ask price is lower than/equal to bid price
            // Or the bid price is greater than the ask price
            log.debug("Price matches...");
            for (Iterator<Order> iterator = entry.getValue().iterator(); iterator.hasNext();) {
                final Order orderToTrade = iterator.next();
                log.info("Trade executing for orderId [{}]... against orderId [{}]", order.getId(), orderToTrade.getId());
                int previousQuantTotal = quantityLeft;
                quantityLeft -= orderToTrade.getQuantity();
                if (quantityLeft > 0) {
                    // Order to trade is completely filled and order is partially filled
                    order.setQuantity(order.getQuantity() - (quantityLeft - previousQuantTotal));
                    removeConsumer.accept(orderToTrade);
                    log.debug(SIDE_TRADE_EXECUTED_LOG_FORMAT, order.getId(), order.getSide(), entry.getKey(), Math.abs(quantityLeft - previousQuantTotal), orderToTrade.getId());
                } else if (quantityLeft == 0) {
                    // Both orders is completely filled
                    orderToTrade.setQuantity(0);
                    order.setQuantity(0);
                    removeConsumer.accept(orderToTrade);
                    removeConsumer.accept(order);
                    log.debug(SIDE_TRADE_EXECUTED_LOG_FORMAT, order.getId(), order.getSide(), entry.getKey(), order.getQuantity(), orderToTrade.getId());
                    return;
                } else {
                    // Order to trade partially filled and order is completely filled
                    orderToTrade.setQuantity(Math.abs(quantityLeft));
                    order.setQuantity(0);
                    removeConsumer.accept(order);
                    log.debug(SIDE_TRADE_EXECUTED_LOG_FORMAT, order.getId(), order.getSide(), entry.getKey(), previousQuantTotal, orderToTrade.getId());
                    return;
                }
            }
        }
    }

    /**
     * 
     * If Ask price is lower than/equal to bid price Or the bid price is greater
     * than the ask price. The Price matches and will return true, otherwise false
     * 
     * @param levelPrice
     * @param order
     * @return
     */
    private boolean priceMatch(BigDecimal levelPrice, Order order) {
        return levelPrice.compareTo(order.getPrice()) != (order.getSide() == Side.BUY ? 1 : -1);
    }

    private ConcurrentNavigableMap<BigDecimal, NavigableSet<Order>> getOppositeSideOrderMap(final String symbol, final Side side) {
        return provider.getOrderBookBySymbol(symbol).getOrders(side == Side.BUY ? Side.SELL : Side.BUY);
    }

}
