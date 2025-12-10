package org.example.lab8;

import java.util.concurrent.TimeUnit;

public class CustomCountDownLatch {
    private volatile int count;
    private final Object lock = new Object();

    public CustomCountDownLatch(int count) {
        this.count = count;
    }

    public void countDown() {
        synchronized (lock) {
            if (count == 0) return;
            count--;
            if (count == 0) {
                lock.notify();//?
            }
        }
    }

    public void await() throws InterruptedException {
        synchronized (this.lock){
            while (this.count > 0)
                this.lock.wait();
        }
    }

    public boolean await(long timeout, TimeUnit unit)
            throws InterruptedException {
        synchronized (this.lock) {
            if (this.count == 0) {
                return true;
            }
            long deadline = System.currentTimeMillis() + unit.toMillis(timeout);

            while (this.count > 0) {
                if (System.currentTimeMillis() >= deadline) {
                    return false;
                }
                this.lock.wait(deadline - System.currentTimeMillis());
            }
            return true;
        }
    }

    public boolean isLocked() throws InterruptedException {
        synchronized (this.lock){
            if (this.count > 0){
                return true;
            }
        }
        return false;
    }
}
