package org.example.lab9;

import org.example.lab9.queuingsystem.Client;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тесты производительности СМО")
class PerformanceTest {

    @Test
    @DisplayName("Сравнение производительности всех реализаций")
    void comparePerformance() throws InterruptedException {
        int numberOfOrders = 1000;
        int numberOfClients = 50;
        int numberOfThreads = 20;

        System.out.println("\n=== Тест производительности ===");
        System.out.println("Количество заказов: " + numberOfOrders);
        System.out.println("Количество клиентов: " + numberOfClients);
        System.out.println("Количество потоков: " + numberOfThreads + "\n");

        long syncTime = measureSyncPerformance(numberOfOrders, numberOfClients, numberOfThreads);
        long queueTime = measureQueuePerformance(numberOfOrders, numberOfClients, numberOfThreads);
        long disruptorTime = measureDisruptorPerformance(numberOfOrders, numberOfClients, numberOfThreads);

        double syncThroughput = (double) numberOfOrders / (syncTime / 1000.0);
        double queueThroughput = (double) numberOfOrders / (queueTime / 1000.0);
        double disruptorThroughput = (double) numberOfOrders / (disruptorTime / 1000.0);

        System.out.println("\n=== Результаты ===");
        System.out.println("Синхронная реализация:");
        System.out.println("  Время выполнения: " + syncTime + " мс");
        System.out.println("  Пропускная способность: " + String.format("%.2f", syncThroughput) + " заказов/сек");

        System.out.println("\nРеализация с очередями (BlockingQueue):");
        System.out.println("  Время выполнения: " + queueTime + " мс");
        System.out.println("  Пропускная способность: " + String.format("%.2f", queueThroughput) + " заказов/сек");

        System.out.println("\nРеализация с Disruptor:");
        System.out.println("  Время выполнения: " + disruptorTime + " мс");
        System.out.println("  Пропускная способность: " + String.format("%.2f", disruptorThroughput) + " заказов/сек");

        double queueImprovement = ((double) (syncTime - queueTime) / syncTime) * 100;
        double disruptorImprovement = ((double) (syncTime - disruptorTime) / syncTime) * 100;
        
        System.out.println("\nУлучшение BlockingQueue: " + String.format("%.2f", queueImprovement) + "%");
        System.out.println("Улучшение Disruptor: " + String.format("%.2f", disruptorImprovement) + "%");

        assertTrue(queueTime > 0, "Время выполнения должно быть положительным");
        assertTrue(syncTime > 0, "Время выполнения должно быть положительным");
        assertTrue(disruptorTime > 0, "Время выполнения должно быть положительным");
    }

    private long measureSyncPerformance(int numberOfOrders, int numberOfClients, int numberOfThreads) 
            throws InterruptedException {
        OnlineStoreImpl store = new OnlineStoreImpl();
        
        Item item1 = createItem("Товар 1", new BigDecimal("10.00"));
        Item item2 = createItem("Товар 2", new BigDecimal("20.00"));

        Map<Item, Integer> delivery = new HashMap<>();
        delivery.put(item1, numberOfOrders * 2);
        delivery.put(item2, numberOfOrders * 2);
        store.deliverGoods(delivery);

        List<UUID> clientIds = new ArrayList<>();
        for (int i = 0; i < numberOfClients; i++) {
            UUID clientId = store.registerClient("Клиент " + i, "client_" + i);
            clientIds.add(clientId);
            Client client = store.getClient(clientId);
            PersonalListener listener = (PersonalListener) client.getClientListener();
            listener.setInitialBalance(new BigDecimal("100000.00"));
        }

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfOrders);
        AtomicInteger successCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfOrders; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    UUID clientId = clientIds.get(index % clientIds.size());
                    Map<Item, Integer> orderItems = new HashMap<>();
                    orderItems.put(item1, 1);
                    orderItems.put(item2, 1);
                    Order order = store.createOrder(clientId, orderItems);
                    if (order != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        store.shutdown();

        System.out.println("Синхронная: успешно обработано " + successCount.get() + " из " + numberOfOrders);
        return duration;
    }

    private long measureQueuePerformance(int numberOfOrders, int numberOfClients, int numberOfThreads) 
            throws InterruptedException {
        OnlineStoreQueueImpl store = new OnlineStoreQueueImpl();
        
        Item item1 = createItem("Товар 1", new BigDecimal("10.00"));
        Item item2 = createItem("Товар 2", new BigDecimal("20.00"));

        Map<Item, Integer> delivery = new HashMap<>();
        delivery.put(item1, numberOfOrders * 2);
        delivery.put(item2, numberOfOrders * 2);
        store.deliverGoods(delivery);

        List<UUID> clientIds = new ArrayList<>();
        for (int i = 0; i < numberOfClients; i++) {
            UUID clientId = store.registerClient("Клиент " + i, "client_" + i);
            clientIds.add(clientId);
            Client client = store.getClient(clientId);
            PersonalListener listener = (PersonalListener) client.getClientListener();
            listener.setInitialBalance(new BigDecimal("100000.00"));
        }

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfOrders);
        AtomicInteger successCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfOrders; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    UUID clientId = clientIds.get(index % clientIds.size());
                    Map<Item, Integer> orderItems = new HashMap<>();
                    orderItems.put(item1, 1);
                    orderItems.put(item2, 1);
                    Order order = store.createOrder(clientId, orderItems);
                    if (order != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        store.shutdown();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("С очередями: успешно обработано " + successCount.get() + " из " + numberOfOrders);
        return duration;
    }

    private long measureDisruptorPerformance(int numberOfOrders, int numberOfClients, int numberOfThreads) 
            throws InterruptedException {
        OnlineStoreDisruptorImpl store = new OnlineStoreDisruptorImpl();
        
        Item item1 = createItem("Товар 1", new BigDecimal("10.00"));
        Item item2 = createItem("Товар 2", new BigDecimal("20.00"));

        Map<Item, Integer> delivery = new HashMap<>();
        delivery.put(item1, numberOfOrders * 2);
        delivery.put(item2, numberOfOrders * 2);
        store.deliverGoods(delivery);

        List<UUID> clientIds = new ArrayList<>();
        for (int i = 0; i < numberOfClients; i++) {
            UUID clientId = store.registerClient("Клиент " + i, "client_" + i);
            clientIds.add(clientId);
            Client client = store.getClient(clientId);
            PersonalListener listener = (PersonalListener) client.getClientListener();
            listener.setInitialBalance(new BigDecimal("100000.00"));
        }

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfOrders);
        AtomicInteger successCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfOrders; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    UUID clientId = clientIds.get(index % clientIds.size());
                    Map<Item, Integer> orderItems = new HashMap<>();
                    orderItems.put(item1, 1);
                    orderItems.put(item2, 1);
                    Order order = store.createOrder(clientId, orderItems);
                    if (order != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        store.shutdown();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Disruptor: успешно обработано " + successCount.get() + " из " + numberOfOrders);
        return duration;
    }

    private Item createItem(String name, BigDecimal price) {
        Item item = new Item();
        item.setName(name);
        item.setPrice(price);
        item.setItemId(UUID.randomUUID());
        return item;
    }
}

