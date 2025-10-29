package org.example.lab4;

import lombok.Builder;

import java.util.Random;

@Builder
public class MonteCarloAdjust{
    private double epsilon;
    private final Random random = new Random();
    private final int threshold = 80;
    private final int threadCount = 4;


    public void estimateProbabilityWithProgress() throws InterruptedException {
        int totalExperiments = (int) (1 / epsilon);
        int experimentsPerThread = totalExperiments / threadCount;

        ProgressMonitor monitor = new ProgressMonitor(totalExperiments);
        LazyResultStorage storage = LazyResultStorage.getInstance();

        Thread[] workers = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            // последний поток берет остаток, если делится не ровно
            int count = (i == threadCount - 1)
                    ? totalExperiments - (experimentsPerThread * i)
                    : experimentsPerThread;

            MonteCarloWorker worker = new MonteCarloWorker(count, threshold, monitor, storage);
            workers[i] = new Thread(worker);
            workers[i].start();
        }

        for (Thread t : workers) t.join();

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
