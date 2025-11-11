package org.example.lab;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ProgressMonitor {
    private final int totalExperiments;
    private final AtomicInteger completedExperiments = new AtomicInteger(0);
    private final ReentrantLock printLock = new ReentrantLock();

    public ProgressMonitor(int totalExperiments) {
        this.totalExperiments = totalExperiments;
    }

    public void updateProgress(int done) {
        int totalDone = completedExperiments.addAndGet(done);
        double progress = (double) totalDone / totalExperiments * 100;
        printLock.lock();
        try {
            System.out.printf("Общий прогресс: %.2f%%\n", progress);
        } finally {
            printLock.unlock();
        }
    }
}