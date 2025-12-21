package org.example.lab9.queuingsystem;

import org.example.lab9.Item;
import org.example.lab9.Order;
import org.example.lab9.PersonalListener;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

public class StoreQueueProcessor implements Runnable {
    private final BlockingQueue<StoreOperation> operationQueue;
    private final Map<Item, Integer> inventory = new HashMap<>();
    private final Map<UUID, Order> orders = new HashMap<>();
    private final Map<UUID, Client> clients = new HashMap<>();
    private BigDecimal storeMoney = BigDecimal.ZERO;
    private volatile boolean running = true;

    public StoreQueueProcessor(BlockingQueue<StoreOperation> operationQueue) {
        this.operationQueue = operationQueue;
    }

    public void shutdown() {
        running = false;
        Thread.currentThread().interrupt();
    }

    @Override
    public void run() {
        while (running || !operationQueue.isEmpty()) {
            try {
                StoreOperation operation = operationQueue.take();
                processOperation(operation);
            } catch (InterruptedException e) {
                if (!running) {
                    break;
                }
                Thread.currentThread().interrupt();
            }
        }
    }

    private void processOperation(StoreOperation operation) {
        if (operation instanceof StoreOperation.RegisterClientOperation) {
            processRegisterClient((StoreOperation.RegisterClientOperation) operation);
        } else if (operation instanceof StoreOperation.CreateOrderOperation) {
            processCreateOrder((StoreOperation.CreateOrderOperation) operation);
        } else if (operation instanceof StoreOperation.CancelOrderOperation) {
            processCancelOrder((StoreOperation.CancelOrderOperation) operation);
        } else if (operation instanceof StoreOperation.GetStoreStatusOperation) {
            processGetStoreStatus((StoreOperation.GetStoreStatusOperation) operation);
        } else if (operation instanceof StoreOperation.DeliverGoodsOperation) {
            processDeliverGoods((StoreOperation.DeliverGoodsOperation) operation);
        }
    }

    private void processRegisterClient(StoreOperation.RegisterClientOperation operation) {
        Client client = new Client(operation.getName(), operation.getUserName());
        clients.put(client.getUserId(), client);
        operation.setClientId(client.getUserId());
        @SuppressWarnings("unchecked")
        CompletableFuture<UUID> future = (CompletableFuture<UUID>) operation.getFuture();
        future.complete(client.getUserId());
    }

    private void processCreateOrder(StoreOperation.CreateOrderOperation operation) {
        Client client = clients.get(operation.getClientId());
        if (client == null) {
            @SuppressWarnings("unchecked")
            CompletableFuture<Order> future = (CompletableFuture<Order>) operation.getFuture();
            future.complete(null);
            return;
        }

        Order order = new Order(operation.getClientId(), operation.getItems());
        Map<Item, Integer> itemsInStock = new HashMap<>();
        Map<Item, Integer> itemsNotInStock = new HashMap<>();
        BigDecimal purchasedCost = BigDecimal.ZERO;
        BigDecimal blockedCost = BigDecimal.ZERO;

        for (Map.Entry<Item, Integer> entry : operation.getItems().entrySet()) {
            Item item = entry.getKey();
            int requestedCount = entry.getValue();
            int availableCount = inventory.getOrDefault(item, 0);

            if (availableCount >= requestedCount) {
                itemsInStock.put(item, requestedCount);
                purchasedCost = purchasedCost.add(item.getPrice().multiply(BigDecimal.valueOf(requestedCount)));
                inventory.put(item, availableCount - requestedCount);
            } else if (availableCount > 0) {
                itemsInStock.put(item, availableCount);
                purchasedCost = purchasedCost.add(item.getPrice().multiply(BigDecimal.valueOf(availableCount)));
                itemsNotInStock.put(item, requestedCount - availableCount);
                blockedCost = blockedCost.add(item.getPrice().multiply(BigDecimal.valueOf(requestedCount - availableCount)));
                inventory.put(item, 0);
            } else {
                itemsNotInStock.put(item, requestedCount);
                blockedCost = blockedCost.add(item.getPrice().multiply(BigDecimal.valueOf(requestedCount)));
            }
        }

        order.setFulfilledItems(itemsInStock);
        order.setPendingItems(itemsNotInStock);
        order.setTotalPrice(purchasedCost);
        order.setBlockedAmount(blockedCost);

        if (itemsInStock.isEmpty() && !itemsNotInStock.isEmpty()) {
            order.setStatus(Order.OrderStatus.PENDING);
        } else if (!itemsInStock.isEmpty() && !itemsNotInStock.isEmpty()) {
            order.setStatus(Order.OrderStatus.PARTIAL);
        } else if (!itemsInStock.isEmpty() && itemsNotInStock.isEmpty()) {
            order.setStatus(Order.OrderStatus.COMPLETED);
        }

        orders.put(order.getOrderId(), order);
        storeMoney = storeMoney.add(purchasedCost);

        StoreListener listener = client.getClientListener();
        if (listener != null) {
            if (!itemsInStock.isEmpty()) {
                listener.onOrderCompleted(order.getOrderId(), itemsInStock, purchasedCost, itemsNotInStock);
            }
            if (!itemsNotInStock.isEmpty() && order.getStatus() == Order.OrderStatus.PARTIAL) {
            } else if (!itemsNotInStock.isEmpty()) {
                listener.onOrderCompleted(order.getOrderId(), new HashMap<>(), BigDecimal.ZERO, itemsNotInStock);
            }
        }

        @SuppressWarnings("unchecked")
        CompletableFuture<Order> future = (CompletableFuture<Order>) operation.getFuture();
        future.complete(order);
    }

