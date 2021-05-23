package com.iggroup.model.concurrent;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

public class AtomicBigDecimal extends AtomicReference<BigDecimal> {

    private static final long serialVersionUID = 1L;

    public AtomicBigDecimal(final BigDecimal bigDecimal) {
        set(bigDecimal);
    }

    public static AtomicBigDecimal valueOf(BigDecimal bigDecimal) {
        return new AtomicBigDecimal(bigDecimal);
    }
    
    public static AtomicBigDecimal valueOf(double bigDecimal) {
        return new AtomicBigDecimal(BigDecimal.valueOf(bigDecimal));
    }
}
