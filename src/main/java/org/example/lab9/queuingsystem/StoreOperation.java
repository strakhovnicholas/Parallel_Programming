package org.example.lab9.queuingsystem;

import org.example.lab9.Item;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class StoreOperation {
    protected final CompletableFuture<?> future;
    protected final UUID operationId;

    public StoreOperation(UUID operationId, CompletableFuture<?> future) {
        this.operationId = operationId;
        this.future = future;
    }

    public UUID getOperationId() {
        return operationId;
    }

    public CompletableFuture<?> getFuture() {
        return future;
    }

    public static class RegisterClientOperation extends StoreOperation {
        private final String name;
        private final String userName;
        private UUID clientId;

        public RegisterClientOperation(String name, String userName, CompletableFuture<UUID> future) {
            super(UUID.randomUUID(), future);
            this.name = name;
            this.userName = userName;
        }

        public String getName() {
            return name;
        }

        public String getUserName() {
            return userName;
        }

        public UUID getClientId() {
            return clientId;
        }

        public void setClientId(UUID clientId) {
            this.clientId = clientId;
        }
    }

    public static class CreateOrderOperation extends StoreOperation {
        private final UUID clientId;
        private final Map<Item, Integer> items;

        public CreateOrderOperation(UUID clientId, Map<Item, Integer> items, CompletableFuture<org.example.lab9.Order> future) {
            super(UUID.randomUUID(), future);
            this.clientId = clientId;
            this.items = items;
        }

        public UUID getClientId() {
            return clientId;
        }

        public Map<Item, Integer> getItems() {
            return items;
        }
    }

    public static class CancelOrderOperation extends StoreOperation {
        private final UUID clientId;
        private final UUID orderId;

        public CancelOrderOperation(UUID clientId, UUID orderId, CompletableFuture<Boolean> future) {
            super(UUID.randomUUID(), future);
            this.clientId = clientId;
            this.orderId = orderId;
        }

        public UUID getClientId() {
            return clientId;
        }

        public UUID getOrderId() {
            return orderId;
        }
    }

    public static class GetStoreStatusOperation extends StoreOperation {
        public GetStoreStatusOperation(CompletableFuture<Map<Item, Integer>> future) {
            super(UUID.randomUUID(), future);
        }
    }

    public static class DeliverGoodsOperation extends StoreOperation {
        private final Map<Item, Integer> items;

        public DeliverGoodsOperation(Map<Item, Integer> items, CompletableFuture<Void> future) {
            super(UUID.randomUUID(), future);
            this.items = items;
        }

        public Map<Item, Integer> getItems() {
            return items;
        }
    }
}

