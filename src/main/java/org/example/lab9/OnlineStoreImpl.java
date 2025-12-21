package org.example.lab9;

import org.example.lab9.queuingsystem.Client;
import org.example.lab9.queuingsystem.OnlineStoreAPI;
import org.example.lab9.queuingsystem.StoreListener;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OnlineStoreImpl implements OnlineStoreAPI {
    private final Map<Item, Integer> inventory = new HashMap<>();
    private final Map<UUID, Order> orders = new HashMap<>();
    private final Map<UUID, Client> clients = new HashMap<>();
    private BigDecimal storeMoney = BigDecimal.ZERO;

    private final Lock inventoryLock = new ReentrantLock();
    private final Lock ordersLock = new ReentrantLock();
    private final Lock clientsLock = new ReentrantLock();

    @Override
    public UUID registerClient(String name, String userName) {
        this.clientsLock.lock();
        try {
            Client client = new Client(name, userName);
            this.clients.put(client.getUserId(), client);
            return client.getUserId();
        } finally {
            this.clientsLock.unlock();
        }
    }

    @Override
    public Order createOrder(UUID clientId, Map<Item, Integer> selectedItemsForOrdering) {
        if (selectedItemsForOrdering == null || selectedItemsForOrdering.isEmpty()) {
            return null;
        }

        this.clientsLock.lock();
        Client client;
        try {
            client = this.clients.get(clientId);
            if (client == null) {
                return null;
            }
        } finally {
            this.clientsLock.unlock();
        }

        Order order = new Order(clientId, selectedItemsForOrdering);
        Map<Item, Integer> itemsInStock = new HashMap<>();
        Map<Item, Integer> itemsNotInStock = new HashMap<>();
        BigDecimal purchasedCost = BigDecimal.ZERO;
        BigDecimal blockedCost = BigDecimal.ZERO;

        this.inventoryLock.lock();
        try {
            for (Map.Entry<Item, Integer> entry : selectedItemsForOrdering.entrySet()) {
                Item item = entry.getKey();
                int requestedCount = entry.getValue();
                int availableCount = this.inventory.getOrDefault(item, 0);

                if (availableCount >= requestedCount) {
                    itemsInStock.put(item, requestedCount);
                    purchasedCost = purchasedCost.add(item.getPrice().multiply(BigDecimal.valueOf(requestedCount)));
                    this.inventory.put(item, availableCount - requestedCount);
                } else if (availableCount > 0) {
                    itemsInStock.put(item, availableCount);
                    purchasedCost = purchasedCost.add(item.getPrice().multiply(BigDecimal.valueOf(availableCount)));
                    itemsNotInStock.put(item, requestedCount - availableCount);
                    blockedCost = blockedCost.add(item.getPrice().multiply(BigDecimal.valueOf(requestedCount - availableCount)));
                    this.inventory.put(item, 0);
                } else {
                    itemsNotInStock.put(item, requestedCount);
                    blockedCost = blockedCost.add(item.getPrice().multiply(BigDecimal.valueOf(requestedCount)));
                }
            }
        } finally {
            this.inventoryLock.unlock();
        }

        this.ordersLock.lock();
        try {
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

            this.orders.put(order.getOrderId(), order);
        } finally {
            this.ordersLock.unlock();
        }

        this.inventoryLock.lock();
        try {
            this.storeMoney = this.storeMoney.add(purchasedCost);
        } finally {
            this.inventoryLock.unlock();
        }

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

        return order;
    }

    @Override
    public boolean cancelOrder(UUID clientId, UUID orderId) {
        this.ordersLock.lock();
        Order order;
        try {
            order = this.orders.get(orderId);
            if (order == null || !order.getClientId().equals(clientId)) {
                return false;
            }
            if (order.getStatus() == Order.OrderStatus.CANCELLED || 
                order.getStatus() == Order.OrderStatus.COMPLETED) {
                return false;
            }
            order.setStatus(Order.OrderStatus.CANCELLED);
        } finally {
            this.ordersLock.unlock();
        }

        this.clientsLock.lock();
        Client client;
        try {
            client = this.clients.get(clientId);
        } finally {
            this.clientsLock.unlock();
        }

        if (client != null && client.getClientListener() != null) {
            client.getClientListener().onOrderCancelled(orderId, "Order cancelled by client");
        }

        return true;
    }

    @Override
    public Map<Item, Integer> getStoreStatus() {
        this.inventoryLock.lock();
        try {
            return new HashMap<>(this.inventory);
        } finally {
            this.inventoryLock.unlock();
        }
    }

    @Override
    public void deliverGoods(Map<Item, Integer> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        this.inventoryLock.lock();
        try {
            for (Map.Entry<Item, Integer> entry : items.entrySet()) {
                Item item = entry.getKey();
                int quantity = entry.getValue();
                this.inventory.put(item, this.inventory.getOrDefault(item, 0) + quantity);
            }
        } finally {
            this.inventoryLock.unlock();
        }

        this.ordersLock.lock();
        try {
            for (Order order : this.orders.values()) {
                if (order.getStatus() == Order.OrderStatus.PENDING || 
                    order.getStatus() == Order.OrderStatus.PARTIAL) {
                    processPendingOrder(order);
                }
            }
        } finally {
            this.ordersLock.unlock();
        }
    }

    private void processPendingOrder(Order order) {
        Map<Item, Integer> newlyFulfilled = new HashMap<>();
        BigDecimal additionalCost = BigDecimal.ZERO;

        this.inventoryLock.lock();
        try {
            Map<Item, Integer> pendingItems = new HashMap<>(order.getPendingItems());
            
            for (Map.Entry<Item, Integer> entry : pendingItems.entrySet()) {
                Item item = entry.getKey();
                int requestedCount = entry.getValue();
                int availableCount = this.inventory.getOrDefault(item, 0);

                if (availableCount >= requestedCount) {
                    newlyFulfilled.put(item, requestedCount);
                    additionalCost = additionalCost.add(item.getPrice().multiply(BigDecimal.valueOf(requestedCount)));
                    this.inventory.put(item, availableCount - requestedCount);
                    order.getPendingItems().remove(item);
                } else if (availableCount > 0) {
                    newlyFulfilled.put(item, availableCount);
                    additionalCost = additionalCost.add(item.getPrice().multiply(BigDecimal.valueOf(availableCount)));
                    order.getPendingItems().put(item, requestedCount - availableCount);
                    this.inventory.put(item, 0);
                }
            }
        } finally {
            this.inventoryLock.unlock();
        }

        if (!newlyFulfilled.isEmpty()) {
            this.ordersLock.lock();
            try {
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
            } finally {
                this.ordersLock.unlock();
            }

            this.inventoryLock.lock();
            try {
                this.storeMoney = this.storeMoney.add(additionalCost);
            } finally {
                this.inventoryLock.unlock();
            }

            this.clientsLock.lock();
            Client client;
            try {
                client = this.clients.get(order.getClientId());
            } finally {
                this.clientsLock.unlock();
            }

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

    public void shutdown() {
        this.ordersLock.lock();
        try {
            for (Order order : this.orders.values()) {
                if (order.getStatus() != Order.OrderStatus.COMPLETED && 
                    order.getStatus() != Order.OrderStatus.CANCELLED &&
                    order.getBlockedAmount().compareTo(BigDecimal.ZERO) > 0) {
                    
                    this.clientsLock.lock();
                    Client client;
                    try {
                        client = this.clients.get(order.getClientId());
                    } finally {
                        this.clientsLock.unlock();
                    }

                    if (client != null && client.getClientListener() != null) {
                        client.getClientListener().onOrderCancelled(order.getOrderId(), "Store shutdown - funds unblocked");
                    }
                    
                    order.setStatus(Order.OrderStatus.CANCELLED);
                    order.setBlockedAmount(BigDecimal.ZERO);
                }
            }
        } finally {
            this.ordersLock.unlock();
        }
    }

    public BigDecimal getStoreMoney() {
        this.inventoryLock.lock();
        try {
            return this.storeMoney;
        } finally {
            this.inventoryLock.unlock();
        }
    }

    public Map<UUID, Order> getAllOrders() {
        this.ordersLock.lock();
        try {
            return new HashMap<>(this.orders);
        } finally {
            this.ordersLock.unlock();
        }
    }

    public Client getClient(UUID clientId) {
        this.clientsLock.lock();
        try {
            return this.clients.get(clientId);
        } finally {
            this.clientsLock.unlock();
        }
    }
}
