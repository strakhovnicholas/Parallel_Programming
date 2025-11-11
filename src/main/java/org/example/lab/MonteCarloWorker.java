package org.example.lab;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class MonteCarloWorker implements Callable {
    private int experimentsCount;
    private final Random random = new Random();
    private final int threshold;
    private final ProgressMonitor progressMonitor;
    private final int workerId;
    private static AtomicInteger counter = new AtomicInteger(0);

    private final long[] completionTimes;
    private final CountDownLatch latch;
    private final Semaphore semaphore;

    public MonteCarloWorker(int experimentsCount, int threshold, ProgressMonitor progressMonitor
    ,long[] completionTimes, CountDownLatch latch, int workerId, Semaphore semaphore) {
        this.experimentsCount = experimentsCount;
        this.threshold = threshold;
        this.progressMonitor = progressMonitor;
        this.workerId = workerId;
        this.completionTimes = completionTimes;
        this.latch = latch;
        this.semaphore = semaphore;
    }

    private int roll10d20() {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += random.nextInt(20) + 1;
        }
        return sum;
    }

    @Override
    public ExperimentResult call() throws Exception {
        try {
            semaphore.acquire();
            System.out.println("Задача " + workerId + " начата, количество экспериментов: " + experimentsCount);

            int success = 0;
            int reportStep = Math.max(1, experimentsCount / 10);

            for (int i = 1; i <= this.experimentsCount; i++) {
                if (roll10d20() > threshold) success++;
                if (i % reportStep == 0) {
                    progressMonitor.updateProgress(reportStep);
                }
            }

            completionTimes[workerId] = System.currentTimeMillis();
            System.out.println("Задача " + workerId + " завершена");

            latch.countDown();

            return new ExperimentResult(success, experimentsCount);

        } catch (Exception e) {
            System.err.println("ERROR in Task " + workerId + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            semaphore.release();
        }
    }
}
