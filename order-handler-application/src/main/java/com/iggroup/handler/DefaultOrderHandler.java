package com.iggroup.handler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.iggroup.exception.OrderModificationException;
import com.iggroup.lock.OrdersLock;
import com.iggroup.model.Order;
import com.iggroup.model.OrderBook;
import com.iggroup.model.Side;
import com.iggroup.provider.OrderBookProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DefaultOrderHandler implements OrderHandler {

    private final OrderBookProvider provider;
    private final BlockingQueue<Order> tradeQueue;

    @Override
    public void addOrder(final Order order) {
        log.debug("Adding Order [{}]...", order);
        provider.getOrderBookBySymbol(order.getSymbol())
                .getOrders(order.getSide())
                .computeIfAbsent(order.getPrice().get(), k -> new ConcurrentSkipListSet<>())
                .add(order);
        log.debug("OrderId [{}] has been added.", order.getId());
        tradeQueue.add(order);
    }

    /**
     *
     * Only can modify price and quantity of an order
     *
     */
    @Override
    public void modifyOrder(final Order order, final Order modifiedOrder) throws OrderModificationException {
        log.debug("Modifying OrderId [{}]...", order.getId());
        OrdersLock.acquireLock(order.getId()).lock();
        if (!provider.checkIfOrderExists(order)) {
            OrdersLock.unlock(order.getId());
            log.debug("Order does not exist anymore for modifying");
            return;
        }

        if (order.getModification().get() > 4) {
            log.debug("OrderId [{}] has more than 4 modifications applied, cannot be modified further", modifiedOrder.getId());
            OrdersLock.unlock(order.getId());
            throw new OrderModificationException("OrderId [" + order.getId() + "] has more than 4 modifications applied, cannot be modified further.");
        }

        log.debug("Modifying OrderId [{}] price [{}] and quantity [{}]...", order.getId(), order.getPrice().get(), order.getQuantity().get());
        order.getModification().incrementAndGet();
        order.getQuantity().set(modifiedOrder.getQuantity().intValue());

        if (order.getPrice().get() != modifiedOrder.getPrice().get()) {
            log.debug("Modifying OrderId [{}] price [{}] and quantity [{}] by removing and adding...", order.getId(), order.getPrice().get(),
                      order.getQuantity().get());
            removeOrder(order);
            order.getPrice().set(modifiedOrder.getPrice().get());
            addOrder(order);
        }
        log.debug("OrderId [{}] has been modified with new price of [{}] and quantity [{}].", modifiedOrder.getId(), modifiedOrder.getPrice().get(),
                  modifiedOrder.getQuantity().get());
        OrdersLock.unlock(order.getId());
    }

    @Override
    public void removeOrder(final Order order) {
        log.debug("Removing orderId [{}]...", order.getId());
        OrdersLock.acquireLock(order.getId()).tryLock();
        OrderBook orderBook = provider.getOrderBookBySymbol(order.getSymbol());
        ConcurrentNavigableMap<BigDecimal, NavigableSet<Order>> ordersMap = orderBook.getOrders(order.getSide());
        NavigableSet<Order> orders = ordersMap.get(order.getPrice().get());

        if (orders.size() == 1) {
            ordersMap.remove(order.getPrice().get()); // Remove price level and (navigableSet as well as last element)
        } else {
            orders.remove(order);
        }
        OrdersLock.unlock(order.getId());
        log.debug("OrderId [{}] has been removed.", order.getId());
    }

    /**
     *
     * Time Complexity: O(n + m) where n is the level and m are the sum of orders
     * which corresponds to -> O(N)
     *
     */
    @Override
    public double getPrice(final String symbol, final int quantity, final Side side) {
        log.debug("Getting best price for symbol [{}], quantity [{}], and order type [{}]...", symbol, quantity, side);
        final BigDecimal quantityBdec = BigDecimal.valueOf(quantity); // For calculation
        final ConcurrentNavigableMap<BigDecimal, NavigableSet<Order>> orders = provider.getOrderBookBySymbol(symbol)
                                                                                       .getOrders(side);
        BigDecimal currentQuantity = BigDecimal.ZERO;
        BigDecimal averagePrice = BigDecimal.ZERO;
        for (Entry<BigDecimal, NavigableSet<Order>> entry : orders.entrySet()) {
            final BigDecimal totalQuantityByPrice = entry.getValue()
                                                         .stream()
                                                         .map(o -> BigDecimal.valueOf(o.getQuantity().get()))
                                                         .reduce((a, b) -> a.add(b))
                                                         .orElseGet(() -> BigDecimal.ZERO);
            final BigDecimal previousCurrQuantity = currentQuantity;
            currentQuantity = currentQuantity.add(totalQuantityByPrice);
            if (currentQuantity.compareTo(quantityBdec) >= 0) {
                final BigDecimal diff = quantityBdec.subtract(previousCurrQuantity);
                averagePrice = averagePrice.add(entry.getKey().multiply(diff));
                break;
            } else {
                averagePrice = averagePrice.add(entry.getKey().multiply(currentQuantity));
            }
        }
        double result = averagePrice.divide(quantityBdec, 4, RoundingMode.HALF_UP).doubleValue();
        log.debug("Best average price for symbol [{}], with quantity [{}], and order type [{}] is: [{}]", symbol, quantity, side, result);
        return result;
    }

}
