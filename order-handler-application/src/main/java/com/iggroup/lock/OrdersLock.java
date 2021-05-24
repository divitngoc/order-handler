package com.iggroup.lock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrdersLock {

    private OrdersLock() {}

    private static final ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>(500);

    public static boolean contains(final Long id) {
        return locks.contains(id);
    }

    public static Lock acquireLock(final Long id) {
        log.debug("Acquring lock for OrderId: {}", id);
        return locks.computeIfAbsent(id, k -> new ReentrantLock());
    }

    public static void unlock(final Long id) {
        log.debug("Notifying lock for OrderId: {}", id);
        locks.get(id).unlock();
    }

}
