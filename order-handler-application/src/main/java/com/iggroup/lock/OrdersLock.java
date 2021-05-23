package com.iggroup.lock;

import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrdersLock {

    private OrdersLock() {}

    private static final ConcurrentHashMap<Long, Object> locks = new ConcurrentHashMap<>(500);

    public static boolean contains(final Long id) {
        return locks.contains(id);
    }

    public static Object acquireLock(final Long id) {
        log.debug("Acquring lock for OrderId: {}", id);
        return locks.computeIfAbsent(id, k -> new Object());
    }

    public static void notifyLock(final Long id) {
        log.debug("Notifying lock for OrderId: {}", id);
        locks.get(id).notifyAll();
    }

}
