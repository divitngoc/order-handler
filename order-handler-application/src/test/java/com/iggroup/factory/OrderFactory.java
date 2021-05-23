package com.iggroup.factory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import com.iggroup.model.Order;
import com.iggroup.model.Side;
import com.iggroup.model.concurrent.AtomicBigDecimal;
import com.iggroup.provider.OrderBookProvider;

public final class OrderFactory {
    private static long ID_COUNTER;

    private OrderFactory() {}

    public static Order createOrder() {
        return createOrder(Side.BUY, BigDecimal.TEN);
    }

    public static Order createOrder(Side side, BigDecimal price) {
        ID_COUNTER++;
        return Order.builder()
                    .id(ID_COUNTER)
                    .arrivalDateTime(Instant.now())
                    .symbol("IGG")
                    .quantity(new AtomicInteger(10))
                    .price(AtomicBigDecimal.valueOf(price))
                    .side(side)
                    .build();
    }

    /**
     * 
     * Adds orders to IGG orderBook
     * <br>
     * Note: There's 250 buy orders, 250 sell orders and orders will have different arrival dates.
     * <br>
     * Each Order has 10 quantity for simplicity
     * <br>
     * <br>
     * Generates:
     * <pre>
  ___________________________________________________________________________________
 | Order Count| Ask Quantity| Ask Price| Level| Bid price| Bid Quantity| Order Count|
 |==================================================================================|
 | 6          | 60          | 51       | 1    | 50       | 40          | 4          |
 | 6          | 60          | 52       | 2    | 49       | 70          | 7          |
 | 2          | 20          | 53       | 3    | 48       | 50          | 5          |
 | 7          | 70          | 54       | 4    | 47       | 30          | 3          |
 | 6          | 60          | 55       | 5    | 46       | 40          | 4          |
 | 3          | 30          | 56       | 6    | 45       | 30          | 3          |
 | 9          | 90          | 57       | 7    | 44       | 100         | 10         |
 | 6          | 60          | 58       | 8    | 43       | 80          | 8          |
 | 4          | 40          | 59       | 9    | 42       | 70          | 7          |
 | 4          | 40          | 60       | 10   | 41       | 20          | 2          |
 | 7          | 70          | 61       | 11   | 40       | 60          | 6          |
 | 4          | 40          | 62       | 12   | 39       | 40          | 4          |
 | 4          | 40          | 63       | 13   | 38       | 60          | 6          |
 | 3          | 30          | 64       | 14   | 37       | 30          | 3          |
 | 5          | 50          | 65       | 15   | 36       | 60          | 6          |
 | 2          | 20          | 66       | 16   | 35       | 70          | 7          |
 | 3          | 30          | 67       | 17   | 34       | 60          | 6          |
 | 5          | 50          | 68       | 18   | 33       | 40          | 4          |
 | 11         | 110         | 69       | 19   | 32       | 50          | 5          |
 | 8          | 80          | 70       | 20   | 31       | 90          | 9          |
 | 8          | 80          | 71       | 21   | 30       | 10          | 1          |
 | 3          | 30          | 72       | 22   | 29       | 30          | 3          |
 | 2          | 20          | 73       | 23   | 28       | 60          | 6          |
 | 8          | 80          | 74       | 24   | 27       | 40          | 4          |
 | 4          | 40          | 75       | 25   | 26       | 40          | 4          |
 | 3          | 30          | 76       | 26   | 25       | 60          | 6          |
 | 8          | 80          | 77       | 27   | 24       | 20          | 2          |
 | 4          | 40          | 78       | 28   | 23       | 20          | 2          |
 | 10         | 100         | 79       | 29   | 22       | 110         | 11         |
 | 7          | 70          | 80       | 30   | 21       | 70          | 7          |
 | 3          | 30          | 81       | 31   | 20       | 20          | 2          |
 | 2          | 20          | 82       | 32   | 19       | 20          | 2          |
 | 4          | 40          | 83       | 33   | 18       | 40          | 4          |
 | 3          | 30          | 84       | 34   | 17       | 70          | 7          |
 | 7          | 70          | 85       | 35   | 16       | 30          | 3          |
 | 2          | 20          | 86       | 36   | 15       | 50          | 5          |
 | 4          | 40          | 87       | 37   | 14       | 40          | 4          |
 | 7          | 70          | 88       | 38   | 13       | 20          | 2          |
 | 2          | 20          | 89       | 39   | 12       | 50          | 5          |
 | 5          | 50          | 90       | 40   | 11       | 70          | 7          |
 | 4          | 40          | 91       | 41   | 10       | 70          | 7          |
 | 4          | 40          | 92       | 42   | 9        | 20          | 2          |
 | 1          | 10          | 93       | 43   | 8        | 50          | 5          |
 | 4          | 40          | 94       | 44   | 7        | 40          | 4          |
 | 7          | 70          | 95       | 45   | 6        | 50          | 5          |
 | 3          | 30          | 96       | 46   | 5        | 50          | 5          |
 | 8          | 80          | 97       | 47   | 4        | 70          | 7          |
 | 6          | 60          | 98       | 48   | 3        | 60          | 6          |
 | 6          | 60          | 99       | 49   | 2        | 70          | 7          |
 | 6          | 60          | 100      | 50   | 1        | 60          | 6          |
     * 
     * </pre>
     */
    public static void initIGGOrders() {
        OrderBookProvider provider = OrderBookProvider.getInstance();
        final Random generator = new Random(1234L);
        IntStream.range(0, 250).forEach(i -> {
            Order order = createOrder(Side.SELL, BigDecimal.valueOf(generator.nextInt(50) + 51));
            order.setArrivalDateTime(order.getArrivalDateTime().minusSeconds(i));
            provider.getOrderBookBySymbol("IGG")
                    .getSellOrders()
                    .computeIfAbsent(order.getPrice().get(), k -> new ConcurrentSkipListSet<>())
                    .add(order);
        });

        IntStream.range(0, 250).forEach(i -> {
            Order order = createOrder(Side.BUY, BigDecimal.valueOf(generator.nextInt(50) + 1));
            order.setArrivalDateTime(order.getArrivalDateTime().minusSeconds(i));
            provider.getOrderBookBySymbol("IGG")
                    .getBuyOrders()
                    .computeIfAbsent(order.getPrice().get(), k -> new ConcurrentSkipListSet<>())
                    .add(order);
        });
        // PrinterUtils.printStatus(provider.getOrderBookBySymbol("IGG"));
    }
}
