package org.example.lab8;

import java.util.concurrent.TimeUnit;


public class CustomSemaphore {
    private final Object lock = new Object();
    private volatile int permits;

    public CustomSemaphore(int countPermissions) {
        this.permits = countPermissions;
    }

    public void acquire () throws InterruptedException {
        synchronized (lock){
            while (this.permits == 0){
                lock.wait();
            }
            this.permits--;
        }
    }

    public boolean acquire(int permits, long timeout, TimeUnit unit)
            throws InterruptedException {
        long endTime = System.currentTimeMillis() + unit.toMillis(timeout);

        synchronized (this.lock) {
            if (this.permits >= permits) {
                this.permits -= permits;
                return true;
            }

            while (this.permits < permits) {
                long remaining = endTime - System.currentTimeMillis();
                if (remaining <= 0) {
                    return false;
                }
                this.lock.wait(remaining);
            }
            this.permits -= permits;
            return true;
        }
    }

    public boolean tryAcquire(int permits){
        synchronized (this.lock){
            if (this.permits >= permits){
                this.permits -= permits;
                return true;
            }
            return false;
        }
    }

    public void release(){
        synchronized (this.lock){
            this.permits += 1;
            this.lock.notify(); //?
        }
    }
}
