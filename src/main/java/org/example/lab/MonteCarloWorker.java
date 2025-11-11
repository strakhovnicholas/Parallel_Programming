package org.example.lab;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

public class MonteCarloWorker implements Callable {
    private int experimentsCount;
    private final Random random = new Random();
    private final int threshold;
    private final ProgressMonitor progressMonitor;
    private static AtomicInteger counter = new AtomicInteger(0);

    public MonteCarloWorker(int experimentsCount, int threshold, ProgressMonitor progressMonitor) {
        this.experimentsCount = experimentsCount;
        this.threshold = threshold;
        this.progressMonitor = progressMonitor;
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
        int success = 0;
        int reportStep = Math.max(1, experimentsCount / 10);
        for (int i = 1; i <= this.experimentsCount; i++) {
            if (roll10d20() > threshold) success++;
            if (i % reportStep == 0) {
                progressMonitor.updateProgress(reportStep);
            }
        }
        return new ExperimentResult(success, experimentsCount);
    }
}
