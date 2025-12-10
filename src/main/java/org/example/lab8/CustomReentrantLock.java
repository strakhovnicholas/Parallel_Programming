package org.example.lab8;

import java.util.concurrent.TimeUnit;

public class CustomReentrantLock {
    private Thread owner = null;
    private int holdCount = 0;
    private Object monitor = new Object();

    public void lock() throws InterruptedException {
        synchronized (monitor) {
            Thread current = Thread.currentThread();
            while (this.owner != null && this.owner != current) {
                this.monitor.wait();
            }
            this.owner = current;
            holdCount++;
        }
    }

    public boolean lock(long timeout, TimeUnit unit) throws InterruptedException {
        synchronized (monitor) {
            Thread current = Thread.currentThread();
            long remaining = unit.toMillis(timeout);
            long deadline = System.currentTimeMillis() + remaining;

            while (owner != null && owner != current) {
                remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    return false;
                }
                monitor.wait(remaining);

                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            }

            owner = current;
            holdCount++;
            return true;
        }
    }

    public boolean tryLock() {
        synchronized (monitor) {
            Thread current = Thread.currentThread();

            if (owner == null) {
                owner = current;
                holdCount++;
                return true;
            }
            return false;
        }
    }

    public void unlock() {
        synchronized (monitor) {
            Thread current = Thread.currentThread();

            if (owner != current) {
                throw new IllegalMonitorStateException(
                        "Текущий поток не владелец"
                );
            }
            holdCount--;
            if (holdCount == 0) {
                owner = null;
                monitor.notify();
            }
        }
    }
}