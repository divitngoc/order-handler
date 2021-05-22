package com.iggroup.producer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import com.iggroup.model.Order;
import com.iggroup.model.Side;

public class OrderProducer {

    public static AtomicLong ATOMIC_LONG = new AtomicLong();
    private Random random = new Random();
    
    public Order produce() {
        return Order.builder()
                    .id(ATOMIC_LONG.addAndGet(1L))
                    .arrivalDateTime(Instant.now())
                    .symbol("IGG")
                    .quantity(random.nextInt(20) + 1)
                    .price(BigDecimal.valueOf(random.nextInt(50) + 1))
                    .side(random.nextBoolean() ? Side.BUY : Side.SELL)
                    .build();
    }
}
