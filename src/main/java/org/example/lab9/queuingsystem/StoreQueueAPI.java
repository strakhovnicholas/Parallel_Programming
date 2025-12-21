package org.example.lab9.queuingsystem;

import org.example.lab9.Item;
import org.example.lab9.Order;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

public class StoreQueueAPI {
    private final BlockingQueue<StoreOperation> operationQueue;

    public StoreQueueAPI(BlockingQueue<StoreOperation> operationQueue) {
        this.operationQueue = operationQueue;
    }

    public CompletableFuture<UUID> registerClient(String name, String userName) {
        CompletableFuture<UUID> future = new CompletableFuture<>();
        StoreOperation.RegisterClientOperation operation = 
            new StoreOperation.RegisterClientOperation(name, userName, future);
        try {
            operationQueue.put(operation);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletableFuture<Order> createOrder(UUID clientId, Map<Item, Integer> items) {
        if (items == null || items.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Order> future = new CompletableFuture<>();
        StoreOperation.CreateOrderOperation operation = 
            new StoreOperation.CreateOrderOperation(clientId, new HashMap<>(items), future);
        try {
            operationQueue.put(operation);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletableFuture<Boolean> cancelOrder(UUID clientId, UUID orderId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        StoreOperation.CancelOrderOperation operation = 
            new StoreOperation.CancelOrderOperation(clientId, orderId, future);
        try {
            operationQueue.put(operation);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletableFuture<Map<Item, Integer>> getStoreStatus() {
        CompletableFuture<Map<Item, Integer>> future = new CompletableFuture<>();
        StoreOperation.GetStoreStatusOperation operation = 
            new StoreOperation.GetStoreStatusOperation(future);
        try {
            operationQueue.put(operation);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletableFuture<Void> deliverGoods(Map<Item, Integer> items) {
        if (items == null || items.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        StoreOperation.DeliverGoodsOperation operation = 
            new StoreOperation.DeliverGoodsOperation(new HashMap<>(items), future);
        try {
            operationQueue.put(operation);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.completeExceptionally(e);
        }
        return future;
    }
}

