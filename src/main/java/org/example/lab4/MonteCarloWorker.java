package org.example.lab4;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class MonteCarloWorker implements Runnable {
    private int experimentsCount;
    private final Random random = new Random();
    private final int threshold;
    private final ProgressMonitor progressMonitor;
    private final LazyResultStorage lazyResultStorage;
    private static AtomicInteger counter = new AtomicInteger(0);
    private final int id;

    public MonteCarloWorker(int experimentsCount, int threshold, ProgressMonitor progressMonitor,
                            LazyResultStorage lazyResultStorage) {
        this.experimentsCount = experimentsCount;
        this.threshold = threshold;
        this.progressMonitor = progressMonitor;
        this.lazyResultStorage = lazyResultStorage;
        this.id = counter.getAndIncrement();

    }

    @Override
    public void run() {
        System.out.printf("Поток %d запущен, количество операций на поток: %d \n",this.id, this.experimentsCount);
        int success = 0;
        for (int i = 1; i <= this.experimentsCount; i++) {
            if (roll10d20() > threshold) success++;
        }
        System.out.printf("Поток %d завершил работу \n", this.id);
        this.lazyResultStorage.addPartialResult(success, experimentsCount);
    }

    private int roll10d20() {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += random.nextInt(20) + 1;
        }
        return sum;
    }
}
