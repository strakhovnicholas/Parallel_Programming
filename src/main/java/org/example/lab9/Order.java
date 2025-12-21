package org.example.lab9;

import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class Order {
    private UUID orderId = UUID.randomUUID();
    private UUID clientId;
    private Map<Item, Integer> requestedItems;
    private Map<Item, Integer> fulfilledItems;
    private Map<Item, Integer> pendingItems;
    private BigDecimal totalPrice = BigDecimal.ZERO;
    private BigDecimal blockedAmount = BigDecimal.ZERO;
    private OrderStatus status;

    public Order(UUID clientId, Map<Item, Integer> requestedItems) {
        this.clientId = clientId;
        this.requestedItems = new HashMap<>(requestedItems);
        this.fulfilledItems = new HashMap<>();
        this.pendingItems = new HashMap<>();
        this.status = OrderStatus.PENDING;
    }

    public enum OrderStatus {
        PENDING,
        PARTIAL,
        COMPLETED,
        CANCELLED
    }
}
