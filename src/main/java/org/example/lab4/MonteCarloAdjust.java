package org.example.lab4;


import java.util.Random;

public class MonteCarloAdjust{
    private double epsilon;
    private final Random random = new Random();
    private final int threshold = 80;
    private final int threadCount = 4;


    public MonteCarloAdjust(double epsilon){
        this.epsilon = epsilon;
    }

    private double estimateProbabilityWithProgress() throws InterruptedException {
        int totalExperiments = (int) (1 / epsilon);
        System.out.println(totalExperiments + " totalExperiments");
        int experimentsPerThread = totalExperiments / threadCount;

        System.out.println(experimentsPerThread + " experimentsPerThread");
        ProgressMonitor monitor = new ProgressMonitor();
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
            workers[i].join();
        }

//        for (Thread t : workers) t.join();

        return storage.getProbability();
    }

    public void tuneEpsilon() throws InterruptedException {
        double minTime = 1.0;
        double maxTime = 10.0;

        double probability = 0;
        double timeSec;

        probability = estimateProbabilityWithProgress();
        System.out.println("Вероятность: " + probability);
//        while (true) {
//            long start = System.currentTimeMillis();
//            probability = estimateProbabilityWithProgress();
//            long end = System.currentTimeMillis();
//
//            timeSec = (end - start) / 1000.0;
//            int iterations = (int) (1 / epsilon);
//
//            System.out.printf("ε = %.1e (%d итераций) → Время = %.2f с%n",
//                    epsilon, iterations, timeSec);
//
//            if (timeSec >= minTime && timeSec <= maxTime) {
//                System.out.println("\nПодходящее значение найдено!");
//                System.out.printf("Результат: ε = %.1e → P(sum>80)=%.5f | Время = %.2f с%n",
//                        epsilon, probability, timeSec);
//                break;
//            }
//
//            if (timeSec < minTime) {
//                epsilon /= 2;
//            } else if (timeSec > maxTime) {
//                epsilon *= 2;
//            }
//
//            if (epsilon < 1e-9 || epsilon > 1e-1) {
//                System.out.println("\n Не удалось подобрать подходящую погрешность в разумных пределах.");
//                break;
//            }
//        }
    }
}
