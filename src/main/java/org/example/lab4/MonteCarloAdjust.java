package org.example.lab4;

import lombok.Builder;

import java.util.Random;

@Builder
public class MonteCarloAdjust{
    private double epsilon;
    private final Random random = new Random();
    private final int threshold = 80;

    private int roll10d20() {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += random.nextInt(20) + 1;
        }
        return sum;
    }

    public double estimateProbabilityWithProgress() {
        int experiments = (int) (1 / epsilon);
        int success = 0;

        int progressSteps = 15;
        int step = experiments / progressSteps;
        if (step == 0) step = 1;

        for (int i = 1; i <= experiments; i++) {
            if (roll10d20() > threshold) success++;

            if (i % step == 0) {
                try {
                    Thread.sleep(3000);
                    double percent = 100.0 * i / experiments;
                    System.out.printf("Прогресс: %.1f%%\n", percent);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return (double) success / experiments;
    }

    public void tuneEpsilon() {
        double minTime = 1.0;
        double maxTime = 10.0;

        double probability = 0;
        double timeSec;

        while (true) {
            long start = System.currentTimeMillis();
            probability = estimateProbabilityWithProgress();
            long end = System.currentTimeMillis();

            timeSec = (end - start) / 1000.0;
            int iterations = (int) (1 / epsilon);

            System.out.printf("ε = %.1e (%d итераций) → Время = %.2f с%n",
                    epsilon, iterations, timeSec);

            if (timeSec >= minTime && timeSec <= maxTime) {
                System.out.println("\nПодходящее значение найдено!");
                System.out.printf("Результат: ε = %.1e → P(sum>80)=%.5f | Время = %.2f с%n",
                        epsilon, probability, timeSec);
                break;
            }

            if (timeSec < minTime) {
                epsilon /= 2;
            } else if (timeSec > maxTime) {
                epsilon *= 2;
            }

            if (epsilon < 1e-9 || epsilon > 1e-1) {
                System.out.println("\n Не удалось подобрать подходящую погрешность в разумных пределах.");
                break;
            }
        }
    }
}
