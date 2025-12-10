package org.example.lab9;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
public class Order {
    private UUID orderId = UUID.randomUUID();
    private UUID clientId;
    private Map<Item, Integer> requestedItems;
    private Map<Item, Integer> fulfilledItems;
    private Map<Item, Integer> pendingItems;
    private BigDecimal totalPrice;
    private OrderStatus status;

    public Order(UUID clientId){
        this.clientId = clientId;
        this.status = OrderStatus.PENDING;
    }

    private enum OrderStatus {
        PENDING,
        PARTIAL,
        COMPLETED,
        CANCELLED;
    }
}
