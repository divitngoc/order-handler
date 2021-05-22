package com.iggroup.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;

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
    private Integer quantity;
    private BigDecimal price;
    private Side side;
    private String symbol;
    @Builder.Default
    private Instant arrivalDateTime = Instant.now();
    @Builder.Default
    private int modification = 0;

    @Override
    public int compareTo(Order o) {
        return Comparator.comparing(Order::getArrivalDateTime)
                         .thenComparing(Order::getId)
                         .compare(this, o);
    }

}
