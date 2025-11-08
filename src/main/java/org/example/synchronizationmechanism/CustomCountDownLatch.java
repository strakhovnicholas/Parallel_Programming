package synchronizationmechanism;

import java.util.concurrent.TimeUnit;

public class CustomCountDownLatch {
    private final Object lock = new Object();
    private volatile int count;

    public CustomCountDownLatch(int count) {
        this.count = count;
    }

    public void countDown() {
        synchronized (lock) {
            count--;
            if (count == 0) {
                lock.notifyAll();
            }
        }
    }

    public boolean await(Long time, TimeUnit timeUnit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeUnit.toMillis(time);
        synchronized (lock) {
            while (count != 0) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    return false;
                }
                lock.wait(remaining);
            }
            count--;
            return true;
        }
    }

    public void await() throws InterruptedException {
        synchronized (lock) {
            while (count != 0) {
                lock.wait();
            }
        }
    }

    public boolean isLocked() {
        synchronized (lock) {
            return count == 0;
        }
    }
}
