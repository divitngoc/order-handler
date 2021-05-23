package com.iggroup.producer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.iggroup.model.Order;
import com.iggroup.model.Side;

/**
 * 
 * Ideally there would be another application that would produce Orders to a MQ.
 * This is a simple producer that creates orders
 * 
 */
public class OrderProducer {

    public static final AtomicLong ATOMIC_LONG = new AtomicLong();
    private Random random = new Random();

    public Order produce(final String symbol) {
        return Order.builder()
                    .id(ATOMIC_LONG.addAndGet(1L))
                    .arrivalDateTime(Instant.now())
                    .symbol(symbol)
                    .quantity(new AtomicInteger(random.nextInt(20) + 1))
                    .price(BigDecimal.valueOf(random.nextInt(50) + 1))
                    .side(random.nextBoolean() ? Side.BUY : Side.SELL)
                    .build();
    }
}
