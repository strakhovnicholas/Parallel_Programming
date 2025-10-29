package org.example.lab4;

import java.util.Random;

public class MonteCarloWorker implements Runnable {
    private int experimentsCount;
    private final Random random = new Random();
    private final int threshold;
    private final ProgressMonitor progressMonitor;
    private final LazyResultStorage lazyResultStorage;


    public MonteCarloWorker(int experimentsCount, int threshold, ProgressMonitor progressMonitor,
                            LazyResultStorage lazyResultStorage) {
        this.experimentsCount = experimentsCount;
        this.threshold = threshold;
        this.progressMonitor = progressMonitor;
        this.lazyResultStorage = lazyResultStorage;

    }

    @Override
    public void run() {
        for (int i = 1; i <= this.experimentsCount; i++) {
            if (roll10d20() > threshold) success++;

            if (i % step == 0) {
                try {
                    double percent = 100.0 * i / experiments;
                    System.out.printf("Прогресс: %.1f%%\n", percent);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private int roll10d20() {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += random.nextInt(20) + 1;
        }
        return sum;
    }
}
