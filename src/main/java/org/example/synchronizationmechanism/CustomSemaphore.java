package synchronizationmechanism;

import java.util.concurrent.TimeUnit;

public class CustomSemaphore {
    private final Object lock = new Object();
    private volatile int permits;


    public CustomSemaphore(int countPermissions) {
        this.permits = countPermissions;
    }

    public void acquire() throws InterruptedException {
        synchronized (lock) {
            while (permits == 0) {
                lock.wait();
            }
            permits--;
        }
    }

        public boolean acquire(Long count, TimeUnit timeUnit) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeUnit.toMillis(count);
            synchronized (lock) {
                while (permits == 0) {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0) {
                        return false;
                    }
                    lock.wait(remaining);
                }
                permits--;
                return true;
            }
        }

    public boolean tryAcquire() {
        synchronized (lock) {
            if (permits == 0) {
                return false;
            }
            permits--;
            return true;
        }
    }

    public void release() {
        synchronized (lock) {
            permits++;
            lock.notify();
        }
    }

}
