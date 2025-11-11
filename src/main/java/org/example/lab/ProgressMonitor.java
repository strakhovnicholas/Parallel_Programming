package org.example.lab;

import java.util.concurrent.atomic.AtomicInteger;

public class ProgressMonitor {
    private final int totalExperiments;
    private final AtomicInteger completedExperiments = new AtomicInteger(0);

    public ProgressMonitor(int totalExperiments) {
        this.totalExperiments = totalExperiments;
    }

    public void updateProgress(int done) {
        int totalDone = completedExperiments.addAndGet(done);
        double progress = (double) totalDone / totalExperiments * 100;
        System.out.printf("Общий прогресс: %.2f%%\n", progress);
    }
}