package org.example.lab;

public class LazyResultStorage {
    private static volatile LazyResultStorage instance;
    private double totalSuccess = 0;
    private double totalExperiments = 0;
    private static final Object monitor = new Object();

    private LazyResultStorage() {
        System.out.println("Создано ленивое хранилище результатов");
    }

    public static LazyResultStorage getInstance() {
        if (instance == null) {
            synchronized (monitor) {
                if (instance == null)
                    instance = new LazyResultStorage();
            }
        }
        return instance;
    }

    public synchronized void addPartialResult(int success, int total) {
        totalSuccess += success;
        totalExperiments += total;
    }

    public synchronized double getProbability() {
        return totalSuccess / totalExperiments;
    }
}