    private void processCancelOrder(StoreOperation.CancelOrderOperation operation) {
        Order order = orders.get(operation.getOrderId());
        if (order == null || !order.getClientId().equals(operation.getClientId())) {
            @SuppressWarnings("unchecked")
            CompletableFuture<Boolean> future = (CompletableFuture<Boolean>) operation.getFuture();
            future.complete(false);
            return;
        }
        if (order.getStatus() == Order.OrderStatus.CANCELLED || 
            order.getStatus() == Order.OrderStatus.COMPLETED) {
            @SuppressWarnings("unchecked")
            CompletableFuture<Boolean> future = (CompletableFuture<Boolean>) operation.getFuture();
            future.complete(false);
            return;
        }
        order.setStatus(Order.OrderStatus.CANCELLED);

        Client client = clients.get(operation.getClientId());
        if (client != null && client.getClientListener() != null) {
            client.getClientListener().onOrderCancelled(operation.getOrderId(), "Order cancelled by client");
        }

        @SuppressWarnings("unchecked")
        CompletableFuture<Boolean> future = (CompletableFuture<Boolean>) operation.getFuture();
        future.complete(true);
    }

    private void processGetStoreStatus(StoreOperation.GetStoreStatusOperation operation) {
        @SuppressWarnings("unchecked")
        CompletableFuture<Map<Item, Integer>> future = (CompletableFuture<Map<Item, Integer>>) operation.getFuture();
        future.complete(new HashMap<>(inventory));
    }

    private void processDeliverGoods(StoreOperation.DeliverGoodsOperation operation) {
        for (Map.Entry<Item, Integer> entry : operation.getItems().entrySet()) {
            Item item = entry.getKey();
            int quantity = entry.getValue();
            inventory.put(item, inventory.getOrDefault(item, 0) + quantity);
        }

        for (Order order : orders.values()) {
            if (order.getStatus() == Order.OrderStatus.PENDING || 
                order.getStatus() == Order.OrderStatus.PARTIAL) {
                processPendingOrder(order);
            }
        }

        @SuppressWarnings("unchecked")
        CompletableFuture<Void> future = (CompletableFuture<Void>) operation.getFuture();
        future.complete(null);
    }

    private void processPendingOrder(Order order) {
        Map<Item, Integer> newlyFulfilled = new HashMap<>();
        BigDecimal additionalCost = BigDecimal.ZERO;

        Map<Item, Integer> pendingItems = new HashMap<>(order.getPendingItems());
        
        for (Map.Entry<Item, Integer> entry : pendingItems.entrySet()) {
            Item item = entry.getKey();
            int requestedCount = entry.getValue();
            int availableCount = inventory.getOrDefault(item, 0);

            if (availableCount >= requestedCount) {
                newlyFulfilled.put(item, requestedCount);
                additionalCost = additionalCost.add(item.getPrice().multiply(BigDecimal.valueOf(requestedCount)));
                inventory.put(item, availableCount - requestedCount);
                order.getPendingItems().remove(item);
            } else if (availableCount > 0) {
                newlyFulfilled.put(item, availableCount);
                additionalCost = additionalCost.add(item.getPrice().multiply(BigDecimal.valueOf(availableCount)));
                order.getPendingItems().put(item, requestedCount - availableCount);
                inventory.put(item, 0);
            }
        }

        if (!newlyFulfilled.isEmpty()) {
            for (Map.Entry<Item, Integer> entry : newlyFulfilled.entrySet()) {
                order.getFulfilledItems().merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
            order.setTotalPrice(order.getTotalPrice().add(additionalCost));
            order.setBlockedAmount(order.getBlockedAmount().subtract(additionalCost));

            if (order.getPendingItems().isEmpty()) {
                order.setStatus(Order.OrderStatus.COMPLETED);
            } else {
                order.setStatus(Order.OrderStatus.PARTIAL);
            }

            storeMoney = storeMoney.add(additionalCost);

            Client client = clients.get(order.getClientId());
            if (client != null && client.getClientListener() != null) {
                client.getClientListener().onOrderFulfilled(order.getOrderId(), newlyFulfilled, additionalCost);
                
                if (order.getStatus() == Order.OrderStatus.COMPLETED) {
                    client.getClientListener().onOrderCompleted(
                        order.getOrderId(), 
                        order.getFulfilledItems(), 
                        order.getTotalPrice(), 
                        new HashMap<>()
                    );
                }
            }
        }
    }

    public void shutdownStore() {
        for (Order order : orders.values()) {
            if (order.getStatus() != Order.OrderStatus.COMPLETED && 
                order.getStatus() != Order.OrderStatus.CANCELLED &&
                order.getBlockedAmount().compareTo(BigDecimal.ZERO) > 0) {
                
                Client client = clients.get(order.getClientId());
                if (client != null && client.getClientListener() != null) {
                    client.getClientListener().onOrderCancelled(order.getOrderId(), "Store shutdown - funds unblocked");
                }
                
                order.setStatus(Order.OrderStatus.CANCELLED);
                order.setBlockedAmount(BigDecimal.ZERO);
            }
        }
    }

    public BigDecimal getStoreMoney() {
        return storeMoney;
    }

    public Map<UUID, Order> getAllOrders() {
        return new HashMap<>(orders);
    }

    public Client getClient(UUID clientId) {
        return clients.get(clientId);
    }
}

