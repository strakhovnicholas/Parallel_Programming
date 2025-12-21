package org.example.lab9.disruptor;

import com.lmax.disruptor.EventHandler;
import org.example.lab9.Item;
import org.example.lab9.Order;
import org.example.lab9.PersonalListener;
import org.example.lab9.queuingsystem.Client;
import org.example.lab9.queuingsystem.StoreListener;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StoreEventHandler implements EventHandler<StoreEvent> {
    private final Map<Item, Integer> inventory = new HashMap<>();
    private final Map<UUID, Order> orders = new HashMap<>();
    private final Map<UUID, Client> clients = new HashMap<>();
    private BigDecimal storeMoney = BigDecimal.ZERO;
    private volatile boolean shutdownRequested = false;

    @Override
    public void onEvent(StoreEvent event, long sequence, boolean endOfBatch) throws Exception {
        switch (event.getType()) {
            case REGISTER_CLIENT:
                handleRegisterClient(event);
                break;
            case CREATE_ORDER:
                handleCreateOrder(event);
                break;
            case CANCEL_ORDER:
                handleCancelOrder(event);
                break;
            case GET_STORE_STATUS:
                handleGetStoreStatus(event);
                break;
            case DELIVER_GOODS:
                handleDeliverGoods(event);
                break;
        }
    }

    private void handleRegisterClient(StoreEvent event) {
        Client client = new Client(event.getName(), event.getUserName());
        clients.put(client.getUserId(), client);
        event.setResultClientId(client.getUserId());
        if (event.getRegisterClientFuture() != null) {
            event.getRegisterClientFuture().complete(client.getUserId());
        }
    }

    private void handleCreateOrder(StoreEvent event) {
        Client client = clients.get(event.getClientId());
        if (client == null) {
            event.setResultOrder(null);
            if (event.getCreateOrderFuture() != null) {
                event.getCreateOrderFuture().complete(null);
            }
            return;
        }

        Order order = new Order(event.getClientId(), event.getItems());
        Map<Item, Integer> itemsInStock = new HashMap<>();
        Map<Item, Integer> itemsNotInStock = new HashMap<>();
        BigDecimal purchasedCost = BigDecimal.ZERO;
        BigDecimal blockedCost = BigDecimal.ZERO;

        for (Map.Entry<Item, Integer> entry : event.getItems().entrySet()) {
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

        event.setResultOrder(order);
        if (event.getCreateOrderFuture() != null) {
            event.getCreateOrderFuture().complete(order);
        }
    }

    private void handleCancelOrder(StoreEvent event) {
        Order order = orders.get(event.getOrderId());
        if (order == null || !order.getClientId().equals(event.getClientId())) {
            event.setResultBoolean(false);
            if (event.getCancelOrderFuture() != null) {
                event.getCancelOrderFuture().complete(false);
            }
            return;
        }
        if (order.getStatus() == Order.OrderStatus.CANCELLED || 
            order.getStatus() == Order.OrderStatus.COMPLETED) {
            event.setResultBoolean(false);
            if (event.getCancelOrderFuture() != null) {
                event.getCancelOrderFuture().complete(false);
            }
            return;
        }
        order.setStatus(Order.OrderStatus.CANCELLED);

        Client client = clients.get(event.getClientId());
        if (client != null && client.getClientListener() != null) {
            client.getClientListener().onOrderCancelled(event.getOrderId(), "Order cancelled by client");
        }

        event.setResultBoolean(true);
        if (event.getCancelOrderFuture() != null) {
            event.getCancelOrderFuture().complete(true);
        }
    }

    private void handleGetStoreStatus(StoreEvent event) {
        event.setResultStatus(new HashMap<>(inventory));
        if (event.getGetStatusFuture() != null) {
            event.getGetStatusFuture().complete(new HashMap<>(inventory));
        }
    }

    private void handleDeliverGoods(StoreEvent event) {
        for (Map.Entry<Item, Integer> entry : event.getItems().entrySet()) {
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

        if (event.getDeliverGoodsFuture() != null) {
            event.getDeliverGoodsFuture().complete(null);
        }
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
        shutdownRequested = true;
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

