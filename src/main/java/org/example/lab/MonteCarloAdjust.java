package org.example.lab;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class MonteCarloAdjust{
    private double epsilon;
    private final Random random = new Random();
    private final int threshold = 80;
    private final int threadCount = 4;
    private static AtomicInteger counter = new AtomicInteger(0);
    private final int id;

    public MonteCarloAdjust(double epsilon){
        this.epsilon = epsilon;
        this.id = counter.getAndIncrement();
    }

    public int getId() {
        return id;
    }

    private double estimateProbabilityWithProgressFuture() throws InterruptedException, ExecutionException {
        int totalExperiments = (int) (1 / this.epsilon);

        int batchSize = 300_000;
        int totalBatches = (totalExperiments + batchSize - 1) / batchSize;

        ProgressMonitor monitor = new ProgressMonitor(totalExperiments);
        LazyResultStorage storage = LazyResultStorage.getInstance();

        ExecutorService executor = Executors.newFixedThreadPool(this.threadCount);
        List<Future<ExperimentResult>> futures = new ArrayList<>();

        for (int i = 0; i < totalBatches; i++) {
            int currentBatchSize = Math.min(batchSize, totalExperiments - i * batchSize);

            MonteCarloWorker worker = new MonteCarloWorker(currentBatchSize, threshold, monitor);
            futures.add(executor.submit(worker));
        }

        for (Future<ExperimentResult> future : futures){
            ExperimentResult result = future.get();
            storage.addPartialResult(result.getSuccess(), result.getTotal());
        }

        executor.shutdown();
        return storage.getProbability();
    }

    public void tuneEpsilon() throws InterruptedException, ExecutionException {
        double minTime = 1.0;
        double maxTime = 10.0;

        double probability = 0;
        double timeSec;

        while (true) {
            long start = System.currentTimeMillis();
            probability = estimateProbabilityWithProgressFuture();
            long end = System.currentTimeMillis();

            timeSec = (end - start) / 1000.0;
            int iterations = (int) (1 / epsilon);

            System.out.printf("ε = %.1e (%d итераций) → Время = %.2f с%n",
                    this.epsilon, iterations, timeSec);

            if (timeSec >= minTime && timeSec <= maxTime) {
                System.out.println("\nПодходящее значение найдено!");
                System.out.printf("Результат: ε = %.1e → P(sum>80)=%.5f | Время = %.2f с%n",
                        this.epsilon, probability, timeSec);
                break;
            }

            if (timeSec < minTime) {
                this.epsilon /= 2;
            } else if (timeSec > maxTime) {
                this.epsilon *= 2;
            }

            if (this.epsilon < 1e-9 || this.epsilon > 1e-1) {
                System.out.println("\n Не удалось подобрать подходящую погрешность в разумных пределах.");
                break;
            }
        }
    }
}
