package org.example.lab9;

import org.example.lab9.queuingsystem.Client;
import org.example.lab9.queuingsystem.OnlineStoreAPI;
import org.example.lab9.queuingsystem.StoreQueueAPI;
import org.example.lab9.queuingsystem.StoreQueueProcessor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class OnlineStoreQueueImpl implements OnlineStoreAPI {
    private final StoreQueueAPI queueAPI;
    private final StoreQueueProcessor processor;
    private final Thread processorThread;

    public OnlineStoreQueueImpl() {
        BlockingQueue<org.example.lab9.queuingsystem.StoreOperation> queue = 
            new ArrayBlockingQueue<>(10000);
        this.queueAPI = new StoreQueueAPI(queue);
        this.processor = new StoreQueueProcessor(queue);
        this.processorThread = new Thread(processor, "StoreQueueProcessor");
        this.processorThread.setDaemon(true);
        this.processorThread.start();
    }

    @Override
    public UUID registerClient(String name, String userName) {
        try {
            return queueAPI.registerClient(name, userName).get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register client", e);
        }
    }

    @Override
    public Order createOrder(UUID clientId, Map<Item, Integer> items) {
        try {
            return queueAPI.createOrder(clientId, items).get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create order", e);
        }
    }

    @Override
    public boolean cancelOrder(UUID clientId, UUID orderId) {
        try {
            return queueAPI.cancelOrder(clientId, orderId).get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to cancel order", e);
        }
    }

    @Override
    public Map<Item, Integer> getStoreStatus() {
        try {
            return queueAPI.getStoreStatus().get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get store status", e);
        }
    }

    @Override
    public void deliverGoods(Map<Item, Integer> items) {
        try {
            queueAPI.deliverGoods(items).get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deliver goods", e);
        }
    }

    public void shutdown() {
        processor.shutdownStore();
        processor.shutdown();
        try {
            processorThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public BigDecimal getStoreMoney() {
        return processor.getStoreMoney();
    }

    public Map<UUID, Order> getAllOrders() {
        return processor.getAllOrders();
    }

    public Client getClient(UUID clientId) {
        return processor.getClient(clientId);
    }
}

