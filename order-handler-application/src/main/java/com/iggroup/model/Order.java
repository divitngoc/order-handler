package com.iggroup.model;

import java.time.Instant;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

import com.iggroup.model.concurrent.AtomicBigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order implements Comparable<Order> {

    @EqualsAndHashCode.Include
    private Long id;
    private AtomicInteger quantity;
    private AtomicBigDecimal price;
    private Side side;
    private String symbol;
    @Builder.Default
    private Instant arrivalDateTime = Instant.now();
    @Builder.Default
    private AtomicInteger modification = new AtomicInteger();

    @Override
    public int compareTo(Order o) {
        return Comparator.comparing(Order::getArrivalDateTime)
                         .thenComparing(Order::getId)
                         .compare(this, o);
    }

}
