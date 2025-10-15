package org.example.lab4;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class Task implements Runnable {
    private static AtomicInteger counter = new AtomicInteger(0);
    private final int id;
    private final MonteCarloAdjust adjust;

    public Task(double epsilon) {
        this.id = counter.getAndIncrement();
        this.adjust = MonteCarloAdjust
                .builder()
                .epsilon(epsilon)
                .build();
    }

    @Override
    public void run() {
        this.adjust.tuneEpsilon();
    }
}
