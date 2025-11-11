package org.example.lab.synchronizationmechanism;

import java.util.concurrent.TimeUnit;

public class CustomReentrantLock {
    private final Object lock = new Object();
    private volatile boolean isLocked = false;

    public void lock() throws InterruptedException {
        synchronized (lock) {
            while (isLocked) {
                lock.wait();
            }
            isLocked = true;
        }
    }

    public boolean lock(Long count, TimeUnit timeUnit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeUnit.toMillis(count);
        synchronized (lock) {
            while (isLocked) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    return false;
                }
                lock.wait(remaining);
            }
            isLocked = true;
            return true;
        }
    }

    public boolean tryLock() {
        synchronized (lock) {
            if (isLocked) {
                return false;
            }
            isLocked = true;
            return true;
        }
    }

    public void unlock() {
        synchronized (lock) {
            isLocked = false;
            lock.notify();
        }
    }
}
